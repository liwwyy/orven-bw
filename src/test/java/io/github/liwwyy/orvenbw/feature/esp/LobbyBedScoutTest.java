package io.github.liwwyy.orvenbw.feature.esp;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class LobbyBedScoutTest {
    private BedGeometry bed(int x) {return new BedGeometry(new BedGeometry.Pos(x,64,0),1,0);}
    @Test void oneMatchingBedKeepsAllScoutedPositionsForTheMatch() {
        var scout=new LobbyBedScout();scout.beginLobby();scout.observe(bed(0),BedTeam.RED);scout.observe(bed(100),BedTeam.BLUE);
        scout.depart(0);assertFalse(scout.validate(500_000_000L,b->false));
        assertTrue(scout.validate(1_000_000_000L,b->b.equals(bed(0))));
        scout.expire(60_000_000_000L);assertEquals(2,scout.matchBeds().size());
        assertTrue(scout.validate(60_000_000_000L,b->false));
    }
    @Test void timeoutAndNewLobbyDiscardOldPositions() {
        var scout=new LobbyBedScout();scout.beginLobby();scout.observe(bed(0),BedTeam.RED);scout.depart(100);
        assertFalse(scout.validate(100+LobbyBedScout.TRANSFER_NANOS,b->true));assertFalse(scout.pending());
        scout.beginLobby();scout.observe(bed(100),BedTeam.BLUE);scout.depart(200);
        assertFalse(scout.validate(201,b->b.equals(bed(0))));assertTrue(scout.validate(202,b->b.equals(bed(100))));
    }
    @Test void worldGapCannotExtendDeadlineAndBrokenLobbyBedsAreNotTransferred() {
        var scout=new LobbyBedScout();scout.beginLobby();scout.observe(bed(0),BedTeam.RED);scout.remove(bed(0).foot());scout.depart(0);assertFalse(scout.pending());
        scout.beginLobby();scout.observe(bed(0),BedTeam.RED);scout.depart(0);scout.depart(1_000_000_000L);
        assertFalse(scout.validate(1_600_000_000L,b->true));
    }
}
