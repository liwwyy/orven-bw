package io.github.liwwyy.orvenbw.feature.esp;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class AlertTrackerTest {
    @Test void fireballOnlyTransitionsAndArmourDeduplicateUntilReset() {
        var tracker = new AlertTracker(); assertTrue(tracker.held("p","Fireball")); assertFalse(tracker.held("p","Fireball"));
        assertFalse(tracker.held("p",null)); assertTrue(tracker.held("p","Fireball"));
        assertTrue(tracker.held("p","Ender Pearl")); assertTrue(tracker.armor("p")); assertFalse(tracker.armor("p"));
        tracker.clear(); assertTrue(tracker.armor("p")); assertTrue(tracker.held("p","Fireball"));
    }
    @Test void defenceWarningsFollowIncreasingEvidenceWithoutRepeatingOnChunkReload() {
        var tracker = new AlertTracker(); var index = new BedIndex(); var bed = index.observe(new BedGeometry(new BedGeometry.Pos(0,60,0),1,0));
        assertFalse(tracker.defence(bed)); bed.obsidian=4; assertTrue(tracker.defence(bed)); assertFalse(tracker.defence(bed));
        bed.obsidian=0; assertFalse(tracker.defence(bed)); bed.obsidian=4; assertFalse(tracker.defence(bed));
        bed.obsidian=8; assertTrue(tracker.defence(bed)); tracker.clear(); assertTrue(tracker.defence(bed));
    }
}
