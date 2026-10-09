package io.github.liwwyy.orvenbw.feature.autoblock;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class AutoBlockCycleTest {
    private final AutoBlockCycle cycle=new AutoBlockCycle();
    private AutoBlockCycle.State step(int t,boolean eligible,boolean manual,int hurt,boolean damage,boolean again) {
        return cycle.step(t,eligible,manual,hurt,damage,200,150,200,true,again);
    }
    @Test void lagDeadlineStartsWithBlockAndImmediateAttackCanRestart() {
        assertEquals(AutoBlockCycle.State.BLOCK,step(1,true,false,0,false,true));
        assertEquals(AutoBlockCycle.State.BLOCK,step(3,true,false,0,false,true));
        assertEquals(AutoBlockCycle.State.LAG,step(4,true,false,0,false,true));
        cycle.attack(false,true,4);assertEquals(AutoBlockCycle.State.LAG,cycle.state());
        assertEquals(AutoBlockCycle.State.BLOCK,step(5,true,false,0,false,true));
        assertEquals(AutoBlockCycle.State.LAG,step(8,true,false,0,false,true));
        cycle.attack(true,true,8);assertEquals(AutoBlockCycle.State.BLOCK,cycle.state());
        cycle.reset();assertEquals(AutoBlockCycle.State.IDLE,cycle.state());
    }
    @Test void manualBlockHoldsAndReleasingRequirementsCancelsEverything() {
        assertEquals(AutoBlockCycle.State.BLOCK,step(1,false,true,0,false,true));
        assertEquals(AutoBlockCycle.State.BLOCK,step(100,false,true,0,false,true));
        assertEquals(AutoBlockCycle.State.IDLE,step(101,false,false,0,false,true));
    }
    @Test void damageThresholdAndFreshDamageReleaseWork() {
        assertEquals(AutoBlockCycle.State.IDLE,step(1,true,false,5,true,false));
        assertEquals(AutoBlockCycle.State.BLOCK,step(2,true,false,4,true,false));
        assertEquals(AutoBlockCycle.State.LAG,step(3,true,false,10,true,false));
        assertEquals(AutoBlockCycle.State.IDLE,step(6,true,false,7,true,false));
    }
    @Test void noLagWhenHoldAlreadyExceedsLagWindow() {
        cycle.step(1,true,false,0,false,200,300,100,true,true);
        assertEquals(AutoBlockCycle.State.IDLE,cycle.step(7,true,false,0,false,200,300,100,true,true));
        assertEquals(3,AutoBlockCycle.ticks(125));
    }
}
