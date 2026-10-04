package io.github.liwwyy.orvenbw.debug;

import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import static org.junit.jupiter.api.Assertions.*;

class ClickDebugLogTest {
    @TempDir Path temp;
    @Test void debugIsOptInAndAppendsMillisecondsAndMethodsAcrossRestarts() throws Exception {
        Path path = temp.resolve("config/orven-bw/click-debug.jsonl");
        var enabled = new AtomicBoolean(false);
        var errors = new java.util.concurrent.CopyOnWriteArrayList<Throwable>();
        long before = System.currentTimeMillis();
        try (var log = new ClickDebugLog(path, enabled::get, errors::add)) {
            log.event("physical", "mouse", "left", "press", -100, 123);
            log.flush().get(5, TimeUnit.SECONDS); assertFalse(Files.exists(path));
            enabled.set(true);
            log.event("physical", "mouse", "left", "press", -100, 123);
            log.event("artificial", "cps_boost", "left", "queue", -100, -1);
        }
        try (var log = new ClickDebugLog(path, () -> true, errors::add)) {
            log.event("artificial", "mouse_hold_click", "right", "queue", -99, -1);
        }
        var rows = Files.readAllLines(path).stream().map(line -> new JsonParser().parse(line).getAsJsonObject()).toList();
        assertEquals(5, rows.size()); assertEquals("session_start", rows.get(0).get("event").getAsString());
        assertNotEquals(rows.get(0).get("session").getAsString(), rows.get(3).get("session").getAsString());
        assertEquals("cps_boost", rows.get(2).get("method").getAsString());
        assertTrue(rows.get(1).get("timestamp_ms").getAsLong() >= before);
        assertTrue(rows.get(1).get("timestamp_ms").getAsLong() <= System.currentTimeMillis());
        assertEquals(123, rows.get(1).get("native_event_ns").getAsLong());
        assertTrue(rows.get(2).get("elapsed_ns").getAsLong() >= rows.get(1).get("elapsed_ns").getAsLong());
        assertTrue(errors.isEmpty());
    }
    @Test void generatedDeadlinesAreLosslessAndSeparateFromNativeEvents() throws Exception {
        Path path=temp.resolve("click-debug.jsonl");
        long queued=System.nanoTime(),intended=queued-25_000_123;
        try(var log=new ClickDebugLog(path,()->true,failure->{throw new AssertionError(failure);})) {
            log.generated("spam_click","right",-99,"Humble",intended,queued);
        }
        var row=JsonParser.parseString(Files.readAllLines(path).get(1)).getAsJsonObject();
        assertEquals("Humble",row.get("profile").getAsString());
        assertEquals(Long.toString(intended),row.get("intended_ns_text").getAsString());
        assertEquals(Long.toString(queued),row.get("queued_ns_text").getAsString());
        assertEquals("25000123",row.get("dispatch_lateness_ns_text").getAsString());
        assertFalse(row.has("native_event_ns")); assertFalse(row.has("native_event_ns_text"));
    }
    @Test void clearIsOrderedWithPendingWritesAndUsesTheSameFile() throws Exception {
        Path path = temp.resolve("click-debug.jsonl");
        try (var log = new ClickDebugLog(path, () -> true, failure -> {})) {
            for (int i = 0; i < 200; i++) log.event("physical", "mouse", "left", "press", -100, -1);
            log.clear().get(5, TimeUnit.SECONDS);
            assertEquals(0, Files.size(path));
            log.event("artificial", "spam_click", "right", "queue", -99, -1);
        }
        assertEquals(2, Files.readAllLines(path).size());
        assertTrue(Files.readString(path).contains("spam_click"));
        assertFalse(Files.readString(path).contains("\"physical\""));
        try (var files = Files.list(temp)) { assertEquals(1, files.count()); }
    }
    @Test void queuedInputIsRetainedAndInterruptedTailDoesNotCorruptNextRecord() throws Exception {
        Path path = temp.resolve("click-debug.jsonl"); Files.writeString(path, "interrupted");
        try (var log = new ClickDebugLog(path, () -> true, failure -> {})) {
            for (int i = 0; i < 500; i++) log.event("physical", "mouse", "left", "press", -100, -1);
        }
        var lines = Files.readAllLines(path); assertEquals("interrupted", lines.get(0)); assertEquals(502, lines.size());
        for (String line : lines.subList(1, lines.size())) assertTrue(new JsonParser().parse(line).isJsonObject());
    }
}
