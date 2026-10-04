package io.github.liwwyy.orvenbw.feature.clickassist;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ClickAssistEngineTest {
    private static final long MS = 1_000_000L;
    @Test void warmupUsesCadenceBeforeAFullSecondOfPresses() {
        var engine = new ClickAssistEngine(() -> 0.5);
        engine.physicalClick(0, 0);
        assertFalse(engine.manuallyActive(0, 0, 4));
        engine.physicalClick(0, 200 * MS);
        assertTrue(engine.manuallyActive(0, 200 * MS, 4));
        assertEquals(5, engine.manualRate(0, 200 * MS), 1e-9);
        assertFalse(engine.manuallyActive(0, 601 * MS, 4));
        var threshold = new ClickAssistEngine(() -> 0.5);
        threshold.physicalClick(0, 0); threshold.physicalClick(0, 250 * MS);
        assertFalse(threshold.manuallyActive(0, 250 * MS, 4));
    }
    @Test void decimalTargetsProduceTheirLongTermRatesOnTwentyHzTicks() {
        for (double rate : new double[]{8, 9.5, 12.5, 14}) {
            var engine = new ClickAssistEngine(() -> 0.5);
            int count = 0;
            for (long t = 0; t < 20_000 * MS; t += 50 * MS)
                if (engine.poll(1, t, rate, true, false, 0)) count++;
            assertEquals(rate, count / 20.0, 0.1);
            assertEquals(0, engine.cps(0, 20_000 * MS).total());
        }
    }
    @Test void changingRateDoesNotWaitForOldRollingCountsToExpire() {
        var engine = new ClickAssistEngine(() -> 0.5);
        for (long t = 0; t < 1000 * MS; t += 50 * MS) engine.poll(0, t, 14, true, false, 0);
        int count = 0;
        for (long t = 1000 * MS; t < 2000 * MS; t += 50 * MS)
            if (engine.poll(0, t, 9.5, true, false, 0)) count++;
        assertTrue(count >= 9 && count <= 10, "Rate change should pace, not freeze: " + count);
    }
    @Test void firstBoostWaitsOnlyForConfiguredInitialDelay() {
        var engine = new ClickAssistEngine(() -> 0.99);
        assertFalse(engine.poll(0, 0, 1, true, true, 40 * MS));
        assertFalse(engine.poll(0, 39 * MS, 1, true, true, 40 * MS));
        assertTrue(engine.poll(0, 40 * MS, 1, true, true, 40 * MS));
    }
    @Test void staleWorkAndDisabledInputAreNeverReplayed() {
        var engine = new ClickAssistEngine(() -> 0.5);
        assertTrue(engine.poll(0, 0, 14, true, false, 0));
        assertFalse(engine.poll(0, 500 * MS, 14, true, false, 0));
        assertFalse(engine.poll(0, 550 * MS, 14, true, false, 0));
        assertTrue(engine.poll(0, 600 * MS, 14, true, false, 0));
        assertFalse(engine.poll(0, 650 * MS, 14, false, false, 0));
        assertFalse(engine.poll(0, 700 * MS, 14, true, false, 40 * MS));
    }
    @Test void physicalAndGeneratedCountsExpireAndResetIndependently() {
        var engine = new ClickAssistEngine(() -> 0.5);
        engine.physicalClick(0, 0);
        assertTrue(engine.poll(0, 10 * MS, 5, true, false, 0));
        assertEquals(new ClickAssistEngine.Cps(0, 1), engine.cps(0, 1000 * MS));
        assertEquals(new ClickAssistEngine.Cps(0, 0), engine.cps(0, 1010 * MS));
        engine.physicalClick(1, 1100 * MS); engine.reset();
        assertEquals(0, engine.cps(1, 1100 * MS).total());
        assertFalse(engine.manuallyActive(1, 1100 * MS, 0));
    }
    @Test void targetAssistanceCanSupplyMoreThanOneBoostPerPhysicalPress() {
        var engine = new ClickAssistEngine(() -> 0.5);
        int generated = 0;
        for (long t = 0; t < 10_000 * MS; t += 50 * MS) {
            if (t % (200 * MS) == 0) engine.physicalClick(0, t);
            boolean active = engine.manuallyActive(0, t, 4);
            if (engine.poll(0, t, 14 - engine.manualRate(0, t), active, false, 0)) generated++;
        }
        assertTrue(generated > 50, "Must exceed the former one-extra-per-press limit");
        assertEquals(5, engine.cps(0, 9950 * MS).base());
        assertEquals(14, engine.cps(0, 9950 * MS).total(), 1);
    }
    @Test void timingVariationAppliesToTheSpamScheduler() {
        int[] index = {0}; double[] draws = {0, 0.95, 0.3, 0.9};
        var varied = new ClickAssistEngine(() -> draws[index[0]++ % draws.length]);
        var fixed = new ClickAssistEngine(() -> 0.5);
        StringBuilder a = new StringBuilder(), b = new StringBuilder();
        for (long t = 0; t < 5000 * MS; t += 50 * MS) {
            a.append(varied.poll(0, t, 12.5, true, true, 0) ? '1' : '0');
            b.append(fixed.poll(0, t, 12.5, true, false, 0) ? '1' : '0');
        }
        assertNotEquals(a.toString(), b.toString());
    }
    @Test void resumedManualClickingGetsTheSameFastWarmupAsTheFirstSession() {
        var engine = new ClickAssistEngine(() -> 0.5);
        engine.physicalClick(0, 0); engine.physicalClick(0, 200 * MS);
        assertTrue(engine.manuallyActive(0, 200 * MS, 4));
        engine.physicalClick(0, 3000 * MS);
        assertFalse(engine.manuallyActive(0, 3000 * MS, 4));
        engine.physicalClick(0, 3200 * MS);
        assertTrue(engine.manuallyActive(0, 3200 * MS, 4));
        assertEquals(5, engine.manualRate(0, 3200 * MS));
    }
    @Test void hudDominanceSwitchesFromRecentCombinedOutputBeforeOldCountsExpire() {
        var engine = new ClickAssistEngine(() -> 0.5);
        for (long t = 0; t <= 400 * MS; t += 50 * MS) engine.physicalClick(0, t);
        assertEquals(0, engine.dominantButton(400 * MS, 1));
        engine.physicalClick(1, 600 * MS); engine.physicalClick(1, 650 * MS);
        assertEquals(1, engine.dominantButton(650 * MS, 0));
        assertTrue(engine.cps(0, 650 * MS).total() > engine.cps(1, 650 * MS).total());
        for (long t = 700 * MS; t <= 900 * MS; t += 50 * MS) engine.poll(0, t, 20, true, false, 0);
        assertEquals(0, engine.dominantButton(900 * MS, 1));
        engine.reset(); assertEquals(1, engine.dominantButton(1000 * MS, 1));
    }
}
