package server.util;

import server.base.BaseServer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

import static server.util.Common.afterSpace;

/**
* Ethan Petuchowski 2/22/15
*/
public class Conn implements Runnable {

    private final Socket socket;
    private BufferedReader reader;
    private PrintWriter writer;
    private final BaseServer brdcstServer;
    private int foreignID = -1;
    private int delay = 0;

    public Conn(Socket socket, BaseServer brdcstServer) {
        this.socket = socket;
        this.brdcstServer = brdcstServer;
        try {
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            writer = new PrintWriter(socket.getOutputStream());
        }
        catch (IOException e) { e.printStackTrace(); }
    }

    public static Conn startWithSocket(Socket socket, BaseServer brdcstServer) {
        Conn conn = new Conn(socket, brdcstServer);
        new Thread(conn).start();
        return conn;
    }

    @Override public void run() {
        while (true) {
            try {
                String cmd = reader.readLine();

                if (cmd == null) {
                    System.out.println("Connection to ["+foreignID+"] closed");
                    brdcstServer.removeConn(foreignID);
                    return;
                }

                if (cmd.startsWith("msg "))
                    receiveMessage(cmd);

                else
                    System.err.println("Received unrecognized command from ["+foreignID+"]\n"+cmd);
            }
            catch (IOException e) { e.printStackTrace(); }
        }
    }

    private void receiveMessage(String cmd) {
        VectorClock rcvdVC = VectorClock.deserialize(afterSpace(cmd));
        System.out.println("Received msg w VC "+rcvdVC+" from ["+foreignID+"]");
        brdcstServer.rcvMsg(rcvdVC, foreignID);
    }

    public String readLine() {
        try {
            return reader.readLine();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    class DelayPrinter implements Runnable {
        String stringToPrint;
        DelayPrinter(String toPrint) {
            stringToPrint = toPrint;
        }
        @Override public void run() {
            try {
                Thread.sleep(delay * Common.MILLI_SEC);
            }
            catch (InterruptedException e) { e.printStackTrace(); }
            writer.println(stringToPrint);
            writer.flush();
        }
    }

    public void println(String string)     { new Thread(new DelayPrinter(string)).start(); }
    public void setForeignID(int i) { foreignID = i; }
    public int getDelay()           { return delay; }
    public void setDelay(int delay) { this.delay = delay; }
}
