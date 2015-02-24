package server.time;

/**
 * Ethan Petuchowski 2/23/15
 */
public abstract class Timestamp implements Comparable<Timestamp> {
    public abstract boolean containsKey(int procID);
    public abstract String serialize();
}
