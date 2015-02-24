package server.unicast;

import server.base.BaseServer;
import server.time.MatrixClock;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.net.ServerSocket;
import java.net.Socket;
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
        /* iterate through msgBacklog */
        /* "entrySet()'s _iterator_ returns the entries in ASCENDING KEY ORDER" */
        for (Map.Entry<MatrixClock, Integer> entry : msgBacklog.entrySet()) {
            /* TODO figures outs the ALGO-RHYTHM DAWG! */
        }
        // TODO remove
        throw new NotImplementedException();
    }

    @Override protected void addConnection(Socket socket, int userPort) {
        UnicastConn conn = UnicastConn.startWithSocket(socket, this, userPort);
        baseAddConnection(userPort, conn);
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
