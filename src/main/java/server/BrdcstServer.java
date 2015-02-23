package server;

import java.io.IOException;
import java.net.BindException;
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
    VectorClock myVC = new VectorClock();

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
        }
        catch (BindException e) {
            System.err.println("Address already in use");
            System.exit(1);
        }
        catch (IOException e) { e.printStackTrace(); System.exit(1); }
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
                getMyVC().put(userPort, 0);
                System.out.println("Connected to "+userPort);
            }
            catch (IOException e) { e.printStackTrace(); }
        }
    }

    private void initVectorClock() {
        myVC.setServer(this);
        myVC.add(myId());
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
        getMyVC().put(userPort, 0);
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
        myVC.incr(myId());
        return myVC.serialize();
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
            if (VectorClock.shouldDeliver(qVC, getMyVC(), procID)) {
                final int msgNum = qVC.get(procID);
                toRem.add(qVC);
                System.out.println("Delivered msg num ["+msgNum+"] from ["+procID+"]");
                getMyVC().put(procID, msgNum);
            }
        }
        toRem.forEach(msgBacklog::remove);
        if (msgBacklog.size() == 0) {
            System.out.println("All received messages have been delivered");
        }
    }

    public VectorClock getMyVC() { return myVC; }

    public void rcvMsg(VectorClock vc, int procID) {
        msgBacklog.put(vc, procID);
        deliverEverythingPossible();
    }

    public void removeConn(int procID) {
        connections.remove(procID);
        myVC.remove(procID);
    }
}
