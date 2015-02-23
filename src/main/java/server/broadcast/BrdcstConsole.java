package server.broadcast;

import server.base.Console;

import java.net.ServerSocket;

/**
 * Ethan Petuchowski 2/23/15
 */
public class BrdcstConsole extends Console<BrdcstServer> {
    public BrdcstConsole(ServerSocket socket) {
        super(new BrdcstServer(socket));
    }

    public static void main(String[] args) {
        ServerSocket serverSocket = reqSocket();
        final BrdcstConsole brdcstConsole = new BrdcstConsole(serverSocket);
        new Thread(brdcstConsole).start();
    }

    @Override public void run() {
        while (true) {
            String cmd = prompt();
            if (cmd.startsWith("connect ")) executeConnect(cmd);
            else if (cmd.equals("broadcast")) executeBroadcast(cmd);
            else if (cmd.startsWith("delay ")) executeDelay(cmd);
            else if (cmd.startsWith("send ")) executeSend(cmd);
            else System.err.println("Unrecognized command: "+cmd);
        }
    }

    private void executeBroadcast(String cmd) {
        server.broadcast();
    }

    @Override protected void executeSend(String cmd) {
        System.err.println("The \"Broadcast Server\" has no ability to \"send\"");
    }
}
