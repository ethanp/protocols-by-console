package server.time;

import server.base.BaseServer;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * Ethan Petuchowski 2/22/15
 */
public class VectorClock extends Timestamp {

    public VectorClock(int senderID) { super(senderID); }

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

    public static VectorClock deserialize(String s, int senderID) {
        VectorClock n = new VectorClock(senderID);
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

    public void    setServer(BaseServer server)  { this.server = server; }

    /* simple helpers */
    public boolean isGreaterThan(VectorClock vc) { return this.compareTo(vc) > 0; }
    public boolean isLesserThan(VectorClock vc)  { return this.compareTo(vc) < 0; }
    public void    incr(int proc)                { map.put(proc, map.get(proc)+1); }
    public int     sum()                    { return map.values().stream().mapToInt(a -> a).sum(); }
    public List<Integer> asList()     { return map.values().stream().collect(Collectors.toList()); }

    /* pass along map operations */
    public void     add(int id)         { map.put(id, 0); }
    public boolean  containsKey(int a)  { return map.containsKey(a); }
    public int      get(int port)       { return map.get(port); }
    public void     put(int port, int count) { map.put(port, count); }
    public void     remove(int portId)  { map.remove(portId); }
    public int      size()              { return map.size(); }

    /**
     * This is only ever called ON the deliveredClock belonging to the BroadcastServer
     */
    public synchronized boolean shouldDeliver(VectorClock receivedVC, int senderID) {
        if (receivedVC.size() != this.size()) {
            System.err.println("Can't deliver, network is not completely connected");
            return false;
        }
        for (Entry vcSlot : receivedVC.entrySet()) {
            if (!this.containsKey(vcSlot.procID)) {
                System.err.println("Can't deliver, network is not completely connected");
                return false;
            }

            final int numDlvrdSoFarForThisProc = this.get(vcSlot.procID);
            if (vcSlot.procID == senderID) {
                if (senderID == server.myId()) {
                    if (numDlvrdSoFarForThisProc != vcSlot.count) {
                        System.out.println("a"+vcSlot.procID);
                        return false;
                    }
                }
                else if (numDlvrdSoFarForThisProc < vcSlot.count-1) {
                    System.out.println("b"+vcSlot.procID);
                    return false;
                }
            }
            else if (numDlvrdSoFarForThisProc < vcSlot.count) {
                System.out.println("c"+vcSlot.procID);
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

    public int indexOf(int procID) {
        Iterator<Integer> it = map.keySet().iterator();
        int idx = 0;
        while (it.next() != procID) idx++;
        return idx;
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
