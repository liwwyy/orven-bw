package io.github.liwwyy.orvenbw.config;

import org.polyfrost.oneconfig.api.config.v1.*;
import org.polyfrost.oneconfig.api.config.v1.annotations.*;
import org.polyfrost.oneconfig.api.ui.v1.OneConfigUI;
import org.polyfrost.oneconfig.api.ui.v1.keybind.KeybindHelper;
import org.polyfrost.oneconfig.api.ui.v1.keybind.OneConfigKeybind;
import org.polyfrost.oneconfig.internal.legacy.InputConstants;
import org.polyfrost.oneconfig.internal.ui.navigation.graph.ModConfigRoute;
import io.github.liwwyy.orvenbw.feature.clickassist.ClickProfileSession;

/** Feature pages with native accordions and hidden persisted-field aliases. */
public final class OrvenConfig extends Config {
    @Include public boolean modEnabled = false;
    @Include public OneConfigKeybind settingsBind = KeybindHelper.builder().key(InputConstants.KEY_P).action((java.util.function.Consumer<Boolean>) down -> { if (down) openSettings(); }).build();
    @Include public boolean scoreboardOnly = false;
    @Include public String scoreboardWord = "Red";
    @Include public boolean enabled = false;
    @Include public int activationCps = 4;
    @Include public boolean requiresPlayer = false;
    @Include public boolean disableInCreative = true;
    @Include public boolean leftClick = true;
    @Include public boolean weaponOnly = true;
    @Include public boolean assistAllowFist = false;
    @Include public boolean onlyWhileTargeting = false;
    @Include public boolean preserveMining = false;
    @Include public boolean rightClick = false;
    @Include public boolean blocksOnly = true;
    @Include public int boostDelayMs = 40;
    @Include public boolean swords = true;
    @Include public boolean axes = false;
    @Include public boolean rods = false;
    @Include public boolean sticks = true;
    @Include public boolean hoes = false;
    @Include public boolean shovels = false;
    @Include public boolean spamEnabled = false;
    @Include public OneConfigKeybind spamBind = KeybindHelper.builder().mouse(InputConstants.MOUSE_BUTTON_MIDDLE).action((java.util.function.Consumer<Boolean>) down -> input(0, down)).build();
    @Include public int spamMode = 0;
    @Include public int spamButton = 0;
    @Include public boolean spamClickThroughBlocks = true;
    @Include public boolean spamWeaponOnly = true;
    @Include public boolean spamAllowFist = true;
    @Include public boolean spamBlocksOnly = true;
    @Include public boolean spamRequiresPlayer = false;
    @Include public boolean spamDisableInCreative = true;
    @Include public boolean spamOnlyWhileTargeting = false;
    @Include public boolean heldClickEnabled = false;
    @Include public boolean heldClickLeft = true;
    @Include public boolean heldClickRight = false;
    @Include public int heldClickDelayMs = 250;
    @Include public boolean heldClickInstant = false;
    @Include public boolean heldClickRequiresPlayer = true;
    @Include public boolean heldClickWeaponOnly = true;
    @Include public boolean heldClickAllowFist = false;
    @Include public boolean heldClickEntityOnly = true;
    @Include public boolean spamSwords = true;
    @Include public boolean spamAxes = false;
    @Include public boolean spamRods = false;
    @Include public boolean spamSticks = true;
    @Include public boolean spamHoes = false;
    @Include public boolean spamShovels = false;
    @Include public double assistHighCps = 14.0;
    @Include public double assistMediumCps = 12.5;
    @Include public double assistLowCps = 9.5;
    @Include public boolean assistVaryTiming = true;
    @Include public boolean assistRampEnabled = true;
    @Include public int assistRampMs = 1000;
    @Include public boolean assistExhaustionEnabled = true;
    @Include public int assistExhaustionAfterMs = 8000;
    @Include public int assistExhaustionIntervalMs = 4000;
    @Include public int assistExhaustionChance = 30;
    @Include public int assistExhaustionDurationMs = 600;
    @Include public double assistExhaustionMinCps = 8.0;
    @Include public double assistExhaustionMaxCps = 9.0;
    @Include public double spamHighCps = 14.0;
    @Include public double spamMediumCps = 12.5;
    @Include public double spamLowCps = 9.5;
    @Include public boolean spamVaryTiming = true;
    @Include public boolean spamRampEnabled = true;
    @Include public int spamRampMs = 1000;
    @Include public boolean spamExhaustionEnabled = true;
    @Include public int spamExhaustionAfterMs = 8000;
    @Include public int spamExhaustionIntervalMs = 4000;
    @Include public int spamExhaustionChance = 30;
    @Include public int spamExhaustionDurationMs = 600;
    @Include public double spamExhaustionMinCps = 8.0;
    @Include public double spamExhaustionMaxCps = 9.0;
    @Include public boolean holdEnabled = false;
    @Include public OneConfigKeybind holdLeftBind = KeybindHelper.builder().action((java.util.function.Consumer<Boolean>) down -> input(1, down)).build();
    @Include public OneConfigKeybind holdRightBind = KeybindHelper.builder().action((java.util.function.Consumer<Boolean>) down -> input(2, down)).build();
    @Include public boolean showHud = true;
    @Include public int configSchema = 0;
    @Include public int totalCpsCap = 13;
    @Include public int leftChance = 80;
    @Include public int rightChance = 80;
    @Include public boolean randomizeTiming = true;
    @Include public boolean rampEnabled = true;
    @Include public int rampMs = 1000;
    @Include public int startCps = 5;
    @Include public boolean exhaustionEnabled = true;
    @Include public int exhaustionAfterMs = 8000;
    @Include public int exhaustionIntervalMs = 4000;
    @Include public int exhaustionChance = 30;
    @Include public int exhaustionDurationMs = 600;
    @Include public int exhaustionMinCps = 8;
    @Include public int exhaustionMaxCps = 9;
    @Include public OneConfigKeybind toggleModBind = KeybindHelper.builder().action((java.util.function.Consumer<Boolean>) down -> { if (down) toggleMod(); }).build();
    @Include public OneConfigKeybind spamLeftBind = KeybindHelper.builder().mouse(InputConstants.MOUSE_BUTTON_MIDDLE).action((java.util.function.Consumer<Boolean>) down -> input(3, down)).build();
    @Include public OneConfigKeybind spamRightBind = KeybindHelper.builder().action((java.util.function.Consumer<Boolean>) down -> input(4, down)).build();
    @Include public int clickingProfile = 0;
    @Include public boolean separateClickSides = true;
    @Include public double profileCpsCeiling = 22;
    @Include public double highCps = 14;
    @Include public double mediumCps = 12.5;
    @Include public double lowCps = 9.5;
    @Include public double levelVariation = 6;
    @Include public double timingVariation = 22;
    @Include public int levelMinMs = 650;
    @Include public int levelMaxMs = 1400;
    @Include public double exhaustedMinCps = 8;
    @Include public double exhaustedMaxCps = 9;
    @Include public boolean hitEffectsEnabled = false;
    @Include public boolean hitEffectsShowHealth = true;
    @Include public boolean hitEffectsIgnoreNpcs = true;
    @Include public int hitComboResetMs = 1500;
    @Include public int hitEffectDurationMs = 350;
    @Include public int hitPopupPosition = 0;
    @Include public int hitEffectOffsetY = 18;

