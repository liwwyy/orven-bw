package io.github.liwwyy.orvenbw.feature.autotool;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AutoToolStateTest {
    private static final long MS = 1_000_000L;
    @Test void variationIsSampledOnceAndStaysWithinConfiguredBounds() {
        for (double sample : new double[]{0, .5, 1}) {
            var state = new AutoToolState();
            int[] samples = {0};
            long due = (long) (160 + (sample * 2 - 1) * 40) * MS;
            assertEquals(-1, state.update(0, "wool", 2, 0, true, true, 160, 40, 0, () -> { samples[0]++; return sample; }));
            assertEquals(-1, state.update(due - 1, "wool", 2, 0, true, true, 160, 40, 0, () -> fail("Resampled delay")));
            assertEquals(2, state.update(due, "wool", 2, 0, true, true, 160, 40, 0, () -> fail("Resampled delay")));
            assertEquals(1, samples[0]);
        }
    }
    @Test void hoverAndSwitchDelayRunTogetherAndChangingBlocksRestartsTheWait() {
        var state = new AutoToolState();
        assertEquals(-1, state.update(0, "one", 2, 0, true, true, 160, 0, 200, () -> .5));
        assertEquals(-1, state.update(160 * MS, "one", 2, 0, true, true, 160, 0, 200, () -> .5));
        assertEquals(2, state.update(200 * MS, "one", 2, 0, true, true, 160, 0, 200, () -> .5));
        assertEquals(-1, state.update(210 * MS, "two", 2, 0, true, true, 160, 0, 200, () -> .5));
        assertEquals(2, state.update(410 * MS, "two", 2, 0, true, true, 160, 0, 200, () -> .5));
    }
    @Test void releasingCancelsPendingSwitchAndHoverOnlyModeStillHasSwitchDelay() {
        var state = new AutoToolState();
        state.update(0, "one", 2, 0, true, true, 160, 0, 0, () -> .5);
        assertEquals(-1, state.update(200 * MS, "one", 2, 0, false, true, 160, 0, 0, () -> .5));
        assertEquals(-1, state.update(300 * MS, "one", 2, 0, false, false, 160, 0, 0, () -> .5));
        assertEquals(2, state.update(460 * MS, "one", 2, 0, false, false, 160, 0, 0, () -> .5));
    }
    @Test void switchBackIsOptionalAndManualRequestsOverrideOnlyWhenEnabled() {
        var state = new AutoToolState();
        state.switched(1, 3); state.switched(3, 5);
        state.requestRestoreSlot(7, false);
        assertEquals(1, state.finish(5, true)); assertFalse(state.ownsSlot());
        state.switched(1, 3); state.requestRestoreSlot(7, true);
        assertEquals(7, state.finish(3, true));
        state.switched(1, 3); assertEquals(-1, state.finish(3, false));
        state.switched(1, 3); assertEquals(-1, state.finish(8, true), "Never overwrite a slot chosen externally");
    }
    @Test void resetDropsSlotOwnershipAndNegativeDelayClampsToZero() {
        var state = new AutoToolState();
        assertEquals(2, state.update(0, "one", 2, 0, true, true, 0, 40, 0, () -> 0));
        state.switched(0, 2); state.reset();
        assertFalse(state.ownsSlot()); assertEquals(-1, state.finish(2, true));
    }
}
