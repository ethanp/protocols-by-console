package server.base;

import server.util.Common;
import server.util.Conn;
import server.util.VectorClock;

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
 * Ethan Petuchowski 2/23/15
 */
public class BaseServer implements Runnable {
    NavigableMap<VectorClock, Integer> msgBacklog = new TreeMap<>();
    /**
     * Hash{ portNo : clockValue }
     */
    VectorClock myVC = new VectorClock();

    public int myId() { return serverSocket.getLocalPort()-Common.LOW_PORT; }
    protected ServerSocket serverSocket;
    protected ConcurrentHashMap<Integer, Conn> connections = new ConcurrentHashMap<>(5,.9f,3);

    public BaseServer(ServerSocket serverSocket) {
        this.serverSocket = serverSocket;
    }

    @Override public void run() {}

    public boolean isConnectedTo(int destination) {
        return connections.containsKey(destination);
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
        if (userPort == myId()) {
            System.out.println("surely I needn't connect to myself");
            return;
        }
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
        Conn conn = Conn.startWithSocket(socket, this);
        conn.println("id "+myId());
        conn.setForeignID(userPort);
        connections.put(userPort, conn);
        getMyVC().put(userPort, 0);
        System.out.println("Connected to "+userPort);

    }

    public void removeConn(int procID) {
        connections.remove(procID);
        myVC.remove(procID);
    }

    void deliverEverythingPossible() {
        Collection<VectorClock> toRem = new ArrayList<>();
        for (Map.Entry<VectorClock, Integer> entry : msgBacklog.entrySet()) {
            VectorClock qVC = entry.getKey();
            final int procID = entry.getValue();
            if (myVC.shouldDeliver(qVC, procID)) {
                final int msgNum = qVC.get(procID);
                toRem.add(qVC);
                System.out.println("Delivered msg w VC "+qVC+" from ["+procID+"]");
                getMyVC().put(procID, msgNum);
            }
        }
        toRem.forEach(msgBacklog::remove);
        if (msgBacklog.size() == 0) {
            System.out.println("All received messages have been delivered -- groovy");
        }
    }

    public VectorClock getMyVC() { return myVC; }

    public void rcvMsg(VectorClock vc, int procID) {
        msgBacklog.put(vc, procID);
        deliverEverythingPossible();
    }
}
