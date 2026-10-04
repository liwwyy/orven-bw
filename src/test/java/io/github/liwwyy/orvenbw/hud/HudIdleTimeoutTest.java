package io.github.liwwyy.orvenbw.hud;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class HudIdleTimeoutTest {
    @Test void idleClockUsesTheLastClickAndNewInputShowsHudImmediately() {
        long ms = 1_000_000;
        assertFalse(HudIdleTimeout.visible(true, 0, Long.MIN_VALUE, 1000));
        assertTrue(HudIdleTimeout.visible(false, 0, Long.MIN_VALUE, 1000));
        assertTrue(HudIdleTimeout.visible(true, 999*ms, 0, 1000));
        assertFalse(HudIdleTimeout.visible(true, 1000*ms, 0, 1000));
        assertTrue(HudIdleTimeout.visible(true, 1100*ms, 1100*ms, 1000));
        assertTrue(HudIdleTimeout.visible(true, 2500*ms, 1100*ms, 2000));
        assertFalse(HudIdleTimeout.visible(true, 3100*ms, 1100*ms, 2000));
    }
}
