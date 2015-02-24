package server.unicast;

import server.base.BaseServer;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.net.ServerSocket;
import java.net.Socket;

/**
 * Ethan Petuchowski 2/23/15
 */
public class UnicastServer extends BaseServer<UnicastConn> {
    public UnicastServer(ServerSocket serverSocket) { super(serverSocket); }

    @Override protected void deliverEverythingPossible() {

    }

    @Override protected UnicastConn createConn(Socket socket, BaseServer server) {
        return UnicastConn.startWithSocket(socket, (UnicastServer) server);
    }

    @Override protected void addConnection(Socket socket, int userPort) {

    }

    public void send(int dest) {
        // TODO UnicastServer send
        throw new NotImplementedException();
    }
}
