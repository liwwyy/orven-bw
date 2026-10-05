package io.github.liwwyy.orvenbw.feature.autosoup;

import org.junit.jupiter.api.Test;
import java.util.Arrays;
import static org.junit.jupiter.api.Assertions.*;
import static io.github.liwwyy.orvenbw.feature.autosoup.SoupInventory.Kind;

class SoupInventoryTest {
    @Test void refillCannotOverwriteValuablesAndNeverUsesArmorOrCrafting() {
        Kind[] slots = new Kind[40]; Arrays.fill(slots, Kind.OTHER);
        slots[39] = Kind.SOUP; slots[2] = Kind.EMPTY;
        assertNull(SoupInventory.nextMove(slots));
        slots[9] = Kind.SOUP; slots[2] = Kind.SWORD;
        assertNull(SoupInventory.nextMove(slots));
        slots[4] = Kind.BOWL;
        assertEquals(new SoupInventory.Move(9, 4), SoupInventory.nextMove(slots));
        slots[7] = Kind.EMPTY;
        assertEquals(new SoupInventory.Move(9, 7), SoupInventory.nextMove(slots));
    }
    @Test void randomSelectionExcludesAttemptedSoupsAndSwordReturnPrefersOriginalSword() {
        Kind[] slots = new Kind[36]; Arrays.fill(slots, Kind.EMPTY);
        slots[0] = slots[5] = Kind.SWORD; slots[2] = slots[6] = Kind.SOUP;
        boolean[] used = new boolean[9]; used[2] = true;
        assertEquals(6, SoupInventory.randomSoup(slots, used, () -> 0));
        used[6] = true; assertEquals(-1, SoupInventory.randomSoup(slots, used, () -> .5));
        assertEquals(5, SoupInventory.swordSlot(slots, 5));
        assertEquals(0, SoupInventory.swordSlot(slots, 6));
        slots[0] = slots[5] = Kind.OTHER; assertEquals(6, SoupInventory.swordSlot(slots, 6));
        int[] counts = new int[36]; counts[2] = 3; counts[6] = 1;
        assertEquals(4, SoupInventory.soupCount(slots, counts));
    }
}
