package io.github.liwwyy.orvenbw.feature.clickassist;

import java.util.Collection;

/** Tab-list membership is the deliberately narrow NPC rule, using profile usernames. */
public final class NearbyPlayers {
    private NearbyPlayers() {}
    public static boolean qualifies(boolean self, boolean alive, boolean spectator,
                                    double distanceSquared, String username, Collection<String> tabNames) {
        return !self && alive && !spectator && distanceSquared <= 16.0 && username != null
                && tabNames != null && tabNames.stream().anyMatch(username::equalsIgnoreCase);
    }
}
