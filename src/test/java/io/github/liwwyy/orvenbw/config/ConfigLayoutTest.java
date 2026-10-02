package io.github.liwwyy.orvenbw.config;

import org.junit.jupiter.api.Test;
import org.polyfrost.oneconfig.api.config.v1.Tree;
import org.polyfrost.oneconfig.internal.legacy.InputConstants;
import io.github.liwwyy.orvenbw.feature.clickassist.ClickAssistFeature;
import static org.junit.jupiter.api.Assertions.*;

class ConfigLayoutTest {
    private Tree tree(OrvenConfig config) throws Exception {
        var method = OrvenConfig.class.getDeclaredMethod("makeTree"); method.setAccessible(true);
        return (Tree) method.invoke(config);
    }
    @Test void generalIsFirstAndEveryEnableSwitchIsInsideItsCollapsedHeader() throws Exception {
        Tree tree = tree(new OrvenConfig());
        assertEquals("general", tree.map.keySet().iterator().next());
        String[] ids = {"general", "assist", "spam", "hold", "hud"};
        String[] switches = {"modEnabled", "enabled", "spamEnabled", "holdEnabled", "showHud"};
        for (int i = 0; i < ids.length; i++) {
            Tree section = tree.getChild(ids[i]);
            assertEquals(true, section.getMetadata("collapsed"));
            assertEquals(switches[i], section.map.keySet().iterator().next());
            assertNull(section.get(switches[i]).getMetadata("visualizer"));
            assertNull(section.get(switches[i]).getMetadata("hidden"));
            assertEquals(true, tree.get(switches[i]).getMetadata("hidden"));
        }
        assertEquals("General", tree.get("general").getMetadata("category"));
        assertEquals("ClickAssist", tree.get("assist").getMetadata("category"));
        assertNotNull(tree.get("hud", "editHud").getMetadata("runnable"));
    }
    @Test void featureControlsAndLegacyAliasesShareOnlyTheirOwnValues() throws Exception {
        var config = new OrvenConfig(); Tree tree = tree(config);
        tree.getProp("assistRates", "assistMediumCps").setAs(11.7);
        assertEquals(11.7, config.assistMediumCps);
        assertEquals(12.5, config.spamMediumCps);
        tree.getProp("spamMediumCps").setAs(10.2);
        assertEquals(10.2, (double) tree.getProp("spamRates", "spamMediumCps").getAs());
        assertEquals(true, tree.get("leftChance").getMetadata("hidden"));
        assertNull(tree.get("assist", "leftChance"));
        assertNull(tree.get("assist", "totalCpsCap"));
    }
    @Test void hotkeyAndDefaultsMatchTheInstalledSdksInputCodes() {
        var config = new OrvenConfig();
        assertArrayEquals(new int[]{InputConstants.KEY_P}, config.settingsBind.getKeyCodes());
        assertArrayEquals(new int[]{InputConstants.MOUSE_BUTTON_MIDDLE}, config.spamBind.getMouseBtns());
        assertEquals(1000, config.assistRampMs); assertEquals(1000, config.spamRampMs);
        assertFalse(config.preserveMining); assertTrue(config.spamClickThroughBlocks);
        assertEquals(14, config.rateOptions(false).high());
        assertEquals(12.5, config.rateOptions(true).medium());
    }
    @Test void installedOneConfigRecognizesEveryAccordionAndItsEmbeddedSwitch() throws Exception {
        Tree tree = tree(new OrvenConfig());
        int sections = 0;
        for (var node : tree.map.values()) {
            if (!(node instanceof Tree section)) continue;
            var row = org.polyfrost.oneconfig.internal.ui.search.SettingIndexKt.buildAccordionNode(section);
            assertNotNull(row, section.getID());
            assertTrue(row.getBody().size() <= 6, section.getID());
            sections++;
        }
        assertEquals(17, sections);
        var held = org.polyfrost.oneconfig.internal.ui.search.SettingIndexKt.buildAccordionNode(tree.getChild("heldClick"));
        assertEquals("heldClickEnabled", held.getHead().getID());
        assertEquals(4, held.getBody().size());
    }
    @Test void smallerAccordionsHaveNoNestedTreesOrDuplicateControls() throws Exception {
        var config = new OrvenConfig(); Tree tree = tree(config);
        var visible = new java.util.HashSet<String>();
        for (var node : tree.map.values()) {
            if (!(node instanceof Tree section)) continue;
            assertEquals(true, section.getMetadata("collapsed"));
            assertTrue(section.map.size() <= 7, section.getID());
            for (var entry : section.map.entrySet()) {
                assertFalse(entry.getValue() instanceof Tree);
                assertTrue(visible.add(entry.getKey()), entry.getKey());
            }
        }
        assertEquals("Spam Clicking", tree.get("heldClick").getMetadata("subcategory"));
        assertNull(tree.get("heldClick", "heldClickEnabled").getMetadata("visualizer"));
        assertFalse(config.heldClickEnabled); assertTrue(config.heldClickLeft); assertFalse(config.heldClickRight);
        assertTrue(config.heldClickEntityOnly); assertTrue(config.heldClickWeaponOnly); assertTrue(config.heldClickRequiresPlayer);
        assertEquals(250, config.heldClickDelayMs); assertFalse(config.heldClickInstant);
        assertEquals(org.polyfrost.oneconfig.api.config.v1.Property.Display.HIDDEN,
                tree.getProp("heldClick", "heldClickInstant").getDisplay());
        tree.getProp("heldClick", "heldClickEnabled").setAs(true);
        assertEquals(org.polyfrost.oneconfig.api.config.v1.Property.Display.SHOWN,
                tree.getProp("heldClick", "heldClickInstant").getDisplay());
        tree.getProp("spamRamp", "spamRampEnabled").setAs(false);
        assertEquals(org.polyfrost.oneconfig.api.config.v1.Property.Display.HIDDEN,
                tree.getProp("spamRamp", "spamRampMs").getDisplay());
    }
    @Test void layoutMigrationPreservesVersionTwoProfilesAndBindings() {
        var config = new OrvenConfig(); config.configSchema = 2;
        config.spamMediumCps = 11.3; config.spamRequiresPlayer = true; config.spamRampMs = 600;
        var bind = config.spamBind;
        assertTrue(config.migrateValues()); assertEquals(3, config.configSchema);
        assertEquals(11.3, config.spamMediumCps); assertTrue(config.spamRequiresPlayer);
        assertEquals(600, config.spamRampMs); assertSame(bind, config.spamBind);
        assertFalse(config.migrateValues());
    }
    @Test void spamBlockBypassIsIndependentOfPhysicalProtection() {
        var config = new OrvenConfig(); config.preserveMining = true;
        assertFalse(ClickAssistFeature.allowsBlock(!config.preserveMining, true));
        assertTrue(ClickAssistFeature.allowsBlock(config.spamClickThroughBlocks, true));
        config.spamClickThroughBlocks = false;
        assertFalse(ClickAssistFeature.allowsBlock(config.spamClickThroughBlocks, true));
        assertTrue(ClickAssistFeature.allowsBlock(config.spamClickThroughBlocks, false));
    }
    @Test void migratesOldDefaultsAndFiltersWithoutReapplyingToNewProfiles() {
        var config = new OrvenConfig();
        config.rampMs = 1200; config.requiresPlayer = true;
        config.exhaustionDurationMs = 900; config.swords = false;
        var bind = config.spamBind;
        assertTrue(config.migrateValues());
        assertEquals(1000, config.assistRampMs); assertEquals(1000, config.spamRampMs);
        assertTrue(config.spamRequiresPlayer); assertFalse(config.spamSwords);
        assertEquals(900, config.spamExhaustionDurationMs); assertSame(bind, config.spamBind);
        config.spamMediumCps = 10.3; config.assistRampMs = 700;
        assertFalse(config.migrateValues());
        assertEquals(10.3, config.spamMediumCps); assertEquals(700, config.assistRampMs);
        var custom = new OrvenConfig(); custom.rampMs = 800;
        custom.migrateValues(); assertEquals(800, custom.assistRampMs);
    }
}
