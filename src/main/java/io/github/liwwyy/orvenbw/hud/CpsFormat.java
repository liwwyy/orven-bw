package io.github.liwwyy.orvenbw.hud;

import io.github.liwwyy.orvenbw.feature.clickassist.ClickAssistEngine.Cps;

public final class CpsFormat {
    public static final String DEFAULT = "{base} + {boosted} = {left} | {right}";
    private CpsFormat() {}
    public static String migrate(String format) {
        if (format == null || format.equals("{base} + {boosted} = {total}")
                || format.equals("{base} + {boosted} = {total} {button}-Cps")
                || format.equals("{base} + {boosted} = {total} {button}-CPS")) return DEFAULT;
        return format.replace("{button}", "").replace("-Cps", "").replace("-CPS", "").replace("Cps", "CPS");
    }
    public static String render(String format, Cps left, Cps right, int dominant, boolean label) {
        Cps selected = dominant == 1 ? right : left;
        String text = migrate(format).replace("{left}", Integer.toString(left.total()))
                .replace("{right}", Integer.toString(right.total()))
                .replace("{base}", Integer.toString(selected.base()))
                .replace("{boosted}", Integer.toString(selected.boosted()))
                .replace("{total}", Integer.toString(selected.total()));
        text = text.replaceAll("(?i)\\s*CPS\\b", "").trim();
        return text + (label ? " CPS" : "");
    }
    public static String suffix(String suffix) {
        return suffix != null && suffix.trim().equalsIgnoreCase("CPS") ? "" : suffix;
    }
}
