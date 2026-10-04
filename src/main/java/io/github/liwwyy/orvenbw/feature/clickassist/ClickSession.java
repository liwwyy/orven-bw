package io.github.liwwyy.orvenbw.feature.clickassist;

import java.util.function.DoubleSupplier;

/** Weighted levels, bounded smooth variation, then optional exhaustion; no input is replayed. */
public final class ClickSession {
    private static final long MS = 1_000_000L, BLEND = 150 * MS;
    public record Options(double high, double medium, double low, boolean ramp, int rampMs,
                          boolean exhaustion, int afterMs, int intervalMs, int chance,
                          int durationMs, double minCps, double maxCps, boolean vary,
                          double levelVariation, double timingVariation, int minLevelMs, int maxLevelMs) {
        public Options(double high, double medium, double low, boolean ramp, int rampMs,
                       boolean exhaustion, int afterMs, int intervalMs, int chance,
                       int durationMs, double minCps, double maxCps) {
            this(high, medium, low, ramp, rampMs, exhaustion, afterMs, intervalMs, chance,
                    durationMs, minCps, maxCps, false, 0, 0, 1000, 1000);
        }
        public Options {
            high = rate(high); medium = rate(medium); low = rate(low);
            rampMs = Math.clamp(rampMs, 100, 5000);
            afterMs = Math.clamp(afterMs, 1000, 60000);
            intervalMs = Math.clamp(intervalMs, 1000, 30000);
            chance = Math.clamp(chance, 0, 100);
            durationMs = Math.clamp(durationMs, 100, 3000);
            minCps = rate(minCps); maxCps = rate(maxCps);
            levelVariation = finiteBound(levelVariation, 0, 15);
            timingVariation = finiteBound(timingVariation, 0, 35);
            minLevelMs = Math.clamp(minLevelMs, 300, 3000);
            maxLevelMs = Math.clamp(maxLevelMs, 300, 3000);
        }
    }
    private final DoubleSupplier random;
    private final int[] bag = new int[5];
    private boolean active;
    private long started, nextCheck, tiredUntil, slotStart, slotEnd, noiseAt;
    private int bagIndex = 5, cycles;
    private double initial, previous, blendFrom, selected, tiredCps, noise = 1, nextNoise = 1;
    public ClickSession(DoubleSupplier random) { this.random = random; }

    public double target(long now, boolean clicking, double initialRate, Options o) {
        if (!clicking) { reset(); return 0; }
        if (!active) {
            active = true; started = now; initial = Math.min(o.high(), Math.max(1, initialRate));
            previous = initial; nextCheck = now + o.afterMs() * MS;
            noiseAt = now;
        }
        long rampTime = o.ramp() ? o.rampMs() * MS : 0;
        long elapsed = Math.max(0, now - started);
        double target;
        if (elapsed < rampTime) {
            target = initial + (o.high() - initial) * elapsed / rampTime;
        } else {
            long steadyTime = elapsed - rampTime;
            int skipped = 0;
            while (steadyTime >= slotEnd) {
                boolean first = slotEnd == 0;
                slotStart = slotEnd;
                if (bagIndex == 5) refill();
                double level = switch (bag[bagIndex++]) { case 0 -> o.high(); case 1 -> o.medium(); default -> o.low(); };
                double spread = o.vary() ? o.levelVariation() / 100.0 : 0;
                selected = rate(level * (1 + triangular() * spread));
                blendFrom = first ? o.high() : previous;
                int min = Math.min(o.minLevelMs(), o.maxLevelMs()), max = Math.max(o.minLevelMs(), o.maxLevelMs());
                slotEnd += (o.vary() ? min + (long) (unit() * (max - min)) : 1000) * MS;
                if (steadyTime >= slotEnd) previous = selected;
                // A large clock jump picks a fresh level instead of replaying arbitrarily many slots.
                if (++skipped >= 256) { slotStart = steadyTime; slotEnd = steadyTime + 1000 * MS; blendFrom = selected; break; }
            }
            double progress = Math.clamp((steadyTime - slotStart) / (double) BLEND, 0, 1);
            target = blendFrom + (selected - blendFrom) * progress;
        }
        previous = target;
        // Slow correlated variation changes the rate gently; interval jitter is applied by the scheduler.
        if (o.vary()) {
            if (now - noiseAt >= 180 * MS) {
                noise = nextNoise;
                nextNoise = 1 + triangular() * o.timingVariation() / 100.0 * 0.15;
                noiseAt = now;
            }
            target *= noise + (nextNoise - noise) * Math.clamp((now - noiseAt) / (double) (180 * MS), 0, 1);
        }
        // Exhaustion is applied last, so variation cannot lift the exhausted target above its range.
        if (o.exhaustion() && now >= nextCheck) {
            nextCheck = now + o.intervalMs() * MS;
            if (unit() * 100 < o.chance()) {
                double min = Math.min(o.minCps(), o.maxCps()), max = Math.max(o.minCps(), o.maxCps());
                tiredCps = min + unit() * (max - min);
                tiredUntil = now + o.durationMs() * MS;
            }
        }
        return rate(o.exhaustion() && now < tiredUntil ? Math.min(target, tiredCps) : target);
    }
    private void refill() {
        bag[0] = 0; bag[1] = bag[2] = 1; bag[3] = bag[4] = 2;
        int first = cycles++ == 0 ? 1 : 0;
        for (int i = 4; i > first; i--) {
            int j = first + Math.min(i - first, (int) (unit() * (i - first + 1)));
            int temp = bag[i]; bag[i] = bag[j]; bag[j] = temp;
        }
        bagIndex = 0;
    }
    public void reset() {
        active = false; tiredUntil = slotStart = slotEnd = 0; bagIndex = 5; cycles = 0;
        noise = nextNoise = 1;
    }
    private double unit() { return Math.clamp(random.getAsDouble(), 0, Math.nextDown(1.0)); }
    private double triangular() { return unit() + unit() - 1; }
    private static double finiteBound(double value, double min, double max) { return Double.isFinite(value) ? Math.clamp(value, min, max) : min; }
    private static double rate(double value) { return finiteBound(value, 1, 20); }
}
