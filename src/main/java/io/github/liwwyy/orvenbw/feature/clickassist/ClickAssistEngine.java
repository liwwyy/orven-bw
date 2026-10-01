package io.github.liwwyy.orvenbw.feature.clickassist;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.function.DoubleSupplier;

/** Client-thread scheduler. Physical input and generated clicks have separate histories. */
public final class ClickAssistEngine {
    private static final long SECOND = 1_000_000_000L;
    private static final long MAX_PENDING_AGE = 250_000_000L;
    private final Channel[] channels = {new Channel(), new Channel()};
    private final DoubleSupplier random;

    public ClickAssistEngine(DoubleSupplier random) { this.random = random; }

    public record Options(int activationCps, int totalCap, double chance, long delayNanos, boolean randomize) {
        public Options {
            activationCps = Math.clamp(activationCps, 0, 20);
            totalCap = Math.clamp(totalCap, 1, 30);
            chance = Math.clamp(chance, 0, 100);
            delayNanos = Math.clamp(delayNanos, 10_000_000L, 150_000_000L);
        }
    }
    public record Cps(int base, int boosted) { public int total() { return base + boosted; } }
    private record Pending(long due, long created) {}
    private static final class Channel {
        final Deque<Long> physical = new ArrayDeque<>();
        final Deque<Long> boosted = new ArrayDeque<>();
        final Deque<Pending> pending = new ArrayDeque<>();
    }

    public void physicalClick(int button, long now, Options options, boolean eligible) {
        Channel c = channel(button);
        prune(c, now);
        c.physical.addLast(now);
        if (!eligible || c.physical.size() <= options.activationCps() || options.chance() <= 0) return;
        if (c.physical.size() + c.boosted.size() + c.pending.size() >= options.totalCap()) return;
        if (random.getAsDouble() * 100 >= options.chance()) return;
        long delay = options.delayNanos();
        if (options.randomize()) delay = (long) (delay * (0.75 + random.getAsDouble() * 0.5));
        c.pending.addLast(new Pending(now + delay, now));
    }

    /** One generated click at most per channel per tick, without catching up after stalls. */
    public boolean pollBoost(int button, long now, Options options, boolean eligible) {
        Channel c = channel(button);
        prune(c, now);
        if (!eligible || c.physical.size() <= options.activationCps()) {
            c.pending.clear();
            return false;
        }
        while (!c.pending.isEmpty() && now - c.pending.peekFirst().created() > MAX_PENDING_AGE) c.pending.removeFirst();
        if (c.pending.isEmpty() || c.pending.peekFirst().due() > now) return false;
        c.pending.removeFirst();
        if (c.physical.size() + c.boosted.size() >= options.totalCap() || options.chance() <= 0) return false;
        c.boosted.addLast(now);
        return true;
    }

    public Cps cps(int button, long now) {
        Channel c = channel(button);
        prune(c, now);
        return new Cps(c.physical.size(), c.boosted.size());
    }

    public void cancelPending() { for (Channel c : channels) c.pending.clear(); }
    public void reset() {
        for (Channel c : channels) { c.physical.clear(); c.boosted.clear(); c.pending.clear(); }
    }
    private Channel channel(int button) {
        if (button < 0 || button > 1) throw new IllegalArgumentException("Unsupported click channel");
        return channels[button];
    }
    private static void prune(Channel c, long now) {
        while (!c.physical.isEmpty() && now - c.physical.peekFirst() >= SECOND) c.physical.removeFirst();
        while (!c.boosted.isEmpty() && now - c.boosted.peekFirst() >= SECOND) c.boosted.removeFirst();
    }
}