    @Include public boolean heldCrouchCancel = true;
    @Include public boolean rockyRamp = true;
    @Include public boolean debugEnabled = false;
    @ItemList(title = "Allowed items", description = "Items that enable left physical boosting. Selecting any sword enables every sword; other items match exactly.", category = "ClickAssist", subcategory = "Advanced")
    public String[] assistItems = {"minecraft:diamond_sword", "minecraft:stick", "minecraft:beef"};
    @ItemList(title = "Allowed items", description = "Items that enable left spam-key clicking. Selecting any sword enables every sword; other items match exactly.", category = "ClickAssist", subcategory = "Advanced")
    public String[] spamItems = {"minecraft:diamond_sword", "minecraft:stick", "minecraft:beef"};
    @ItemList(title = "Allowed items", description = "Items that enable mouse-hold clicking. Selecting any sword enables every sword; other items match exactly.", category = "ClickAssist", subcategory = "Advanced")
    public String[] heldItems = {"minecraft:diamond_sword", "minecraft:stick", "minecraft:beef"};
    @Include public boolean autoToolEnabled = false;
    @Include public int autoToolSwitchDelayMs = 160;
    @Include public int autoToolVariationMs = 40;
    @Include public int autoToolHoverDelayMs = 0;
    @Include public boolean autoToolOnlyCrouching = false;
    @Include public boolean autoToolRequireLeftMouse = true;
    @Include public boolean autoToolSwitchBack = false;
    @Include public boolean autoToolOverrideSwitchBack = true;
    @Include public boolean autoToolIgnoreHeldItems = false;
    @Include public boolean autoToolWhitelistEnabled = true;
    @Include public boolean autoToolBlacklistEnabled = false;
    @ItemList(title = "Ignored held items", description = "Pause AutoTool while holding any item in this list. Items match exactly.", category = "AutoTool", subcategory = "Filters")
    public String[] autoToolIgnoredItems = {};
    @ItemList(title = "Allowed blocks", description = "Select block items. One wool, glass or hardened-clay entry includes every color; one wooden block includes the wood family. Non-block items are ignored.", category = "AutoTool", subcategory = "Filters")
    public String[] autoToolWhitelist = {"minecraft:wool", "minecraft:sandstone", "minecraft:glass", "minecraft:ladder", "minecraft:planks", "minecraft:log", "minecraft:obsidian", "minecraft:hardened_clay"};
    @ItemList(title = "Blocked blocks", description = "Never switch for these block families, even if allowed above. Uses the same color and wood grouping as the whitelist.", category = "AutoTool", subcategory = "Filters")
    public String[] autoToolBlacklist = {};
    @Button(title = "Clear debug cache", text = "Clear click log", description = "Erase the existing click-debug.jsonl file. Debugging continues in the same file when enabled.", category = "ClickAssist", subcategory = "Advanced")
    public void clearDebugCache() {
        var mod = io.github.liwwyy.orvenbw.OrvenBw.instance();
        if (mod != null) mod.debugLog().clear();
    }

