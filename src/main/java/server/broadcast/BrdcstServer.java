package server.broadcast;

import server.base.BaseServer;
import server.time.VectorClock;

import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collection;
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
public class BrdcstServer extends BaseServer<BrdcstConn> {

    public BrdcstServer(ServerSocket socket) { super(socket); }

    @Override protected BrdcstConn createConn(Socket socket, BaseServer server) {
        return BrdcstConn.startWithSocket(socket, (BrdcstServer) server);
    }

    @Override protected void addConnection(Socket socket, int userPort) {
        BrdcstConn conn = BrdcstConn.startWithSocket(socket, this);
        conn.println("id "+myId());
        conn.setForeignID(userPort);
        connections.put(userPort, conn);
        getDeliveredClock().put(userPort, 0);
        System.out.println("Connected to "+userPort);
    }

    @Override protected void deliverEverythingPossible() {
        Collection<VectorClock> toRem = new ArrayList<>();
        for (Map.Entry<VectorClock, Integer> entry : msgBacklog.entrySet()) {
            VectorClock qVC = entry.getKey();
            final int procID = entry.getValue();
            if (deliveredClock.shouldDeliver(qVC, procID)) {
                final int msgNum = qVC.get(procID);
                toRem.add(qVC);
                System.out.println("Delivered msg w VC "+qVC+" from ["+procID+"]");
                getDeliveredClock().put(procID, msgNum);
            }
        }
        toRem.forEach(msgBacklog::remove);
        if (msgBacklog.size() == 0) {
            System.out.println("All received messages have been delivered -- groovy");
        }
    }

    /**
     * Increments element representing this process in the "delivered-clock",
     * And sends the clock to all known peers
     */
    public void broadcast() {
        /* create message to broadcast */
        String msg = "msg "+serializedIncrementedVectorClock();

        /* broadcast to everyone else */
        for (Map.Entry<Integer, BrdcstConn> entry : connections.entrySet()) {
            System.out.println("broadcasting to "+entry.getKey());
            entry.getValue().println(msg);
        }

        /* also broadcast to self */
        System.out.println("Received msg w VC "+getDeliveredClock()+" from ["+myId()+"]");
        rcvMsg(getDeliveredClock(), myId());
    }

    /**
     * Called by broadcast(),
     * Increments THIS process in the delivered-clock [only]
     */
    private String serializedIncrementedVectorClock() {
        getDeliveredClock().incr(myId());
        return getDeliveredClock().serialize();
    }
}