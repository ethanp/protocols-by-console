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
public class BrdcstServer extends BaseServer<BrdcstConn, VectorClock> {

    public BrdcstServer(ServerSocket socket) { super(socket); }

    @Override protected BrdcstConn createConn(Socket socket, BaseServer server) {
        return BrdcstConn.startWithSocket(socket, (BrdcstServer) server);
    }

    @Override protected BrdcstConn createConnObj(Socket socket, int userPort) {
        return BrdcstConn.startWithSocket(socket, this, userPort);
    }

    @Override protected void deliverEverythingPossible() {
        Collection<VectorClock> toRem = new ArrayList<>();
        boolean didSomething;
        do {
            didSomething = false;
            for (VectorClock qVC : msgBacklog) {
                final int senderID = qVC.getSenderID();
                System.out.println("Trying to deliver: "+qVC);
                System.out.println("Current D = "+deliveredClock);
                if (deliveredClock.shouldDeliver(qVC, senderID)) {
                    final int msgNum = qVC.get(senderID);
                    toRem.add(qVC);
                    System.out.println("Delivered msg w VC "+qVC+" from ["+senderID+"]");
                    getDeliveredClock().put(senderID, msgNum);
                    didSomething = true;
                }
            }
            toRem.forEach(msgBacklog::remove);
        } while (didSomething);
        if (msgBacklog.isEmpty())
            System.out.println("All received messages have been delivered -- groovy");
    }

    /**
     * Increments element representing this process in the "delivered-clock",
     * And sends the clock to all known peers
     */
    public void broadcast() {
        /* create message to broadcast */
        String msg = "msg "+serializedIncrementedVectorClock();

        /* broadcast to everyone */
        for (Map.Entry<Integer, BrdcstConn> entry : connections.entrySet()) {
            System.out.println("broadcasting to "+entry.getKey());
            entry.getValue().println(msg);
        }
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
