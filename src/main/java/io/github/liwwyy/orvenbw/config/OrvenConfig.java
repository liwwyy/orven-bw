package io.github.liwwyy.orvenbw.config;

import org.polyfrost.oneconfig.api.config.v1.*;
import org.polyfrost.oneconfig.api.config.v1.annotations.*;
import org.polyfrost.oneconfig.api.ui.v1.OneConfigUI;
import org.polyfrost.oneconfig.api.ui.v1.keybind.KeybindHelper;
import org.polyfrost.oneconfig.api.ui.v1.keybind.OneConfigKeybind;
import org.polyfrost.oneconfig.internal.legacy.InputConstants;
import org.polyfrost.oneconfig.internal.ui.navigation.graph.ModConfigRoute;
import io.github.liwwyy.orvenbw.feature.clickassist.ClickSession;

/** Native accordions share fields with hidden legacy aliases for profile compatibility. */
public final class OrvenConfig extends Config {
    @Switch(title = "Enable orven-bw", description = "", category = "General", subcategory = "General")
    public boolean modEnabled = true;
    @Keybind(title = "Open settings", description = "Open this mod’s OneConfig page.", category = "General", subcategory = "General")
    public OneConfigKeybind settingsBind = KeybindHelper.builder().key(InputConstants.KEY_P).action((java.util.function.Consumer<Boolean>) down -> { if (down) openSettings(); }).build();
    @Switch(title = "Scoreboard filter", description = "Activate only when the sidebar contains the text below.", category = "General", subcategory = "General")
    public boolean scoreboardOnly = false;
    @Text(title = "Matching text", description = "Ignore case and colors.", category = "General", subcategory = "General")
    public String scoreboardWord = "Red";
    @Switch(title = "Enable ClickAssist", description = "", category = "ClickAssist", subcategory = "Physical Assist")
    public boolean enabled = false;
    @Slider(title = "Activation CPS", description = "Your manual CPS must be above this.", category = "ClickAssist", subcategory = "Physical Assist", min = 0, max = 20, step = 1)
    public int activationCps = 4;
    @Switch(title = "Require nearby player", description = "Another player within four blocks.", category = "ClickAssist", subcategory = "Physical Assist")
    public boolean requiresPlayer = false;
    @Switch(title = "Disable in creative", description = "", category = "ClickAssist", subcategory = "Physical Assist")
    public boolean disableInCreative = true;
    @Switch(title = "Assist left click", description = "", category = "ClickAssist", subcategory = "Physical Assist")
    public boolean leftClick = true;
    @Switch(title = "Weapon only", description = "Use the selected weapons below.", category = "ClickAssist", subcategory = "Physical Assist")
    public boolean weaponOnly = true;
    @Switch(title = "Target an entity", description = "Pause when no entity is under the crosshair.", category = "ClickAssist", subcategory = "Physical Assist")
    public boolean onlyWhileTargeting = false;
    @Switch(title = "Preserve block breaking", description = "Pause left boosts while aiming at blocks.", category = "ClickAssist", subcategory = "Physical Assist")
    public boolean preserveMining = false;
    @Switch(title = "Assist right click", description = "", category = "ClickAssist", subcategory = "Physical Assist")
    public boolean rightClick = false;
    @Switch(title = "Right: blocks only", description = "", category = "ClickAssist", subcategory = "Physical Assist")
    public boolean blocksOnly = true;
    @Slider(title = "Initial boost delay (ms)", description = "Delay the first boost after activation.", category = "ClickAssist", subcategory = "Physical Assist", min = 0, max = 150, step = 5)
    public int boostDelayMs = 40;
    @Switch(title = "Swords", description = "", category = "ClickAssist", subcategory = "Physical Assist")
    public boolean swords = true;
    @Switch(title = "Axes", description = "", category = "ClickAssist", subcategory = "Physical Assist")
    public boolean axes = false;
    @Switch(title = "Fishing rods", description = "", category = "ClickAssist", subcategory = "Physical Assist")
    public boolean rods = false;
    @Switch(title = "Sticks", description = "", category = "ClickAssist", subcategory = "Physical Assist")
    public boolean sticks = true;
    @Switch(title = "Hoes", description = "", category = "ClickAssist", subcategory = "Physical Assist")
    public boolean hoes = false;
    @Switch(title = "Shovels", description = "", category = "ClickAssist", subcategory = "Physical Assist")
    public boolean shovels = false;
    @Switch(title = "Enable spam clicking", description = "", category = "ClickAssist", subcategory = "Spam Clicking")
    public boolean spamEnabled = false;
    @Keybind(title = "Activation button", description = "", category = "ClickAssist", subcategory = "Spam Clicking")
    public OneConfigKeybind spamBind = KeybindHelper.builder().mouse(InputConstants.MOUSE_BUTTON_MIDDLE).action((java.util.function.Consumer<Boolean>) down -> input(0, down)).build();
    @RadioButton(title = "Handle mode", options = {"Hold", "Toggle"}, category = "ClickAssist", subcategory = "Spam Clicking")
    public int spamMode = 0;
    @RadioButton(title = "Click button", options = {"Left", "Right"}, category = "ClickAssist", subcategory = "Spam Clicking")
    public int spamButton = 0;
    @Switch(title = "Click through blocks", description = "Continue left spam while aiming at blocks.", category = "ClickAssist", subcategory = "Spam Clicking")
    public boolean spamClickThroughBlocks = true;
    @Switch(title = "Weapon only", description = "Use the selected weapons below.", category = "ClickAssist", subcategory = "Spam Clicking")
    public boolean spamWeaponOnly = true;
    @Switch(title = "Right: blocks only", description = "", category = "ClickAssist", subcategory = "Spam Clicking")
    public boolean spamBlocksOnly = true;
    @Switch(title = "Require nearby player", description = "Another player within four blocks.", category = "ClickAssist", subcategory = "Spam Clicking")
    public boolean spamRequiresPlayer = false;
    @Switch(title = "Disable in creative", description = "", category = "ClickAssist", subcategory = "Spam Clicking")
    public boolean spamDisableInCreative = true;
    @Switch(title = "Target an entity", description = "Pause when no entity is under the crosshair.", category = "ClickAssist", subcategory = "Spam Clicking")
    public boolean spamOnlyWhileTargeting = false;
    @Switch(title = "Swords", category = "ClickAssist", subcategory = "Spam Clicking")
    public boolean spamSwords = true;
    @Switch(title = "Axes", category = "ClickAssist", subcategory = "Spam Clicking")
    public boolean spamAxes = false;
    @Switch(title = "Fishing rods", category = "ClickAssist", subcategory = "Spam Clicking")
    public boolean spamRods = false;
    @Switch(title = "Sticks", category = "ClickAssist", subcategory = "Spam Clicking")
    public boolean spamSticks = true;
    @Switch(title = "Hoes", category = "ClickAssist", subcategory = "Spam Clicking")
    public boolean spamHoes = false;
    @Switch(title = "Shovels", category = "ClickAssist", subcategory = "Spam Clicking")
    public boolean spamShovels = false;
    @Slider(title = "High CPS · 20%", description = "Upper level for one fifth of the session.", category = "ClickAssist", subcategory = "Physical Assist", min = 1, max = 20, step = 0.1f)
    public double assistHighCps = 14.0;
    @Slider(title = "Medium CPS · 40%", description = "Middle level for two fifths of the session.", category = "ClickAssist", subcategory = "Physical Assist", min = 1, max = 20, step = 0.1f)
    public double assistMediumCps = 12.5;
    @Slider(title = "Low CPS · 40%", description = "Lower level for two fifths of the session.", category = "ClickAssist", subcategory = "Physical Assist", min = 1, max = 20, step = 0.1f)
    public double assistLowCps = 9.5;
    @Switch(title = "Vary click timing", description = "Vary intervals around the current rate.", category = "ClickAssist", subcategory = "Physical Assist")
    public boolean assistVaryTiming = true;
    @Switch(title = "Gradual ramp", description = "Start gently and increase to the high rate.", category = "ClickAssist", subcategory = "Physical Assist")
    public boolean assistRampEnabled = true;
    @Slider(title = "Ramp time (ms)", description = "Full rate by the end of this time.", category = "ClickAssist", subcategory = "Physical Assist", min = 100, max = 5000, step = 100)
    public int assistRampMs = 1000;
    @Switch(title = "Exhaustion", description = "Occasional slower periods during long sessions.", category = "ClickAssist", subcategory = "Physical Assist")
    public boolean assistExhaustionEnabled = true;
    @Slider(title = "Exhaustion starts after (ms)", description = "", category = "ClickAssist", subcategory = "Physical Assist", min = 1000, max = 60000, step = 1000)
    public int assistExhaustionAfterMs = 8000;
    @Slider(title = "Exhaustion check interval (ms)", description = "", category = "ClickAssist", subcategory = "Physical Assist", min = 1000, max = 30000, step = 1000)
    public int assistExhaustionIntervalMs = 4000;
    @Slider(title = "Exhaustion chance (%)", description = "", category = "ClickAssist", subcategory = "Physical Assist", min = 0, max = 100, step = 1)
    public int assistExhaustionChance = 30;
    @Slider(title = "Exhaustion duration (ms)", description = "", category = "ClickAssist", subcategory = "Physical Assist", min = 100, max = 3000, step = 100)
    public int assistExhaustionDurationMs = 600;
    @Slider(title = "Exhausted minimum CPS", description = "", category = "ClickAssist", subcategory = "Physical Assist", min = 1, max = 20, step = 0.1f)
    public double assistExhaustionMinCps = 8.0;
    @Slider(title = "Exhausted maximum CPS", description = "", category = "ClickAssist", subcategory = "Physical Assist", min = 1, max = 20, step = 0.1f)
    public double assistExhaustionMaxCps = 9.0;
    @Slider(title = "High CPS · 20%", description = "Upper level for one fifth of the session.", category = "ClickAssist", subcategory = "Spam Clicking", min = 1, max = 20, step = 0.1f)
    public double spamHighCps = 14.0;
    @Slider(title = "Medium CPS · 40%", description = "Middle level for two fifths of the session.", category = "ClickAssist", subcategory = "Spam Clicking", min = 1, max = 20, step = 0.1f)
    public double spamMediumCps = 12.5;
    @Slider(title = "Low CPS · 40%", description = "Lower level for two fifths of the session.", category = "ClickAssist", subcategory = "Spam Clicking", min = 1, max = 20, step = 0.1f)
    public double spamLowCps = 9.5;
    @Switch(title = "Vary click timing", description = "Vary intervals around the current rate.", category = "ClickAssist", subcategory = "Spam Clicking")
    public boolean spamVaryTiming = true;
    @Switch(title = "Gradual ramp", description = "Start gently and increase to the high rate.", category = "ClickAssist", subcategory = "Spam Clicking")
    public boolean spamRampEnabled = true;
    @Slider(title = "Ramp time (ms)", description = "Full rate by the end of this time.", category = "ClickAssist", subcategory = "Spam Clicking", min = 100, max = 5000, step = 100)
    public int spamRampMs = 1000;
    @Switch(title = "Exhaustion", description = "Occasional slower periods during long sessions.", category = "ClickAssist", subcategory = "Spam Clicking")
    public boolean spamExhaustionEnabled = true;
    @Slider(title = "Exhaustion starts after (ms)", description = "", category = "ClickAssist", subcategory = "Spam Clicking", min = 1000, max = 60000, step = 1000)
    public int spamExhaustionAfterMs = 8000;
    @Slider(title = "Exhaustion check interval (ms)", description = "", category = "ClickAssist", subcategory = "Spam Clicking", min = 1000, max = 30000, step = 1000)
    public int spamExhaustionIntervalMs = 4000;
    @Slider(title = "Exhaustion chance (%)", description = "", category = "ClickAssist", subcategory = "Spam Clicking", min = 0, max = 100, step = 1)
    public int spamExhaustionChance = 30;
    @Slider(title = "Exhaustion duration (ms)", description = "", category = "ClickAssist", subcategory = "Spam Clicking", min = 100, max = 3000, step = 100)
    public int spamExhaustionDurationMs = 600;
    @Slider(title = "Exhausted minimum CPS", description = "", category = "ClickAssist", subcategory = "Spam Clicking", min = 1, max = 20, step = 0.1f)
    public double spamExhaustionMinCps = 8.0;
    @Slider(title = "Exhausted maximum CPS", description = "", category = "ClickAssist", subcategory = "Spam Clicking", min = 1, max = 20, step = 0.1f)
    public double spamExhaustionMaxCps = 9.0;
    @Switch(title = "Enable button hold", description = "", category = "ClickAssist", subcategory = "Button Hold")
    public boolean holdEnabled = false;
    @Keybind(title = "Toggle left hold", description = "Hold the attack binding until toggled off.", category = "ClickAssist", subcategory = "Button Hold")
    public OneConfigKeybind holdLeftBind = KeybindHelper.builder().action((java.util.function.Consumer<Boolean>) down -> input(1, down)).build();
    @Keybind(title = "Toggle right hold", description = "Hold the use binding until toggled off.", category = "ClickAssist", subcategory = "Button Hold")
    public OneConfigKeybind holdRightBind = KeybindHelper.builder().action((java.util.function.Consumer<Boolean>) down -> input(2, down)).build();
    @Switch(title = "Show CPS HUD", description = "", category = "ClickAssist", subcategory = "HUD")
    public boolean showHud = true;
    @Button(title = "Customize HUD", text = "Open HUD editor", description = "Change placement, colors, font and format.", category = "ClickAssist", subcategory = "HUD")
    public void editHud() { org.polyfrost.oneconfig.api.hud.v1.HudManager.INSTANCE.openEditor(); }

