package io.github.liwwyy.orvenbw.feature.clickassist;

import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class NearbyPlayersTest {
    @Test void tabMembershipUsesUndecoratedNamesAndNoNpcNameHeuristics() {
        assertFalse(NearbyPlayers.qualifies(false, true, false, 4, "Shopkeeper", List.of("Steve")));
        assertTrue(NearbyPlayers.qualifies(false, true, false, 4, "Steve", List.of("steve")));
        assertTrue(NearbyPlayers.qualifies(false, true, false, 4, "NPCSteve", List.of("NPCSteve")));
        assertFalse(NearbyPlayers.qualifies(false, true, false, 4, "Steve", List.of("[VIP] Steve")));
        assertFalse(NearbyPlayers.qualifies(false, true, false, 4, "Steve", null));
        assertFalse(NearbyPlayers.qualifies(false, true, false, 4, "Steve", List.of()));
    }
    @Test void selfDeadSpectatorsAndOutOfRangePlayersDoNotCount() {
        var tab = List.of("Steve");
        assertTrue(NearbyPlayers.qualifies(false, true, false, 16, "Steve", tab));
        assertFalse(NearbyPlayers.qualifies(false, true, false, 16.001, "Steve", tab));
        assertFalse(NearbyPlayers.qualifies(true, true, false, 0, "Steve", tab));
        assertFalse(NearbyPlayers.qualifies(false, false, false, 4, "Steve", tab));
        assertFalse(NearbyPlayers.qualifies(false, true, true, 4, "Steve", tab));
    }
}
