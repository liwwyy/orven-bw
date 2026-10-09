package io.github.liwwyy.orvenbw.feature.esp;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.util.concurrent.atomic.AtomicBoolean;
import static org.junit.jupiter.api.Assertions.*;
class EspDiagnosticsTest {
    @TempDir Path root;
    @Test void optInThrottlesRepeatsAndAppendsAcrossSessions() throws Exception {
        Path file=root.resolve("orven-bw/esp-debug.jsonl"); var enabled=new AtomicBoolean(false);
        var log=new EspDiagnostics(file,enabled::get);
        log.record("team","off");assertFalse(Files.exists(file));
        enabled.set(true);log.record("team","unknown");log.record("team","repeat");
        log.record("obsidian","4/8");
        new EspDiagnostics(file,enabled::get).record("team","new session");
        var lines=Files.readAllLines(file);assertEquals(3,lines.size());
        assertTrue(lines.getFirst().contains("unknown"));assertTrue(lines.getLast().contains("new session"));
        enabled.set(false);log.record("another","disabled");assertEquals(3,Files.readAllLines(file).size());
    }
    @Test void fileFailureCannotInterruptGameDetection() throws Exception {
        Path parent=root.resolve("blocked");Files.writeString(parent,"file");
        var log=new EspDiagnostics(parent.resolve("debug.jsonl"),()->true);
        assertDoesNotThrow(()->log.record("failure","Cannot write beneath a file"));
    }
}
