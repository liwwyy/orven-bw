package io.github.liwwyy.orvenbw.feature.esp;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class BedWaypointAnchorTest {
    @Test void homesToSupportAtCloseRangeAndIsBoundedSmoothAndMonotonic() {
        assertEquals(0,BedWaypointAnchor.height(0));assertEquals(0,BedWaypointAnchor.height(6));assertEquals(6,BedWaypointAnchor.height(48));assertEquals(6,BedWaypointAnchor.height(500));
        assertEquals(3,BedWaypointAnchor.height(27));
        double last=0;for(double d=0;d<100;d+=.1) { double h=BedWaypointAnchor.height(d);assertTrue(h>=last);assertTrue(h-last<.03);last=h; }
    }
}
