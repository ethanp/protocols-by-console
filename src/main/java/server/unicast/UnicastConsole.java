package server.unicast;

import server.base.BaseConsole;
import server.util.Common;

import java.net.ServerSocket;

/**
 * Ethan Petuchowski 2/23/15
 */
public class UnicastConsole extends BaseConsole<UnicastServer> {

    public UnicastConsole(ServerSocket socket) {
        super(new UnicastServer(socket));
    }

    public static void main(String[] args) {
        ServerSocket serverSocket = reqSocket();
        final UnicastConsole brdcstConsole = new UnicastConsole(serverSocket);
        new Thread(brdcstConsole).start();
    }

    @Override protected void executeBroadcast(String cmd) {
        System.err.println("The \"Unicast Server\" has no ability to \"broadcast\"");
    }

    protected void executeSend(String cmd) {
        int dest;
        try {
            dest = Integer.parseInt(Common.afterSpace(cmd));
        } catch (NumberFormatException e) {
            System.err.println("Invalid destination");
            return;
        }
        if (!server.isConnectedTo(dest)) {
            System.err.println("Invalid, not connected to "+dest);
            return;
        }
        server.send(dest);
    }
}
