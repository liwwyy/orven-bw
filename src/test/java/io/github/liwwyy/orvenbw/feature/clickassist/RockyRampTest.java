package io.github.liwwyy.orvenbw.feature.clickassist;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RockyRampTest {
    @Test void startsAndMiddleRiseMatchRangesAndEverySessionUsesDifferentBursts() {
        var aRandom = new java.util.Random(1); var bRandom = new java.util.Random(2);
        var a = new RockyRamp(aRandom::nextDouble, 14, 1); var b = new RockyRamp(bRandom::nextDouble, 14, 1);
        assertTrue(a.target(0)>=3.1 && a.target(0)<=4.6);
        assertTrue(a.target(.45)>=6.3 && a.target(.45)<=9.7);
        assertNotEquals(a.target(.4), b.target(.4));
        assertEquals(14, a.target(1));
        double previous = 0;
        for (int i = 0; i <= 100; i++) {
            double rate = a.target(i/100.0); assertTrue(rate>=previous && rate<=14); previous=rate;
        }
        assertNotEquals(a.target(.2)-a.target(.1), a.target(.4)-a.target(.3), .1);
        var low = new RockyRamp(aRandom::nextDouble, 4, 1); assertEquals(4, low.target(1));
    }
    @Test void physicalRampNeverRequiresDroppingExistingManualRate() {
        var ramp = new RockyRamp(() -> .5, 14, 8);
        assertEquals(8, ramp.target(0)); assertEquals(14, ramp.target(1));
    }
}
