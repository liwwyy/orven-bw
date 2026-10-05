package io.github.liwwyy.orvenbw.feature.autotool;

import java.util.Objects;
import java.util.function.DoubleSupplier;

/** Client-thread timer and slot ownership; one random delay sample per requested switch. */
public final class AutoToolState {
    private Object hover, pendingTarget;
    private long hoverSince, leftSince = -1, pendingSince, delay;
    private int pendingSlot = -1, previousSlot = -1, ownedSlot = -1;
    private int lastDelay = -1, lastVariation = -1;

    public int update(long now, Object target, int bestSlot, int currentSlot, boolean leftDown,
                      boolean requireLeft, int switchMs, int variationMs, int hoverMs, DoubleSupplier random) {
        if (leftDown) { if (leftSince < 0) leftSince = now; } else leftSince = -1;
        if (!Objects.equals(target, hover)) { hover = target; hoverSince = now; }
        if (target == null || requireLeft && !leftDown || bestSlot < 0 || bestSlot == currentSlot) {
            pendingSlot = -1; pendingTarget = null; return -1;
        }
        if (pendingSlot != bestSlot || !Objects.equals(pendingTarget, target)
                || switchMs != lastDelay || variationMs != lastVariation) {
            pendingSlot = bestSlot; pendingTarget = target; pendingSince = now;
            lastDelay = switchMs; lastVariation = variationMs;
            double variedMs = Math.clamp(switchMs, 0, 1000);
            // The same sample drives this whole switch; no per-tick resampling.
            variedMs += (random.getAsDouble() * 2 - 1) * Math.clamp(variationMs, 0, 500);
            delay = (long) (Math.max(0, variedMs) * 1_000_000L);
        }
        long readyAt = Math.max(pendingSince + delay, hoverSince + Math.clamp(hoverMs, 0, 1000) * 1_000_000L);
        if (requireLeft) readyAt = Math.max(readyAt, leftSince + delay);
        return now >= readyAt ? bestSlot : -1;
    }
    public void switched(int from, int to) {
        if (previousSlot < 0) previousSlot = from;
        ownedSlot = to; pendingSlot = -1; pendingTarget = null;
    }
    public boolean ownsSlot() { return ownedSlot >= 0; }
    public int ownedSlot() { return ownedSlot; }
    public void requestRestoreSlot(int slot, boolean override) {
        if (ownsSlot() && override) previousSlot = Math.floorMod(slot, 9);
    }
    public int finish(int currentSlot, boolean restore) {
        int result = restore && currentSlot == ownedSlot ? previousSlot : -1;
        reset(); return result;
    }
    public void reset() {
        hover = pendingTarget = null; leftSince = -1; pendingSlot = previousSlot = ownedSlot = -1;
        hoverSince = pendingSince = delay = 0; lastDelay = lastVariation = -1;
    }
}
