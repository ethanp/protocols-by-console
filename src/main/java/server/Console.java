package server;

import java.util.Arrays;
import java.util.Scanner;
import java.util.StringTokenizer;

/**
 * Ethan Petuchowski 2/20/15
 */
public class Console implements Runnable {
    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println("Please enter a port number: ");
            Scanner sc = new Scanner(System.in);
            args = new String[]{sc.nextLine()};
        }
        new Thread(new Console(args)).start();
    }

    public static final int EXIT_FAILURE = 1;

    Scanner scanner = new Scanner(System.in);
    BrdcstServer server;

    public Console(String[] args) {
        /* start server */
        System.out.println("Args: "+Arrays.toString(args));
        int portOffset = Integer.parseInt(args[0]);
        server = new BrdcstServer(portOffset);
        new Thread(server).start();
    }


    @Override public void run() {
        while (true) {
            String cmd = prompt();

            /* e.g. "connect 1-5" or "connect 3"*/
            if (cmd.startsWith("connect ")) {

                String portStr = cmd.substring(cmd.indexOf(' ')+1, cmd.length());
                /* connect to range of ports */
                if (cmd.indexOf('-') > -1) {
                    final int dash = portStr.indexOf('-');
                    int portMin = Integer.parseInt(portStr.substring(0, dash));
                    int portMax = Integer.parseInt(portStr.substring(dash+1), portStr.length());
                    for (int i = portMin; i <= portMax; i++)
                        server.connectToPort(i);
                }

                /* connect to specific port */
                else {
                    int portNum = Integer.parseInt(portStr);
                    server.connectToPort(portNum);
                }
            }

            else if (cmd.equals("broadcast")) {
                server.broadcast();
            }

            /* usage e.g: "delay 3 4" creates a 4-second delay to process 3 */
            else if (cmd.startsWith("delay ")) {
                StringTokenizer stringTokenizer = new StringTokenizer(cmd, " ");
                stringTokenizer.nextToken(); // skip word "delay itself
                int peerNum = Integer.parseInt(stringTokenizer.nextToken());
                int delaySize = Integer.parseInt(stringTokenizer.nextToken());
                server.setDelay(peerNum, delaySize);
            }

            else {
                System.err.println("Unrecognized command: "+cmd);
            }
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
