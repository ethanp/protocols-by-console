package server.unicast;

import server.base.BaseConn;
import server.base.BaseServer;
import server.time.VectorClock;

import java.net.Socket;

import static server.util.Common.afterSpace;

/**
 * Ethan Petuchowski 2/23/15
 */
public class UnicastConn extends BaseConn {

    public UnicastConn(Socket socket, BaseServer brdcstServer) { super(socket, brdcstServer); }

    public static UnicastConn startWithSocket(Socket socket, UnicastServer server) {
        UnicastConn conn = new UnicastConn(socket, server);
        new Thread(conn).start();
        return conn;
    }

    @Override protected void receiveMessage(String cmd) {
        VectorClock rcvdVC = VectorClock.deserialize(afterSpace(cmd));
        System.out.println("Received msg w VC "+rcvdVC+" from ["+foreignID+"]");
        baseServer.rcvMsg(rcvdVC, foreignID);
    }
}
