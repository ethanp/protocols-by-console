package server;

import java.util.NavigableMap;
import java.util.TreeMap;

/**
 * Ethan Petuchowski 2/22/15
 */
public class VectorClock {
    NavigableMap<Integer, Integer> map = new TreeMap<>();
    void incr(int proc) { map.put(proc, map.get(proc)+1); }

}
