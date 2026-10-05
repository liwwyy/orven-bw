package io.github.liwwyy.orvenbw.debug;

import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class ClickOriginTrackerTest {
    @TempDir Path temp;

    @Test void physicalAndArtificialQueuesKeepTheirOriginsThroughActionsAndPackets() throws Exception {
        Path path = temp.resolve("click-debug.jsonl");
        long now = System.nanoTime();
        try (var log = new ClickDebugLog(path, () -> true, failure -> { throw new AssertionError(failure); })) {
            var tracker = new ClickOriginTracker(log);
            tracker.prepare("physical", "mouse", null, -1);
            var physical = tracker.queued(0, -100, now);
            tracker.prepare("artificial", "spam_click", "Humble", now - 20_000_000);
            var generated = tracker.queued(0, -100, now + 1);
            tracker.consumed(0);
            assertEquals(physical.id(), tracker.beginAction(0, now + 2).origin().id());
            tracker.packet("left", "entity_attack", now + 3);
            tracker.packet("right", "use", now + 4);
            tracker.endAction();
            tracker.consumed(0);
            assertEquals(generated.id(), tracker.beginAction(0, now + 5).origin().id());
            tracker.packet("left", "entity_attack", now + 6);
            tracker.endAction();
        }
        List<com.google.gson.JsonObject> rows = Files.readAllLines(path).stream()
                .map(line -> JsonParser.parseString(line).getAsJsonObject()).toList();
        assertEquals(7, rows.size());
        assertEquals("input", rows.get(1).get("event").getAsString());
        assertEquals("artificial", rows.get(2).get("source").getAsString());
        assertEquals("action", rows.get(3).get("event").getAsString());
        assertEquals("packet", rows.get(4).get("event").getAsString());
        assertEquals(rows.get(3).get("action_id"), rows.get(4).get("action_id"));
        assertEquals(rows.get(5).get("action_id"), rows.get(6).get("action_id"));
        assertNotEquals(rows.get(3).get("origin_id"), rows.get(5).get("origin_id"));
        assertTrue(rows.get(6).has("monotonic_ns_text"));
    }

    @Test void unmatchedConsumptionIsUnknownAndResetClearsStaleSources() throws Exception {
        Path path = temp.resolve("click-debug.jsonl");
        try (var log = new ClickDebugLog(path, () -> true, failure -> { throw new AssertionError(failure); })) {
            var tracker = new ClickOriginTracker(log);
            tracker.consumed(1);
            assertEquals("unknown", tracker.beginAction(1, System.nanoTime()).origin().source());
            tracker.endAction();
            tracker.prepare("physical", "mouse", null, -1);
            tracker.queued(0, -100, System.nanoTime());
            tracker.reset();
            tracker.consumed(0);
            assertEquals(0, tracker.beginAction(0, System.nanoTime()).origin().id());
        }
    }
    @Test void directSoupUseLogsItsPacketWithoutStealingPhysicalBindingOrigins() throws Exception {
        Path path = temp.resolve("click-debug.jsonl");
        try (var log = new ClickDebugLog(path, () -> true, failure -> fail(failure))) {
            var tracker = new ClickOriginTracker(log); long now = System.nanoTime();
            tracker.prepare("physical", "mouse", null, -1);
            var physical = tracker.queued(1, -99, now);
            tracker.consumed(1);
            tracker.beginDirectAction("auto_soup", 1, -99, now + 1);
            tracker.packet("right", "use", now + 2); tracker.endAction();
            assertEquals(physical.id(), tracker.beginAction(1, now + 3).origin().id()); tracker.endAction();
        }
        var rows = Files.readAllLines(path).stream().map(line -> JsonParser.parseString(line).getAsJsonObject()).toList();
        var packet = rows.stream().filter(row -> "packet".equals(row.get("event").getAsString())).findFirst().orElseThrow();
        assertEquals("auto_soup", packet.get("method").getAsString());
        assertEquals("artificial", packet.get("source").getAsString());
    }
}
