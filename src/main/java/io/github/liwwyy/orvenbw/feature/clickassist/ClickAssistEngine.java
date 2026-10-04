package io.github.liwwyy.orvenbw.feature.clickassist;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.function.DoubleSupplier;

/** Counts real input separately and dispatches fractional generated rates on the client thread. */
public final class ClickAssistEngine {
    private static final long SECOND = 1_000_000_000L;
    private final Channel[] channels = {new Channel(), new Channel()};
    private final DoubleSupplier random;
    private long lastClick = Long.MIN_VALUE;
    public ClickAssistEngine(DoubleSupplier random) { this.random = random; }
    public record Cps(int base, int boosted) { public int total() { return base + boosted; } }
    private static final class Channel {
        final Deque<Long> physical = new ArrayDeque<>(), boosted = new ArrayDeque<>();
        long lastPhysical, previousPhysical, countingSince, lastPoll;
        boolean hasPhysical, hasPrevious, running;
        double credit, threshold;
    }

    public void physicalClick(int button, long now) {
        Channel c = channel(button);
        prune(c, now);
        if (!c.hasPhysical || now - c.lastPhysical > 400_000_000L) {
            c.countingSince = now;
            c.hasPhysical = c.hasPrevious = false;
        }
        c.previousPhysical = c.lastPhysical;
        c.hasPrevious = c.hasPhysical;
        c.lastPhysical = now;
        if (!c.hasPhysical) c.countingSince = now;
        c.hasPhysical = true;
        c.physical.addLast(now);
        lastClick = now;
    }

    /** Use cadence during warm-up; after one second use measured physical CPS. */
    public double manualRate(int button, long now) {
        Channel c = channel(button);
        prune(c, now);
        if (!c.hasPrevious || c.physical.size() < 2) return 0;
        long interval = c.lastPhysical - c.previousPhysical;
        if (interval <= 0) return 0;
        long freshness = Math.clamp(interval * 2, 150_000_000L, 400_000_000L);
        if (now - c.lastPhysical > freshness) return 0;
        if (now - c.countingSince < SECOND) return Math.min(20, SECOND / (double) interval);
        return c.physical.size();
    }
    public boolean manuallyActive(int button, long now, int activation) {
        Channel c = channel(button);
        if (activation <= 0) return c.hasPhysical && now - c.lastPhysical <= 400_000_000L;
        return manualRate(button, now) > activation;
    }

    /** At most one click per tick; a stall discards accrued work. Same scheduler for hold/toggle spam. */
    public boolean poll(int button, long now, double generatedRate, boolean eligible, boolean vary, long initialDelay) {
        return poll(button, now, generatedRate, eligible, vary, initialDelay, 0.15);
    }
    public boolean poll(int button, long now, double generatedRate, boolean eligible, boolean vary, long initialDelay, double variation) {
        Channel c = channel(button);
        prune(c, now);
        if (!eligible || !Double.isFinite(generatedRate) || generatedRate <= 0) {
            cancel(button); return false;
        }
        double rate = Math.min(20, generatedRate);
        if (!c.running) {
            c.running = true; c.lastPoll = now + Math.max(0, initialDelay);
            c.threshold = intervalWeight(vary, variation); c.credit = c.threshold;
        }
        if (now < c.lastPoll) return false;
        long elapsed = now - c.lastPoll;
        c.lastPoll = now;
        if (elapsed > 250_000_000L) {
            c.credit = 0; c.threshold = intervalWeight(vary, variation); return false;
        }
        c.credit = Math.min(c.threshold + 1, c.credit + Math.min(elapsed, 50_000_000L) * rate / SECOND);
        if (c.credit + 1e-9 < c.threshold) return false;
        c.credit -= c.threshold;
        c.threshold = intervalWeight(vary, variation);
        c.boosted.addLast(now);
        lastClick = now;
        return true;
    }
    private double intervalWeight(boolean vary, double variation) {
        double spread = Double.isFinite(variation) ? Math.clamp(variation, 0, 0.35) : 0;
        // Symmetric triangular jitter avoids repeatedly hitting the extreme interval bounds.
        return vary ? 1 + (random.getAsDouble() + random.getAsDouble() - 1) * spread : 1;
    }
    public int dominantButton(long now, int previous) {
        Cps left = cps(0, now), right = cps(1, now);
        int l = recent(channels[0], now), r = recent(channels[1], now);
        if (l != r) return l > r ? 0 : 1;
        if (left.total() != right.total()) return left.total() > right.total() ? 0 : 1;
        return previous == 1 ? 1 : 0;
    }
    private static int recent(Channel channel, long now) {
        int count = 0;
        for (long time : channel.physical) if (now - time < 250_000_000L) count++;
        for (long time : channel.boosted) if (now - time < 250_000_000L) count++;
        return count;
    }
    public void cancel(int button) { Channel c = channel(button); c.running = false; c.credit = 0; }
    public Cps cps(int button, long now) {
        Channel c = channel(button); prune(c, now);
        return new Cps(c.physical.size(), c.boosted.size());
    }
    public long lastClickNanos() { return lastClick; }
    public void reset() {
        lastClick = Long.MIN_VALUE;
        for (Channel c : channels) {
            c.physical.clear(); c.boosted.clear(); c.hasPhysical = c.hasPrevious = c.running = false;
            c.credit = 0;
        }
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
