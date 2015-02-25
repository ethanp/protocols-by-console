package server.unicast;

import server.base.BaseServer;
import server.time.MatrixClock;

import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Ethan Petuchowski 2/23/15
 */
public class UnicastServer extends BaseServer<UnicastConn, MatrixClock> {
    public UnicastServer(ServerSocket serverSocket) {
        super(serverSocket);
        myMtx = new MatrixClock(myId());
    }

    private final MatrixClock myMtx;

    @Override protected void deliverEverythingPossible() {

        Collection<MatrixClock> toRem = new ArrayList<>();

        /* iterate through msgBacklog
         *  NB: "entrySet()'s _iterator_ returns the entries in ASCENDING KEY ORDER" */
        boolean didSomething;
        do {
            didSomething = false;
            for (MatrixClock sentMatrix : msgBacklog) {

                final Integer sendingProcID = sentMatrix.getSenderID();
                final int senderProcIdx = deliveredClock.indexOf(sendingProcID);

                final int msgsSentFromSender = sentMatrix.get(sendingProcID, myId());
                final int msgsDlvrdFromSender = deliveredClock.get(sendingProcID);
                final boolean isNextMsgFromSender = msgsSentFromSender == msgsDlvrdFromSender+1;

                if (isNextMsgFromSender) {
                    boolean hasDlvrdAllPrecedingMsgs = true;
                    List<Integer> theirSide = sentMatrix.getColForID(myId());
                    List<Integer> mySide = deliveredClock.asList();
                    for (int i = 0; i < theirSide.size(); i++) {
                        if (i == senderProcIdx) continue;
                        if (theirSide.get(i) > mySide.get(i)) {
                            hasDlvrdAllPrecedingMsgs = false;
                            break;
                        }
                    }
                    if (hasDlvrdAllPrecedingMsgs) {
                        System.out.println("Delivered msg w mtx "+sentMatrix+" from ["+sendingProcID+"]");
                        deliveredClock.incr(sendingProcID);
                        myMtx.setVC(sendingProcID, sentMatrix.getVC(sendingProcID));
                        toRem.add(sentMatrix);
                        didSomething = true;
                    }
                }
            }
            toRem.forEach(msgBacklog::remove);
        } while (didSomething);
        if (msgBacklog.isEmpty())
            System.out.println("All received messages have been delivered -- groovy");
    }

    @Override protected UnicastConn createConnObj(Socket socket, int userPort) {
        return UnicastConn.startWithSocket(socket, this, userPort);
    }

    @Override protected void optnlInitConnection(UnicastConn conn) {
        /* add connection to matrix */
        getMyMtx().addConn(conn.getForeignID());
    }

    public void send(int dest) {
        if (!connections.containsKey(dest)) {
            System.err.println("Not connected to "+dest);
            return;
        }
        UnicastConn conn = connections.get(dest);

        /* alter the associated data structures appropriately
         * i.e. set SEND[i,j]+=1 */
        getMyMtx().incr(myId(), dest);

        String msg = "msg "+getMyMtx().serialize();
        System.out.println("sending to "+dest);
        conn.println(msg);
    }

    @Override protected UnicastConn createConn(Socket socket, BaseServer server) {
        return UnicastConn.startWithSocket(socket, (UnicastServer) server);
    }

    public MatrixClock getMyMtx() {
        return myMtx;
    }
}
