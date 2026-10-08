package io.github.liwwyy.orvenbw.feature.esp;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
class BedwarsSidebarTest {
    @Test void lobbyUsesMapAndMatchRowsOverrideIt() {
        var lobby = BedwarsSidebar.parse(List.of("§eBED WARS","§fMap: §aLighthouse","Waiting..."));
        assertEquals(BedwarsSidebar.Phase.LOBBY,lobby.phase()); assertEquals("Lighthouse",lobby.map());
        var match = BedwarsSidebar.parse(List.of("BED WARS","Map: Lighthouse","R Red: ✔","B Blue: 2","G Green: ✘","Y Yellow: ✔"));
        assertEquals(BedwarsSidebar.Phase.MATCH,match.phase()); assertEquals(4,match.teams());
        assertEquals(java.util.Set.of(BedTeam.BLUE,BedTeam.GREEN),match.destroyed());
        assertEquals(BedwarsSidebar.Phase.NONE,BedwarsSidebar.parse(List.of("SKYWARS","Map: Tribute")).phase());
    }
    @Test void pikaFullNamesAndEightTeamRowsAreRecognizedWithoutGuessingSoloVersusDoubles() {
        var match = BedwarsSidebar.parse(List.of("BEDWARS","play.pika-network.net","Red: ✔","Blue: ✔","Green: ✔","Yellow: ✔","Aqua: ✔","White: ✔","Pink: ✔","Gray: x"));
        assertEquals(BedwarsSidebar.Phase.MATCH,match.phase()); assertEquals("pika",match.server()); assertEquals(8,match.teams());
        assertTrue(match.destroyed().contains(BedTeam.GRAY));
    }
}
