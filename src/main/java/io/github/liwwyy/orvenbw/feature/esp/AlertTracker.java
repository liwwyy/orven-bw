package io.github.liwwyy.orvenbw.feature.esp;

import java.util.*;

/** Once-per-match armour, held-item transitions and monotonic observed defence warnings. */
public final class AlertTracker {
    private final Set<String> armored = new HashSet<>();
    private final Map<String, String> held = new HashMap<>();
    private final Map<BedGeometry.Pos, Integer> defence = new HashMap<>();
    public boolean armor(String player) { return armored.add(player); }
    public boolean held(String player, String type) {
        String previous = type == null ? held.remove(player) : held.put(player, type);
        return type != null && !type.equals(previous);
    }
    public boolean defence(BedIndex.Bed bed) {
        int previous = defence.getOrDefault(bed.geometry.foot(), 0);
        if (bed.obsidian <= previous) return false;
        defence.put(bed.geometry.foot(), bed.obsidian); return true;
    }
    public void clear() { armored.clear(); held.clear(); defence.clear(); }
}