    @Info(title = "Global Keybinds", description = "Open settings or toggle every enabled feature with a keybind.")
    public String globalKeysInfo = "";
    @Info(title = "Conditions", description = "Only enables the mod if these conditions are met")
    public String conditionsInfo = "";
    @Info(title = "Advanced", description = "Shared CPS and timing for physical boosts, activation-key spam and mouse-hold clicks. Weapon filters remain feature-specific.", category = "ClickAssist", subcategory = "Advanced")
    public String advancedInfo = "";
    @Button(title = "ClickAssist", text = "Open ClickAssist", icon = "assets/orvenbw/icons/clickassist.svg", description = "Open physical CPS boost, spam click button, mouse button hold click, hit effects and shared timing settings.")
    public void openClickAssist() { OneConfigUI.open(new ModConfigRoute("orven-bw.json", "ClickAssist")); }
    @Button(title = "AutoTool", text = "Open AutoTool", icon = "assets/orvenbw/icons/autotool.svg", description = "Choose the best hotbar tool for mining, with switch timing, block filters and optional switch-back.")
    public void openAutoTool() { OneConfigUI.open(new ModConfigRoute("orven-bw.json", "AutoTool")); }
    @Button(title = "Customize CPS HUD", text = "Open HUD editor", description = "Edit placement, colors, font, the CPS suffix and the dominant-side calculation.", category = "ClickAssist", subcategory = "HUD")
    public void editHud() { org.polyfrost.oneconfig.api.hud.v1.HudManager.INSTANCE.openEditor(); }

