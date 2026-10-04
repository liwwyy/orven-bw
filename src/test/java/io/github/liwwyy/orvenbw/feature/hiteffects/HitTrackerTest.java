package io.github.liwwyy.orvenbw.feature.hiteffects;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class HitTrackerTest {
    private static final long MS = 1_000_000L, RESET = 1500 * MS;
    @Test void swingsAndUncorrelatedDamageDoNotRegisterAndEachDecreaseCountsOnce() {
        var tracker = new HitTracker(); var target = new Object();
        tracker.attack(target, 20, false, 0, RESET);
        assertFalse(tracker.observe(20, 100 * MS, RESET)); assertEquals(0, tracker.combo());
        assertTrue(tracker.observe(17.5, 150 * MS, RESET));
        assertEquals(1, tracker.combo()); assertEquals(2.5, tracker.damage());
        assertFalse(tracker.observe(17.5, 200 * MS, RESET));
        assertFalse(tracker.observe(16, 300 * MS, RESET)); assertEquals(1, tracker.combo());
    }
    @Test void confirmationsExpireAndHealUpdatesDoNotCount() {
        var tracker = new HitTracker(); var target = new Object();
        tracker.attack(target, 20, false, 0, RESET);
        assertFalse(tracker.observe(18, 751 * MS, RESET));
        tracker.attack(target, 18, false, 900 * MS, RESET);
        assertFalse(tracker.observe(19, 950 * MS, RESET));
        assertTrue(tracker.observe(17, 1000 * MS, RESET)); assertEquals(2, tracker.damage());
    }
    @Test void combosAndCriticalColorsResetOnTimeoutTargetChangeAndContextReset() {
        var tracker = new HitTracker(); var target = new Object();
        for (int i = 0; i < 3; i++) {
            tracker.attack(target, 20-i, false, i*300*MS, RESET);
            tracker.observe(19-i, (i*300+100)*MS, RESET);
        }
        assertEquals(3, tracker.combo()); assertEquals(0xFFAD66, tracker.color());
        tracker.attack(target, 17, true, 1000*MS, RESET); tracker.observe(15, 1100*MS, RESET);
        assertTrue(tracker.critical()); assertEquals(0xFFD166, tracker.color());
        tracker.observe(15, 2600*MS, RESET); assertEquals(0, tracker.combo());
        assertEquals(4, tracker.lastCombo(), "The last popup outlives the combo reset if its animation lasts longer");
        tracker.attack(new Object(), 20, false, 2700*MS, RESET); assertEquals(0, tracker.combo());
        tracker.reset(); assertFalse(tracker.observe(18, 2800*MS, RESET)); assertEquals(0, tracker.combo());
    }
    @Test void batchedDamageDoesNotInventMultipleRegisteredHits() {
        var tracker = new HitTracker(); var target = new Object();
        tracker.attack(target, 20, false, 0, RESET);
        tracker.attack(target, 20, false, 50*MS, RESET);
        assertTrue(tracker.observe(14, 100*MS, RESET));
        assertEquals(1, tracker.combo()); assertEquals(6, tracker.damage());
        assertFalse(tracker.observe(13, 150*MS, RESET));
    }
}
