package server.unicast;

import server.base.BaseServer;

import java.net.ServerSocket;

/**
 * Ethan Petuchowski 2/23/15
 */
public class UnicastServer extends BaseServer {
    public UnicastServer(ServerSocket serverSocket) {
        super(serverSocket);
    }

    public void send(int dest) {

    }
}
