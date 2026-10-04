package io.github.liwwyy.orvenbw.debug;

import com.google.gson.JsonObject;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/** Timestamp on the input thread; serialize append/clear operations on one background writer. */
public final class ClickDebugLog implements AutoCloseable {
    private final Path path;
    private final String modVersion;
    private final BooleanSupplier enabled;
    private final Consumer<Throwable> report;
    private final String session = UUID.randomUUID().toString();
    private final long started = System.nanoTime();
    private final AtomicLong sequence = new AtomicLong();
    private final AtomicBoolean closed = new AtomicBoolean();
    private final ExecutorService executor = Executors.newSingleThreadExecutor(task -> {
        Thread thread = new Thread(task, "orven-bw-click-debug"); thread.setDaemon(true); return thread;
    });
    private BufferedWriter writer;
    private boolean failed;
    public ClickDebugLog(Path path, BooleanSupplier enabled, Consumer<Throwable> report) {
        this(path, enabled, report, "unknown");
    }
    public ClickDebugLog(Path path, BooleanSupplier enabled, Consumer<Throwable> report, String modVersion) {
        this.path = path; this.enabled = enabled; this.report = report; this.modVersion = modVersion;
    }
    public Path path() { return path; }
    public void event(String source, String method, String side, String action, int keyCode, long nativeEventNanos) {
        if (!enabled.getAsBoolean() || closed.get()) return;
        long nanos = System.nanoTime(), millis = System.currentTimeMillis();
        JsonObject row = stamp("input", millis, nanos);
        row.addProperty("source", source); row.addProperty("method", method);
        row.addProperty("side", side); row.addProperty("action", action); row.addProperty("key_code", keyCode);
        if (nativeEventNanos >= 0) {
            row.addProperty("native_event_ns", nativeEventNanos);
            row.addProperty("native_event_ns_text", Long.toString(nativeEventNanos));
        }
        submit(() -> append(row));
    }
    /** Intended model deadlines are distinct from actual queue times and native hardware timestamps. */
    public void generated(String method, String side, int keyCode, String profile, long intended, long queued) {
        if (!enabled.getAsBoolean() || closed.get()) return;
        JsonObject row = stamp("input", System.currentTimeMillis(), queued);
        row.addProperty("source", "artificial"); row.addProperty("method", method);
        row.addProperty("side", side); row.addProperty("action", "queue"); row.addProperty("key_code", keyCode);
        row.addProperty("profile", profile);
        row.addProperty("intended_ns_text", Long.toString(intended));
        row.addProperty("intended_elapsed_ns_text", Long.toString(intended - started));
        row.addProperty("queued_ns_text", Long.toString(queued));
        row.addProperty("dispatch_lateness_ns_text", Long.toString(Math.max(0, queued - intended)));
        submit(() -> append(row));
    }
    private JsonObject stamp(String event, long millis, long nanos) {
        JsonObject row = new JsonObject();
        row.addProperty("schema", 1); row.addProperty("event", event); row.addProperty("session", session);
        row.addProperty("sequence", event.equals("input") ? sequence.incrementAndGet() : 0);
        row.addProperty("timestamp_ms", millis); row.addProperty("monotonic_ns", nanos);
        row.addProperty("elapsed_ns", nanos - started);
        if (event.equals("session_start")) {
            row.addProperty("build_flavor", "standard");
            row.addProperty("mod_version", modVersion);
        }
        return row;
    }
    private void append(JsonObject row) throws IOException {
        if (writer == null) {
            Files.createDirectories(path.getParent());
            // Recover an interrupted final row without concatenating the next record onto it.
            if (Files.exists(path) && Files.size(path) > 0) {
                try (var file = new java.io.RandomAccessFile(path.toFile(), "rw")) {
                    file.seek(file.length() - 1);
                    if (file.read() != '\n') { file.seek(file.length()); file.write('\n'); }
                }
            }
            writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            writer.write(stamp("session_start", System.currentTimeMillis(), System.nanoTime()).toString()); writer.newLine();
        }
        writer.write(row.toString()); writer.newLine(); writer.flush();
    }
    public CompletableFuture<Void> clear() {
        return submit(() -> {
            if (writer != null) { writer.close(); writer = null; }
            failed = false;
            Files.createDirectories(path.getParent());
            Files.writeString(path, "", StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        }, true);
    }
    public CompletableFuture<Void> flush() { return submit(() -> { if (writer != null) writer.flush(); }); }
    private CompletableFuture<Void> submit(IOAction action) { return submit(action, false); }
    private CompletableFuture<Void> submit(IOAction action, boolean recover) {
        CompletableFuture<Void> result = new CompletableFuture<>();
        try {
            executor.execute(() -> {
                if (failed && !recover) { result.completeExceptionally(new IOException("Debug writer unavailable")); return; }
                try { action.run(); result.complete(null); }
                catch (IOException exception) {
                    if (!failed) report.accept(exception);
                    failed = true;
                    try { if (writer != null) writer.close(); } catch (IOException ignored) {}
                    writer = null; result.completeExceptionally(exception);
                }
            });
        } catch (RejectedExecutionException exception) { result.completeExceptionally(exception); }
        return result;
    }
    @FunctionalInterface private interface IOAction { void run() throws IOException; }
    @Override public void close() {
        if (!closed.compareAndSet(false, true)) return;
        submit(() -> { if (writer != null) { writer.close(); writer = null; } });
        executor.shutdown();
        try { executor.awaitTermination(3, TimeUnit.SECONDS); }
        catch (InterruptedException exception) { Thread.currentThread().interrupt(); }
    }
}
