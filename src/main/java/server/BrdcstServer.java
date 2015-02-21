package server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Ethan Petuchowski 2/20/15
 */
public class BrdcstServer implements Runnable {

    public static final int LOW_PORT = 3000;
    public static final String LOCALHOST = "0.0.0.0";
    public static final int MILLI_SEC = 1_000;

    ServerSocket serverSocket;
    ConcurrentHashMap<Integer, Conn> connections = new ConcurrentHashMap<>(5,.9f,3);
    int msg_num = 0;

    public BrdcstServer(int portOffset) {
        try {
            serverSocket = new ServerSocket(LOW_PORT+portOffset);
        }
        catch (IOException e) { e.printStackTrace(); }
    }

    @Override public void run() {
        System.out.println("Listening on port "+serverSocket.getLocalPort());
        while (true) {
            try {
                Socket socket = serverSocket.accept(); // can't do try-w-r bc it'll close() it
                addSocketConn(socket);
            }
            catch (IOException e) { e.printStackTrace(); }
        }
    }

    public void addSocketConn(Socket socket) {
        Conn conn = new Conn(socket);
        new Thread(conn).start();
        final int userPort = socket.getPort()-LOW_PORT;
        connections.put(userPort, conn);
        System.out.println("Connected to "+userPort);
    }

    public void connectToPort(int userPort) {
        try {
            Socket socket = new Socket(LOCALHOST, userPort+LOW_PORT);
            addSocketConn(socket);
        }
        catch (IOException e) {
            e.printStackTrace();
        }

    }

    public void broadcast() {
        for (Conn conn : connections.values()) {
            System.out.println("broadcasting to "+conn.processNum);
            conn.println("msg "+next_msg_num());
        }
    }

    private String next_msg_num() {
        return ""+(++msg_num);
    }

    public void setDelay(int peerNum, int delaySize) {
        connections.get(peerNum).setDelay(delaySize);
    }

    class Conn implements Runnable {

        final Socket    socket;
        BufferedReader  reader;
        PrintWriter     writer;

        final int processNum;
        int delay = 0;

        public Conn(Socket socket) {
            this.socket = socket;
            processNum = socket.getPort()-LOW_PORT;
            try {
                reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                writer = new PrintWriter(socket.getOutputStream());
            }
            catch (IOException e) { e.printStackTrace(); }
        }

        @Override public void run() {
            while (true) {
                try {
                    String cmd = reader.readLine();
                    if (cmd.startsWith("msg ")) {
                        int msgNum = Integer.parseInt(cmd.substring(cmd.indexOf(' ')+1), cmd.length());
                        System.out.println("Process ["+processNum+"] received msg num"+msgNum);
                        System.out.println("Process ["+processNum+"] delivered msg num"+msgNum);
                    }
                    else {
                        System.err.println("Received unrecognized command from process: "+processNum);
                        System.err.println(cmd);
                    }
                }
                catch (IOException e) { e.printStackTrace(); }
            }
        }

        void println(String string) {
            new Thread(new DelayPrinter(string)).start();
        }

        class DelayPrinter implements Runnable {
            String stringToPrint;
            DelayPrinter(String toPrint) {
                stringToPrint = toPrint;
            }
            @Override public void run() {
                try {
                    Thread.sleep(delay * MILLI_SEC);
                }
                catch (InterruptedException e) { e.printStackTrace(); }
                writer.println(stringToPrint);
                writer.flush();
            }
        }

        public int getDelay() { return delay; }
        public void setDelay(int delay) { this.delay = delay; }
    }
}
