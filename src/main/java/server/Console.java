package server;

import java.util.Scanner;
import java.util.StringTokenizer;

/**
 * Ethan Petuchowski 2/20/15
 */
public class Console implements Runnable {
    static Scanner sc = new Scanner(System.in);
    public static void main(String[] args) {
        int portOffset;
        if (args.length > 0) {
            try {
                portOffset = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                System.err.println("Input port was invalid");
                portOffset = readPort();
            }
        } else {
            portOffset = readPort();
        }
        new Thread(new Console(portOffset)).start();
    }

    private static int readPort() {
        int port = 0;
        while (port == 0) {
            System.out.println("Please enter a port number: ");
            try {
                port = Integer.parseInt(sc.nextLine());
            } catch (NumberFormatException e) {
                System.err.println("Input port was invalid");
            }
        }
        return port;
    }

    Scanner scanner = new Scanner(System.in);
    BrdcstServer server;

    public Console(int portOffset) {
        server = new BrdcstServer(portOffset);
        new Thread(server).start();
    }


    /**
     * "connect 1-5" or "connect 3"
     * "delay 3 4" creates a 4-second delay to process 3
     */
    @Override public void run() {
        while (true) {
            String cmd = prompt();
            if (cmd.startsWith("connect "))   executeConnect(cmd);
            else if (cmd.equals("broadcast")) server.broadcast();
            else if (cmd.startsWith("delay ")) executeDelay(cmd);
            else System.err.println("Unrecognized command: "+cmd);
        }
    }

    private void executeDelay(String cmd) {StringTokenizer stringTokenizer = new StringTokenizer(cmd, " ");
        stringTokenizer.nextToken(); // skip word "delay itself
        int peerNum = Integer.parseInt(stringTokenizer.nextToken());
        int delaySize = Integer.parseInt(stringTokenizer.nextToken());
        if (server.setDelay(peerNum, delaySize)) {
            System.out.println("added delay of "+delaySize+" seconds to server "+peerNum);
        } else {
            System.out.println("no delay added");
        }
    }

    private void executeConnect(String cmd) {
        String portStr = Common.afterSpace(cmd);
                /* connect to range of ports */
        if (cmd.indexOf('-') > -1) {
            final int dash = portStr.indexOf('-');
            int portMin = Integer.parseInt(portStr.substring(0, dash));
            int portMax = Integer.parseInt(portStr.substring(dash+1, portStr.length()));
            for (int i = portMin; i <= portMax; i++)
                server.connectToServerAtPort(i);
        }

        /* connect to specific port */
        else {
            int portNum = Integer.parseInt(portStr);
            server.connectToServerAtPort(portNum);
        }
    }

    private String prompt() {
        return prompt("#$> ");
    }

    private String prompt(String promptString) {
        System.out.print(promptString);
        String userInput = scanner.nextLine();
        return userInput;
    }
}
