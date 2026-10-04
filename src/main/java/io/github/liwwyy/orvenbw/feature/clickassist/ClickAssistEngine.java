package io.github.liwwyy.orvenbw.feature.clickassist;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.function.DoubleSupplier;
import java.util.function.LongConsumer;

/** Counts real input separately and dispatches fractional generated rates on the client thread. */
public final class ClickAssistEngine {
    private static final long SECOND = 1_000_000_000L;
    private final Channel[] channels = {new Channel(), new Channel()};
    private long lastClick = Long.MIN_VALUE;
    public ClickAssistEngine() {}
    public record Cps(int base, int boosted) { public int total() { return base + boosted; } }
    private static final class Channel {
        final Deque<Long> physical = new ArrayDeque<>(), boosted = new ArrayDeque<>();
        long lastPhysical, previousPhysical, countingSince, lastPoll;
        boolean hasPhysical, hasPrevious, running;
        double rate;
        long nextDue, notBefore;
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
        if (now - c.countingSince < SECOND) return SECOND / (double) interval;
        return c.physical.size();
    }
    public boolean manuallyActive(int button, long now, int activation) {
        Channel c = channel(button);
        if (activation <= 0) return c.hasPhysical && now - c.lastPhysical <= 400_000_000L;
        return manualRate(button, now) > activation;
    }

    /** Deadline scheduler: at most two due clicks per tick; stalls and excess debt are discarded. */
    public int pollDue(int button, long now, double generatedRate, boolean eligible, long initialDelay,
                       double ceiling, DoubleSupplier intervalWeight, LongConsumer dispatch) {
        Channel c = channel(button);
        prune(c, now);
        if (!eligible || !Double.isFinite(generatedRate) || generatedRate <= 0) {
            cancel(button); return 0;
        }
        double rate = Math.clamp(generatedRate, .01, 22);
        if (!c.running) {
            c.running = true; c.lastPoll = now; c.nextDue = now + Math.max(0, initialDelay); c.notBefore = c.nextDue; c.rate = rate;
        } else {
            long elapsed = now - c.lastPoll;
            if (elapsed < 0 || elapsed > 250_000_000L) {
                c.lastPoll = now; c.rate = rate; c.nextDue = now + interval(rate, intervalWeight); return 0;
            }
            if (now >= c.notBefore && rate != c.rate && c.nextDue > now)
                c.nextDue = now + (long) ((c.nextDue - now) * c.rate / rate);
            c.lastPoll = now; c.rate = rate;
        }
        if (now < c.notBefore) return 0;
        int count = 0;
        int limit = (int) Math.ceil(Double.isFinite(ceiling) ? Math.clamp(ceiling, 1, 22) : 22);
        while (now >= c.nextDue && count < 2) {
            long intended = c.nextDue;
            c.nextDue += interval(rate, intervalWeight);
            // The generated total ceiling never suppresses physical input.
            if (c.physical.size() + c.boosted.size() >= limit) break;
            dispatch.accept(intended);
            c.boosted.addLast(now); lastClick = now; count++;
        }
        if (now >= c.nextDue) c.nextDue = now + interval(rate, intervalWeight);
        return count;
    }
    private static long interval(double rate, DoubleSupplier weight) {
        double value = weight.getAsDouble();
        if (!Double.isFinite(value) || value <= 0) value = 1;
        return (long) Math.clamp(value * SECOND / rate, 8_000_000, 120_000_000_000L);
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
    public void cancel(int button) { Channel c = channel(button); c.running = false; c.nextDue = 0; }
    public Cps cps(int button, long now) {
        Channel c = channel(button); prune(c, now);
        return new Cps(c.physical.size(), c.boosted.size());
    }
    public long lastClickNanos() { return lastClick; }
    public void reset() {
        lastClick = Long.MIN_VALUE;
        for (Channel c : channels) {
            c.physical.clear(); c.boosted.clear(); c.hasPhysical = c.hasPrevious = c.running = false;
            c.nextDue = 0;
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
