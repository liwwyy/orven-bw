package io.github.liwwyy.orvenbw.feature.esp;

import java.util.Locale;

public enum BedTeam {
    RED("Red", 'c', 0xff5555), BLUE("Blue", '9', 0x5555ff), GREEN("Green", 'a', 0x55ff55),
    YELLOW("Yellow", 'e', 0xffff55), AQUA("Aqua", 'b', 0x55ffff), WHITE("White", 'f', 0xffffff),
    PINK("Pink", 'd', 0xff55ff), GRAY("Gray", '7', 0xaaaaaa), UNKNOWN("Unknown team", '7', 0xaaaaaa);
    public final String label;
    public final char code;
    public final int rgb;
    BedTeam(String label, char code, int rgb) { this.label = label; this.code = code; this.rgb = rgb; }
    public String initial() { return this == UNKNOWN ? "?" : label.substring(0, 1); }
    public static BedTeam fromColor(int rgb) {
        int value = rgb & 0xffffff;
        for (BedTeam team : values()) if (team != UNKNOWN && team.rgb == value) return team;
        switch (value) { case 0xaa0000: return RED; case 0x0000aa: return BLUE; case 0x00aa00: return GREEN;
            case 0x00aaaa: return AQUA; case 0xaa00aa: return PINK; case 0x555555: return GRAY; }
        return UNKNOWN;
    }
    public static BedTeam fromName(String name) {
        String normalized = name.toLowerCase(Locale.ROOT);
        if (normalized.equals("grey")) return GRAY;
        for (BedTeam team : values()) if (team.label.toLowerCase(Locale.ROOT).equals(normalized)) return team;
        return UNKNOWN;
    }
}
