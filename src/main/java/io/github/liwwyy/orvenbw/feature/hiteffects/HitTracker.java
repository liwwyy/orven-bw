package io.github.liwwyy.orvenbw.feature.hiteffects;

import java.util.ArrayDeque;

/** Correlates attempted local attacks with health decreases; swings alone never add hits. */
public final class HitTracker {
    private static final long CONFIRM_WINDOW = 750_000_000L;
    private record Attempt(long at, boolean critical) {}
    private final ArrayDeque<Attempt> attempts = new ArrayDeque<>();
    private Object target;
    private double health, damage;
    private int combo, displayCombo;
    private long lastHit;
    private boolean critical, initialized;
    public void attack(Object entity, double currentHealth, boolean critical, long now, long comboReset) {
        if (target != entity) { reset(); target = entity; }
        observe(currentHealth, now, comboReset);
        if (attempts.size() >= 32) attempts.removeFirst();
        attempts.addLast(new Attempt(now, critical));
    }
    public boolean observe(double currentHealth, long now, long comboReset) {
        if (!Double.isFinite(currentHealth)) return false;
        expire(now);
        if (combo > 0 && now - lastHit >= comboReset) combo = 0;
        double drop = initialized ? health - currentHealth : 0;
        health = Math.max(0, currentHealth); initialized = true;
        if (drop <= 0.001 || attempts.isEmpty()) return false;
        // A health update cannot identify multiple hits: count one observed decrease, never invent a count.
        Attempt matched = attempts.removeFirst();
        attempts.clear();
        damage = drop; critical = matched.critical(); combo++; displayCombo = combo; lastHit = now;
        return true;
    }
    private void expire(long now) {
        while (!attempts.isEmpty() && now - attempts.peekFirst().at() > CONFIRM_WINDOW) attempts.removeFirst();
    }
    public int combo() { return combo; }
    public int lastCombo() { return displayCombo; }
    public double damage() { return damage; }
    public boolean critical() { return critical; }
    public long lastHit() { return lastHit; }
    public int color() { return critical ? 0xFFD166 : displayCombo >= 5 ? 0xC792EA : displayCombo >= 3 ? 0xFFAD66 : 0x7EE0A1; }
    public void reset() { target = null; attempts.clear(); combo = displayCombo = 0; health = damage = 0; initialized = critical = false; lastHit = 0; }
}
