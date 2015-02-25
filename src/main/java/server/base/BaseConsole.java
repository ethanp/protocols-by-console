package server.base;

import server.util.Common;

import java.io.IOException;
import java.net.BindException;
import java.net.ServerSocket;
import java.util.Scanner;

/**
 * Ethan Petuchowski 2/20/15
 */
public abstract class BaseConsole<Server extends BaseServer> implements Runnable {

    /****** STATIC SECTION ******/

    static Scanner sc = new Scanner(System.in);

    protected static ServerSocket reqSocket() {
        int port = 0;
        ServerSocket ss = null;
        while (ss == null) {
            while (port == 0) {
                System.out.println("Please enter a port number: ");
                try {
                    port = Integer.parseInt(sc.nextLine());
                }
                catch (NumberFormatException e) {
                    System.err.println("Input port was invalid");
                }
            }
            try {
                ss = new ServerSocket(Common.LOW_PORT+port);
            }
            catch (BindException e) {
                System.err.println("Port ["+port+"] is already taken.");
                port = 0;
            }
            catch (IOException e) {
                System.err.println("Some strange error occurred, please restart");
                System.exit(1);
            }
        }
        Common.MY_PORT = ss.getLocalPort()-Common.LOW_PORT;
        return ss;
    }

    public static String prompt() {
        return prompt("#$> ");
    }

    private static String prompt(String promptString) {
        System.out.print(promptString);
        return sc.nextLine();
    }


    /****** NON-STATIC SECTION ******/

    protected Server server;

    public BaseConsole(Server server) {
        this.server = server;
        new Thread(server).start();
    }

    @Override public void run() {
        executeConnect("connect "+server.myId());
        while (true) {
            String cmd = prompt();
            switch (Common.beforeSpace(cmd)) {
                case "connect":     executeConnect(cmd);    break;
                case "broadcast":   executeBroadcast(cmd);  break;
                case "delay":       executeDelay(cmd);      break;
                case "send":        executeSend(cmd);       break;
                default:    System.err.println("Unrecognized command: "+cmd);
            }
        }
    }

    protected abstract void executeBroadcast(String cmd);

    /**
     * an extension of vector clocks where each message carries a timestamp
     * composed of `n` vector clocks
     */
    protected abstract void executeSend(String cmd);

    protected void executeDelay(String cmd) {
        String[] ss = cmd.split(" ");
        if (ss.length != 3) {
            System.err.println("Incorrect format, use `delay <from> <to>`");
            return;
        }
        final int peerNum = Integer.parseInt(ss[1]);
        final int delaySize = Integer.parseInt(ss[2]);

        if (server.setDelay(peerNum, delaySize)) {
            System.out.println("set delay of "+delaySize+" seconds on msgs to server "+peerNum);
        }
        else {
            System.out.println("no delay added");
        }
    }

    protected void executeConnect(String cmd) {
        String portStr = Common.afterSpace(cmd);

        /* connect to range of ports */
        if (cmd.indexOf('-') > -1) {
            final int dash = portStr.indexOf('-');
            try {
                final int portMin = Integer.parseInt(portStr.substring(0, dash));
                final int portMax = Integer.parseInt(portStr.substring(dash+1, portStr.length()));
                for (int i = portMin; i <= portMax; i++)
                    server.connectToServerAtPort(i);
            } catch (NumberFormatException e) {
                System.err.println("Invalid number in range");
                System.err.println(e.getMessage());
            }
        }

        /* connect to specific port */
        else {
            try {
                final int portNum = Integer.parseInt(portStr);
                server.connectToServerAtPort(portNum);
            } catch (NumberFormatException e) {
                System.err.println(portStr+" is not a valid number");
            }
        }
    }
}
