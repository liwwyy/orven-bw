package io.github.liwwyy.orvenbw.feature.hiteffects;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class HitPopupTest {
    private static final long MS = 1_000_000L;

    @Test void confirmedHitsFlyInTheirChosenDirectionAndFadeAfterHalfSecond() {
        var left = new HitPopup(0, 2, 2.0, false, 0, -1, 0);
        var right = new HitPopup(0, 2, 2.0, false, 0, 1, 0);
        assertEquals(0.0, left.x(0, 500 * MS), 0.0001);
        assertTrue(left.x(200 * MS, 500 * MS) < -20);
        assertTrue(right.x(200 * MS, 500 * MS) > 20);
        assertTrue(left.y(200 * MS, 500 * MS) < 0);
        assertEquals(255, left.alpha(300 * MS, 500 * MS));
        assertTrue(left.alpha(450 * MS, 500 * MS) < 128);
        assertEquals(0, left.alpha(500 * MS, 500 * MS));
    }

    @Test void criticalComboAndWeakDamageGetDistinctLabels() {
        assertEquals("3 hits -2 CRIT x2", new HitPopup(0, 3, 2, true, 2, 1, 0).text());
        assertEquals("1 hit -0.5 WEAK", new HitPopup(0, 1, .5, false, 0, -1, 0).text());
        assertEquals("1 hit -1", new HitPopup(0, 1, 1, false, 0, -1, 0).text());
    }
}
