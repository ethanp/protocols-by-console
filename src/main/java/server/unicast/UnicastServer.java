package server.unicast;

import server.base.BaseServer;
import server.time.MatrixClock;

import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

/**
 * Ethan Petuchowski 2/23/15
 */
public class UnicastServer extends BaseServer<UnicastConn, MatrixClock> {
    public UnicastServer(ServerSocket serverSocket) {
        super(serverSocket);
    }

    private MatrixClock myMtx = new MatrixClock();

    @Override protected void deliverEverythingPossible() {

        Collection<MatrixClock> toRem = new ArrayList<>();

        /* iterate through msgBacklog
         *  NB: "entrySet()'s _iterator_ returns the entries in ASCENDING KEY ORDER" */
        for (Map.Entry<MatrixClock, Integer> entry : msgBacklog.entrySet()) {

            final Integer sendingProcess = entry.getValue();
            final MatrixClock sentMatrix = entry.getKey();
            final int msgsDeliveredFromThisSender = deliveredClock.get(sendingProcess);

            if (sentMatrix.get(sendingProcess, myId()) == msgsDeliveredFromThisSender + 1) {
                if (sentMatrix.precede_ij() == deliveredClock.sum() + 1) {
                    System.out.println("Delivered msg w mtx "+sentMatrix+" from ["+sendingProcess+"]");
                    deliveredClock.incr(sendingProcess);
                    myMtx.setVC(sendingProcess, sentMatrix.getVC(sendingProcess));
                    toRem.add(sentMatrix);
                }
            }
        }
        toRem.forEach(msgBacklog::remove);
        if (msgBacklog.isEmpty())
            System.out.println("All received messages have been delivered -- groovy");
    }

    @Override protected UnicastConn createConnObj(Socket socket, int userPort) {
        return UnicastConn.startWithSocket(socket, this, userPort);
    }

    @Override protected void optnlInitConnection(UnicastConn conn) {
        /* add connection to matrix */
        getMyMtx().addConn(conn.getForeignID());
    }

    public void send(int dest) {
        if (!connections.containsKey(dest)) {
            System.err.println("Not connected to "+dest);
            return;
        }
        UnicastConn conn = connections.get(dest);

        /* alter the associated data structures appropriately
         * i.e. set SEND[i,j]+=1 */
        getMyMtx().incr(myId(), dest);

        String msg = "msg "+getMyMtx().serialize();
        System.out.println("sending to "+dest);
        conn.println(msg);
    }

    @Override protected UnicastConn createConn(Socket socket, BaseServer server) {
        return UnicastConn.startWithSocket(socket, (UnicastServer) server);
    }

    public MatrixClock getMyMtx() {
        return myMtx;
    }

    public void setMyMtx(MatrixClock myMtx) {
        this.myMtx = myMtx;
    }
}
