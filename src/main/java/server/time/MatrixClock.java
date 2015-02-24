package server.time;

import java.util.Collection;
import java.util.Iterator;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * Ethan Petuchowski 2/23/15
 */
public class MatrixClock extends Timestamp {

    public MatrixClock() {}
    public MatrixClock(Collection<Integer> ports) { reinitMtx(ports); }

    SortedMap<Integer, VectorClock> mtx = new TreeMap<>();

    public void addConn(int port) {
        Set<Integer> s = mtx.keySet();
        s.add(port);
        reinitMtx(s);
    }

    private void reinitMtx(Collection<Integer> s) {
        mtx = new TreeMap<>();
        for (int i : s) {
            final VectorClock vc = new VectorClock();
            for (int j : s) vc.add(j);
            mtx.put(i, vc);
        }
    }




    @Override public String serialize() {
        StringBuilder sb = new StringBuilder();
        for (VectorClock vc : mtx.values())
            sb.append(vc.serialize()+"|");
        return sb.toString();
    }


    /**
     * @return a negative integer, zero, or a positive integer as this object is less than, equal
     * to, or greater than the specified object.
     *
     * For my purposes, maybe this thing is Greater Than iff :
     *
     *      forall VC, thisVC ≥ thatVC
     *      and thisVC ≠ thatVC
     *
     *   which is similar to the normal VectorClock comparison operator
     */
    @Override public int compareTo(Timestamp o) {
        final MatrixClock that = (MatrixClock) o;

        Iterator<VectorClock> these = mtx.values().iterator();
        Iterator<VectorClock> those = that.mtx.values().iterator();

        boolean isMore = false;
        boolean isLess = false;

        while (these.hasNext()) {
            final VectorClock mine = these.next();
            final VectorClock theirs = those.next();
            if (mine.isGreaterThan(theirs)) {
                if (isLess) return 0;
                else isMore = true;
            }
            else if (mine.isLesserThan(theirs)) {
                if (isMore) return 0;
                else isLess = true;
            }
        }

        return isLess ? -1
             : isMore ? 1
             : 0;
    }

    public MatrixClock deserialize(String s) {
        String[] vectorClocks = s.split("|");
        final Set<Integer> ports = mtx.keySet();
        Iterator<Integer> portsIt = ports.iterator(); // this WILL be sorted (so say the docs)
        MatrixClock rcvdMtx = new MatrixClock(ports);
        for (String vectorClock : vectorClocks) {
            VectorClock vc = VectorClock.deserialize(vectorClock);
            final Integer port = portsIt.next();
            rcvdMtx.setVC(port, vc);
        }
        return rcvdMtx;
    }

    @Override public boolean containsKey(int procID) { return mtx.containsKey(procID); }
    public void incr(int i, int j) { mtx.get(i).incr(j); }
    private void setVC(Integer port, VectorClock vc) { mtx.put(port, vc); }
}
