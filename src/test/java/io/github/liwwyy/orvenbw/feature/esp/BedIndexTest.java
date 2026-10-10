package io.github.liwwyy.orvenbw.feature.esp;

import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class BedIndexTest {
    private BedGeometry bed(int x, int dx, int dz) { return new BedGeometry(new BedGeometry.Pos(x,64,20),dx,dz); }
    @Test void everyOrientationHasEightUniqueDefenceSlotsAndAnAccurateFoundationAnchor() {
        for (var direction : List.of(new int[]{1,0},new int[]{-1,0},new int[]{0,1},new int[]{0,-1})) {
            var bed = bed(10,direction[0],direction[1]);
            assertEquals(8,new HashSet<>(bed.defence()).size());
            assertFalse(bed.defence().contains(bed.foot())); assertFalse(bed.defence().contains(bed.head()));
            assertEquals(2,bed.defence().stream().filter(p -> p.y()==65).count());
            assertEquals(64,bed.y()); assertEquals(10.5 + direction[0]*.5,bed.x());
            assertEquals(20.5 + direction[1]*.5,bed.z());
        }
        assertThrows(IllegalArgumentException.class,() -> bed(1,1,1));
    }
    @Test void observationDeduplicatesScoutedBedsAndEitherHalfBreaksWithoutResurrection() {
        var index = new BedIndex(); var geometry = bed(1,1,0);
        index.assignScouted(geometry,BedTeam.RED); assertFalse(index.beds().iterator().next().confirmed);
        var observed = index.observe(geometry); assertTrue(observed.confirmed); assertEquals(BedTeam.RED,observed.team);
        index.observe(geometry); assertEquals(1,index.beds().size());
        index.broken(geometry.head()); assertTrue(index.beds().isEmpty());
        index.assignScouted(geometry,BedTeam.RED); assertTrue(index.beds().isEmpty()); assertNull(index.observe(geometry));
        index.clear(); assertNotNull(index.observe(geometry));
    }
    @Test void destroyedTeamCannotReappearEvenBeforeItsBedWasObserved() {
        var index = new BedIndex(); index.broken(BedTeam.BLUE);
        index.assignScouted(bed(1,0,1),BedTeam.BLUE); assertTrue(index.beds().isEmpty());
        index.assignScouted(bed(2,0,1),BedTeam.RED); assertEquals(1,index.beds().size());
    }
    @Test void unloadedDefenceIsUnknownAndDoesNotDeleteBeds() {
        var index = new BedIndex(); var geometry = bed(1,1,0); var observed = index.observe(geometry);
        Set<BedGeometry.Pos> obsidian = new HashSet<>(geometry.defence().subList(0,4));
        index.updateDefence(p -> obsidian.contains(p) ? 1 : 0); assertEquals("4/8",observed.count());
        index.updateDefence(p -> obsidian.contains(p) ? 1 : -1); assertEquals("4/8?",observed.count());
        index.updateDefence(p -> -1); assertEquals(1,index.confirmedCount()); assertEquals(0,observed.knownDefence);
    }
    @Test void validatedLobbyTeamsApplyToUnloadedBedsAndSurviveConfirmation() {
        var index=new BedIndex();var geometry=bed(0,1,0);
        index.assignScouted(geometry,BedTeam.BLUE);var cached=index.beds().iterator().next();
        assertTrue(cached.teamObserved);assertFalse(cached.confirmed);
        assertEquals(BedTeam.BLUE,index.observe(geometry).team);
        index.broken(BedTeam.BLUE);assertTrue(index.beds().isEmpty());
        index.assignScouted(geometry,BedTeam.BLUE);assertTrue(index.beds().isEmpty());
    }
    @Test void ambiguousWoolLeavesUnknownWithoutInventingATeam() {
        var index=new BedIndex();var geometry=bed(0,1,0);
        index.observe(geometry);index.assignScouted(geometry,BedTeam.UNKNOWN);
        var bed=index.beds().iterator().next();assertFalse(bed.teamObserved);assertEquals(BedTeam.UNKNOWN,bed.team);
    }
}
