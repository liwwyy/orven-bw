package io.github.liwwyy.orvenbw.feature.clickassist;

import java.util.ArrayList;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ClickAssistEngineTest {
    private static final long MS = 1_000_000L;
    private ClickAssistEngine engine() { return new ClickAssistEngine(); }
    @Test void physicalWarmupRemainsFastAndRatesAboveTwentyArePreserved() {
        var e = engine(); e.physicalClick(0, 0); assertFalse(e.manuallyActive(0, 0, 4));
        e.physicalClick(0, 200*MS); assertEquals(5, e.manualRate(0, 200*MS));
        assertTrue(e.manuallyActive(0, 200*MS, 4)); assertFalse(e.manuallyActive(0, 601*MS, 4));
        e.reset(); e.physicalClick(0, 0); e.physicalClick(0, 30*MS);
        assertEquals(1000.0/30, e.manualRate(0, 30*MS), .001);
    }
    @Test void decimalRatesWorkAndGeneratedClicksAreBoundedByClientTicks() {
        for (double rate : new double[]{.2, 8, 9.5, 12.5, 22}) {
            var e=engine(); var intended=new ArrayList<Long>();
            for (long t=0;t<60_000*MS;t+=50*MS)
                e.pollDue(0,t,rate,true,0,22,()->1,intended::add);
            assertEquals(Math.min(rate,20),intended.size()/60.0,.08);
            for (int i=1;i<intended.size();i++) assertTrue(intended.get(i)>intended.get(i-1));
        }
    }
    @Test void shortIntervalsNeverQueueTwoGeneratedClicksInOneTick() {
        var e=engine();var due=new ArrayList<Long>();
        assertEquals(1,e.pollDue(0,0,14,true,0,22,()->.28,due::add));
        assertEquals(1,e.pollDue(0,50*MS,14,true,0,22,()->.28,due::add));
        assertEquals(0,e.pollDue(0,50*MS,14,true,0,22,()->.28,due::add));
        assertEquals(1,e.pollDue(0,100*MS,14,true,0,22,()->.28,due::add));
    }
    @Test void physicalClickTakesPrecedenceAndBriefZeroRateDoesNotRestartImmediately() {
        var e=engine();var due=new ArrayList<Long>();
        e.beginTick();e.physicalClick(0,0);
        assertEquals(0,e.pollDue(0,0,10,true,0,22,()->1,due::add));
        e.beginTick();assertEquals(1,e.pollDue(0,100*MS,10,true,0,22,()->1,due::add));
        e.beginTick();assertEquals(0,e.pollDue(0,125*MS,0,true,0,22,()->1,due::add));
        e.beginTick();assertEquals(0,e.pollDue(0,175*MS,10,true,0,22,()->1,due::add));
        e.beginTick();assertEquals(0,e.pollDue(0,225*MS,10,true,0,22,()->1,due::add));
        e.beginTick();assertEquals(1,e.pollDue(0,250*MS,10,true,0,22,()->1,due::add));
    }
    @Test void initialDelayStallsDisableAndClockReversalDoNotReplayClicks() {
        var e=engine(); var due=new ArrayList<Long>();
        assertEquals(0,e.pollDue(0,0,14,true,40*MS,22,()->1,due::add));
        assertEquals(0,e.pollDue(0,39*MS,14,true,40*MS,22,()->1,due::add));
        assertEquals(1,e.pollDue(0,40*MS,14,true,40*MS,22,()->1,due::add));
        assertEquals(0,e.pollDue(0,500*MS,14,true,0,22,()->1,due::add));
        assertEquals(0,e.pollDue(0,501*MS,14,false,0,22,()->1,due::add));
        assertEquals(0,e.pollDue(0,550*MS,14,true,40*MS,22,()->1,due::add));
        assertEquals(0,e.pollDue(0,100*MS,14,true,0,22,()->1,due::add));
    }
    @Test void rampChangesNeverShortenTheConfiguredFirstBoostDelay() {
        var e=engine();var due=new ArrayList<Long>();
        assertEquals(0,e.pollDue(0,0,3,true,150*MS,22,()->1,due::add));
        assertEquals(0,e.pollDue(0,50*MS,8,true,150*MS,22,()->1,due::add));
        assertEquals(0,e.pollDue(0,100*MS,14,true,150*MS,22,()->1,due::add));
        assertEquals(0,e.pollDue(0,149*MS,18,true,150*MS,22,()->1,due::add));
        assertEquals(1,e.pollDue(0,150*MS,18,true,150*MS,22,()->1,due::add));
        assertEquals(150*MS,due.getFirst());
    }
    @Test void manualInputAboveCeilingIsNeverSuppressedAndStopsBoosts() {
        var e=engine();
        for(int i=0;i<25;i++)e.physicalClick(0,i*30*MS);
        assertEquals(25,e.cps(0,750*MS).base());
        assertEquals(0,e.pollDue(0,750*MS,5,true,0,22,()->1,t->fail("Must not add clicks")));
        assertEquals(25,e.cps(0,750*MS).base());
        assertEquals(0,e.cps(1,750*MS).total());
    }
    @Test void totalsAndDominanceUseActualDispatchTimesAndResetCleanly() {
        var e=engine(); e.physicalClick(0,0);
        e.pollDue(1,10*MS,22,true,0,22,()->1,t->{});
        e.pollDue(1,60*MS,22,true,0,22,()->1,t->{});
        assertEquals(1,e.dominantButton(60*MS,0));
        assertEquals(0,e.cps(0,1000*MS).total()); assertEquals(1,e.cps(1,1010*MS).boosted());
        e.reset(); assertEquals(0,e.cps(1,1010*MS).total());
    }
}
