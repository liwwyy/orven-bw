package io.github.liwwyy.orvenbw.hud;

import io.github.liwwyy.orvenbw.OrvenBw;
import io.github.liwwyy.orvenbw.feature.clickassist.ClickAssistEngine.Cps;
import org.polyfrost.oneconfig.api.config.v1.annotations.*;
import org.polyfrost.oneconfig.api.hud.v1.Hud;
import org.polyfrost.oneconfig.api.hud.v1.HudManager;
import org.polyfrost.oneconfig.api.hud.v1.TextHud;

/** Native HUD placement and appearance with independent left/right rolling counts. */
public final class ClickAssistHud extends TextHud {
    @Include public int button = 0; // Read old HUD profiles without preserving their side lock.
    @Text(title = "CPS display format", description = "{base}, {boosted} and {total} follow the currently busier side. {left} and {right} always show both raw one-second totals.")
    public String format = CpsFormat.DEFAULT;
    @Switch(title = "Show CPS text", description = "Append uppercase CPS after the left | right totals.")
    public boolean showCpsLabel = true;
    @Switch(title = "Hide when unused", description = "Hide after no physical or generated click has occurred for the timeout below.")
    public boolean hideWhenIdle = false;
    @Slider(title = "HUD hide timeout (ms)", description = "Time since the last click before hiding. New input shows the HUD immediately.", min = 0, max = 10000, step = 100)
    public int hideTimeoutMs = 1000;
    public ClickAssistHud() { super("orven-bw-clickassist-hud.json", "ClickAssist CPS", Hud.Category.getCOMBAT(), "", ""); }
    @Override public long updateFrequency() { return 50_000_000L; }
    @Override protected String getText() {
        if (OrvenBw.instance() == null || HudManager.INSTANCE.isEditing())
            return CpsFormat.render(format, new Cps(7, 6), new Cps(3, 1), 0, showCpsLabel);
        var feature = OrvenBw.instance().clickAssist();
        return CpsFormat.render(format, feature.cps(0), feature.cps(1), feature.activeButton(), showCpsLabel);
    }
    @Override public boolean shouldShow() {
        if (HudManager.INSTANCE.isEditing()) return true;
        OrvenBw mod = OrvenBw.instance();
        return mod != null && mod.config().showHud && mod.config().modEnabled
                && HudIdleTimeout.visible(hideWhenIdle, System.nanoTime(), mod.clickAssist().lastClickNanos(), hideTimeoutMs);
    }
    @Override public String concat(String prefix, String value, String suffix) {
        return super.concat(prefix, value, CpsFormat.suffix(suffix));
    }
}
