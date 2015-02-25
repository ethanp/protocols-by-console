package server.broadcast;

import server.base.BaseConn;
import server.time.VectorClock;

import java.net.Socket;

import static server.util.Common.afterSpace;

/**
 * Ethan Petuchowski 2/23/15
 */
public class BrdcstConn extends BaseConn {

    public BrdcstConn(Socket socket, BrdcstServer brdcstServer) { super(socket, brdcstServer); }

    public static BrdcstConn startWithSocket(Socket socket, BrdcstServer server) {
        BrdcstConn conn = new BrdcstConn(socket, server);
        new Thread(conn).start();
        return conn;
    }

    @Override protected void receiveMessage(String cmd) {
        VectorClock rcvdVC = VectorClock.deserialize(afterSpace(cmd), foreignID);
        System.out.println("Received msg w VC "+rcvdVC+" from ["+foreignID+"]");
        server.rcvMsg(rcvdVC);
    }

    public static BrdcstConn startWithSocket(Socket socket, BrdcstServer server, int portID) {
        BrdcstConn b = BrdcstConn.startWithSocket(socket, server);
        b.setForeignID(portID);
        return b;
    }
}
