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
    @Dropdown(title = "Button", options = {"Active button", "Right", "Both", "Left"})
    public int button = 0;
    @Text(title = "CPS format", description = "Placeholders: {base}, {boosted}, {total}, {button}. Counts cover the last second.")
    public String format = CpsFormat.DEFAULT;
    @Switch(title = "Hide when idle")
    public boolean hideWhenIdle = false;

    public ClickAssistHud() {
        super("orven-bw-clickassist-hud.json", "ClickAssist CPS", Hud.Category.getCOMBAT(), "", "");
    }
    @Override public long updateFrequency() { return 50_000_000L; }
    @Override protected String getText() {
        if (OrvenBw.instance() == null || HudManager.INSTANCE.isEditing()) {
            return button == 2 ? line(new Cps(7, 6), 0) + "\n" + line(new Cps(5, 3), 1) : line(new Cps(7, 6), 0);
        }
        var feature = OrvenBw.instance().clickAssist();
        if (button == 2) return line(feature.cps(0), 0) + "\n" + line(feature.cps(1), 1);
        int selected = button == 1 ? 1 : button == 3 ? 0 : feature.activeButton();
        return line(feature.cps(selected), selected);
    }
    @Override public boolean shouldShow() {
        if (HudManager.INSTANCE.isEditing()) return true;
        OrvenBw mod = OrvenBw.instance();
        if (mod == null || !mod.config().showHud || !mod.config().modEnabled) return false;
        if (!hideWhenIdle) return true;
        int selected = button == 1 ? 1 : button == 3 ? 0 : mod.clickAssist().activeButton();
        return mod.clickAssist().cps(selected).total() > 0 || button == 2 && mod.clickAssist().cps(1 - selected).total() > 0;
    }
    @Override public String concat(String prefix, String value, String suffix) {
        return super.concat(prefix, value, CpsFormat.suffix(suffix));
    }
    private String line(Cps cps, int selected) { return CpsFormat.render(format, cps, selected); }
}
