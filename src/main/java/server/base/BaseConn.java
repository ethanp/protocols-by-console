package server.base;

import server.util.Common;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

/**
* Ethan Petuchowski 2/22/15
*/
public abstract class BaseConn<Server extends BaseServer> implements Runnable {

    /* Fields */
    private BufferedReader reader;
    private PrintWriter writer;
    protected final Server server;
    protected int foreignID = -1;
    private int delay = 0;

    /* To Override */
    protected abstract void receiveMessage(String cmd);

    /* Base Methods (not for overriding) */
    public BaseConn(Socket socket, Server server) {
        this.server = server;
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

                if (cmd == null) {
                    System.out.println("Connection to ["+foreignID+"] closed");
                    server.removeConn(foreignID);
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

    public String readLine() {
        try {
            return reader.readLine();
        }
        catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Simulates a unidirectional and persistent network delay in sending messages from this
     * machine to the connected machine
     */
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

    public void println(String string)  { new Thread(new DelayPrinter(string)).start(); }
    public int  getForeignID()          { return foreignID; }
    public void setForeignID(int i)     { foreignID = i; }
    public void setDelay(int delay)     { this.delay = delay; }
}
