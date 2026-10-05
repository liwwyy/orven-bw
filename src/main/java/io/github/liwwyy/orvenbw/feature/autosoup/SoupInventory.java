package io.github.liwwyy.orvenbw.feature.autosoup;

import java.util.ArrayList;
import java.util.function.DoubleSupplier;

/** Inventory indexes 0..8 are hotbar; 9..35 are main inventory. No crafting/armor/cursor moves. */
public final class SoupInventory {
    public enum Kind { EMPTY, SOUP, BOWL, SWORD, OTHER }
    public record Move(int source, int hotbar) {}
    private SoupInventory() {}
    public static int randomSoup(Kind[] slots, boolean[] used, DoubleSupplier random) {
        var candidates = new ArrayList<Integer>();
        for (int i = 0; i < Math.min(9, slots.length); i++)
            if (slots[i] == Kind.SOUP && (used == null || !used[i])) candidates.add(i);
        return candidates.isEmpty() ? -1 : candidates.get(Math.min(candidates.size() - 1, (int) (random.getAsDouble() * candidates.size())));
    }
    public static int soupCount(Kind[] slots, int[] counts) {
        int count = 0;
        for (int i = 0; i < Math.min(9, slots.length); i++) if (slots[i] == Kind.SOUP) count += Math.max(1, counts[i]);
        return count;
    }
    public static int swordSlot(Kind[] slots, int preferred) {
        if (preferred >= 0 && preferred < Math.min(9, slots.length) && slots[preferred] == Kind.SWORD) return preferred;
        for (int i = 0; i < Math.min(9, slots.length); i++) if (slots[i] == Kind.SWORD) return i;
        return Math.clamp(preferred, 0, 8);
    }
    public static Move nextMove(Kind[] slots) {
        int source = -1, target = -1;
        for (int i = 9; i < Math.min(36, slots.length); i++) if (slots[i] == Kind.SOUP) { source = i; break; }
        if (source < 0) return null;
        for (Kind kind : new Kind[]{Kind.EMPTY, Kind.BOWL}) {
            for (int i = 0; i < Math.min(9, slots.length); i++) if (slots[i] == kind) { target = i; break; }
            if (target >= 0) break;
        }
        return target < 0 ? null : new Move(source, target);
    }
}
