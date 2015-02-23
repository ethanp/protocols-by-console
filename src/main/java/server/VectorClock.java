package server;

import java.util.Map;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Ethan Petuchowski 2/22/15
 */
public class VectorClock implements Comparable<VectorClock> {

    /** Map<ProcID, Causal-Count>*/
    NavigableMap<Integer, Integer> map = new TreeMap<>();
    private BrdcstServer server;

    public String serialize() {
        StringBuilder sb = new StringBuilder();
        int i = 0;
        int f = map.size()-1;
        for (Map.Entry<Integer, Integer> entry : map.entrySet()) {
            sb.append(entry.getKey()+":"+entry.getValue());
            if (i<f) sb.append(",");
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
     * @return a negative integer, zero, or a positive integer as this object is less than,
     * concurrent to, or greater than the specified object.
     */
    @Override public int compareTo(VectorClock o) {
        boolean isLess = false;
        boolean isMore = false;
        for (Map.Entry<Integer, Integer> entry : map.entrySet()) {
            int procID = entry.getKey();
            if (o.containsKey(procID)) {
                if (entry.getValue() < o.get(procID)) {
                    if (isMore) return 0;
                    isLess = true;
                }
                else if (entry.getValue() > o.get(procID)) {
                    if (isLess) return 0;
                    isMore = true;
                }
            }
        }
        return isLess ? -1
             : isMore ? 1
             : 0;
    }

    public void setServer(BrdcstServer server) { this.server = server; }

    void incr(int proc)        { map.put(proc, map.get(proc)+1); }
    public void add(int id)    { map.put(id, 0); }
    boolean containsKey(int a) { return map.containsKey(a); }
    int get(int a)             { return map.get(a);         }
    void put(int a, int b)     { map.put(a, b);             }
    public void remove(int id) { map.remove(id); }
    private int size()         { return map.size(); }

    public static boolean shouldDeliver(VectorClock receivedVC, VectorClock myVC, int procID) {
        if (receivedVC.size() != myVC.size()) {
            System.err.println("Can't deliver, network is not completely connected");
        }
        for (Entry e : receivedVC.entrySet()) {
            if (!myVC.containsKey(e.procID)) {
                System.err.println("Can't deliver, network is not completely connected");
            }
            final int myCount = myVC.get(e.procID);
            if (e.procID == procID) {
                if (myCount != e.count-1) {
                    return false;
                }
            }
            else if (myCount < e.count) {
                return false;
            }
        }

        return true;
    }

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

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Entry)) return false;
            Entry entry = (Entry) o;
            if (count != entry.count) return false;
            if (procID != entry.procID) return false;
            return true;
        }

        @Override
        public int hashCode() {
            int result = procID;
            result = 31*result+count;
            return result;
        }
    }
}
