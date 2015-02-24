package server.broadcast;

import server.base.BaseConsole;

import java.net.ServerSocket;

/**
 * Ethan Petuchowski 2/23/15
 */
public class BrdcstConsole extends BaseConsole<BrdcstServer> {
    public BrdcstConsole(ServerSocket socket) {
        super(new BrdcstServer(socket));
    }

    public static void main(String[] args) {
        ServerSocket serverSocket = reqSocket();
        final BrdcstConsole brdcstConsole = new BrdcstConsole(serverSocket);
        new Thread(brdcstConsole).start();
    }

    @Override protected void executeBroadcast(String cmd) {
        server.broadcast();
    }

    @Override protected void executeSend(String cmd) {
        System.err.println("The \"Broadcast Server\" has no ability to \"send\"");
    }
}
