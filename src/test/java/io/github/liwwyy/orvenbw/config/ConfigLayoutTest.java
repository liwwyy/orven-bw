package io.github.liwwyy.orvenbw.config;

import org.junit.jupiter.api.Test;
import org.polyfrost.oneconfig.api.config.v1.*;
import org.polyfrost.oneconfig.internal.legacy.InputConstants;
import org.polyfrost.oneconfig.internal.ui.search.SettingIndexKt;
import static org.junit.jupiter.api.Assertions.*;

class ConfigLayoutTest {
    private Tree tree(OrvenConfig config) { return config.makeTree(); }
    @Test void generalComesFirstWithVisibleEnableAndIconShortcut() {
        var config = new OrvenConfig(); Tree tree = tree(config);
        assertFalse(config.modEnabled);
        assertEquals("modEnabled", tree.map.keySet().iterator().next());
        assertEquals("Enable orven-bw", tree.get("modEnabled").getTitle());
        assertNotNull(tree.get("modEnabled").getMetadata("visualizer"));
        assertNull(tree.get("modEnabled").getMetadata("hidden"));
        assertEquals("assets/orvenbw/icons/clickassist.svg", tree.get("openClickAssist").getMetadata("icon"));
        assertNotNull(tree.get("openClickAssist").getMetadata("runnable"));
        assertNotNull(getClass().getClassLoader().getResource("assets/orvenbw/icons/clickassist.svg"));
        assertEquals("Only enables the mod if these conditions are met", tree.get("conditionsInfo").description);
        assertArrayEquals(new int[]{InputConstants.KEY_P}, config.settingsBind.getKeyCodes());
        assertFalse(config.toggleModBind.isBound());
    }
    @Test void pageOrderAndVisibleControlsMatchTheRedesign() {
        Tree tree = tree(new OrvenConfig());
        var categories = new java.util.LinkedHashSet<String>();
        var sections = new java.util.LinkedHashSet<String>();
        for (Node node : tree.map.values()) {
            if (node.getMetadata("hidden") != null) continue;
            categories.add(node.getMetadata("category"));
            if ("ClickAssist".equals(node.getMetadata("category"))) sections.add(node.getMetadata("subcategory"));
        }
        assertEquals(java.util.List.of("General", "ClickAssist", "AutoTool", "AutoSoup"), java.util.List.copyOf(categories));
        assertEquals(java.util.List.of("Physical CPS Boost", "Spam click button", "Mouse button hold click", "Button Hold", "Hit effects", "Advanced", "HUD"), java.util.List.copyOf(sections));
        for (String field : new String[]{"enabled", "leftClick", "rightClick", "spamEnabled", "spamLeftBind", "spamRightBind"}) {
            assertNotNull(tree.get(field).getMetadata("visualizer"), field);
            assertNull(tree.get(field).getMetadata("hidden"), field);
        }
        assertNotNull(tree.get("heldFilters", "heldClickEnabled"));
    }
    @Test void everyVisibleControlHasANameAndDescriptionAndAccordionsRender() {
        Tree tree = tree(new OrvenConfig());
        var visible = new java.util.HashSet<String>();
        for (Node node : tree.map.values()) {
            if (node.getMetadata("hidden") != null) continue;
            if (node instanceof Tree section) {
                assertEquals(true, section.getMetadata("collapsed"));
                assertNotNull(SettingIndexKt.buildAccordionNode(section), section.getID());
                for (Node child : section.map.values()) {
                    assertFalse(child instanceof Tree);
                    assertNotNull(child.getTitle(), child.getID());
                    assertNotNull(child.description, child.getID());
                    assertTrue(visible.add(child.getID()), child.getID());
                }
            } else {
                assertNotNull(node.getTitle(), node.getID());
                assertNotNull(node.description, node.getID());
                assertTrue(visible.add(node.getID()), node.getID());
            }
        }
        assertEquals("Advanced", tree.get("assistWeapons").getMetadata("subcategory"));
        assertEquals("Advanced", tree.get("spamWeapons").getMetadata("subcategory"));
        assertEquals("Advanced", tree.get("heldWeapons").getMetadata("subcategory"));
    }
    @Test void profilesAreLiveAndLegacyTimingIsHidden() {
        var config = new OrvenConfig(); Tree tree = tree(config);
        assertEquals(0, config.clickingProfile); assertTrue(config.separateClickSides);
        assertEquals(22, config.profileCpsCeiling);
        assertArrayEquals(new String[]{"Humble", "Performative"}, (String[]) tree.get("behavior", "clickingProfile").getMetadata("options"));
        assertEquals(Visualizer.RadioVisualizer.class, tree.get("behavior", "clickingProfile").getMetadata("visualizer"));
        tree.getProp("behavior", "clickingProfile").setAs(1);
        tree.getProp("behavior", "separateClickSides").setAs(false);
        tree.getProp("behavior", "profileCpsCeiling").setAs(12.5);
        assertEquals("Performative", config.profileOptions().name());
        assertFalse(config.profileOptions().separateSides()); assertEquals(12.5, config.profileOptions().ceiling());
        for (String field : new String[]{"highCps", "mediumCps", "randomizeTiming", "rockyRamp", "rampMs", "exhaustionEnabled"})
            assertEquals(true, tree.get(field).getMetadata("hidden"), field);
        assertEquals(Property.Display.HIDDEN, tree.getProp("scoreboardWord").getDisplay());
        tree.getProp("scoreboardOnly").setAs(true);
        assertEquals(Property.Display.SHOWN, tree.getProp("scoreboardWord").getDisplay());
    }
    @Test void migrationPreservesActiveSpamProfileBindingsAndWeaponFilters() {
        var config = new OrvenConfig(); config.configSchema = 3;
        config.spamEnabled = true; config.spamButton = 1;
        config.spamMediumCps = 11.3; config.spamRampMs = 600;
        config.spamRequiresPlayer = true; config.spamSwords = false;
        assertTrue(config.migrateValues()); assertEquals(8, config.configSchema);
        assertEquals(11.3, config.mediumCps); assertEquals(600, config.rampMs);
        assertTrue(config.spamRequiresPlayer); assertFalse(config.spamSwords);
        assertArrayEquals(config.spamBind.getMouseBtns(), config.spamRightBind.getMouseBtns());
        assertFalse(config.spamLeftBind.isBound());
        config.highCps = 13.2; assertFalse(config.migrateValues()); assertEquals(13.2, config.highCps);
    }
    @Test void legacyMigrationKeepsCustomTimesAndPhysicalProfileWinsWhenBothEnabled() {
        var old = new OrvenConfig(); old.rampMs = 800; old.exhaustionDurationMs = 900;
        assertTrue(old.migrateValues()); assertEquals(800, old.rampMs); assertEquals(900, old.exhaustionDurationMs);
        var config = new OrvenConfig(); config.configSchema = 3; config.enabled = config.spamEnabled = true;
        config.assistMediumCps = 11.1; config.spamMediumCps = 13.1;
        config.migrateValues(); assertEquals(11.1, config.mediumCps); assertEquals(13.1, config.spamMediumCps);
        assertFalse(config.heldClickEnabled); assertTrue(config.heldClickEntityOnly);
        assertTrue(config.heldClickRequiresPlayer); assertTrue(config.heldClickWeaponOnly);
        assertEquals(250, config.heldClickDelayMs);
    }
    @Test void nativeItemListsCrouchConditionAndDebugFooterAreExposed() {
        var config = new OrvenConfig(); Tree tree = tree(config);
        for (String field : new String[]{"assistItems", "spamItems", "heldItems"}) {
            String section = field.equals("assistItems") ? "assistWeapons" : field.equals("spamItems") ? "spamWeapons" : "heldWeapons";
            assertEquals(Visualizer.ItemListVisualizer.class, tree.get(section, field).getMetadata("visualizer"));
        }
        assertTrue(java.util.Arrays.asList(config.heldItems).contains("minecraft:beef"));
        assertNotNull(tree.get("heldFilters", "heldCrouchCancel")); assertTrue(config.heldCrouchCancel);
        assertEquals(true, tree.get("rockyRamp").getMetadata("hidden"));
        String lastAdvanced = null;
        for (Node node : tree.map.values()) if ("ClickAssist".equals(node.getMetadata("category")) && "Advanced".equals(node.getMetadata("subcategory"))) lastAdvanced=node.getID();
        assertEquals("debug", lastAdvanced); assertFalse(config.debugEnabled);
        assertNotNull(tree.get("debug", "clearDebugCache").getMetadata("runnable"));
        config.configSchema=4; config.mediumCps=11.1; config.swords=false;
        assertTrue(config.migrateValues()); assertEquals(11.1, config.mediumCps);
        assertFalse(java.util.Arrays.asList(config.assistItems).contains("minecraft:diamond_sword"));
        config.heldItems=new String[]{"minecraft:diamond"}; assertFalse(config.migrateValues());
        assertArrayEquals(new String[]{"minecraft:diamond"}, config.heldItems);
    }
    @Test void featureSubsettingsHideWithMastersAndFistDefaultsAreSeparate() {
        var config = new OrvenConfig(); Tree tree = tree(config);
        assertTrue(config.spamAllowFist); assertFalse(config.heldClickAllowFist);
        assertEquals(Property.Display.HIDDEN, tree.getProp("spamWeapons", "spamAllowFist").getDisplay());
        assertEquals(Property.Display.HIDDEN, tree.getProp("spamWeapons", "spamItems").getDisplay());
        assertEquals(Property.Display.HIDDEN, tree.getProp("heldWeapons", "heldClickAllowFist").getDisplay());
        assertEquals(Property.Display.HIDDEN, tree.getProp("hitFilters", "hitEffectsShowHealth").getDisplay());
        assertEquals(Property.Display.SHOWN, tree.getProp("debugEnabled").getDisplay());
        tree.getProp("spamEnabled").setAs(true);
        assertEquals(Property.Display.SHOWN, tree.getProp("spamWeapons", "spamAllowFist").getDisplay());
        assertEquals(Property.Display.SHOWN, tree.getProp("spamWeapons", "spamItems").getDisplay());
        tree.getProp("spamWeapons", "spamWeaponOnly").setAs(false);
        assertEquals(Property.Display.HIDDEN, tree.getProp("spamWeapons", "spamItems").getDisplay());
        assertEquals(Property.Display.SHOWN, tree.getProp("spamWeapons", "spamAllowFist").getDisplay());
        tree.getProp("heldFilters", "heldClickEnabled").setAs(true);
        assertEquals(Property.Display.SHOWN, tree.getProp("heldWeapons", "heldClickAllowFist").getDisplay());
        tree.getProp("heldFilters", "heldClickLeft").setAs(false);
        assertEquals(Property.Display.HIDDEN, tree.getProp("heldWeapons", "heldClickAllowFist").getDisplay());
    }
    @Test void mouseHoldAccordionKeepsItsMasterAndHidesEveryConditionWhenDisabledAgain() {
        Tree tree = tree(new OrvenConfig());
        var node = SettingIndexKt.buildAccordionNode((Tree) tree.get("heldFilters"));
        assertEquals("heldClickEnabled", node.getHead().getID());
        tree.getProp("heldFilters", "heldClickEnabled").setAs(true);
        for (Property<?> property : node.getBody()) assertEquals(Property.Display.SHOWN, property.getDisplay(), property.getID());
        tree.getProp("heldFilters", "heldClickEnabled").setAs(false);
        for (Property<?> property : node.getBody()) assertEquals(Property.Display.HIDDEN, property.getDisplay(), property.getID());
        assertEquals(Property.Display.SHOWN, node.getHead().getDisplay());
    }
    @Test void physicalFistAndAutoToolControlsHaveIndependentDefaultsAndDependencies() {
        var config = new OrvenConfig(); Tree tree = tree(config);
        assertFalse(config.assistAllowFist); assertFalse(config.autoToolEnabled);
        assertTrue(config.autoToolWhitelistEnabled); assertFalse(config.autoToolSwitchBack);
        assertEquals(160, config.autoToolSwitchDelayMs); assertEquals(40, config.autoToolVariationMs);
        assertNotNull(getClass().getClassLoader().getResource("assets/orvenbw/icons/autotool.svg"));
        tree.getProp("enabled").setAs(true);
        assertEquals(Property.Display.SHOWN, tree.getProp("assistWeapons", "assistAllowFist").getDisplay());
        tree.getProp("leftClick").setAs(false);
        assertEquals(Property.Display.HIDDEN, tree.getProp("assistWeapons", "assistAllowFist").getDisplay());
        tree.getProp("autoToolEnabled").setAs(true);
        assertEquals(Property.Display.SHOWN, tree.getProp("autoToolAllowedBlocks", "autoToolWhitelist").getDisplay());
        assertEquals(Property.Display.HIDDEN, tree.getProp("autoToolSwap", "autoToolOverrideSwitchBack").getDisplay());
        tree.getProp("autoToolSwap", "autoToolSwitchBack").setAs(true);
        assertEquals(Property.Display.SHOWN, tree.getProp("autoToolSwap", "autoToolOverrideSwitchBack").getDisplay());
        tree.getProp("autoToolEnabled").setAs(false);
        for (Node section : tree.map.values()) {
            if (!"AutoTool".equals(section.getMetadata("category")) || !(section instanceof Tree nested)) continue;
            for (Node child : nested.map.values()) assertEquals(Property.Display.HIDDEN, ((Property<?>) child).getDisplay(), child.getID());
        }
        config.configSchema = 5; config.hitEffectDurationMs = 500;
        assertTrue(config.migrateValues()); assertEquals(350, config.hitEffectDurationMs);
    }
    @Test void autoSoupDefaultsTimingPlacementDependenciesAndWhitelistMigration() {
        var config = new OrvenConfig(); Tree tree = tree(config);
        assertFalse(config.autoSoupEnabled); assertFalse(config.autoSoupScoreboardOnly);
        assertTrue(config.autoSoupRefill); assertTrue(config.autoSoupDisableLeft);
        assertEquals("mineberry.org", config.autoSoupScoreboardWord);
        assertEquals(4, config.autoSoupHealthMin); assertEquals(14, config.autoSoupHealthMax);
        assertEquals(2, config.autoSoupMaxPerCycle);
        assertEquals(2000, config.autoSoupHoldTimeoutMs);
        assertNotNull(getClass().getClassLoader().getResource("assets/orvenbw/icons/soup.svg"));
        for (String group : new String[]{"autoSoupUseTiming", "autoSoupRefillTiming", "autoSoupRecoveryTiming"}) {
            assertEquals("Advanced", tree.get(group).getMetadata("subcategory"));
            for (Node node : ((Tree) tree.get(group)).map.values()) assertEquals(Property.Display.HIDDEN, ((Property<?>) node).getDisplay());
        }
        tree.getProp("autoSoupEnabled").setAs(true);
        assertEquals(Property.Display.SHOWN, tree.getProp("autoSoupUseTiming", "autoSoupConsumeMinMs").getDisplay());
        assertEquals(Property.Display.HIDDEN, tree.getProp("autoSoupConditions", "autoSoupScoreboardWord").getDisplay());
        tree.getProp("autoSoupConditions", "autoSoupScoreboardOnly").setAs(true);
        assertEquals(Property.Display.SHOWN, tree.getProp("autoSoupConditions", "autoSoupScoreboardWord").getDisplay());
        tree.getProp("autoSoupRefill").setAs(false);
        assertEquals(Property.Display.HIDDEN, tree.getProp("autoSoupRefillTiming", "autoSoupMoveMinMs").getDisplay());
        config.configSchema = 6; config.autoToolWhitelist = new String[]{"minecraft:planks", "minecraft:chest", "trapped_chest", "minecraft:ender_chest", "test:custom"};
        assertTrue(config.migrateValues()); assertEquals(8, config.configSchema);
        assertArrayEquals(new String[]{"minecraft:planks", "test:custom", "minecraft:end_stone"}, config.autoToolWhitelist);
        assertFalse(config.migrateValues());
    }
    @Test void autoSoupTimingUpgradePreservesCustomRangesAndOnlyRunsOnce() {
        var config = new OrvenConfig(); config.configSchema = 7;
        config.autoSoupConsumeMinMs = 30; config.autoSoupConsumeMaxMs = 55;
        config.autoSoupReturnMinMs = 33; config.autoSoupReturnMaxMs = 55;
        config.autoSoupMoveMinMs = 33; config.autoSoupMoveMaxMs = 44;
        config.autoSoupResponseTimeoutMs = 750; config.autoSoupCycleCooldownMs = 250;
        assertTrue(config.migrateValues()); assertEquals(8, config.configSchema);
        assertEquals(110, config.autoSoupConsumeMinMs); assertEquals(135, config.autoSoupConsumeMaxMs);
        assertEquals(113, config.autoSoupReturnMinMs); assertEquals(135, config.autoSoupReturnMaxMs);
        assertEquals(113, config.autoSoupMoveMinMs); assertEquals(124, config.autoSoupMoveMaxMs);
        assertEquals(830, config.autoSoupResponseTimeoutMs); assertEquals(330, config.autoSoupCycleCooldownMs);
        assertFalse(config.migrateValues());
        config.configSchema = 7; config.autoSoupConsumeMinMs = 200; config.autoSoupConsumeMaxMs = 300;
        assertTrue(config.migrateValues());
        assertEquals(200, config.autoSoupConsumeMinMs); assertEquals(300, config.autoSoupConsumeMaxMs);
    }
}
