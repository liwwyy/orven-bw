package io.github.liwwyy.orvenbw.feature.clickassist;

import java.util.function.DoubleSupplier;

/** One-second weighted levels with a fractional ramp and short transitions. */
public final class ClickSession {
    private static final long SECOND = 1_000_000_000L;
    private static final long BLEND = 150_000_000L;
    public record Options(double high, double medium, double low, boolean ramp, int rampMs,
                          boolean exhaustion, int afterMs, int intervalMs, int chance,
                          int durationMs, double minCps, double maxCps) {
        public Options {
            high = rate(high); medium = rate(medium); low = rate(low);
            rampMs = Math.clamp(rampMs, 100, 5000);
            afterMs = Math.clamp(afterMs, 1000, 60000);
            intervalMs = Math.clamp(intervalMs, 1000, 30000);
            chance = Math.clamp(chance, 0, 100);
            durationMs = Math.clamp(durationMs, 100, 3000);
            minCps = rate(minCps); maxCps = rate(maxCps);
        }
    }
    private final DoubleSupplier random;
    private final int[] bag = new int[5];
    private boolean active;
    private long started, nextCheck, tiredUntil, cycle = -1, slot = -1;
    private double initial, previous, blendFrom, tiredCps;
    public ClickSession(DoubleSupplier random) { this.random = random; }

    public double target(long now, boolean clicking, double initialRate, Options o) {
        if (!clicking) { reset(); return 0; }
        if (!active) {
            active = true; started = now; initial = Math.min(o.high(), Math.max(1, initialRate));
            previous = initial; nextCheck = now + o.afterMs() * 1_000_000L;
        }
        long rampTime = o.ramp() ? o.rampMs() * 1_000_000L : 0;
        long elapsed = Math.max(0, now - started);
        double target;
        if (elapsed < rampTime) {
            target = initial + (o.high() - initial) * elapsed / rampTime;
        } else {
            long nextSlot = (elapsed - rampTime) / SECOND;
            if (nextSlot / 5 != cycle) {
                cycle = nextSlot / 5;
                bag[0] = 0; bag[1] = bag[2] = 1; bag[3] = bag[4] = 2;
                // The first session reaches its high rate before the weighted variation begins.
                int first = cycle == 0 ? 1 : 0;
                for (int i = 4; i > first; i--) {
                    int j = first + Math.min(i - first, (int) (random.getAsDouble() * (i - first + 1)));
                    int temp = bag[i]; bag[i] = bag[j]; bag[j] = temp;
                }
            }
            double selected = switch (bag[(int) (nextSlot % 5)]) {
                case 0 -> o.high(); case 1 -> o.medium(); default -> o.low();
            };
            if (slot != nextSlot) {
                blendFrom = nextSlot == 0 ? o.high() : slot == nextSlot - 1 ? previous : selected;
                slot = nextSlot;
            }
            double progress = Math.clamp((elapsed - rampTime - nextSlot * SECOND) / (double) BLEND, 0, 1);
            target = blendFrom + (selected - blendFrom) * progress;
        }
        previous = target;
        if (o.exhaustion() && now >= nextCheck) {
            nextCheck = now + o.intervalMs() * 1_000_000L;
            if (random.getAsDouble() * 100 < o.chance()) {
                double low = Math.min(o.minCps(), o.maxCps()), high = Math.max(o.minCps(), o.maxCps());
                tiredCps = low + random.getAsDouble() * (high - low);
                tiredUntil = now + o.durationMs() * 1_000_000L;
            }
        }
        return o.exhaustion() && now < tiredUntil ? Math.min(target, tiredCps) : target;
    }
    public void reset() { active = false; tiredUntil = 0; slot = cycle = -1; }
    private static double rate(double value) { return Double.isFinite(value) ? Math.clamp(value, 1, 20) : 1; }
}