    // Previous keys remain readable, but the old hard cap/chance no longer drives clicking.
    @Include public int configSchema = 0;
    @Include public int totalCpsCap = 13;
    @Include public int leftChance = 80;
    @Include public int rightChance = 80;
    @Include public boolean randomizeTiming = true;
    @Include public boolean rampEnabled = true;
    @Include public int rampMs = 1200;
    @Include public int startCps = 5;
    @Include public boolean exhaustionEnabled = true;
    @Include public int exhaustionAfterMs = 8000;
    @Include public int exhaustionIntervalMs = 4000;
    @Include public int exhaustionChance = 30;
    @Include public int exhaustionDurationMs = 600;
    @Include public int exhaustionMinCps = 8;
    @Include public int exhaustionMaxCps = 9;

    @Override protected Tree makeTree() {
        Tree collected = super.makeTree();
        Tree root = Tree.tree("orven-bw.json");
        String[] names = {"General", "Physical Assist", "Spam Clicking", "Button Hold", "HUD"};
        String[] ids = {"general", "assist", "spam", "hold", "hud"};
        String[] toggles = {"modEnabled", "enabled", "spamEnabled", "holdEnabled", "showHud"};
        for (int i = 0; i < names.length; i++) {
            Tree section = Tree.tree(ids[i]);
            section.addMetadata("title", names[i]);
            section.addMetadata("category", i == 0 ? "General" : "ClickAssist");
            section.addMetadata("subcategory", "Settings");
            section.addMetadata("collapsed", true);
            Node toggle = collected.get(toggles[i]);
            toggle.removeMetadata("visualizer");
            toggle.removeMetadata("hidden");
            section.put(toggle);
            for (var entry : collected.map.entrySet()) {
                if (entry.getKey().equals(toggles[i])) continue;
                if (names[i].equals(entry.getValue().getMetadata("subcategory"))) section.put(entry.getValue());
            }
            root.put(section);
        }
        // Append compatibility aliases after visible sections to preserve General-first order.
        for (var field : getClass().getFields()) {
            if (java.lang.reflect.Modifier.isStatic(field.getModifiers())) continue;
            var alias = Properties.field(null, null, field, this);
            alias.addMetadata("hidden", true);
            root.put(alias);
        }
        return root;
    }

