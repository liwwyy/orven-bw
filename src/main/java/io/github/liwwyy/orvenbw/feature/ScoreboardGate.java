package io.github.liwwyy.orvenbw.feature;

import io.github.liwwyy.orvenbw.config.OrvenConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.scoreboard.ScoreboardScore;
import net.minecraft.scoreboard.team.Team;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class ScoreboardGate {
    private ScoreboardGate() {}
    public static boolean allows(Minecraft mc, OrvenConfig config) {
        if (!config.modEnabled) return false;
        if (!config.scoreboardOnly) return true;
        return sidebarMatches(mc, config.scoreboardWord);
    }
    public static boolean sidebarMatches(Minecraft mc, String word) {
        if (mc.world == null || mc.player == null) return false;
        var board = mc.world.getScoreboard();
        ScoreboardObjective objective = null;
        Team team = board.getTeamOfMember(mc.player.getName());
        if (team != null && team.getColor().getId() >= 0) objective = board.getDisplayObjective(3 + team.getColor().getId());
        if (objective == null) objective = board.getDisplayObjective(1);
        if (objective == null) return false;
        List<String> visible = new ArrayList<>();
        visible.add(objective.getDisplayName());
        List<ScoreboardScore> scores = new ArrayList<>();
        for (ScoreboardScore score : board.getScores(objective)) {
            if (score.getOwner() != null && !score.getOwner().startsWith("#")) scores.add(score);
        }
        for (int i = Math.max(0, scores.size() - 15); i < scores.size(); i++) {
            String owner = scores.get(i).getOwner();
            visible.add(Team.getMemberDisplayName(board.getTeamOfMember(owner), owner));
        }
        return matches(visible, word);
    }
    public static boolean matches(List<String> lines, String word) {
        if (word == null || word.isBlank()) return false;
        String needle = clean(word).trim().toLowerCase(Locale.ROOT);
        if (needle.isEmpty()) return false;
        return lines.stream().anyMatch(line -> clean(line).toLowerCase(Locale.ROOT).contains(needle));
    }
    private static String clean(String s) { return s == null ? "" : s.replaceAll("(?i)§[0-9A-FK-OR]", ""); }
}
