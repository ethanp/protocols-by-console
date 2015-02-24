package server.time;

import sun.reflect.generics.reflectiveObjects.NotImplementedException;

/**
 * Ethan Petuchowski 2/23/15
 */
public class MatrixClock extends Timestamp {
    @Override public boolean containsKey(int procID) {
        return false;
    }

    public VectorClock get(int procID) {
        throw new NotImplementedException();
    }

    @Override public String serialize() {
        throw new NotImplementedException();
    }

    /**
     * @return a negative integer, zero, or a positive integer as this object is less than, equal
     * to, or greater than the specified object.
     */
    @Override public int compareTo(Timestamp o) {
        throw new NotImplementedException();
    }
}
