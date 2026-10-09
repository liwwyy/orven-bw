package io.github.liwwyy.orvenbw.feature.esp;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class ProjectilePathTest {
    private final ProjectilePath.Point zero=new ProjectilePath.Point(0,0,0);
    private ProjectilePath.Environment environment(boolean water,int stop) {
        return new ProjectilePath.Environment() {
            public boolean loaded(ProjectilePath.Point p){return p.x()<stop;}
            public boolean water(ProjectilePath.Point p){return water;}
            public ProjectilePath.Point collision(ProjectilePath.Point a,ProjectilePath.Point b,int tick){return null;}
        };
    }
    @Test void arrowDragGravityAndFireballAccelerationFollowVanillaTickOrder() {
        var arrow=ProjectilePath.predict(zero,new ProjectilePath.Point(1,0,0),zero,ProjectilePath.Kind.ARROW,environment(false,999));
        assertEquals(1,arrow.points().get(4).x(),1e-9);
        assertEquals(1.99,arrow.points().get(8).x(),1e-9);assertEquals(-.05,arrow.points().get(8).y(),1e-9);
        var fire=ProjectilePath.predict(zero,new ProjectilePath.Point(1,0,0),new ProjectilePath.Point(.1,0,0),ProjectilePath.Kind.FIREBALL,environment(false,999));
        assertEquals(2.045,fire.points().get(8).x(),1e-9);assertEquals(0,fire.points().get(8).y());
    }
    @Test void waterDragAndUnloadedTerrainStopPredictionWithoutInventingAnImpact() {
        var arrow=ProjectilePath.predict(zero,new ProjectilePath.Point(1,0,0),zero,ProjectilePath.Kind.ARROW,environment(true,2));
        assertEquals(1.6,arrow.points().get(8).x(),1e-9);assertNull(arrow.impact());
        assertTrue(arrow.points().stream().allMatch(p->p.x()<2));
    }
    @Test void waterFlowAcceleratesBeforeMovementWithoutMutatingAnEntity() {
        var path=ProjectilePath.predict(zero,zero,zero,ProjectilePath.Kind.PEARL,new ProjectilePath.Environment() {
            public boolean loaded(ProjectilePath.Point p){return true;}
            public boolean water(ProjectilePath.Point p){return true;}
            public ProjectilePath.Point flow(ProjectilePath.Point p){return new ProjectilePath.Point(1,0,0);}
            public ProjectilePath.Point collision(ProjectilePath.Point a,ProjectilePath.Point b,int tick){return null;}
        });
        assertEquals(.014,path.points().get(4).x(),1e-9);
        assertEquals(.0392,path.points().get(8).x(),1e-9);
    }
    @Test void sweptCollisionStopsExactlyAtItsReportedPoint() {
        var hit=new ProjectilePath.Point(.5,0,0);
        var path=ProjectilePath.predict(zero,new ProjectilePath.Point(3,0,0),zero,ProjectilePath.Kind.PEARL,new ProjectilePath.Environment() {
            public boolean loaded(ProjectilePath.Point p){return true;}
            public boolean water(ProjectilePath.Point p){return false;}
            public ProjectilePath.Point collision(ProjectilePath.Point a,ProjectilePath.Point b,int tick){return b.x()>=.5?hit:null;}
        });
        assertEquals(hit,path.impact());assertEquals(hit,path.points().getLast());assertEquals(2,path.points().size());
    }
}
