package server.time;

/**
 * Ethan Petuchowski 2/23/15
 */
public abstract class Timestamp implements Comparable<Timestamp> {
    public Timestamp(int senderID) { this.senderID = senderID; }
    public abstract boolean containsKey(int procID);
    public abstract String serialize();
    protected int senderID = -1;
    public int getSenderID() { return senderID; }
}
