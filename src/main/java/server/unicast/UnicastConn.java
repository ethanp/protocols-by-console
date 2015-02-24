package server.unicast;

import server.base.BaseConn;
import server.time.MatrixClock;

import java.net.Socket;

import static server.util.Common.afterSpace;

/**
 * Ethan Petuchowski 2/23/15
 */
public class UnicastConn extends BaseConn<UnicastServer> {

    public UnicastConn(Socket socket, UnicastServer unicastServer) { super(socket, unicastServer); }

    public static UnicastConn startWithSocket(Socket socket, UnicastServer server) {
        UnicastConn conn = new UnicastConn(socket, server);
        new Thread(conn).start();
        return conn;
    }

    @Override protected void receiveMessage(String cmd) {
        MatrixClock rcvdMtx = server.getMyMtx().deserialize(afterSpace(cmd));
        System.out.println("Received msg w MTX\n\n"+rcvdMtx+"\n\nfrom ["+foreignID+"]");
        server.rcvMsg(rcvdMtx, foreignID);
    }

    public static UnicastConn startWithSocket(Socket socket, UnicastServer server, int port) {
        UnicastConn c = UnicastConn.startWithSocket(socket, server);
        c.setForeignID(port);
        return c;
    }
}
