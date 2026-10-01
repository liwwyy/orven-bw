package io.github.liwwyy.orvenbw.feature.clickassist;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ClickAssistEngineTest {
    private final ClickAssistEngine engine = new ClickAssistEngine(() -> 0.0);
    private final ClickAssistEngine.Options defaults = new ClickAssistEngine.Options(4, 13, 100, 40_000_000L, false);
    private static final long MS = 1_000_000L;

    @Test void activatesStrictlyAboveFourPhysicalClicksAndWaitsForDelay() {
        for (int i = 0; i < 4; i++) engine.physicalClick(0, i * 100 * MS, defaults, true);
        assertFalse(engine.pollBoost(0, 350 * MS, defaults, true));
        engine.physicalClick(0, 400 * MS, defaults, true);
        assertFalse(engine.pollBoost(0, 439 * MS, defaults, true));
        assertTrue(engine.pollBoost(0, 440 * MS, defaults, true));
        assertEquals(new ClickAssistEngine.Cps(5, 1), engine.cps(0, 440 * MS));
        assertFalse(engine.pollBoost(0, 500 * MS, defaults, true));
    }

    @Test void holdsRollingCapWithoutSuppressingPhysicalInput() {
        var options = new ClickAssistEngine.Options(0, 13, 100, 10 * MS, false);
        for (int i = 0; i < 30; i++) {
            long now = i * 20 * MS;
            engine.physicalClick(0, now, options, true);
            int before = engine.cps(0, now + 10 * MS).total();
            if (engine.pollBoost(0, now + 10 * MS, options, true)) assertTrue(before < 13);
        }
        assertEquals(30, engine.cps(0, 610 * MS).base());
        assertTrue(engine.cps(0, 610 * MS).boosted() <= 6);
        assertFalse(engine.pollBoost(0, 610 * MS, options, true));
    }

    @Test void pendingBudgetPreventsOverQueueingAndRechecksLoweredCap() {
        var high = new ClickAssistEngine.Options(0, 4, 100, 40 * MS, false);
        engine.physicalClick(0, 0, high, true);
        engine.physicalClick(0, MS, high, true);
        engine.physicalClick(0, 2 * MS, high, true);
        var lowered = new ClickAssistEngine.Options(0, 3, 100, 40 * MS, false);
        assertFalse(engine.pollBoost(0, 50 * MS, lowered, true));
        assertEquals(0, engine.cps(0, 50 * MS).boosted());
    }

    @Test void physicalAndBoostedCountsExpireAtOneSecond() {
        var opt = new ClickAssistEngine.Options(0, 13, 100, 10 * MS, false);
        engine.physicalClick(0, 0, opt, true);
        assertTrue(engine.pollBoost(0, 10 * MS, opt, true));
        assertEquals(new ClickAssistEngine.Cps(0, 1), engine.cps(0, 1000 * MS));
        assertEquals(new ClickAssistEngine.Cps(0, 0), engine.cps(0, 1010 * MS));
    }

    @Test void losingEligibilityCancelsPendingAndDoesNotReplayOnReentry() {
        var opt = new ClickAssistEngine.Options(0, 13, 100, 40 * MS, false);
        engine.physicalClick(0, 0, opt, true);
        assertFalse(engine.pollBoost(0, 20 * MS, opt, false));
        assertFalse(engine.pollBoost(0, 50 * MS, opt, true));
        engine.physicalClick(0, 100 * MS, opt, false);
        assertFalse(engine.pollBoost(0, 150 * MS, opt, true));
    }

    @Test void longStallDropsBoostsInsteadOfBursting() {
        var opt = new ClickAssistEngine.Options(0, 30, 100, 40 * MS, false);
        for (int i = 0; i < 8; i++) engine.physicalClick(0, i * MS, opt, true);
        assertFalse(engine.pollBoost(0, 300 * MS, opt, true));
        assertEquals(0, engine.cps(0, 300 * MS).boosted());
    }

    @Test void zeroChanceNeverQueuesAndProbabilityBoundaryIsExclusive() {
        var zero = new ClickAssistEngine.Options(0, 13, 0, 10 * MS, false);
        engine.physicalClick(0, 0, zero, true);
        assertFalse(engine.pollBoost(0, 20 * MS, zero, true));
        var boundary = new ClickAssistEngine(() -> 0.8);
        var eighty = new ClickAssistEngine.Options(0, 13, 80, 10 * MS, false);
        boundary.physicalClick(0, 0, eighty, true);
        assertFalse(boundary.pollBoost(0, 20 * MS, eighty, true));
    }

    @Test void channelsAreIndependentAndResetClearsBoth() {
        var opt = new ClickAssistEngine.Options(0, 13, 100, 10 * MS, false);
        engine.physicalClick(0, 0, opt, true);
        assertTrue(engine.pollBoost(0, 20 * MS, opt, true));
        assertFalse(engine.pollBoost(1, 20 * MS, opt, true));
        assertEquals(0, engine.cps(1, 20 * MS).total());
        engine.physicalClick(1, 30 * MS, opt, true);
        engine.reset();
        assertEquals(0, engine.cps(0, 40 * MS).total());
        assertEquals(0, engine.cps(1, 40 * MS).total());
        assertFalse(engine.pollBoost(1, 50 * MS, opt, true));
    }

    @Test void assistanceNeverRunsWithoutNewPhysicalInput() {
        var opt = new ClickAssistEngine.Options(0, 13, 100, 10 * MS, false);
        engine.physicalClick(0, 0, opt, true);
        assertTrue(engine.pollBoost(0, 20 * MS, opt, true));
        for (long t = 50; t < 2000; t += 50) assertFalse(engine.pollBoost(0, t * MS, opt, true));
    }
}
