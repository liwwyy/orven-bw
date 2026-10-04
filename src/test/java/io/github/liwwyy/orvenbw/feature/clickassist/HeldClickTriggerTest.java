package io.github.liwwyy.orvenbw.feature.clickassist;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class HeldClickTriggerTest {
    private static final long MS = 1_000_000L;
    @Test void delayStartsOnlyWhenAllFiltersPassAndRestartsAfterLosingEligibility() {
        var trigger = new HeldClickTrigger();
        assertFalse(trigger.active(0, true, false, 250 * MS, false));
        assertFalse(trigger.active(900 * MS, true, true, 250 * MS, false));
        assertFalse(trigger.active(1149 * MS, true, true, 250 * MS, false));
        assertTrue(trigger.active(1150 * MS, true, true, 250 * MS, false));
        assertFalse(trigger.active(1200 * MS, true, false, 250 * MS, false));
        assertFalse(trigger.active(1400 * MS, true, true, 250 * MS, false));
        assertTrue(trigger.active(1650 * MS, true, true, 250 * MS, false));
        assertFalse(trigger.active(1700 * MS, false, true, 250 * MS, true));
        assertFalse(trigger.active(1800 * MS, true, true, 250 * MS, false));
    }
    @Test void instantSkipsOnlyDelayAndResetClearsElapsedTime() {
        var trigger = new HeldClickTrigger();
        assertFalse(trigger.active(0, true, false, 250 * MS, true));
        assertTrue(trigger.active(100 * MS, true, true, 250 * MS, true));
        trigger.reset();
        assertFalse(trigger.active(1000 * MS, true, true, 250 * MS, false));
        assertTrue(trigger.active(1000 * MS, true, true, 0, false));
    }
    @Test void filtersAllowBlocksOnlyWhenEntityRequirementIsOffAndInstantAlwaysRequiresAllThree() {
        var blocks = new HeldClickTrigger.Conditions(true, false, true, true);
        assertFalse(blocks.allows(true, true, true));
        assertTrue(blocks.allows(false, true, true)); assertFalse(blocks.instantReady());
        var noPlayer = new HeldClickTrigger.Conditions(true, true, true, false);
        assertFalse(noPlayer.allows(true, true, true));
        assertTrue(noPlayer.allows(true, true, false)); assertFalse(noPlayer.instantReady());
        var noWeapon = new HeldClickTrigger.Conditions(true, true, false, true);
        assertFalse(noWeapon.allows(true, true, true));
        assertTrue(noWeapon.allows(true, false, true)); assertFalse(noWeapon.instantReady());
        var usingItem = new HeldClickTrigger.Conditions(false, true, true, true);
        assertFalse(usingItem.allows(false, false, false)); assertFalse(usingItem.instantReady());
        assertTrue(new HeldClickTrigger.Conditions(true, true, true, true).instantReady());
    }
    @Test void eachSideHasItsOwnTimerAndOnlyOneSourceWins() {
        var left = new HeldClickTrigger(); var right = new HeldClickTrigger();
        assertFalse(left.active(0, true, true, 250 * MS, false));
        assertTrue(left.active(250 * MS, true, true, 250 * MS, false));
        assertFalse(right.active(250 * MS, true, true, 250 * MS, false));
        assertEquals(1, HeldClickTrigger.source(false, true, true, true));
        assertEquals(2, HeldClickTrigger.source(false, false, true, true));
        assertEquals(0, HeldClickTrigger.source(false, false, false, true));
        assertEquals(-1, HeldClickTrigger.source(true, true, true, true));
        assertEquals(-1, HeldClickTrigger.source(false, false, false, false));
    }
    @Test void crouchCancellationCannotBeBypassedAndRequiresFreshEligibleHoldAfterRelease() {
        var trigger = new HeldClickTrigger();
        assertTrue(trigger.active(0, true, true, 250 * MS, true));
        var crouched = new HeldClickTrigger.Conditions(false, true, true, true);
        assertFalse(trigger.active(100 * MS, true, crouched.allows(false, false, false), 250 * MS, crouched.instantReady()));
        assertFalse(trigger.active(500 * MS, true, true, 250 * MS, false));
        assertTrue(trigger.active(750 * MS, true, true, 250 * MS, false));
    }
}
