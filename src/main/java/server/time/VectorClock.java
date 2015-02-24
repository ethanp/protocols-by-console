package server.time;

import server.base.BaseServer;

import java.util.Map;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Ethan Petuchowski 2/22/15
 */
public class VectorClock extends Timestamp {

    /** Map<ProcID, Causal-Count>*/
    NavigableMap<Integer, Integer> map = new TreeMap<>();
    private BaseServer server;

    @Override public String serialize() {
        StringBuilder sb = new StringBuilder();
        int i = 0;
        final int f = map.size()-1;
        for (Map.Entry<Integer, Integer> entry : map.entrySet()) {
            sb.append(entry.getKey()+":"+entry.getValue());
            if (i++<f) sb.append(",");
        }
        return sb.toString();
    }

    public static VectorClock deserialize(String s) {
        VectorClock n = new VectorClock();
        String[] items = s.split(",");
        for (String t : items) {
            String[] p = t.split(":");
            n.put(Integer.parseInt(p[0]), Integer.parseInt(p[1]));
        }
        return n;
    }


    /**
     * @return a negative integer, zero, or a positive integer as this VectorClock is less than,
     * concurrent to, or greater than the given VectorClock.
     */
    @Override public int compareTo(Timestamp a) {
        VectorClock o = (VectorClock) a;
        boolean isLess = false;
        boolean isMore = false;
        for (Map.Entry<Integer, Integer> entry : map.entrySet()) {
            final int procID = entry.getKey();
            if (o.containsKey(procID)) {
                if (entry.getValue() < o.get(procID)) {
                    if (isMore) return 0;
                    else isLess = true;
                }
                else if (entry.getValue() > o.get(procID)) {
                    if (isLess) return 0;
                    else isMore = true;
                }
            } else System.err.println("Your network is not fully connected. "+
                                      "Results are undefined in this situation.");
        }

        return isLess ? -1
             : isMore ? 1
             : 0;
    }

    public void setServer(BaseServer server) { this.server = server; }
    public boolean isGreaterThan(VectorClock vc) { return this.compareTo(vc) > 0; }
    public boolean isLesserThan(VectorClock vc) { return this.compareTo(vc) < 0; }
    /* pass along map operations */
    public void     incr(int proc)      { map.put(proc, map.get(proc)+1); }
    public void     add(int id)         { map.put(id, 0); }
    public boolean  containsKey(int a)  { return map.containsKey(a); }
    public int      get(int a)          { return map.get(a); }
    public void     put(int a, int b)   { map.put(a, b); }
    public void     remove(int id)      { map.remove(id); }
    public int      size()              { return map.size(); }

    public boolean shouldDeliver(VectorClock receivedVC, int procID) {
        if (receivedVC.size() != this.size()) {
            System.err.println("Can't deliver, network is not completely connected");
        }
        for (Entry e : receivedVC.entrySet()) {
            if (!this.containsKey(e.procID)) {
                System.err.println("Can't deliver, network is not completely connected");
            }
            final int myCount = this.get(e.procID);
            if (e.procID == procID) {
                if (procID == server.myId()) {
                    if (myCount != e.count) {
                        return false;
                    }
                }
                else if (myCount != e.count-1) {
                    return false;
                }
            }
            else if (myCount < e.count) {
                return false;
            }
        }

        return true;
    }

    @Override public String toString() { return "{"+this.serialize()+"}"; }

    private NavigableSet<Entry> entrySet() {
        NavigableSet<Entry> toRet = new TreeSet<>();
        final Set<Map.Entry<Integer, Integer>> entries = map.entrySet();
        for (Map.Entry<Integer, Integer> entry : entries)
            toRet.add(new Entry(entry.getKey(), entry.getValue()));
        return toRet;
    }

    class Entry implements Comparable<Entry> {
        final int procID;
        final int count;
        public Entry(int procID, int count) {
            this.procID = procID;
            this.count = count;
        }

        @Override public int compareTo(Entry o) {
            if (procID != o.procID) return procID-o.procID;
            else return count-o.count;
        }

        @Override public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Entry)) return false;
            Entry entry = (Entry) o;
            if (count != entry.count) return false;
            if (procID != entry.procID) return false;
            return true;
        }

        @Override public int hashCode() {
            int result = procID;
            result = 31*result+count;
            return result;
        }
    }
}