    @Override protected void initialize(boolean byManager) {
        super.initialize(byManager);
        migrate();
    }
    public void migrate() { if (migrateValues()) save(); }
    boolean migrateValues() {
        if (configSchema >= 2) return false;
        assistRampEnabled = spamRampEnabled = rampEnabled;
        assistRampMs = spamRampMs = rampMs == 1200 ? 1000 : rampMs;
        assistVaryTiming = spamVaryTiming = randomizeTiming;
        assistExhaustionEnabled = spamExhaustionEnabled = exhaustionEnabled;
        assistExhaustionAfterMs = spamExhaustionAfterMs = exhaustionAfterMs;
        assistExhaustionIntervalMs = spamExhaustionIntervalMs = exhaustionIntervalMs;
        assistExhaustionChance = spamExhaustionChance = exhaustionChance;
        assistExhaustionDurationMs = spamExhaustionDurationMs = exhaustionDurationMs;
        assistExhaustionMinCps = spamExhaustionMinCps = exhaustionMinCps;
        assistExhaustionMaxCps = spamExhaustionMaxCps = exhaustionMaxCps;
        spamWeaponOnly = weaponOnly;
        spamOnlyWhileTargeting = onlyWhileTargeting;
        spamSwords = swords; spamAxes = axes; spamRods = rods;
        spamSticks = sticks; spamHoes = hoes; spamShovels = shovels;
        spamBlocksOnly = blocksOnly;
        spamRequiresPlayer = requiresPlayer;
        spamDisableInCreative = disableInCreative;
        configSchema = 2;
        return true;
    }

    public ClickSession.Options rateOptions(boolean spam) {
        return spam ? new ClickSession.Options(spamHighCps, spamMediumCps, spamLowCps, spamRampEnabled, spamRampMs,
                spamExhaustionEnabled, spamExhaustionAfterMs, spamExhaustionIntervalMs, spamExhaustionChance,
                spamExhaustionDurationMs, spamExhaustionMinCps, spamExhaustionMaxCps)
                : new ClickSession.Options(assistHighCps, assistMediumCps, assistLowCps, assistRampEnabled, assistRampMs,
                assistExhaustionEnabled, assistExhaustionAfterMs, assistExhaustionIntervalMs, assistExhaustionChance,
                assistExhaustionDurationMs, assistExhaustionMinCps, assistExhaustionMaxCps);
    }
    public static void openSettings() { OneConfigUI.open(new ModConfigRoute("orven-bw.json", "General")); }
    private static void input(int action, boolean down) {
        var mod = io.github.liwwyy.orvenbw.OrvenBw.instance();
        if (mod != null) mod.clickAssist().activation(action, down);
    }
    public OrvenConfig() { super("orven-bw.json", "orven-bw", Category.UTILITY); }
}
