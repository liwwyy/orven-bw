package io.github.liwwyy.orvenbw.hud;

import io.github.liwwyy.orvenbw.feature.clickassist.ClickAssistEngine.Cps;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CpsFormatTest {
    @Test void formatsTheCurrentButtonAfterTheTotal() {
        assertEquals("7 + 6 = 13 R-Cps", CpsFormat.render(CpsFormat.DEFAULT, new Cps(7, 6), 1));
        assertEquals("5 + 4 = 9 L-Cps", CpsFormat.render(CpsFormat.DEFAULT, new Cps(5, 4), 0));
    }
    @Test void oldDefaultsAndOldLetterPlacementMigrateWithoutDuplicateSuffixes() {
        assertEquals("7 + 6 = 13 R-Cps", CpsFormat.render("{base} + {boosted} = {total}", new Cps(7, 6), 1));
        assertEquals("13 L-Cps", CpsFormat.render("{button}{total} CPS", new Cps(7, 6), 0));
        assertEquals("", CpsFormat.suffix(" CPS"));
        assertEquals(" clicks", CpsFormat.suffix(" clicks"));
    }
    @Test void preservesCustomFormatsWithExplicitButtonPlacement() {
        assertEquals("R: 6 extra / 13", CpsFormat.render("{button}: {boosted} extra / {total}", new Cps(7, 6), 1));
    }
}
