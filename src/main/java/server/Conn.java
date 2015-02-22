package server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.NavigableSet;
import java.util.TreeSet;

import static server.Common.afterSpace;

/**
* Ethan Petuchowski 2/22/15
*/
class Conn implements Runnable {

    final Socket socket;
    BufferedReader reader;
    PrintWriter writer;
    final BrdcstServer brdcstServer;

    int foreignID = -1;
    int delay = 0;
    int lastMsgDlvrd = 0;

    NavigableSet<Integer> msgBacklog = new TreeSet<>();

    public Conn(Socket socket, BrdcstServer brdcstServer) {
        this.socket = socket;
        this.brdcstServer = brdcstServer;
        try {
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            writer = new PrintWriter(socket.getOutputStream());
        }
        catch (IOException e) { e.printStackTrace(); }
    }

    public void setForeignID(int i) { foreignID = i; }

    public static Conn startWithSocket(Socket socket, BrdcstServer brdcstServer) {
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
                    return;
                }
                if (cmd.startsWith("msg ")) {
                    int msgNum = Integer.parseInt(afterSpace(cmd));
                    System.out.println("Received msg num ["+msgNum+"] from ["+foreignID+"]");
                    msgBacklog.add(msgNum);
                    deliverEverythingPossible();
                }
                else {
                    System.err.println("Received unrecognized command from ["+foreignID+"]");
                    System.err.println(cmd);
                }
            }
            catch (IOException e) { e.printStackTrace(); }
        }
    }

    public void deliverEverythingPossible() {
        Iterator it = msgBacklog.iterator();
        Collection<Integer> toRem = new ArrayList<>();
        while (it.hasNext()) {
            int msgNum = (int) it.next();
            if (msgNum == lastMsgDlvrd + 1) {
                System.out.println("Delivered msg num ["+msgNum+"] from ["+foreignID+"]");
                toRem.add(msgNum);
                lastMsgDlvrd++;
            } else {
                break;
            }
        }

        /* clear whatever was delivered from backlog */
        for (int r : toRem)
            msgBacklog.remove(r);
    }

    void println(String string) {
        new Thread(new DelayPrinter(string)).start();
    }

    String readLine() {
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

    public int getDelay() { return delay; }
    public void setDelay(int delay) { this.delay = delay; }
}
