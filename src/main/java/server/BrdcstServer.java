package server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
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

    /**
     * Hash{ portNo : clockValue }
     */
    NavigableMap<Integer, Integer> vectorClock = new TreeMap<>();

    /**
     * This is how we implement Causal Delivery It is based on the description in ["Consistent
     * Global States", pg. 20] We deliver message m from process p_j as soon as both of the
     * following conditions are satisfied
     *
     * D[j] = TS(m)[j]-1 D[k] ≥ TS(m)[k], forall k≠j
     */

    public BrdcstServer(int portOffset) {
        try {
            serverSocket = new ServerSocket(Common.LOW_PORT+portOffset);
            // initialize vector clock with me already inside it
            vectorClock.put(myId(), 0);
        }
        catch (IOException e) { e.printStackTrace(); }
    }

    @Override public void run() {
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

    public void connectToServerAtPort(int userPort) {
        if (userPort == myId()) {
            System.out.println("surely I needn't connect to myself");
            return;
        }
        if (connections.containsKey(userPort)) {
            System.err.println("Already connected to "+userPort);
            return;
        }
        try {
            Socket socket = new Socket(Common.LOCALHOST, userPort+Common.LOW_PORT);
            Conn conn = Conn.startWithSocket(socket, this);
            conn.println("id "+myId());
            conn.setForeignID(userPort);
            connections.put(userPort, conn);
            System.out.println("Connected to "+userPort);
        }
        catch (IOException e) {
            e.printStackTrace();
        }

    }

    public void broadcast() {
        String msg = "msg "+nextMsg();
        for (Map.Entry<Integer, Conn> entry : connections.entrySet()) {
            System.out.println("broadcasting to "+entry.getKey());
            entry.getValue().println(msg);
        }
    }

    private String nextMsg() {
        /* TODO this should be a vector clock not just a number
         * It should get serialized and sent through the socket */

         return ""+(++msg_num);
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
}
