package io.github.liwwyy.orvenbw.feature.esp;

import com.google.gson.Gson;
import io.github.liwwyy.orvenbw.OrvenBw;
import java.nio.file.*;
import java.io.IOException;
import java.time.Instant;
import java.util.*;
import java.util.function.BooleanSupplier;

/** Opt-in, throttled, append-only diagnostics; never interrupts game logic on an I/O failure. */
public final class EspDiagnostics {
    private final Path file;
    private final BooleanSupplier enabled;
    private final Map<String,Long> last = new HashMap<>();
    private boolean warned;
    public EspDiagnostics(Path file, BooleanSupplier enabled) { this.file=file; this.enabled=enabled; }
    public void record(String key, String detail) {
        if (!enabled.getAsBoolean()) return;
        long now=System.currentTimeMillis();
        if (now-last.getOrDefault(key,0L)<5000) return;
        if(last.size()>512) last.clear();
        last.put(key,now);
        OrvenBw.LOGGER.info("ESP [{}] {}",key,detail);
        try {
            Files.createDirectories(file.getParent());
            Files.writeString(file,new Gson().toJson(Map.of("time",Instant.ofEpochMilli(now).toString(),"event",key,"detail",detail))+"\n",StandardOpenOption.CREATE,StandardOpenOption.APPEND);
        } catch(IOException error) { if(!warned) { warned=true; OrvenBw.LOGGER.warn("Could not append ESP diagnostics",error); } }
    }
}
