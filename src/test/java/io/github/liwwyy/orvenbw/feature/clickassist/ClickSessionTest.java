package io.github.liwwyy.orvenbw.feature.clickassist;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ClickSessionTest {
    private static final long MS = 1_000_000L;
    private static ClickSession.Options options(boolean ramp, boolean tired) {
        return new ClickSession.Options(14, 12.5, 9.5, ramp, 1000, tired, 8000, 4000, 100, 600, 8, 9);
    }
    @Test void rampHasNoDeadZoneOrIntegerStepsAndFinishesInOneSecond() {
        var session = new ClickSession(() -> 0.5);
        assertEquals(6, session.target(0, true, 6, options(true, false)));
        assertEquals(6.4, session.target(50 * MS, true, 6, options(true, false)), 1e-9);
        assertEquals(10, session.target(500 * MS, true, 6, options(true, false)));
        assertEquals(14, session.target(1000 * MS, true, 6, options(true, false)));
        session.target(1100 * MS, false, 6, options(true, false));
        assertEquals(6, session.target(1200 * MS, true, 6, options(true, false)));
    }
    @Test void everyFiveSlotCycleContainsTheRequestedTimeWeights() {
        var session = new ClickSession(() -> 0.37);
        session.target(0, true, 5, options(false, false));
        for (int cycle = 0; cycle < 10; cycle++) {
            int high = 0, medium = 0, low = 0;
            for (int slot = 0; slot < 5; slot++) {
                double target = session.target((cycle * 5000L + slot * 1000 + 500) * MS, true, 5, options(false, false));
                if (target == 14) high++; else if (target == 12.5) medium++; else if (target == 9.5) low++;
            }
            assertEquals(1, high); assertEquals(2, medium); assertEquals(2, low);
        }
    }
    @Test void levelChangesBlendInsteadOfJumping() {
        var session = new ClickSession(() -> 0.99);
        session.target(0, true, 5, options(false, false));
        assertEquals(14, session.target(950 * MS, true, 5, options(false, false)));
        assertEquals(14, session.target(1000 * MS, true, 5, options(false, false)));
        double middle = session.target(1075 * MS, true, 5, options(false, false));
        assertTrue(middle > 12.5 && middle < 14);
        assertEquals(12.5, session.target(1150 * MS, true, 5, options(false, false)));
    }
    @Test void exhaustionUsesDecimalRatesAndExpiresAfterSixHundredMilliseconds() {
        var session = new ClickSession(() -> 0.5);
        session.target(0, true, 5, options(false, true));
        assertEquals(8.5, session.target(8000 * MS, true, 5, options(false, true)));
        assertEquals(8.5, session.target(8599 * MS, true, 5, options(false, true)));
        assertTrue(session.target(8600 * MS, true, 5, options(false, true)) >= 9.5);
    }
    @Test void independentProfilesDoNotShareRampOrExhaustionState() {
        var assist = new ClickSession(() -> 0.5); var spam = new ClickSession(() -> 0.5);
        assist.target(0, true, 6, options(true, true));
        assertEquals(5, spam.target(5000 * MS, true, 5, options(true, true)));
        assertEquals(8.5, assist.target(8000 * MS, true, 6, options(true, true)));
        assertTrue(spam.target(8000 * MS, true, 5, options(true, true)) >= 9.5);
    }
    @Test void variedProfileIsBoundedNonPeriodicAndExhaustionCapsItLast() {
        var random = new java.util.Random(42);
        var session = new ClickSession(random::nextDouble);
        var varied = new ClickSession.Options(14, 12.5, 9.5, true, 1000, true,
                8000, 4000, 100, 600, 8, 9, true, 6, 22, 650, 1400);
        var values = new java.util.HashSet<Double>();
        for (long t = 0; t < 20_000 * MS; t += 50 * MS) {
            double target = session.target(t, true, 5, varied);
            assertTrue(target >= 1 && target <= 20);
            if (t > 1500 * MS && t < 8000 * MS) values.add(target);
            if (t >= 8000 * MS && t < 8600 * MS) assertTrue(target >= 8 && target <= 9);
        }
        assertTrue(values.size() > 50, "Variation must change fractional rates beyond three fixed plateaus");
        session.reset(); assertEquals(5, session.target(25_000 * MS, true, 5, varied));
    }
}
