package io.github.liwwyy.orvenbw.feature.hiteffects;

import java.util.ArrayDeque;

/** Correlates local attacks with server hurt events or health decreases; swings alone never add hits. */
public final class HitTracker {
    private static final long CONFIRM_WINDOW = 750_000_000L;
    private record Attempt(long at, boolean critical) {}
    private final ArrayDeque<Attempt> attempts = new ArrayDeque<>();
    private Object target;
    private double health, damage;
    private int combo, displayCombo, criticalStreak;
    private long lastHit;
    private boolean critical, initialized;
    private boolean waitingDamage, healthFirst;
    public void attack(Object entity, double currentHealth, boolean critical, long now, long comboReset) {
        if (target != entity) { reset(); target = entity; }
        observe(currentHealth, now, comboReset);
        if (attempts.size() >= 32) attempts.removeFirst();
        attempts.addLast(new Attempt(now, critical));
    }
    public boolean observe(double currentHealth, long now, long comboReset) {
        if (!Double.isFinite(currentHealth)) return false;
        expire(now);
        if (combo > 0 && now - lastHit >= comboReset) { combo = 0; criticalStreak = 0; }
        double drop = initialized ? health - currentHealth : 0;
        health = Math.max(0, currentHealth); initialized = true;
        if (drop <= 0.001) return false;
        if (waitingDamage && now - lastHit <= CONFIRM_WINDOW) {
            damage = drop; waitingDamage = false; return false;
        }
        if (attempts.isEmpty()) return false;
        // A health update cannot identify multiple hits: count one observed decrease, never invent a count.
        Attempt matched = attempts.removeFirst();
        attempts.clear();
        confirm(matched, drop, now, comboReset); healthFirst = true;
        return true;
    }
    public boolean hurt(Object entity, long now, long comboReset) {
        if (target != entity) return false;
        expire(now);
        // Metadata may precede the hurt event. Do not count both packets as different hits.
        if (attempts.isEmpty() || healthFirst && now - lastHit < 150_000_000L) return false;
        Attempt matched = attempts.removeFirst(); attempts.clear();
        confirm(matched, Double.NaN, now, comboReset);
        waitingDamage = true; healthFirst = false;
        return true;
    }
    private void confirm(Attempt matched, double amount, long now, long comboReset) {
        if (combo > 0 && now - lastHit >= comboReset) { combo = 0; criticalStreak = 0; }
        damage = amount; critical = matched.critical(); combo++; displayCombo = combo; lastHit = now;
        criticalStreak = critical ? criticalStreak + 1 : 0;
        waitingDamage = false;
    }
    private void expire(long now) {
        while (!attempts.isEmpty() && now - attempts.peekFirst().at() > CONFIRM_WINDOW) attempts.removeFirst();
    }
    public int combo() { return combo; }
    public int lastCombo() { return displayCombo; }
    public double damage() { return damage; }
    public boolean critical() { return critical; }
    public int criticalStreak() { return criticalStreak; }
    public long lastHit() { return lastHit; }
    public int color() { return critical ? 0xFFD166 : displayCombo >= 5 ? 0xC792EA : displayCombo >= 3 ? 0xFFAD66 : 0x7EE0A1; }
    public void reset() { target = null; attempts.clear(); combo = displayCombo = criticalStreak = 0; health = damage = 0; initialized = critical = waitingDamage = healthFirst = false; lastHit = 0; }
}
