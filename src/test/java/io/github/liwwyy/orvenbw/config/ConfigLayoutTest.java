package io.github.liwwyy.orvenbw.config;

import org.junit.jupiter.api.Test;
import org.polyfrost.oneconfig.api.config.v1.*;
import org.polyfrost.oneconfig.internal.legacy.InputConstants;
import org.polyfrost.oneconfig.internal.ui.search.SettingIndexKt;
import static org.junit.jupiter.api.Assertions.*;

class ConfigLayoutTest {
    private Tree tree(OrvenConfig config) { return config.makeTree(); }
    @Test void recordingStartsWithOnlyLoggingAndKeepsDeliberateChanges() {
        var config = new OrvenConfig();
        assertTrue(config.debugEnabled);
        assertFalse(config.modEnabled); assertFalse(config.enabled);
        assertFalse(config.spamEnabled); assertFalse(config.heldClickEnabled);
        assertFalse(config.holdEnabled); assertFalse(config.hitEffectsEnabled); assertFalse(config.showHud);
        assertEquals("orven-bw-recording.json", config.makeTree().getID());
        assertNotEquals("orven-bw.json", OrvenConfig.CONFIG_ID);
        // Initialization/migration must not force the defaults over saved recording choices.
        config.configSchema = 5;
        config.modEnabled = config.spamEnabled = config.showHud = true;
        config.debugEnabled = false;
        assertFalse(config.migrateValues());
        assertTrue(config.modEnabled); assertTrue(config.spamEnabled); assertTrue(config.showHud);
        assertFalse(config.debugEnabled);
    }
    @Test void generalComesFirstWithVisibleEnableAndIconShortcut() {
        var config = new OrvenConfig(); Tree tree = tree(config);
        assertFalse(config.modEnabled);
        assertEquals("modEnabled", tree.map.keySet().iterator().next());
        assertEquals("Enable orven-bw:", tree.get("modEnabled").getTitle());
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
        assertEquals(java.util.List.of("General", "ClickAssist"), java.util.List.copyOf(categories));
        assertEquals(java.util.List.of("Physical CPS Boost", "Spam click button", "Mouse button hold click", "Button Hold", "Hit effects", "Advanced", "HUD"), java.util.List.copyOf(sections));
        for (String field : new String[]{"enabled", "leftClick", "rightClick", "spamEnabled", "spamLeftBind", "spamRightBind", "heldClickEnabled", "heldClickLeft", "heldClickRight"}) {
            assertNotNull(tree.get(field).getMetadata("visualizer"), field);
            assertNull(tree.get(field).getMetadata("hidden"), field);
        }
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
        assertTrue(config.migrateValues()); assertEquals(5, config.configSchema);
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
        for (Node node : tree.map.values()) if ("Advanced".equals(node.getMetadata("subcategory"))) lastAdvanced=node.getID();
        assertEquals("debug", lastAdvanced); assertTrue(config.debugEnabled);
        assertNotNull(tree.get("debug", "clearDebugCache").getMetadata("runnable"));
        config.configSchema=4; config.mediumCps=11.1; config.swords=false;
        assertTrue(config.migrateValues()); assertEquals(11.1, config.mediumCps);
        assertFalse(java.util.Arrays.asList(config.assistItems).contains("minecraft:diamond_sword"));
        config.heldItems=new String[]{"minecraft:diamond"}; assertFalse(config.migrateValues());
        assertArrayEquals(new String[]{"minecraft:diamond"}, config.heldItems);
    }
}
