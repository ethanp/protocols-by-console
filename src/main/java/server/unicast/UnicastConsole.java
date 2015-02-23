package server.unicast;

import server.base.Console;
import server.util.Common;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.net.ServerSocket;

/**
 * Ethan Petuchowski 2/23/15
 */
public class UnicastConsole extends Console<UnicastServer> {

    public UnicastConsole(ServerSocket socket) {
        super(new UnicastServer(socket));
    }

    /**
     * "connect 1-5" or "connect 3" "delay 3 4" creates a 4-second delay to process 3
     */
    @Override public void run() {throw new NotImplementedException();}

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
