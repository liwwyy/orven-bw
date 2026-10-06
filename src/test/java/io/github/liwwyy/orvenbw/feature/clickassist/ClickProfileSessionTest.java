package io.github.liwwyy.orvenbw.feature.clickassist;

import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ClickProfileSessionTest {
    private static final long SECOND=1_000_000_000L;
    private static final ClickProfileSession.Options HUMBLE=new ClickProfileSession.Options(0,true,22);
    private record Sample(double[] rates, double correlation, double shortShare) {}
    private Sample sample(int button,int profile,int seed) {
        Random random=new Random(seed);
        var session=new ClickProfileSession(random::nextDouble,button);
        var options=new ClickProfileSession.Options(profile,true,22);
        double[] rates=new double[400]; var a=new ArrayList<Double>(); var b=new ArrayList<Double>();
        int shortCount=0,total=0;
        for(int run=0;run<rates.length;run++) {
            session.reset(); long now=0;int clicks=1;double previous=-1;
            while(now<8*SECOND) {
                double rate=session.target(now,true,options);
                double interval=Math.clamp(session.intervalWeight()/rate,.008,1.5);
                if(now+(long)(interval*SECOND)>=8*SECOND)break;
                if(previous>0){a.add(previous);b.add(interval);}
                previous=interval; total++;if(interval<.045)shortCount++;
                now+=(long)(interval*SECOND);clicks++;
            }
            rates[run]=(clicks-1)/(now/(double)SECOND);
        }
        double ma=a.stream().mapToDouble(Double::doubleValue).average().orElseThrow();
        double mb=b.stream().mapToDouble(Double::doubleValue).average().orElseThrow();
        double xy=0,xx=0,yy=0;
        for(int i=0;i<a.size();i++){double x=a.get(i)-ma,y=b.get(i)-mb;xy+=x*y;xx+=x*x;yy+=y*y;}
        Arrays.sort(rates);return new Sample(rates,xy/Math.sqrt(xx*yy),shortCount/(double)total);
    }
    @Test void fittedSidesHaveDifferentRatesAndRightKeepsShortLongAlternation() {
        var left=sample(0,0,34);var right=sample(1,0,34);
        assertTrue(left.rates[200]>=12&&left.rates[200]<=16, "Left median "+left.rates[200]);
        assertTrue(right.rates[200]>=5&&right.rates[200]<=9, "Right median "+right.rates[200]);
        assertTrue(left.shortShare>.2&&left.shortShare<.45,"Left short gaps "+left.shortShare);
        assertTrue(right.shortShare>.25&&right.shortShare<.5,"Right short gaps "+right.shortShare);
        assertTrue(right.correlation<-.2,"Right correlation "+right.correlation);
    }
    @Test void performativeNarrowsBoutRatesAndStaysMedianCentered() {
        for(int button=0;button<2;button++) {
            var humble=sample(button,0,56);var performative=sample(button,1,56);
            double h=humble.rates[300]-humble.rates[100],p=performative.rates[300]-performative.rates[100];
            assertTrue(p<=h*.7,"IQR reduction: "+h+" -> "+p);
            assertEquals(button==0?14:6,performative.rates[200],1.0);
        }
    }
    @Test void sharedModelReducesOnlyRightRateAndResetsOnOptionChange() {
        Random a=new Random(3),b=new Random(3);
        var left=new ClickProfileSession(a::nextDouble,0);var right=new ClickProfileSession(b::nextDouble,1);
        var shared=new ClickProfileSession.Options(0,false,22);
        for(long t=0;t<10*SECOND;t+=50_000_000)assertEquals(left.target(t,true,shared)*.9,right.target(t,true,shared),1e-9);
        assertEquals(0,left.target(11*SECOND,false,shared));
        assertTrue(left.target(12*SECOND,true,new ClickProfileSession.Options(1,true,8.5))<=8.5);
    }
    @Test void measuredStartsCanBuildSettleOrStartFastWithoutAForcedWaitingPeriod() {
        Random random=new Random(73);var session=new ClickProfileSession(random::nextDouble,0);
        int up=0,down=0;
        for(int i=0;i<200;i++) {
            session.reset();long now=0;int first=1,later=0;
            assertTrue(session.target(0,true,HUMBLE)>=1);
            while(now<3*SECOND) {
                double rate=session.target(now,true,HUMBLE);
                now+=(long)(Math.clamp(session.intervalWeight()/rate,.008,1.5)*SECOND);
                if(now<SECOND)first++;else if(now<3*SECOND)later++;
            }
            if(later>0) {
                double ratio=first/(later/2.0);
                if(ratio<.9)up++;if(ratio>1.1)down++;
            }
        }
        assertTrue(up>20,"Building starts "+up);assertTrue(down>20,"Settling starts "+down);
    }
    @Test void longClockJumpsAreBoundedAndExtrapolatedPeaksRemainRare() {
        Random random=new Random(2);var session=new ClickProfileSession(random::nextDouble,0);
        int high=0;
        for(long t=0;t<1800*SECOND;t+=50_000_000) {
            double rate=session.target(t,true,HUMBLE);
            assertTrue(rate>=1&&rate<=22);if(rate>20)high++;
        }
        assertTrue(high>0);assertTrue(high/36000.0<.005,"Rare target occupancy "+high/36000.0);
        assertTrue(Double.isFinite(session.target(Long.MAX_VALUE/2,true,HUMBLE)));
        assertEquals(new ClickProfileSession.Options(0,true,22),new ClickProfileSession.Options(99,true,Double.NaN));
    }
    @Test void entityFloorPreservesRampAndCeilingAndDoesNotResetOnTargetChanges() {
        var baselineRandom = new Random(34); var floorRandom = new Random(34);
        var baseline = new ClickProfileSession(baselineRandom::nextDouble, 0);
        var floored = new ClickProfileSession(floorRandom::nextDouble, 0);
        boolean foundDip = false;
        for (long t = 0; t < 120*SECOND; t += 50_000_000) {
            double normal = baseline.target(t, true, HUMBLE);
            double boosted = floored.target(t, true, HUMBLE, true, 8);
            if (t < 1_500_000_000L) assertEquals(normal, boosted);
            else { assertEquals(Math.max(normal, 8), boosted); foundDip |= normal < 8; }
        }
        assertTrue(foundDip);
        long next = 120*SECOND;
        assertEquals(baseline.target(next, true, HUMBLE), floored.target(next, true, HUMBLE, false, 8));
        assertEquals(0, floored.target(next+SECOND, false, HUMBLE, true, 8));
        var limited = new ClickProfileSession(new Random(4)::nextDouble, 0);
        var options = new ClickProfileSession.Options(0, true, 6.5);
        limited.target(0, true, options, true, 8);
        assertEquals(6.5, limited.target(2*SECOND, true, options, true, 8));
    }
    @Test void entityFloorNeverChangesRightClickProfile() {
        var a = new ClickProfileSession(new Random(12)::nextDouble, 1);
        var b = new ClickProfileSession(new Random(12)::nextDouble, 1);
        for (long t = 0; t < 10*SECOND; t += 50_000_000)
            assertEquals(a.target(t, true, HUMBLE), b.target(t, true, HUMBLE, true, 22));
    }
}
