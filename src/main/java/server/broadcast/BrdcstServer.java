package server.broadcast;

import server.base.BaseServer;
import server.util.Common;
import server.util.Conn;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;

/**
 * Ethan Petuchowski 2/20/15
 *
 * This is how we implement Causal Delivery. It is based on the description in ["Consistent
 * Global States", pg. 20]. We deliver message m from process p_j as soon as both of the
 * following conditions are satisfied
 *
 * myVC[j] = rcvdVC[j]-1
 *          &&
 * myVC[k] ≥ rcvdVC[k], forall k≠j
 */
public class BrdcstServer extends BaseServer {

    public BrdcstServer(ServerSocket socket) {
        super(socket);
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
        getMyVC().setServer(this);
        getMyVC().add(myId());
    }

    public void broadcast() {
        /* create message to broadcast */
        String msg = "msg "+serializedIncrementedVectorClock();

        /* broadcast to everyone else */
        for (Map.Entry<Integer, Conn> entry : connections.entrySet()) {
            System.out.println("broadcasting to "+entry.getKey());
            entry.getValue().println(msg);
        }

        /* also broadcast to self */
        System.out.println("Received msg w VC "+getMyVC()+" from ["+myId()+"]");
        rcvMsg(getMyVC(), myId());
    }

    /** increments THIS process's vector clock [only] */
    private String serializedIncrementedVectorClock() {
        getMyVC().incr(myId());
        return getMyVC().serialize();
    }

    public void send(int dest) {
        // TODO send
        throw new NotImplementedException();
    }
}
