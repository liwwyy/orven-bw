package io.github.liwwyy.orvenbw.feature;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class ScoreboardGateTest {
    @Test void matchesVisibleFormattedLinesIgnoringCase() {
        assertTrue(ScoreboardGate.matches(List.of("BED WARS", "§cR§fed Team: 1"), "Red"));
        assertTrue(ScoreboardGate.matches(List.of("Red wins"), " red "));
        assertFalse(ScoreboardGate.matches(List.of("Blue Team"), "Red"));
    }
    @Test void absentOrEmptyWordsNeverEnableTheGate() {
        assertFalse(ScoreboardGate.matches(List.of(), "Red"));
        assertFalse(ScoreboardGate.matches(List.of("Red"), " "));
        assertFalse(ScoreboardGate.matches(List.of("Red"), null));
        assertFalse(ScoreboardGate.matches(List.of("Red"), "§c"));
    }
}
