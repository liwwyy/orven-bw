package io.github.liwwyy.orvenbw.feature.autoblock;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class CombatGeometryTest {
    @Test void usesNearestHitboxPointRatherThanCentreAndHandlesVerticalSeparation() {
        assertEquals(16,CombatGeometry.distanceSquared(0,1.62,0,4,0,-.3,4.6,1.8,.3),1e-9);
        assertEquals(0,CombatGeometry.distanceSquared(0,1,0,-.3,0,-.3,.3,1.8,.3));
        assertEquals(25,CombatGeometry.distanceSquared(0,5,0,4,-2,-.3,4.6,2,.3));
    }
}
