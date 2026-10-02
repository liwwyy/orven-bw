package io.github.liwwyy.orvenbw.hud;

import io.github.liwwyy.orvenbw.feature.clickassist.ClickAssistEngine.Cps;

public final class CpsFormat {
    public static final String DEFAULT = "{base} + {boosted} = {total} {button}-Cps";
    private CpsFormat() {}
    public static String migrate(String format) {
        if (format == null || format.equals("{base} + {boosted} = {total}")) return DEFAULT;
        String migrated = format.replace("{button}{total} CPS", "{total} {button}-Cps")
                .replace("{button}{total}", "{total} {button}-Cps");
        if (!migrated.contains("{button}")) {
            migrated = migrated.replace("{total} CPS", "{total} {button}-Cps");
            if (!migrated.contains("{button}")) migrated = migrated.replace("{total}", "{total} {button}-Cps");
        }
        return migrated;
    }
    public static String render(String format, Cps cps, int button) {
        return migrate(format).replace("{button}", button == 0 ? "L" : "R")
                .replace("{base}", Integer.toString(cps.base()))
                .replace("{boosted}", Integer.toString(cps.boosted()))
                .replace("{total}", Integer.toString(cps.total()));
    }
    public static String suffix(String suffix) {
        return suffix != null && suffix.trim().equalsIgnoreCase("CPS") ? "" : suffix;
    }
}
