package server.base;

import server.time.Timestamp;
import server.time.VectorClock;
import server.util.Common;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.PriorityQueue;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Ethan Petuchowski 2/23/15
 */
public abstract class BaseServer<Conn extends BaseConn, TSType extends Timestamp> implements Runnable {

    public BaseServer(ServerSocket serverSocket) {
        this.serverSocket = serverSocket;
        deliveredClock = new VectorClock(myId());
    }

    /* Fields */
    protected ServerSocket serverSocket;
    protected ConcurrentHashMap<Integer, Conn> connections = new ConcurrentHashMap<>(5,.9f,3);
    public int myId() { return serverSocket.getLocalPort()-Common.LOW_PORT; }
    protected final VectorClock deliveredClock;
    protected PriorityQueue<TSType> msgBacklog = new PriorityQueue<>();

    protected void baseAddConnection(int userPort, Conn conn) {

        /* add to collection */
        connections.put(userPort, conn);

        /* put myself in the array of "delivered message counts by processor" */
        getDeliveredClock().add(userPort);

        /* for Unicast this adds the peer to the matrix */
        optnlInitConnection(conn);

        /* Tell the user that it worked.
         *
         * When connecting to self this gets printed twice:
         *   - 1st for creating connection as client
         *   - 2nd for receiving connection as server.
         */
        System.out.println("Connected to "+userPort);
    }

    /* Must be Overridden */
    protected abstract void deliverEverythingPossible();
    protected abstract Conn createConn(Socket socket, BaseServer server);
    protected abstract Conn createConnObj(Socket socket, int userPort);

    /* CAN be Overridden */
    protected void optnlInitConnection(Conn conn) {/*nothing*/}

    /* Must be Extended */
        /* NONE */

    /* Utility Functions */
    public boolean isConnectedTo(int destination) { return connections.containsKey(destination); }

    protected void acceptConnections() {
        try {
            Socket socket = serverSocket.accept(); // can't do try-w-r bc it'll close() it
            Conn conn = createConn(socket, this);
            int userPort = Integer.parseInt(Common.afterSpace(conn.readLine()));
            conn.setForeignID(userPort);
            baseAddConnection(userPort, conn);
        }
        catch (IOException e) { e.printStackTrace(); }
    }

    @Override public void run() {
        initDeliveredClock();
        System.out.println("Listening on port "+serverSocket.getLocalPort());
        while (true) acceptConnections();
    }


    public boolean setDelay(int peerNum, int delaySize) {
        if (!isConnectedTo(peerNum)) {
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

    public void connectToServerAtPort(int userPort) {
        if (isConnectedTo(userPort)) {
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
        Conn conn = createConnObj(socket, userPort);
        /* send over my "real name" so they really know me */
        conn.println("id "+myId());
        baseAddConnection(userPort, conn);
    }


    public void removeConn(int procID) {
        connections.remove(procID);
        deliveredClock.remove(procID);
    }

    public VectorClock getDeliveredClock() { return deliveredClock; }

    public synchronized void rcvMsg(TSType timestamp) {
        System.out.println(msgBacklog.hashCode());
        msgBacklog.add(timestamp); // throws Exception if doesn't work, unlike offer()
        System.out.println("msgBacklog.size(): "+msgBacklog.size());
        deliverEverythingPossible();
    }

    private void initDeliveredClock() {
        getDeliveredClock().setServer(this);
        getDeliveredClock().add(myId());
    }
}
