package io.github.liwwyy.orvenbw.config;

import org.polyfrost.oneconfig.api.config.v1.Config;
import org.polyfrost.oneconfig.api.config.v1.annotations.*;

/** One mod entry, separate feature categories as the utility collection grows. */
public final class OrvenConfig extends Config {
    @Switch(title = "Enable ClickAssist", category = "ClickAssist", subcategory = "General")
    public boolean enabled = false;
    @Slider(title = "Total CPS cap", description = "Per button, physical + boosted over the last second. Never suppresses your physical clicks.", category = "ClickAssist", subcategory = "General", min = 1, max = 30, step = 1)
    public int totalCpsCap = 13;
    @Slider(title = "Activation CPS", description = "Boost only when physical CPS is strictly above this value. Generated clicks do not activate assistance.", category = "ClickAssist", subcategory = "General", min = 0, max = 20, step = 1)
    public int activationCps = 4;
    @Switch(title = "Requires player within reach", description = "Requires another living, non-spectator player within four blocks (not necessarily under the crosshair).", category = "ClickAssist", subcategory = "General")
    public boolean requiresPlayer = false;
    @Switch(title = "Disable in creative", category = "ClickAssist", subcategory = "General")
    public boolean disableInCreative = true;
    @Slider(title = "Boost delay (ms)", category = "ClickAssist", subcategory = "Timing", min = 10, max = 150, step = 5)
    public int boostDelayMs = 40;
    @Switch(title = "Vary boost timing", description = "Vary the configured delay by up to 25%. Never replay missed clicks after a stall.", category = "ClickAssist", subcategory = "Timing")
    public boolean randomizeTiming = true;

    @Switch(title = "Assist left click", category = "ClickAssist", subcategory = "Left click")
    public boolean leftClick = true;
    @Slider(title = "Left boost chance (%)", category = "ClickAssist", subcategory = "Left click", min = 0, max = 100, step = 1)
    public int leftChance = 80;
    @Switch(title = "Weapon only", category = "ClickAssist", subcategory = "Left click")
    public boolean weaponOnly = true;
    @Switch(title = "Only while targeting an entity", category = "ClickAssist", subcategory = "Left click")
    public boolean onlyWhileTargeting = false;
    @Switch(title = "Preserve block breaking", description = "Do not add attack clicks while the crosshair targets a block.", category = "ClickAssist", subcategory = "Left click")
    public boolean preserveMining = true;

    @Switch(title = "Assist right click", category = "ClickAssist", subcategory = "Right click")
    public boolean rightClick = false;
    @Slider(title = "Right boost chance (%)", category = "ClickAssist", subcategory = "Right click", min = 0, max = 100, step = 1)
    public int rightChance = 80;
    @Switch(title = "Blocks only", category = "ClickAssist", subcategory = "Right click")
    public boolean blocksOnly = true;

    @Switch(title = "Swords", category = "ClickAssist", subcategory = "Allowed weapons")
    public boolean swords = true;
    @Switch(title = "Axes", category = "ClickAssist", subcategory = "Allowed weapons")
    public boolean axes = false;
    @Switch(title = "Fishing rods", category = "ClickAssist", subcategory = "Allowed weapons")
    public boolean rods = false;
    @Switch(title = "Sticks", category = "ClickAssist", subcategory = "Allowed weapons")
    public boolean sticks = true;
    @Switch(title = "Hoes", category = "ClickAssist", subcategory = "Allowed weapons")
    public boolean hoes = false;
    @Switch(title = "Shovels", category = "ClickAssist", subcategory = "Allowed weapons")
    public boolean shovels = false;

    @Switch(title = "Show ClickAssist HUD", category = "ClickAssist", subcategory = "HUD")
    public boolean showHud = true;

    public OrvenConfig() {
        super("orven-bw.json", "orven-bw", Category.UTILITY);
    }
}
