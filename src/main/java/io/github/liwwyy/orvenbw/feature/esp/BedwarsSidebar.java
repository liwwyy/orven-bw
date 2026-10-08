package io.github.liwwyy.orvenbw.feature.esp;

import java.util.*;
import java.util.regex.Pattern;

/** Hypixel/Pika scoreboard evidence, independent of Minecraft for replay tests. */
public record BedwarsSidebar(Phase phase, String map, int teams, Set<BedTeam> destroyed, String server) {
    public enum Phase { NONE, LOBBY, MATCH }
    private static final Pattern TEAM = Pattern.compile("(?i)^(?:[RBGYAWP]|G[Rr])?\\s*(red|blue|green|yellow|aqua|white|pink|gr[ae]y)\\s*[:：]\\s*(.*)$");
    public static String clean(String line) { return line == null ? "" : line.replaceAll("(?i)§[0-9A-FK-OR]", "").trim(); }
    public static BedwarsSidebar parse(List<String> raw) {
        List<String> lines = raw.stream().map(BedwarsSidebar::clean).toList();
        boolean bedwars = lines.stream().anyMatch(l -> l.toLowerCase(Locale.ROOT).replace(" ", "").contains("bedwars"));
        boolean pika = lines.stream().anyMatch(l -> l.toLowerCase(Locale.ROOT).contains("pika"));
        String map = ""; Set<BedTeam> found = EnumSet.noneOf(BedTeam.class), broken = EnumSet.noneOf(BedTeam.class);
        for (String line : lines) {
            if (line.toLowerCase(Locale.ROOT).startsWith("map:")) map = line.substring(4).trim();
            var match = TEAM.matcher(line);
            if (match.matches()) {
                BedTeam team = BedTeam.fromName(match.group(1)); found.add(team);
                String status = match.group(2).trim();
                if (status.startsWith("✘") || status.startsWith("✗") || status.startsWith("X") || status.startsWith("x")
                        || status.matches("\\d+(?:\\s+.*)?") || status.toLowerCase(Locale.ROOT).startsWith("eliminated")) broken.add(team);
            }
        }
        // Team rows override Map: because some servers keep the map name during play.
        Phase phase = bedwars && found.size() >= 2 ? Phase.MATCH : bedwars && !map.isEmpty() ? Phase.LOBBY : Phase.NONE;
        int teams = found.size() == 8 ? 8 : found.size() == 4 ? 4 : 0;
        return new BedwarsSidebar(phase, map, teams, Set.copyOf(broken), pika ? "pika" : "hypixel-compatible");
    }
}
