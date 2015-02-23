package server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Ethan Petuchowski 2/20/15
 */
public class BrdcstServer implements Runnable {

    int myId() { return serverSocket.getLocalPort()-Common.LOW_PORT; }
    ServerSocket serverSocket;
    ConcurrentHashMap<Integer, Conn> connections = new ConcurrentHashMap<>(5,.9f,3);
    int msg_num = 0;
    NavigableMap<VectorClock, Integer> msgBacklog = new TreeMap<>();

    /**
     * Hash{ portNo : clockValue }
     */
    VectorClock myVectorClock = new VectorClock();

    /**
     * This is how we implement Causal Delivery. It is based on the description in ["Consistent
     * Global States", pg. 20]. We deliver message m from process p_j as soon as both of the
     * following conditions are satisfied
     *
     * D[j] = TS(m)[j]-1 D[k] ≥ TS(m)[k], forall k≠j
     */

    public BrdcstServer(int portOffset) {
        try {
            serverSocket = new ServerSocket(Common.LOW_PORT+portOffset);
            // initialize vector clock with me already inside it
        }
        catch (IOException e) { e.printStackTrace(); }
    }

    @Override public void run() {
        initVectorClock();
        System.out.println("Listening on port "+serverSocket.getLocalPort());
        while (true) {
            try {
                Socket socket = serverSocket.accept(); // can't do try-w-r bc it'll close() it
                Conn conn = Conn.startWithSocket(socket, this);
                int userPort = Integer.parseInt(Common.afterSpace(conn.readLine()));
                conn.setForeignID(userPort);
                connections.put(userPort, conn);
                System.out.println("Connected to "+userPort);
            }
            catch (IOException e) { e.printStackTrace(); }
        }
    }

    private void initVectorClock() {
        myVectorClock.setServer(this);
        myVectorClock.add(myId());
    }

    public void connectToServerAtPort(int userPort) {
        if (userPort == myId()) {
            System.out.println("surely I needn't connect to myself");
            return;
        }
        if (connections.containsKey(userPort)) {
            System.err.println("Already connected to "+userPort);
            return;
        }

        Socket socket;
        try {
            socket = new Socket(Common.LOCALHOST, userPort+Common.LOW_PORT);
        }
        catch (UnknownHostException e) {
            System.err.println("Couldn't connect to "+userPort+", Unknown Host Exception");
            System.err.println(e.getMessage());
            return;
        }
        catch (IOException e) {
            System.err.println("Couldn't connect to "+userPort+", I/O Exception");
            System.err.println(e.getMessage());
            return;
        }
        Conn conn = Conn.startWithSocket(socket, this);
        conn.println("id "+myId());
        conn.setForeignID(userPort);
        connections.put(userPort, conn);
        System.out.println("Connected to "+userPort);

    }

    public void broadcast() {
        String msg = "msg "+serializedIncrementedVectorClock();
        for (Map.Entry<Integer, Conn> entry : connections.entrySet()) {
            System.out.println("broadcasting to "+entry.getKey());
            entry.getValue().println(msg);
        }
    }

    /** increments THIS process's vector clock [only] */
    private String serializedIncrementedVectorClock() {
        myVectorClock.incr(myId());
        return myVectorClock.serialize();
    }

    public boolean setDelay(int peerNum, int delaySize) {
        if (!connections.containsKey(peerNum)) {
            System.err.println("Not connected to "+peerNum);
            return false;
        }
        if (delaySize < 0) {
            System.err.println("Delay size can't be less than zero");
            return false;
        }
        connections.get(peerNum).setDelay(delaySize);
        return true;
    }

    void deliverEverythingPossible() {
        Collection<VectorClock> toRem = new ArrayList<>();
        for (Map.Entry<VectorClock, Integer> entry : msgBacklog.entrySet()) {
            VectorClock qVC = entry.getKey();
            final int procID = entry.getValue();
            final int valueForProc = qVC.get(procID);
            if (getMyVectorClock().get(procID) == valueForProc-1) {
                toRem.add(qVC);
                System.out.println("Delivered msg num ["+valueForProc+"] from ["+procID+"]");
                getMyVectorClock().put(procID, valueForProc);
            }
        }
        toRem.forEach(msgBacklog::remove);
    }

    public VectorClock getMyVectorClock()            { return myVectorClock; }
    public void rcvMsg(VectorClock vc, int procID) {
        msgBacklog.put(vc, procID);
        deliverEverythingPossible();
    }
}
