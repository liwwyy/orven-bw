package io.github.liwwyy.orvenbw.hud;

import io.github.liwwyy.orvenbw.feature.clickassist.ClickAssistEngine.Cps;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CpsFormatTest {
    @Test void dominantCalculationChangesWhileRawLeftRightOrderDoesNot() {
        var left = new Cps(7, 6); var right = new Cps(3, 1);
        assertEquals("7 + 6 = 13 | 4 CPS", CpsFormat.render(CpsFormat.DEFAULT, left, right, 0, true));
        assertEquals("3 + 1 = 13 | 4 CPS", CpsFormat.render(CpsFormat.DEFAULT, left, right, 1, true));
        assertEquals("3 + 1 = 13 | 4", CpsFormat.render(CpsFormat.DEFAULT, left, right, 1, false));
    }
    @Test void oldDefaultMigratesAndOptionalUppercaseSuffixNeverDuplicates() {
        var left = new Cps(7, 6); var right = new Cps(3, 1);
        assertEquals("7 + 6 = 13 | 4 CPS", CpsFormat.render("{base} + {boosted} = {total} {button}-Cps", left, right, 0, true));
        assertEquals("13 | 4 CPS", CpsFormat.render("{left} | {right} Cps", left, right, 0, true));
        assertEquals("13 | 4", CpsFormat.render("{left} | {right} CPS", left, right, 0, false));
        assertEquals("", CpsFormat.suffix(" CPS"));
        assertEquals(" clicks", CpsFormat.suffix(" clicks"));
    }
    @Test void customCalculationUsesTheDominantSide() {
        assertEquals("1 extra / 4", CpsFormat.render("{boosted} extra / {total}", new Cps(7, 6), new Cps(3, 1), 1, false));
    }
}
