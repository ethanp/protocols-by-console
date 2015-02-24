package server.time;

import server.util.Common;

import java.util.Collection;
import java.util.Iterator;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Ethan Petuchowski 2/23/15
 */
public class MatrixClock extends Timestamp {

    public MatrixClock() {}
    public MatrixClock(Collection<Integer> ports) { reinitMtx(ports); }

    /* TODO this will have problems if it is actually manipulated concurrently */
    /* I could most easily resolve this by SYNCHRONIZing all the methods */
    SortedMap<Integer, VectorClock> mtx = new TreeMap<>();

    public void addConn(int port) {
        Set<Integer> s = new TreeSet<>(mtx.keySet());
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
/*    @Override public int compareTo(Timestamp o) {
        final MatrixClock that = (MatrixClock) o;

        Iterator<VectorClock> these = mtx.values().iterator();
        Iterator<VectorClock> those = that.mtx.values().iterator();

        int LESS = -1, MORE = 1, EVEN = 0;
        boolean isMore = false;
        boolean isLess = false;

        while (these.hasNext()) {
            final VectorClock mine = these.next();
            final VectorClock theirs = those.next();
            if (mine.isGreaterThan(theirs)) {
                if (isLess) return EVEN;
                else isMore = true;
            }
            else if (mine.isLesserThan(theirs)) {
                if (isMore) return EVEN;
                else isLess = true;
            }
        }

        return isLess ? LESS
             : isMore ? MORE
             : EVEN;
    }*/

    /**
     * @return a negative integer, zero, or a positive integer as this object is less than, equal
     * to, or greater than the specified object.
     *
     * For my purposes, maybe this thing is Greater Than iff :
     *
     *      this.precede_i^j < that.precede_i^j
     *
     *      where precede_i^j := \sum{k=1}{n}{Send_{i}[k,j]}
     */
    @Override public int compareTo(Timestamp o) {
        final MatrixClock that = (MatrixClock) o;
        return this.precede_ij()-that.precede_ij();
    }

    public int precede_ij() {
        return mtx.values().stream().mapToInt(vc -> vc.get(Common.MY_PORT)).sum();
    }

    public MatrixClock deserialize(String s) {
        String[] vectorClocks = s.split("\\|");
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

    @Override public String toString() { return "\n{\n"+serialize().replaceAll("\\|","\n")+"}\n"; }
    @Override public boolean containsKey(int procID)    { return mtx.containsKey(procID); }
    public void incr(int from, int to)                  { mtx.get(from).incr(to); }
    public void setVC(Integer port, VectorClock vc)     { mtx.put(port, vc); }
    public VectorClock getVC(int port)                  { return mtx.get(port); }
    public int get(int i, int j)                        { return mtx.get(i).get(j); }
}