    @Override protected Tree makeTree() {
        Tree collected = super.makeTree();
        option(collected, "modEnabled", "Enable orven-bw", "Allow enabled features to run while all global conditions are met. The settings key works while disabled.", Visualizer.SwitchVisualizer.class);
        option(collected, "settingsBind", "Config open key", "Open the orven-bw General settings page. Default: P.", Visualizer.KeybindVisualizer.class);
        option(collected, "toggleModBind", "Enable/Disable toggle", "Turn the whole mod on or off. Disabling clears active clicking and held buttons.", Visualizer.KeybindVisualizer.class);
        option(collected, "scoreboardOnly", "Scoreboard filter", "Run features only when the sidebar title or a visible scoreboard line contains the matching text.", Visualizer.SwitchVisualizer.class);
        option(collected, "scoreboardWord", "Scoreboard matching text", "Text to find in the sidebar. Ignores colors and case; an empty value never matches. Default: Red.", Visualizer.TextVisualizer.class);
        option(collected, "enabled", "Enable physical assist:", "Add generated clicks to repeated manual clicking above the activation CPS. Holding once does not activate this feature.", Visualizer.SwitchVisualizer.class);
        option(collected, "leftClick", "Assist left click", "Boost manual attack clicks using the shared Advanced CPS profile.", Visualizer.SwitchVisualizer.class);
        option(collected, "rightClick", "Assist right click", "Boost manual use clicks using the shared Advanced CPS profile. Pauses during item use.", Visualizer.SwitchVisualizer.class);
        option(collected, "activationCps", "Minimum manual CPS", "Physical assistance starts only when your manual click rate is greater than this value.", Visualizer.SliderVisualizer.class);
        slider(collected, "activationCps", 0.0f, 20.0f, 1.0f);
        option(collected, "requiresPlayer", "Require nearby player", "Require another living, non-spectator player within four blocks whose username appears in the tab list.", Visualizer.SwitchVisualizer.class);
        option(collected, "disableInCreative", "Disable in Creative mode", "Pause physical assistance while you are in Creative mode.", Visualizer.SwitchVisualizer.class);
        option(collected, "onlyWhileTargeting", "Require an entity target", "Pause left physical assistance unless your crosshair points at an entity.", Visualizer.SwitchVisualizer.class);
        option(collected, "preserveMining", "Preserve block breaking", "Pause left physical boosts while aiming at blocks so vanilla mining can continue.", Visualizer.SwitchVisualizer.class);
        option(collected, "blocksOnly", "Right click: blocks only", "Allow right physical boosts only while holding a placeable block.", Visualizer.SwitchVisualizer.class);
        option(collected, "spamEnabled", "Enable spam click button", "Use the left and right activation keybinds below to generate repeated clicks without manual clicking.", Visualizer.SwitchVisualizer.class);
        option(collected, "spamLeftBind", "Spam left click — Keybind", "Generate attack clicks while held, or latch on/off in Toggle mode. Default: middle mouse.", Visualizer.KeybindVisualizer.class);
        option(collected, "spamRightBind", "Spam right click — Keybind", "Generate use clicks while held, or latch on/off in Toggle mode. Unassigned by default.", Visualizer.KeybindVisualizer.class);
        option(collected, "spamMode", "Activation keybind mode", "Hold runs only while the bind is pressed; Toggle starts on one press and stops on the next.", Visualizer.RadioVisualizer.class);
        option(collected, "spamClickThroughBlocks", "Click through blocks", "Allow left keybind spam while aiming at blocks. Turn off to preserve block breaking.", Visualizer.SwitchVisualizer.class);
        option(collected, "spamBlocksOnly", "Right click: blocks only", "Allow right keybind spam only while holding a placeable block.", Visualizer.SwitchVisualizer.class);
        option(collected, "spamRequiresPlayer", "Require nearby player", "Require a living player in the tab list within four blocks before keybind spam can run.", Visualizer.SwitchVisualizer.class);
        option(collected, "spamDisableInCreative", "Disable in Creative mode", "Pause activation-keybind spam while you are in Creative mode.", Visualizer.SwitchVisualizer.class);
        option(collected, "spamOnlyWhileTargeting", "Require an entity target", "Pause left activation-keybind spam unless your crosshair points at an entity.", Visualizer.SwitchVisualizer.class);
        option(collected, "heldClickEnabled", "Enable mouse button hold click", "Generate repeated clicks while physically holding Minecraft’s attack or use binding. Independent of the spam activation keys.", Visualizer.SwitchVisualizer.class);
        option(collected, "heldClickLeft", "Left click", "Generate attack clicks while physically holding your configured attack binding.", Visualizer.SwitchVisualizer.class);
        option(collected, "heldClickRight", "Right click", "Generate use clicks while physically holding your configured use binding. Pauses during blocking, eating and charging.", Visualizer.SwitchVisualizer.class);
        option(collected, "heldClickDelayMs", "Hold activation delay (ms)", "Hold continuously with all enabled filters satisfied for this long before clicking begins. Losing eligibility resets the timer.", Visualizer.SliderVisualizer.class);
        slider(collected, "heldClickDelayMs", 0.0f, 2000.0f, 10.0f);
        option(collected, "heldClickInstant", "Instant activation", "Skip only the hold delay when an entity is targeted, a listed item is held, and a living tab-listed player is nearby. Ramp still applies.", Visualizer.SwitchVisualizer.class);
        option(collected, "heldClickRequiresPlayer", "Require nearby player", "Require a living tab-listed player within four blocks. NPCs absent from the tab list do not count.", Visualizer.SwitchVisualizer.class);
        option(collected, "heldClickEntityOnly", "Require an entity target", "Require an entity under the crosshair. Turn off to allow block and air targets.", Visualizer.SwitchVisualizer.class);
        option(collected, "heldClickWeaponOnly", "Require allowed item", "Run mouse-hold clicking only with an item from its Advanced item list. Any selected sword enables all swords.", Visualizer.SwitchVisualizer.class);
        option(collected, "heldClickAllowFist", "Allow fist", "Allow left mouse-hold clicking with an empty hand. Off by default, including when the allowed-item filter is off.", Visualizer.SwitchVisualizer.class);
        option(collected, "weaponOnly", "Physical assist: allowed items only", "Restrict left physical assistance to items in the editable list below.", Visualizer.SwitchVisualizer.class);
        option(collected, "assistAllowFist", "Allow fist", "Allow left physical CPS boosts with an empty hand. Off by default; independent of the allowed-item filter.", Visualizer.SwitchVisualizer.class);
        option(collected, "spamWeaponOnly", "Keybind spam: allowed items only", "Restrict left activation-keybind spam to items in the editable list below.", Visualizer.SwitchVisualizer.class);
        option(collected, "spamAllowFist", "Allow fist", "Allow left spam-key clicking with an empty hand. On by default, including when the allowed-item filter is on.", Visualizer.SwitchVisualizer.class);
        option(collected, "boostDelayMs", "Physical first-boost delay (ms)", "Wait this long before the first generated physical-assist click. Does not delay activation-keybind or mouse-hold spam.", Visualizer.SliderVisualizer.class);
        slider(collected, "boostDelayMs", 0.0f, 150.0f, 5.0f);
        option(collected, "clickingProfile", "Clicking profile", "Humble follows the sampled bursts and dips. Performative narrows tempo swings around the sampled median. Both preserve paired-click rhythm.", Visualizer.RadioVisualizer.class);
        collected.get("clickingProfile").addMetadata("options", new String[]{"Humble", "Performative"});
        option(collected, "separateClickSides", "Separate left/right behavior", "Use Wren’s slower right-click rhythm. Turn off to use independent left-model rhythms for both buttons, with right CPS reduced by 10%.", Visualizer.SwitchVisualizer.class);
        option(collected, "profileCpsCeiling", "Target CPS ceiling", "Highest generated total-rate target. Manual clicks are never suppressed. Humble can briefly target 21–22 left CPS; these rare peaks extend beyond the sample.", Visualizer.SliderVisualizer.class);
        slider(collected, "profileCpsCeiling", 1.0f, 22.0f, 0.1f);
        option(collected, "holdEnabled", "Enable button hold", "Latch Minecraft’s attack or use binding down until its toggle key is pressed again. Suppresses generated clicks on that side.", Visualizer.SwitchVisualizer.class);
        option(collected, "holdLeftBind", "Toggle left hold", "Press once to hold the attack binding; press again to release it. Unassigned by default.", Visualizer.KeybindVisualizer.class);
        option(collected, "holdRightBind", "Toggle right hold", "Press once to hold the use binding; press again to release it. Unassigned by default.", Visualizer.KeybindVisualizer.class);
        option(collected, "hitEffectsEnabled", "Enable hit effects", "Show a floating hit count, observed damage and target health after your attacks. Critical hits and longer combos use different colors.", Visualizer.SwitchVisualizer.class);
        option(collected, "hitEffectsShowHealth", "Show target health", "Display the target's current and maximum health beside a red heart under the crosshair.", Visualizer.SwitchVisualizer.class);
        option(collected, "hitEffectsIgnoreNpcs", "Ignore players outside the tab list", "Hide health and hit popups for entities whose player name is absent from the tab list. Enabled by default; also excludes non-player mobs.", Visualizer.SwitchVisualizer.class);
        option(collected, "hitComboResetMs", "Combo reset delay (ms)", "Reset the registered-hit combo after this long without an observed hit, or when attacking a different target.", Visualizer.SliderVisualizer.class);
        slider(collected, "hitComboResetMs", 500.0f, 5000.0f, 100.0f);
        option(collected, "hitEffectDurationMs", "Floating text duration (ms)", "Time for each confirmed hit to pop, fly sideways, vibrate and fade. Default: 350 ms.", Visualizer.SliderVisualizer.class);
        slider(collected, "hitEffectDurationMs", 150.0f, 1500.0f, 10.0f);
        option(collected, "hitPopupPosition", "Popup position", "Show floating hit text above the crosshair or beneath the health indicator. The lower position remains available when health is hidden.", Visualizer.RadioVisualizer.class);
        collected.get("hitPopupPosition").addMetadata("options", new String[]{"Above crosshair", "Below heart"});
        option(collected, "hitEffectOffsetY", "Text vertical offset", "Distance below the crosshair for health and floating hit text, in scaled screen pixels.", Visualizer.SliderVisualizer.class);
        slider(collected, "hitEffectOffsetY", 0.0f, 100.0f, 1.0f);
        option(collected, "heldCrouchCancel", "Crouch cancel", "Cancel mouse-hold clicking while your sneak binding is held or your player is crouching. Release crouch to restart its activation timer.", Visualizer.SwitchVisualizer.class);
        option(collected, "debugEnabled", "Debug mode", "Append every observed user click and generated queue click to config/orven-bw/click-debug.jsonl, including millisecond timestamps and the generating method. Existing data is retained across restarts.", Visualizer.SwitchVisualizer.class);
        option(collected, "showHud", "Show CPS HUD", "Show both left and right totals plus the dominant side’s base + boost calculation. Customize its appearance in the HUD editor.", Visualizer.SwitchVisualizer.class);
        collected.get("spamMode").addMetadata("options", new String[]{"Hold", "Toggle"});
        Tree root = Tree.tree("orven-bw.json");
        leaves(root, collected, "General", "General", "modEnabled", "openClickAssist", "openAutoTool", "globalKeysInfo");
        section(root, collected, "globalKeys", "Keybinds", "General", "General", null, "settingsBind", "toggleModBind");
        leaves(root, collected, "General", "General", "conditionsInfo", "scoreboardOnly", "scoreboardWord");
        leaves(root, collected, "ClickAssist", "Physical CPS Boost", "enabled", "leftClick", "rightClick");
        section(root, collected, "assistFilters", "Filters", "ClickAssist", "Physical CPS Boost", null,
                "activationCps", "requiresPlayer", "disableInCreative", "onlyWhileTargeting", "preserveMining", "blocksOnly");
        leaves(root, collected, "ClickAssist", "Spam click button", "spamEnabled", "spamLeftBind", "spamRightBind", "spamMode");
        section(root, collected, "spamFilters", "Filters", "ClickAssist", "Spam click button", null,
                "spamClickThroughBlocks", "spamBlocksOnly", "spamRequiresPlayer", "spamDisableInCreative", "spamOnlyWhileTargeting");
        section(root, collected, "heldFilters", "Mouse button hold click", "ClickAssist", "Mouse button hold click", "heldClickEnabled",
                "heldClickLeft", "heldClickRight", "heldClickDelayMs", "heldClickInstant", "heldClickRequiresPlayer", "heldClickEntityOnly", "heldCrouchCancel");
        section(root, collected, "hold", "Button Hold", "ClickAssist", "Button Hold", "holdEnabled", "holdLeftBind", "holdRightBind");
        leaves(root, collected, "ClickAssist", "Hit effects", "hitEffectsEnabled");
        section(root, collected, "hitFilters", "Display", "ClickAssist", "Hit effects", null,
                "hitEffectsShowHealth", "hitEffectsIgnoreNpcs");
        section(root, collected, "hitOptions", "Animation", "ClickAssist", "Hit effects", null,
                "hitComboResetMs", "hitEffectDurationMs", "hitEffectOffsetY", "hitPopupPosition");
        leaves(root, collected, "ClickAssist", "Advanced", "advancedInfo");
        section(root, collected, "behavior", "Clicking behavior", "ClickAssist", "Advanced", null,
                "clickingProfile", "separateClickSides", "profileCpsCeiling", "boostDelayMs");
        section(root, collected, "assistWeapons", "Physical boost items", "ClickAssist", "Advanced", "weaponOnly", "assistItems", "assistAllowFist");
        section(root, collected, "spamWeapons", "Spam click button items", "ClickAssist", "Advanced", "spamWeaponOnly", "spamItems", "spamAllowFist");
        section(root, collected, "heldWeapons", "Mouse hold items", "ClickAssist", "Advanced", "heldClickWeaponOnly", "heldItems", "heldClickAllowFist");
        section(root, collected, "debug", "Debugging", "ClickAssist", "Advanced", "debugEnabled", "clearDebugCache");
        section(root, collected, "hud", "CPS HUD", "ClickAssist", "HUD", "showHud", "editHud");
        buildAutoTool(root, collected);
        depends(collected, "scoreboardOnly", "scoreboardWord");
        depends(collected, "weaponOnly", "assistItems");
        depends(collected, "spamWeaponOnly", "spamItems");
        depends(collected, "heldClickWeaponOnly", "heldItems");
        depends(collected, "enabled", "leftClick", "rightClick", "activationCps", "requiresPlayer", "disableInCreative",
                "onlyWhileTargeting", "preserveMining", "blocksOnly", "weaponOnly", "assistItems", "assistAllowFist", "boostDelayMs");
        depends(collected, "leftClick", "weaponOnly", "assistItems", "assistAllowFist", "onlyWhileTargeting", "preserveMining");
        depends(collected, "rightClick", "blocksOnly");
        depends(collected, "spamEnabled", "spamLeftBind", "spamRightBind", "spamMode", "spamClickThroughBlocks",
                "spamBlocksOnly", "spamRequiresPlayer", "spamDisableInCreative", "spamOnlyWhileTargeting",
                "spamWeaponOnly", "spamItems", "spamAllowFist");
        depends(collected, "heldClickEnabled", "heldClickLeft", "heldClickRight", "heldClickDelayMs", "heldClickInstant",
                "heldClickRequiresPlayer", "heldClickEntityOnly", "heldCrouchCancel", "heldClickWeaponOnly", "heldItems", "heldClickAllowFist");
        depends(collected, "heldClickLeft", "heldClickAllowFist");
        depends(collected, "holdEnabled", "holdLeftBind", "holdRightBind");
        depends(collected, "hitEffectsEnabled", "hitEffectsShowHealth", "hitEffectsIgnoreNpcs", "hitComboResetMs",
                "hitEffectDurationMs", "hitEffectOffsetY", "hitPopupPosition");
        // Flat field aliases retain settings saved by every earlier layout.
        for (var field : getClass().getFields()) {
            if (java.lang.reflect.Modifier.isStatic(field.getModifiers()) || root.map.containsKey(field.getName())) continue;
            var alias = Properties.field(null, null, field, this);
            alias.addMetadata("hidden", true);
            root.put(alias);
        }
        return root;
    }
    private static void buildAutoTool(Tree root, Tree collected) {
        option(collected, "autoToolEnabled", "Enable AutoTool", "Select the best mining tool from your hotbar for the block under the crosshair. Requires the global mod switch and conditions.", Visualizer.SwitchVisualizer.class);
        option(collected, "autoToolSwitchDelayMs", "Switch delay (ms)", "Wait before selecting a new tool. Each switch samples the variation below. Default: 160 ms.", Visualizer.SliderVisualizer.class);
        slider(collected, "autoToolSwitchDelayMs", 0, 1000, 10);
        option(collected, "autoToolVariationMs", "Switch variation (±ms)", "Randomly add or subtract up to this duration for each switch; sampled once, never every tick. Default: ±40 ms.", Visualizer.SliderVisualizer.class);
        slider(collected, "autoToolVariationMs", 0, 500, 10);
        option(collected, "autoToolHoverDelayMs", "Hover delay (ms)", "Also require the crosshair to remain on the same block this long. Runs alongside switch delay, not after it.", Visualizer.SliderVisualizer.class);
        slider(collected, "autoToolHoverDelayMs", 0, 1000, 10);
        option(collected, "autoToolOnlyCrouching", "Only while crouching", "Allow tool switching only while your player is crouched.", Visualizer.SwitchVisualizer.class);
        option(collected, "autoToolRequireLeftMouse", "Require left mouse", "Switch only while physically holding the left mouse button. Turn off to select a tool just by aiming at a block.", Visualizer.SwitchVisualizer.class);
        option(collected, "autoToolSwitchBack", "Switch back when done", "Restore the previous slot after releasing the mouse, looking away, or no longer meeting the conditions. Off by default.", Visualizer.SwitchVisualizer.class);
        option(collected, "autoToolOverrideSwitchBack", "Override switch-back slot", "While AutoTool owns the slot, number keys and scrolling update the slot to restore when done instead of interrupting mining.", Visualizer.SwitchVisualizer.class);
        option(collected, "autoToolIgnoreHeldItems", "Held item blacklist", "Pause switching while holding an item selected below. Pauses during right-click item use regardless of this setting.", Visualizer.SwitchVisualizer.class);
        option(collected, "autoToolWhitelistEnabled", "Block whitelist", "Switch tools only for the selected block families. Starts with wool, sandstone, glass, ladders, wood, obsidian and hardened clay.", Visualizer.SwitchVisualizer.class);
        option(collected, "autoToolBlacklistEnabled", "Block blacklist", "Prevent tool switching for the selected block families. The blacklist takes priority over the whitelist.", Visualizer.SwitchVisualizer.class);
        leaves(root, collected, "AutoTool", "General", "autoToolEnabled");
        section(root, collected, "autoToolConditions", "Conditions", "AutoTool", "General", null, "autoToolOnlyCrouching", "autoToolRequireLeftMouse");
        section(root, collected, "autoToolTiming", "Timing", "AutoTool", "General", null, "autoToolSwitchDelayMs", "autoToolVariationMs", "autoToolHoverDelayMs");
        section(root, collected, "autoToolSwap", "Switch-back", "AutoTool", "General", null, "autoToolSwitchBack", "autoToolOverrideSwitchBack");
        section(root, collected, "autoToolHeldItems", "Held item blacklist", "AutoTool", "Filters", "autoToolIgnoreHeldItems", "autoToolIgnoredItems");
        section(root, collected, "autoToolAllowedBlocks", "Block whitelist", "AutoTool", "Filters", "autoToolWhitelistEnabled", "autoToolWhitelist");
        section(root, collected, "autoToolBlockedBlocks", "Block blacklist", "AutoTool", "Filters", "autoToolBlacklistEnabled", "autoToolBlacklist");
        depends(collected, "autoToolEnabled", "autoToolOnlyCrouching", "autoToolRequireLeftMouse", "autoToolSwitchDelayMs", "autoToolVariationMs", "autoToolHoverDelayMs", "autoToolSwitchBack", "autoToolOverrideSwitchBack", "autoToolIgnoreHeldItems", "autoToolIgnoredItems", "autoToolWhitelistEnabled", "autoToolWhitelist", "autoToolBlacklistEnabled", "autoToolBlacklist");
        depends(collected, "autoToolSwitchBack", "autoToolOverrideSwitchBack");
        depends(collected, "autoToolIgnoreHeldItems", "autoToolIgnoredItems");
        depends(collected, "autoToolWhitelistEnabled", "autoToolWhitelist");
        depends(collected, "autoToolBlacklistEnabled", "autoToolBlacklist");
    }
    private static void option(Tree tree, String id, String title, String description, Class<?> visualizer) {
        Node node = tree.get(id);
        node.removeMetadata("hidden");
        node.addMetadata("title", title);
        node.addMetadata("description", description);
        node.addMetadata("visualizer", visualizer);
    }
    private static void slider(Tree tree, String id, float min, float max, float step) {
        tree.get(id).addMetadata("min", min);
        tree.get(id).addMetadata("max", max);
        tree.get(id).addMetadata("step", step);
    }
    private static void leaves(Tree root, Tree collected, String category, String subcategory, String... fields) {
        for (String field : fields) {
            Node node = collected.get(field);
            node.addMetadata("category", category);
            node.addMetadata("subcategory", subcategory);
            root.put(node);
        }
    }
    private static void section(Tree root, Tree collected, String id, String title, String category,
                                String subcategory, String toggle, String... fields) {
        Tree section = Tree.tree(id);
        section.addMetadata("title", title);
        section.addMetadata("description", "Expand to configure " + title.toLowerCase(java.util.Locale.ROOT) + ".");
        section.addMetadata("category", category);
        section.addMetadata("subcategory", subcategory);
        section.addMetadata("collapsed", true);
        if (toggle != null) {
            Node head = collected.get(toggle);
            head.removeMetadata("visualizer");
            head.removeMetadata("hidden");
            section.put(head);
        }
        for (String field : fields) section.put(collected.get(field));
        root.put(section);
    }
    @SuppressWarnings("unchecked")
    private static void depends(Tree collected, String toggle, String... fields) {
        for (String field : fields) collected.getProp(field).addDisplayCondition((Property<Boolean>) collected.getProp(toggle), true);
    }
    @Override protected void initialize(boolean byManager) { super.initialize(byManager); migrate(); }
    public void migrate() { if (migrateValues()) save(); }
    boolean migrateValues() {
        if (configSchema >= 6) return false;
        if (configSchema == 5) {
            if (hitEffectDurationMs == 500) hitEffectDurationMs = 350;
            configSchema = 6; return true;
        }
        if (configSchema == 4) { migrateItems(); configSchema = 5; migrateValues(); return true; }
        if (configSchema < 2) {
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
            spamWeaponOnly = weaponOnly; spamOnlyWhileTargeting = onlyWhileTargeting;
            spamSwords = swords; spamAxes = axes; spamRods = rods; spamSticks = sticks; spamHoes = hoes; spamShovels = shovels;
            spamBlocksOnly = blocksOnly; spamRequiresPlayer = requiresPlayer; spamDisableInCreative = disableInCreative;
        }
        boolean useSpam = !enabled && (spamEnabled || heldClickEnabled);
        highCps = useSpam ? spamHighCps : assistHighCps;
        mediumCps = useSpam ? spamMediumCps : assistMediumCps;
        lowCps = useSpam ? spamLowCps : assistLowCps;
        rampEnabled = useSpam ? spamRampEnabled : assistRampEnabled;
        rampMs = useSpam ? spamRampMs : assistRampMs;
        randomizeTiming = useSpam ? spamVaryTiming : assistVaryTiming;
        exhaustionEnabled = useSpam ? spamExhaustionEnabled : assistExhaustionEnabled;
        exhaustionAfterMs = useSpam ? spamExhaustionAfterMs : assistExhaustionAfterMs;
        exhaustionIntervalMs = useSpam ? spamExhaustionIntervalMs : assistExhaustionIntervalMs;
        exhaustionChance = useSpam ? spamExhaustionChance : assistExhaustionChance;
        exhaustionDurationMs = useSpam ? spamExhaustionDurationMs : assistExhaustionDurationMs;
        exhaustedMinCps = useSpam ? spamExhaustionMinCps : assistExhaustionMinCps;
        exhaustedMaxCps = useSpam ? spamExhaustionMaxCps : assistExhaustionMaxCps;
        OneConfigKeybind bind = spamButton == 1 ? spamRightBind : spamLeftBind;
        bind.setKeyCodes(spamBind.getKeyCodes() == null ? new int[0] : spamBind.getKeyCodes().clone());
        bind.setMouseBtns(spamBind.getMouseBtns() == null ? new int[0] : spamBind.getMouseBtns().clone());
        bind.setMods(spamBind.getMods());
        bind.setDurationNanos(spamBind.getDurationNanos());
        if (spamButton == 1) { spamLeftBind.setKeyCodes(new int[0]); spamLeftBind.setMouseBtns(new int[0]); }
        migrateItems();
        configSchema = 5;
        migrateValues();
        return true;
    }
    private void migrateItems() {
        assistItems = legacyItems(swords, sticks, axes, rods, hoes, shovels);
        spamItems = legacyItems(spamSwords, spamSticks, spamAxes, spamRods, spamHoes, spamShovels);
    }
    private static String[] legacyItems(boolean swords, boolean sticks, boolean axes, boolean rods, boolean hoes, boolean shovels) {
        var items = new java.util.ArrayList<String>();
        items.add("minecraft:beef");
        if (swords) items.add("minecraft:diamond_sword");
        if (sticks) items.add("minecraft:stick");
        if (rods) items.add("minecraft:fishing_rod");
        for (String material : new String[]{"wooden", "stone", "iron", "golden", "diamond"}) {
            if (axes) items.add("minecraft:" + material + "_axe");
            if (hoes) items.add("minecraft:" + material + "_hoe");
            if (shovels) items.add("minecraft:" + material + "_shovel");
        }
        return items.toArray(String[]::new);
    }
    public ClickProfileSession.Options profileOptions() {
        return new ClickProfileSession.Options(clickingProfile, separateClickSides, profileCpsCeiling);
    }
    public static void openSettings() { OneConfigUI.open(new ModConfigRoute("orven-bw.json", "General")); }
    public void toggleMod() {
        getProperty("modEnabled").setAs(!modEnabled);
        var mod = io.github.liwwyy.orvenbw.OrvenBw.instance();
        if (mod != null) mod.features().reset();
        save();
    }
    private static void input(int action, boolean down) {
        var mod = io.github.liwwyy.orvenbw.OrvenBw.instance();
        if (mod != null) mod.clickAssist().activation(action, down);
    }
    public OrvenConfig() { super("orven-bw.json", "orven-bw", Category.UTILITY); }
}
