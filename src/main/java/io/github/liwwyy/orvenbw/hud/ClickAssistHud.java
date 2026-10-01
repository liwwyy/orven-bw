package io.github.liwwyy.orvenbw.hud;

import io.github.liwwyy.orvenbw.OrvenBw;
import io.github.liwwyy.orvenbw.feature.clickassist.ClickAssistEngine.Cps;
import org.polyfrost.oneconfig.api.config.v1.annotations.Dropdown;
import org.polyfrost.oneconfig.api.config.v1.annotations.Switch;
import org.polyfrost.oneconfig.api.config.v1.annotations.Text;
import org.polyfrost.oneconfig.api.hud.v1.Hud;
import org.polyfrost.oneconfig.api.hud.v1.HudManager;
import org.polyfrost.oneconfig.api.hud.v1.TextHud;

/** Inherits OneConfig's draggable placement, font, colors, scale, background and profiles. */
public final class ClickAssistHud extends TextHud {
    @Dropdown(title = "Button", options = {"Left", "Right", "Both"})
    public int button = 0;
    @Text(title = "CPS format", description = "Placeholders: {base}, {boosted}, {total}. Counts cover the last second.")
    public String format = "{base} + {boosted} = {total}";
    @Switch(title = "Hide when idle")
    public boolean hideWhenIdle = false;

    public ClickAssistHud() {
        super("orven-bw-clickassist-hud.json", "ClickAssist CPS", Hud.Category.getCOMBAT(), "", " CPS");
    }
    @Override public long updateFrequency() { return 50_000_000L; }
    @Override protected String getText() {
        if (OrvenBw.instance() == null || HudManager.INSTANCE.isEditing()) {
            return button == 2 ? "L: " + line(new Cps(7, 6)) + "\nR: " + line(new Cps(5, 3)) : line(new Cps(7, 6));
        }
        return switch (button) {
            case 1 -> line(OrvenBw.instance().clickAssist().cps(1));
            case 2 -> "L: " + line(OrvenBw.instance().clickAssist().cps(0)) + "\nR: " + line(OrvenBw.instance().clickAssist().cps(1));
            default -> line(OrvenBw.instance().clickAssist().cps(0));
        };
    }
    @Override public boolean shouldShow() {
        if (HudManager.INSTANCE.isEditing()) return true;
        OrvenBw mod = OrvenBw.instance();
        if (mod == null || !mod.config().showHud) return false;
        return !hideWhenIdle || (button != 1 && mod.clickAssist().cps(0).total() > 0)
                || (button != 0 && mod.clickAssist().cps(1).total() > 0);
    }
    private String line(Cps cps) {
        String template = format == null ? "{base} + {boosted} = {total}" : format;
        return template.replace("{base}", Integer.toString(cps.base()))
                .replace("{boosted}", Integer.toString(cps.boosted()))
                .replace("{total}", Integer.toString(cps.total()));
    }
}
