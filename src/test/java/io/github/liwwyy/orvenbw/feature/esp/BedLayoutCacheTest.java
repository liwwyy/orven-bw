package io.github.liwwyy.orvenbw.feature.esp;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import static org.junit.jupiter.api.Assertions.*;

class BedLayoutCacheTest {
    @TempDir Path directory;
    private BedGeometry bed(int x) { return new BedGeometry(new BedGeometry.Pos(x,64,30),1,0); }
    private BedIndex complete() {
        var index = new BedIndex(); for (int i=0;i<4;i++) index.observe(bed(i*30)); return index;
    }
    @Test void cachePersistsAndOneObservedBedMatchesOnlyTheSameServerMapAndGeometry() {
        var file = directory.resolve("layouts.json"); var cache = new BedLayoutCache(file);
        cache.learn("hypixel.net","test-map",4,complete()); assertTrue(Files.isRegularFile(file));
        cache = new BedLayoutCache(file); var sample = new BedIndex(); sample.observe(bed(30));
        assertTrue(cache.match("hypixel.net","test-map",0,sample.beds()).isPresent());
        assertTrue(cache.match("pika.host","test-map",4,sample.beds()).isEmpty());
        assertTrue(cache.match("hypixel.net","different-map",4,sample.beds()).isEmpty());
        assertTrue(cache.match("hypixel.net","test-map",8,sample.beds()).isEmpty());
        sample.observe(bed(1000)); assertTrue(cache.match("hypixel.net","test-map",4,sample.beds()).isEmpty());
    }
    @Test void ambiguityNeedsAnotherBedAndPartialOrUnnamedLayoutsAreNeverLearned() {
        var cache = new BedLayoutCache(null); var first = complete(); cache.learn("s","map",4,first);
        var second = new BedIndex(); second.observe(bed(0)); second.observe(bed(10)); second.observe(bed(20)); second.observe(bed(40));
        cache.learn("s","map",4,second);
        var partial = new BedIndex(); partial.observe(bed(0));
        assertTrue(cache.match("s","map",4,partial.beds()).isEmpty());
        partial.observe(bed(30)); assertTrue(cache.match("s","map",4,partial.beds()).isPresent());
        cache.learn("s","partial",4,partial); cache.learn("s","",4,first);
        assertTrue(cache.match("s","partial",4,partial.beds()).isEmpty()); assertTrue(cache.match("s","",4,partial.beds()).isEmpty());
    }
    @Test void changedColoursReplaceTheSameLayoutRatherThanIntroducingFalseAmbiguity() {
        var index = complete(); var cache = new BedLayoutCache(null); cache.learn("s","map",4,index);
        var bed = index.beds().iterator().next(); bed.team = BedTeam.RED; bed.teamObserved=true; cache.learn("s","map",4,index);
        var sample = new BedIndex(); sample.observe(bed.geometry);
        assertEquals(BedTeam.RED,cache.match("s","map",4,sample.beds()).orElseThrow().beds().getFirst().team());
    }
}
