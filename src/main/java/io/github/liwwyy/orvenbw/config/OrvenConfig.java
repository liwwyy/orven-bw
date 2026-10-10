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
    @Include public boolean playerEspEnabled = false;
    @MultiSelectDropdown(title = "Render styles", description = "Combine any styles: silhouette outline, 2D bounds, wireframe box, feet ring, filled bounds and animated skeleton.", category = "ESP", subcategory = "Player ESP", options = {"2D", "Box", "Outline", "Ring", "Shaded", "Skeleton"})
    public boolean[] espStyles = {false, false, true, false, false, false};
    @Color(alpha = false, title = "Custom colour", description = "Fallback colour when no team colour is available. Rainbow can replace this fallback.", category = "ESP", subcategory = "Player ESP")
    public org.polyfrost.compose.render.PolyColor espColor = org.polyfrost.compose.render.PolyColor.Companion.rgba(0, 255, 0, 255);
    @Include public boolean espTeamColor = true;
    @Include public boolean espRainbow = false;
    @Include public boolean espHealthBar = true;
    @Include public boolean espRedOnDamage = true;
    @Include public boolean espRenderSelf = false;
    @Include public boolean espShowInvisible = true;
    @Include public boolean espIgnoreNpcs = true;
    @Include public int espMaxDistance = 128;
    @Include public boolean bedWaypointsEnabled = false;
    @Include public int bedWoolRadius = 16;
    @Include public float espOutlineWidth = 3f;
    @Include public boolean bedShowDistance = true;
    @Include public boolean bedEdgeMarkers = false;
    @Include public boolean bedObsidianMarkers = true;
    @Include public float bedMarkerScale = 1.0f;
    @Include public boolean bedAlertsEnabled = false;
    @Include public boolean bedAlertArmor = true;
    @Include public boolean bedAlertFireball = false;
    @Include public boolean bedAlertPearl = true;
    @Include public boolean bedAlertHeldObsidian = true;
    @Include public boolean bedAlertPlacedObsidian = true;
    @Include public boolean bedAlertHighlightObsidian = true;
    @Include public boolean bedAlertSound = true;
    @Include public boolean bedAlertIgnoreNpcs = true;
    @Include public boolean espOccludedOnly = false;
    @Include public boolean espShowHotbar = false;
    @Include public boolean espDebug = false;
    @Include public boolean bedAlertFireballVisible = true;
    @Include public boolean bedAlertFlyingFireball = false;
    @Include public boolean bedAlertBow = false;
    @Include public boolean bedAlertArrow = false;
    @Include public boolean bedAlertStick = false;
    @Include public int bedWarningSound = 0;
    @Include public boolean autoBlockEnabled = false;
    @Include public double autoBlockRange = 4;
    @Include public int autoBlockHurtMs = 200;
    @Include public int autoBlockHoldMs = 150;
    @Include public int autoBlockLagChance = 100;
    @Include public int autoBlockLagMs = 200;
    @Include public boolean autoBlockPreventAttackDelay = true;
    @Include public boolean autoBlockAgain = true;
    @Include public boolean autoBlockAnimation = true;
    @Include public boolean autoBlockRequireLeft = true;
    @Include public boolean autoBlockRequireRight = false;
    @Include public boolean autoBlockDamagedOnly = false;
    @Include public boolean autoBlockIgnoreTeam = true;
    @Include public boolean indicatorsEnabled = false;
    @Include public boolean indicatorArrows = true;
    @Include public boolean indicatorPearls = true;
    @Include public boolean indicatorFireballs = true;
    @Include public boolean indicatorEggs = false;
    @Include public boolean indicatorSnowballs = false;
    @Include public boolean indicatorArrowPath = false;
    @Include public boolean indicatorPearlPath = false;
    @Include public boolean indicatorFireballPath = true;
    @Include public int indicatorShape = 0;
    @Include public int indicatorRadius = 50;
    @Include public int indicatorFont = 0;
    @Include public boolean indicatorColors = true;
    @Include public boolean indicatorItems = true;
    @Include public boolean indicatorDistance = true;
    @Include public boolean indicatorApproaching = false;
    @Include public boolean indicatorOffscreen = false;

    @Info(title = "Waypoint reliability", description = "Works most reliably with render distance 8 or higher on servers such as Hypixel and PikaNetwork. Teams come only from the nearest lobby wool. Join before the match starts so beds can be scouted.", category = "ESP", subcategory = "Bed Waypoints")
    public boolean bedReliabilityInfo;

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
    @Include public boolean entityCpsFloorEnabled = true;
    @Include public double entityCpsFloor = 8;
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
    public String[] autoToolWhitelist = {"minecraft:wool", "minecraft:sandstone", "minecraft:ladder", "minecraft:planks", "minecraft:log", "minecraft:obsidian", "minecraft:hardened_clay", "minecraft:end_stone"};
    @ItemList(title = "Blocked blocks", description = "Never switch for these block families, even if allowed above. Uses the same color and wood grouping as the whitelist.", category = "AutoTool", subcategory = "Filters")
    public String[] autoToolBlacklist = {};
    @Include public boolean autoSoupEnabled = false;
    @Include public double autoSoupHealthMin = 4;
    @Include public double autoSoupHealthMax = 14;
    @Include public int autoSoupMaxPerCycle = 2;
    @Include public boolean autoSoupRefill = true;
    @Include public boolean autoSoupDisableLeft = true;
    @Include public boolean autoSoupScoreboardOnly = false;
    @Include public String autoSoupScoreboardWord = "mineberry.org";
    @Include public int autoSoupConsumeMinMs = 140;
    @Include public int autoSoupConsumeMaxMs = 220;
    @Include public int autoSoupReturnMinMs = 160;
    @Include public int autoSoupReturnMaxMs = 260;
    @Include public int autoSoupMoveMinMs = 150;
    @Include public int autoSoupMoveMaxMs = 230;
    @Include public int autoSoupResponseTimeoutMs = 830;
    @Include public int autoSoupCycleCooldownMs = 330;
    @Include public int autoSoupHoldTimeoutMs = 2000;
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
    @Button(title = "AutoSoup", text = "Open AutoSoup", icon = "assets/orvenbw/icons/soup.svg", description = "Configure automatic soup healing, hotbar refill and timing.")
    public void openAutoSoup() { OneConfigUI.open(new ModConfigRoute("orven-bw.json", "AutoSoup")); }
    @Button(title = "ESP", text = "Open ESP", icon = "assets/orvenbw/icons/esp.svg", description = "Open player outlines, bed waypoints and obsidian defence markers.")
    public void openEsp() { OneConfigUI.open(new ModConfigRoute("orven-bw.json", "ESP")); }
    @Button(title = "Indicators", text = "Open Indicators", icon = "assets/orvenbw/icons/indicators.svg", description = "Open projectile pointers, trajectories and Bedwars alerts.")
    public void openIndicators() { OneConfigUI.open(new ModConfigRoute("orven-bw.json", "Indicators")); }
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
        option(collected, "entityCpsFloorEnabled", "Try to maintain high CPS when targeting an entity", "After the initial 1.5-second ramp, keep the left-click target above the minimum while looking at a living entity. Applies to eligible physical boost and both spam modes; never bypasses filters or the CPS ceiling.", Visualizer.SwitchVisualizer.class);
        option(collected, "entityCpsFloor", "Entity target minimum CPS", "Minimum total-rate target while looking at a living entity. Actual one-second counts can vary; physical clicks are never suppressed. Default: 8 CPS.", Visualizer.SliderVisualizer.class);
        slider(collected, "entityCpsFloor", 1, 22, .1f);
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
        leaves(root, collected, "General", "General", "modEnabled", "openClickAssist", "openAutoTool", "openAutoSoup", "openEsp", "openIndicators", "globalKeysInfo");
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
        buildCombatExtras(root,collected);
        leaves(root, collected, "ClickAssist", "Advanced", "advancedInfo");
        section(root, collected, "behavior", "Clicking behavior", "ClickAssist", "Advanced", null,
                "clickingProfile", "separateClickSides", "profileCpsCeiling", "boostDelayMs", "entityCpsFloorEnabled", "entityCpsFloor");
        section(root, collected, "assistWeapons", "Physical boost items", "ClickAssist", "Advanced", "weaponOnly", "assistItems", "assistAllowFist");
        section(root, collected, "spamWeapons", "Spam click button items", "ClickAssist", "Advanced", "spamWeaponOnly", "spamItems", "spamAllowFist");
        section(root, collected, "heldWeapons", "Mouse hold items", "ClickAssist", "Advanced", "heldClickWeaponOnly", "heldItems", "heldClickAllowFist");
        section(root, collected, "debug", "Debugging", "ClickAssist", "Advanced", "debugEnabled", "clearDebugCache");
        section(root, collected, "hud", "CPS HUD", "ClickAssist", "HUD", "showHud", "editHud");
        buildAutoTool(root, collected);
        buildAutoSoup(root, collected);
        buildEsp(root, collected);
        buildIndicators(root,collected);
        disableFeatureControlsWithGlobal(root,collected);
        depends(collected, "entityCpsFloorEnabled", "entityCpsFloor");
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
        root.addMetadata("mod_card_icon_path","assets/orvenbw/icons/icon.png");
        return root;
    }
    private void buildEsp(Tree root, Tree collected) {
        option(collected, "playerEspEnabled", "Enable Player ESP", "Highlight players using one or several render styles. Requires the global mod switch and conditions.", Visualizer.SwitchVisualizer.class);
        option(collected, "espTeamColor", "Team colours", "Use each player's current scoreboard-team or nametag colour. Unknown colours use the custom colour or rainbow.", Visualizer.SwitchVisualizer.class);
        option(collected, "espRainbow", "Rainbow fallback", "Cycle colours for players without a team colour, or for everyone when Team colours is off.", Visualizer.SwitchVisualizer.class);
        option(collected, "espHealthBar", "Health bars", "Draw a shorter, borderless pill bar that becomes smaller with distance.", Visualizer.SwitchVisualizer.class);
        option(collected, "espRedOnDamage", "Red on damage", "Temporarily colour damaged players red while their hurt animation is active.", Visualizer.SwitchVisualizer.class);
        option(collected,"espOccludedOnly","Only highlight behind walls","Show only occluded silhouette pixels in Outline mode. Other styles and bars require a partially obstructed player.",Visualizer.SwitchVisualizer.class);
        option(collected,"espShowHotbar","Observed player hotbar items","Show up to nine recently seen held items, without armour. Servers do not reveal remote hotbar slots; this is an observation history.",Visualizer.SwitchVisualizer.class);
        option(collected,"espDebug","ESP debug logging","Append throttled detection/render diagnostics to config/orven-bw/esp-debug.jsonl and the console. Includes bed slots, scoreboard signals and team-assignment failures.",Visualizer.SwitchVisualizer.class);
        option(collected, "espRenderSelf", "Render yourself", "Include your player in third-person views. First-person rendering is excluded.", Visualizer.SwitchVisualizer.class);
        option(collected, "espShowInvisible", "Show invisible players", "Include invisible players in overlays and the silhouette outline pass.", Visualizer.SwitchVisualizer.class);
        option(collected, "espIgnoreNpcs", "Ignore NPCs", "Exclude players whose username is absent from the server tab list.", Visualizer.SwitchVisualizer.class);
        option(collected, "espMaxDistance", "Maximum distance (blocks)", "Do not render player overlays beyond this distance. Default: 128 blocks.", Visualizer.SliderVisualizer.class);
        slider(collected, "espMaxDistance", 32, 256, 8);
        option(collected, "bedWaypointsEnabled", "Enable Bed Waypoints", "Show see-through team initials anchored above beds in recognized Bedwars lobbies and matches.", Visualizer.SwitchVisualizer.class);
        option(collected,"bedWoolRadius","Lobby wool search radius","Search this many blocks around each bed for the nearest wool. Equally near conflicting colours remain unassigned and are logged in ESP debug mode. Default: 16.",Visualizer.SliderVisualizer.class);
        slider(collected,"bedWoolRadius",1,32,1);
        option(collected,"espOutlineWidth","Outline border width","Near-player border width in screen pixels. It scales down at distance. Default: 3 pixels.",Visualizer.SliderVisualizer.class);
        slider(collected,"espOutlineWidth",1,6,.25f);
        option(collected, "bedShowDistance", "Show distance", "Display distance below bed labels; hide distance within five blocks.", Visualizer.SwitchVisualizer.class);
        option(collected, "bedEdgeMarkers", "Screen-edge markers", "Keep off-screen beds visible at the screen edge. Markers move onto their world position as you approach.", Visualizer.SwitchVisualizer.class);
        option(collected, "bedObsidianMarkers", "Obsidian defence markers", "Show a purple count below beds: six horizontal defence positions plus two above the bed. Unknown positions are marked with a question mark.", Visualizer.SwitchVisualizer.class);
        option(collected, "bedMarkerScale", "Marker size", "Scale bed initials, distance labels and obsidian counters together.", Visualizer.SliderVisualizer.class);
        slider(collected, "bedMarkerScale", .5f, 2, .1f);
        leaves(root, collected, "ESP", "Player ESP", "playerEspEnabled");
        section(root, collected, "espAppearance", "Appearance", "ESP", "Player ESP", null, "espStyles", "espTeamColor", "espColor", "espRainbow", "espHealthBar", "espRedOnDamage", "espOutlineWidth", "espOccludedOnly", "espShowHotbar");
        section(root, collected, "espFilters", "Filters", "ESP", "Player ESP", null, "espRenderSelf", "espShowInvisible", "espIgnoreNpcs", "espMaxDistance");
        leaves(root, collected, "ESP", "Bed Waypoints", "bedWaypointsEnabled", "bedReliabilityInfo");
        section(root, collected, "bedDisplay", "Display and discovery", "ESP", "Bed Waypoints", null, "bedWoolRadius", "bedShowDistance", "bedEdgeMarkers", "bedMarkerScale", "bedObsidianMarkers");
        depends(collected,"playerEspEnabled","espStyles","espTeamColor","espColor","espRainbow","espHealthBar","espRedOnDamage","espOutlineWidth","espOccludedOnly","espShowHotbar","espRenderSelf","espShowInvisible","espIgnoreNpcs","espMaxDistance");
        depends(collected,"bedWaypointsEnabled","bedWoolRadius","bedShowDistance","bedEdgeMarkers","bedMarkerScale","bedObsidianMarkers");
        leaves(root,collected,"ESP","Advanced","espDebug");
        var styles=collected.getProp("espStyles");
        var occluded=collected.getProp("espOccludedOnly");
        collected.getProp("espOutlineWidth").addDisplayCondition(() -> espStyles != null && espStyles.length > 2 && espStyles[2] ? Property.Display.SHOWN : Property.Display.DISABLED);
        styles.addCallback(value -> { collected.getProp("espOutlineWidth").revaluateDisplay(); return false; });
        occluded.addDisplayCondition(() -> espStyles != null && espStyles.length > 2 && espStyles[2] ? Property.Display.SHOWN : Property.Display.DISABLED);
        styles.addCallback(value -> { occluded.revaluateDisplay(); return false; });
    }
    private static void buildCombatExtras(Tree root,Tree collected) {
        option(collected,"autoBlockEnabled","Enable AutoBlock","Hold and release your sword block against nearby players using vanilla keybindings. Includes Raven's timed outbound-lag options. Off by default.",Visualizer.SwitchVisualizer.class);
        String[] ids={"autoBlockRange","autoBlockHurtMs","autoBlockHoldMs","autoBlockLagChance","autoBlockLagMs","autoBlockPreventAttackDelay","autoBlockAgain","autoBlockAnimation","autoBlockRequireLeft","autoBlockRequireRight","autoBlockDamagedOnly","autoBlockIgnoreTeam"};
        String[] titles={"Target range","Maximum hurt time (ms)","Maximum block hold (ms)","Lag chance (%)","Maximum lag duration (ms)","Prevent delaying attacks","Block again immediately","Force block animation","Require left mouse","Require right mouse","Only when damaged","Ignore teammates"};
        String[] descriptions={"Find a real, tab-listed player whose nearest hitbox point is within this many blocks of your eyes. Default: 4.","While damaged-only is enabled, begin blocking at this remaining hurt-animation time. Default: 200 ms.","Release the block after this duration. Timings round up to Minecraft ticks. Default: 150 ms.","Chance of briefly buffering vanilla outbound play packets after a block. Zero disables lag. Default: 100%.","Lag deadline measured from the start of the block, as in Raven. A hold already beyond this deadline does not start lag. Default: 200 ms.","Flush buffered packets before an attack so the attack is sent immediately.","Restart blocking after lag expires, or an attack releases lag, while the conditions still hold.","Keep the local sword block animation during outbound lag without changing server-side item-use state.","Run automatic block cycles only while physically holding your attack binding.","Run automatic block cycles only while physically holding your use binding. Manual right-only blocking stays available.","Start at the configured hurt-time threshold and release when a fresh damage animation begins.","Do not target a player on your scoreboard team or with the same effective nametag colour."};
        for(int i=0;i<ids.length;i++) option(collected,ids[i],titles[i],descriptions[i],i<5?Visualizer.SliderVisualizer.class:Visualizer.SwitchVisualizer.class);
        slider(collected,"autoBlockRange",2,6,.1f); slider(collected,"autoBlockHurtMs",50,500,50); slider(collected,"autoBlockHoldMs",50,500,50); slider(collected,"autoBlockLagChance",0,100,5); slider(collected,"autoBlockLagMs",50,500,50);
        leaves(root,collected,"ClickAssist","AutoBlock","autoBlockEnabled");
        section(root,collected,"autoBlockConditions","Conditions","ClickAssist","AutoBlock",null,"autoBlockRange","autoBlockRequireLeft","autoBlockRequireRight","autoBlockDamagedOnly","autoBlockIgnoreTeam");
        section(root,collected,"autoBlockTiming","Blocking and lag","ClickAssist","AutoBlock",null,"autoBlockHurtMs","autoBlockHoldMs","autoBlockLagChance","autoBlockLagMs","autoBlockPreventAttackDelay","autoBlockAgain","autoBlockAnimation");
        depends(collected,"autoBlockEnabled",ids); disable(collected,"autoBlockDamagedOnly","autoBlockHurtMs");
    }
    private static void buildIndicators(Tree root,Tree collected) {
        option(collected,"indicatorsEnabled","Enable Indicators","Show directional projectile pointers around the crosshair and optional predicted paths. Includes arrows, pearls, fireballs, eggs and snowballs.",Visualizer.SwitchVisualizer.class);
        String[] projectiles={"indicatorArrows","indicatorPearls","indicatorFireballs","indicatorEggs","indicatorSnowballs","indicatorArrowPath","indicatorPearlPath","indicatorFireballPath"};
        String[] names={"Arrows","Ender pearls","Fireballs","Eggs","Snowballs","Arrow trajectory","Pearl trajectory","Fireball trajectory"};
        for(int i=0;i<projectiles.length;i++) option(collected,projectiles[i],names[i],i<5?"Include these airborne projectile entities in directional indicators.":"Draw the predicted flight path and impact location, including block/entity collisions. Prediction stops at unloaded terrain.",Visualizer.SwitchVisualizer.class);
        option(collected,"indicatorShape","Pointer style","Choose Raven's caret, greater-than character or triangle pointer.",Visualizer.DropdownVisualizer.class);
        collected.get("indicatorShape").addMetadata("options",new String[]{"Caret","Greater than","Triangle"});
        option(collected,"indicatorFont","Font","Use the Minecraft or OneConfig HUD font for indicator text.",Visualizer.DropdownVisualizer.class);
        collected.get("indicatorFont").addMetadata("options",new String[]{"Minecraft","OneConfig"});
        option(collected,"indicatorRadius","Circle radius","Distance in GUI pixels from the crosshair to projectile pointers. Item icons add twenty pixels.",Visualizer.SliderVisualizer.class); slider(collected,"indicatorRadius",30,200,5);
        String[] flags={"indicatorColors","indicatorItems","indicatorDistance","indicatorApproaching","indicatorOffscreen"};
        String[] flagTitles={"Item colours","Show item icon","Show distance","Only when approaching","Only off-screen"};
        String[] desc={"Colour pearls purple, fireballs orange, eggs pale yellow and other projectiles white.","Show a projectile's Minecraft item icon inside the pointer ring.","Show distance from your player beside each indicator.","Require the projectile to move at least one block closer over five ticks, matching Raven's approach filter.","Hide indicators for projectiles already inside the camera view."};
        for(int i=0;i<flags.length;i++) option(collected,flags[i],flagTitles[i],desc[i],Visualizer.SwitchVisualizer.class);
        leaves(root,collected,"Indicators","Projectiles","indicatorsEnabled");
        section(root,collected,"indicatorEntities","Entities and paths","Indicators","Projectiles",null,projectiles);
        section(root,collected,"indicatorAppearance","Appearance and filters","Indicators","Projectiles",null,"indicatorShape","indicatorRadius","indicatorFont","indicatorColors","indicatorItems","indicatorDistance","indicatorApproaching","indicatorOffscreen");
        depends(collected,"indicatorsEnabled",projectiles); depends(collected,"indicatorsEnabled",flags); depends(collected,"indicatorsEnabled","indicatorShape","indicatorRadius","indicatorFont");
        disable(collected,"indicatorArrows","indicatorArrowPath"); disable(collected,"indicatorPearls","indicatorPearlPath"); disable(collected,"indicatorFireballs","indicatorFireballPath");
        option(collected, "bedAlertsEnabled", "Enable Bedwars Alerts", "Show local chat warnings during recognized Bedwars matches. NPC filtering and alert sounds are configurable below.", Visualizer.SwitchVisualizer.class);
        String[] ids = {"bedAlertArmor", "bedAlertFireball", "bedAlertFireballVisible", "bedAlertFlyingFireball", "bedAlertBow", "bedAlertArrow", "bedAlertStick", "bedAlertPearl", "bedAlertHeldObsidian", "bedAlertPlacedObsidian", "bedAlertHighlightObsidian", "bedAlertSound", "bedAlertIgnoreNpcs"};
        String[] titles = {"Diamond armour", "Held fireball", "Fireball holder: require line of sight", "Fireball in flight", "Held bow", "Arrow in flight", "Held stick", "Held ender pearl", "Held obsidian", "Bed-defence obsidian", "Highlight placed obsidian", "Alert sound", "Ignore NPCs"};
        String[] descriptions = {"Warn once per player per match when diamond leggings are observed.", "Warn when a player starts holding a fireball; includes their distance.", "Warn only if at least one eye/body ray to the fireball holder is unobstructed by solid blocks. On by default.", "Warn once per newly observed fireball entity in flight.", "Warn when another player starts holding a bow.", "Warn once per airborne arrow; embedded arrows do not count.", "Warn when another player starts holding a stick.", "Warn when a player starts holding an ender pearl; includes their distance.", "Warn when a player starts holding obsidian; includes their distance.", "Warn when a bed's observed obsidian defence count increases. Include the team only when its identity is confirmed.", "Outline obsidian touching a bed through blocks. Remove highlights when blocks disappear.", "Play the custom warning sound selected below with chat warnings.", "Ignore players whose username is absent from the server tab list."};
        for (int j = 0; j < ids.length; j++) option(collected, ids[j], titles[j], descriptions[j], Visualizer.SwitchVisualizer.class);
        option(collected,"bedWarningSound","Warning sound","Choose one of the two supplied warning sounds, converted to compact mono Ogg for Minecraft playback.",Visualizer.DropdownVisualizer.class);
        collected.get("bedWarningSound").addMetadata("options",new String[]{"Warning 1","Warning 2"});
        leaves(root, collected, "Indicators", "Alerts", "bedAlertsEnabled");
        section(root, collected, "bedWarnings", "Warnings", "Indicators", "Alerts", null, ids);
        leaves(root,collected,"Indicators","Alerts","bedWarningSound");
        depends(collected, "bedAlertsEnabled", ids);
        depends(collected,"bedAlertsEnabled","bedWarningSound");
        disable(collected,"bedAlertSound","bedWarningSound");
        disable(collected,"bedAlertFireball","bedAlertFireballVisible");

    }
    @SuppressWarnings("unchecked")
    private static void disable(Tree collected,String toggle,String... fields) {
        for(String field:fields) collected.getProp(field).addDisplayCondition((Property<Boolean>)collected.getProp(toggle),false);
    }
    @SuppressWarnings("unchecked")
    private static void disableFeatureControlsWithGlobal(Tree root,Tree collected) {
        var master=(Property<Boolean>)collected.getProp("modEnabled");
        for(Node node:root.map.values()) {
            if("General".equals(node.getMetadata("category"))) continue;
            if(node instanceof Tree section) for(Node child:section.map.values()) {
                if(child instanceof Property<?> property) property.addDisplayCondition(master,false);
            }
            else if(node instanceof Property<?> property) property.addDisplayCondition(master,false);
        }
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
        option(collected, "autoToolWhitelistEnabled", "Block whitelist", "Switch tools only for the selected block families. Includes end stone; the wood family excludes chests and ladders.", Visualizer.SwitchVisualizer.class);
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
    private static void buildAutoSoup(Tree root, Tree collected) {
        option(collected, "autoSoupEnabled", "Enable AutoSoup", "Use random hotbar soups when health is low, then return to a sword. Intended for servers where right-clicking soup heals instantly. Off by default.", Visualizer.SwitchVisualizer.class);
        option(collected, "autoSoupHealthMin", "Minimum health threshold", "Lowest possible activation health, in health points (20 = full health). A threshold is sampled once per cycle between this and the maximum.", Visualizer.SliderVisualizer.class);
        slider(collected, "autoSoupHealthMin", 1, 20, .5f);
        option(collected, "autoSoupHealthMax", "Maximum health threshold", "Highest possible activation health. Heal at or below the sampled threshold; stop the cycle when health rises above it.", Visualizer.SliderVisualizer.class);
        slider(collected, "autoSoupHealthMax", 1, 20, .5f);
        option(collected, "autoSoupMaxPerCycle", "Soups per cycle", "Maximum soups to use in one healing cycle. Stops sooner when health recovers. Default: 2.", Visualizer.SliderVisualizer.class);
        slider(collected, "autoSoupMaxPerCycle", 1, 9, 1);
        option(collected, "autoSoupRefill", "Refill hotbar", "At a randomly chosen zero or one soups remaining, open your inventory and swap reserve soup into empty slots or bowls. Preserves swords and other items; never enables inventory movement.", Visualizer.SwitchVisualizer.class);
        option(collected, "autoSoupDisableLeft", "Disable left click while AutoSoup", "Cancel attack clicks and block mining during soup use and automatic refill. Click Assist pauses while AutoSoup owns the hotbar.", Visualizer.SwitchVisualizer.class);
        option(collected, "autoSoupScoreboardOnly", "Scoreboard filter", "Allow AutoSoup only when the visible sidebar contains the text below. Optional and off by default.", Visualizer.SwitchVisualizer.class);
        option(collected, "autoSoupScoreboardWord", "Scoreboard matching text", "Match sidebar text ignoring case and colors. Default: mineberry.org. Also obeys General's global conditions.", Visualizer.TextVisualizer.class);
        String[] timings = {"autoSoupConsumeMinMs", "autoSoupConsumeMaxMs", "autoSoupReturnMinMs", "autoSoupReturnMaxMs", "autoSoupMoveMinMs", "autoSoupMoveMaxMs", "autoSoupResponseTimeoutMs", "autoSoupCycleCooldownMs", "autoSoupHoldTimeoutMs"};
        String[] titles = {"Minimum consume delay (ms)", "Maximum consume delay (ms)", "Minimum sword-return delay (ms)", "Maximum sword-return delay (ms)", "Minimum item-move delay (ms)", "Maximum item-move delay (ms)", "Server response timeout (ms)", "Cycle cooldown (ms)", "Maximum soup hold (ms)"};
        String[] descriptions = {"Shortest wait after selecting a soup before using it. Default: 140 ms.", "Longest wait before using a selected soup. Sampled once per soup; default: 220 ms.", "Minimum sampled wait after pressing right-click before switching back. Also wait for consumption or the maximum hold. Default: 160 ms.", "Maximum sampled minimum wait after pressing right-click. Consumption can require a longer hold. Default: 260 ms. With no sword, restore the previous slot.", "Shortest wait between inventory swaps. Default: 150 ms. One item moves per action.", "Longest sampled wait between inventory swaps. Default: 230 ms. Always recheck source and destination.", "Timeout for inventory refill responses and final consumption confirmation. The maximum soup hold controls how long right-click stays pressed.", "Pause between healing/refill cycles, including failed attempts. Default: 330 ms.", "Keep right-click pressed and the soup selected until it is consumed or this limit expires. Default: 2000 ms, allowing a complete food-use animation. Slot/count changes confirm consumption; health recovery also confirms when no item is still in use."};
        for (int i = 0; i < timings.length; i++) {
            option(collected, timings[i], titles[i], descriptions[i], Visualizer.SliderVisualizer.class);
            slider(collected, timings[i], i == 6 || i == 8 ? 100 : 0, i == 8 ? 5000 : i == 6 ? 3000 : 1000, 1);
        }
        leaves(root, collected, "AutoSoup", "General", "autoSoupEnabled");
        section(root, collected, "autoSoupHealing", "Healing", "AutoSoup", "General", null, "autoSoupHealthMin", "autoSoupHealthMax", "autoSoupMaxPerCycle", "autoSoupDisableLeft");
        leaves(root, collected, "AutoSoup", "General", "autoSoupRefill");
        section(root, collected, "autoSoupConditions", "Conditions", "AutoSoup", "General", null, "autoSoupScoreboardOnly", "autoSoupScoreboardWord");
        section(root, collected, "autoSoupUseTiming", "Consumption timing", "AutoSoup", "Advanced", null, "autoSoupConsumeMinMs", "autoSoupConsumeMaxMs", "autoSoupReturnMinMs", "autoSoupReturnMaxMs", "autoSoupHoldTimeoutMs");
        section(root, collected, "autoSoupRefillTiming", "Refill timing", "AutoSoup", "Advanced", null, "autoSoupMoveMinMs", "autoSoupMoveMaxMs");
        section(root, collected, "autoSoupRecoveryTiming", "Response and cooldown", "AutoSoup", "Advanced", null, "autoSoupResponseTimeoutMs", "autoSoupCycleCooldownMs");
        depends(collected, "autoSoupEnabled", "autoSoupHealthMin", "autoSoupHealthMax", "autoSoupMaxPerCycle", "autoSoupRefill", "autoSoupDisableLeft", "autoSoupScoreboardOnly", "autoSoupScoreboardWord");
        depends(collected, "autoSoupEnabled", timings);
        depends(collected, "autoSoupScoreboardOnly", "autoSoupScoreboardWord");
        depends(collected, "autoSoupRefill", "autoSoupMoveMinMs", "autoSoupMoveMaxMs");
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
        if (configSchema >= 10) return false;
        if (configSchema == 9) { bedEdgeMarkers=false; configSchema=10; return true; }
        if (configSchema == 8) {
            if (autoToolWhitelist != null) autoToolWhitelist = java.util.Arrays.stream(autoToolWhitelist)
                    .filter(id -> id == null || !java.util.Set.of("glass", "stained_glass").contains(id.replace("minecraft:", ""))).toArray(String[]::new);
            if (autoSoupConsumeMinMs == 110 && autoSoupConsumeMaxMs == 135) { autoSoupConsumeMinMs = 140; autoSoupConsumeMaxMs = 220; }
            if (autoSoupReturnMinMs == 113 && autoSoupReturnMaxMs == 135) { autoSoupReturnMinMs = 160; autoSoupReturnMaxMs = 260; }
            if (autoSoupMoveMinMs == 113 && autoSoupMoveMaxMs == 124) { autoSoupMoveMinMs = 150; autoSoupMoveMaxMs = 230; }
            configSchema = 9; migrateValues(); return true;
        }
        if (configSchema == 7) {
            // Upgrade old presets while preserving deliberately customized ranges.
            if (autoSoupConsumeMinMs == 30 && autoSoupConsumeMaxMs == 55) { autoSoupConsumeMinMs = 110; autoSoupConsumeMaxMs = 135; }
            if (autoSoupReturnMinMs == 33 && autoSoupReturnMaxMs == 55) { autoSoupReturnMinMs = 113; autoSoupReturnMaxMs = 135; }
            if (autoSoupMoveMinMs == 33 && autoSoupMoveMaxMs == 44) { autoSoupMoveMinMs = 113; autoSoupMoveMaxMs = 124; }
            if (autoSoupResponseTimeoutMs == 750) autoSoupResponseTimeoutMs = 830;
            if (autoSoupCycleCooldownMs == 250) autoSoupCycleCooldownMs = 330;
            configSchema = 8; migrateValues(); return true;
        }
        if (configSchema == 6) {
            var updated = new java.util.LinkedHashSet<String>();
            if (autoToolWhitelist != null) for (String id : autoToolWhitelist) {
                if (id == null) continue;
                String name = id.replace("minecraft:", "");
                if (!java.util.Set.of("chest", "trapped_chest", "ender_chest").contains(name)) updated.add(id);
            }
            updated.add("minecraft:end_stone"); autoToolWhitelist = updated.toArray(String[]::new);
            configSchema = 7; migrateValues(); return true;
        }
        if (configSchema == 5) {
            if (hitEffectDurationMs == 500) hitEffectDurationMs = 350;
            configSchema = 6; migrateValues(); return true;
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
    public OrvenConfig() { super("orven-bw.json", "assets/orvenbw/icons/icon.png", "orven-bw", Category.UTILITY); }
}
