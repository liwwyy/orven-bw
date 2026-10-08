package io.github.liwwyy.orvenbw.feature.esp;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class EspProjectionTest {
    private float[] identity() { return new float[]{1,0,0,0,0,1,0,0,0,0,1,0,0,0,0,1}; }
    @Test void centreAndCornersProjectUsingColumnMajorMatrices() {
        var centre=EspProjection.project(identity(),identity(),0,0,0,800,600);
        assertEquals(400,centre.x()); assertEquals(300,centre.y()); assertTrue(centre.front());
        var corner=EspProjection.project(identity(),identity(),1,1,0,800,600); assertEquals(800,corner.x()); assertEquals(0,corner.y());
        var view=identity(); view[12]=.5f; assertEquals(600,EspProjection.project(view,identity(),0,0,0,800,600).x());
    }
    @Test void edgeMarkersClampContinuouslyAndBehindCameraNeverStaysAtCentre() {
        var point = new EspProjection.Point(1000,300,0,true);
        assertEquals(776,EspProjection.edge(point,800,600,24).x());
        var behind=EspProjection.edge(new EspProjection.Point(400,300,0,false),800,600,24);
        assertEquals(576,behind.y());
        var inside=new EspProjection.Point(420,310,0,true); assertSame(inside,EspProjection.edge(inside,800,600,24));
    }
}
