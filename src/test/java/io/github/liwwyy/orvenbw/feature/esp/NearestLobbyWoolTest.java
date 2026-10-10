package io.github.liwwyy.orvenbw.feature.esp;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class NearestLobbyWoolTest {
    private final BedGeometry bed=new BedGeometry(new BedGeometry.Pos(0,64,0),1,0);
    @Test void nearestWinsRegardlessOfScanOrderAndBudget() {
        var search=new NearestLobbyWool(bed,16);int spent=0;
        while(!search.complete()) spent+=search.scan(1,p->p.equals(new BedGeometry.Pos(2,64,0))?14:p.equals(new BedGeometry.Pos(4,64,0))?11:-1);
        assertEquals(BedTeam.RED,search.team());assertTrue(spent<100);
    }
    @Test void equalDistanceDifferentColoursStayUnassigned() {
        var search=new NearestLobbyWool(bed,16);
        search.scan(100,p->p.equals(new BedGeometry.Pos(0,63,0))?14:p.equals(new BedGeometry.Pos(1,63,0))?11:-1);
        assertTrue(search.complete());assertTrue(search.conflict());assertEquals(BedTeam.UNKNOWN,search.team());
    }
    @Test void differentShadesOfTheSameTeamStillConflict() {
        var search=new NearestLobbyWool(bed,16);
        search.scan(100,p->p.equals(new BedGeometry.Pos(0,63,0))?5:p.equals(new BedGeometry.Pos(1,63,0))?13:-1);
        assertTrue(search.conflict());assertEquals(BedTeam.UNKNOWN,search.team());
    }
    @Test void missingNearerChunksAndUnsupportedColoursDoNotGuess() {
        var search=new NearestLobbyWool(bed,2);
        search.scan(100,p->p.equals(bed.foot())?-2:p.equals(new BedGeometry.Pos(0,63,0))?14:-1);
        assertTrue(search.unknown());assertEquals(BedTeam.UNKNOWN,search.team());
        assertEquals(BedTeam.UNKNOWN,NearestLobbyWool.fromWool(1));assertEquals(BedTeam.AQUA,NearestLobbyWool.fromWool(9));
    }
}
