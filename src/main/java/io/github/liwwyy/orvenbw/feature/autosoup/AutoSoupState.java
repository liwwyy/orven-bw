package io.github.liwwyy.orvenbw.feature.autosoup;

import java.util.Arrays;
import java.util.function.DoubleSupplier;
import static io.github.liwwyy.orvenbw.feature.autosoup.SoupInventory.Kind;

/** One action per poll, monotonic deadlines, and explicit confirmation before further actions. */
public final class AutoSoupState {
    public enum Phase { IDLE, USE, RETURN, CONFIRM, REFILL, REFILL_CONFIRM }
    public enum Type { NONE, SELECT, USE, OPEN, MOVE, CLOSE }
    public record Command(Type type, int slot, int hotbar) {
        public static final Command NONE = new Command(Type.NONE, -1, -1);
    }
    public record Options(double healthMin, double healthMax, int maxSoups, boolean refill,
                          int consumeMin, int consumeMax, int returnMin, int returnMax,
                          int moveMin, int moveMax, int responseMs, int cooldownMs) {}
    public record Snapshot(double health, Kind[] slots, int[] counts, int selected,
                           boolean gameplay, boolean ownInventory, boolean cursorEmpty, boolean usingItem) {}
    private final DoubleSupplier random;
    private final boolean[] used = new boolean[9];
    private Phase phase = Phase.IDLE;
    private long due, cooldown, consumeAt, moveDue;
    private double threshold = Double.NaN, beforeHealth;
    private int refillAt = -1, soupSlot = -1, original = -1, usedCount, beforeCount;
    private SoupInventory.Move pendingMove;
    public AutoSoupState(DoubleSupplier random) { this.random = random; }
    public boolean busy() { return phase != Phase.IDLE; }
    public int soupSlot() { return soupSlot; }
    public int originalSlot() { return original; }
    public Phase phase() { return phase; }
    public Command poll(long now, Snapshot s, Options o) {
        if (!s.cursorEmpty()) { abandon(now, o); return Command.NONE; }
        if (phase == Phase.IDLE) {
            if (!s.gameplay() || s.usingItem() || now < cooldown) return Command.NONE;
            if (Double.isNaN(threshold)) {
                double low = Math.clamp(Math.min(o.healthMin(), o.healthMax()), 1, 20);
                double high = Math.clamp(Math.max(o.healthMin(), o.healthMax()), 1, 20);
                threshold = low + random.getAsDouble() * (high - low);
            }
            if (refillAt < 0) refillAt = random.getAsDouble() < .5 ? 0 : 1;
            int soup = SoupInventory.randomSoup(s.slots(), null, random);
            if (s.health() <= threshold && soup >= 0) {
                original = s.selected(); usedCount = 0; Arrays.fill(used, false);
                return selectSoup(now, soup, o);
            }
            if (o.refill() && SoupInventory.soupCount(s.slots(), s.counts()) <= refillAt
                    && SoupInventory.nextMove(s.slots()) != null) {
                phase = Phase.REFILL; due = now + delay(o.moveMin(), o.moveMax());
                return new Command(Type.OPEN, -1, -1);
            }
            return Command.NONE;
        }
        if (phase == Phase.REFILL || phase == Phase.REFILL_CONFIRM) {
            if (!s.ownInventory()) { abandon(now, o); return Command.NONE; }
            if (!o.refill()) { abandon(now, o); return new Command(Type.CLOSE, -1, -1); }
            if (phase == Phase.REFILL_CONFIRM) {
                if (s.slots()[pendingMove.hotbar()] == Kind.SOUP && s.slots()[pendingMove.source()] != Kind.SOUP) {
                    phase = Phase.REFILL; due = moveDue;
                } else if (now >= due) { abandon(now, o); return new Command(Type.CLOSE, -1, -1); }
                else return Command.NONE;
            }
            if (now < due) return Command.NONE;
            SoupInventory.Move move = SoupInventory.nextMove(s.slots());
            if (move == null) { abandon(now, o); return new Command(Type.CLOSE, -1, -1); }
            pendingMove = move; phase = Phase.REFILL_CONFIRM;
            moveDue = now + delay(o.moveMin(), o.moveMax());
            due = now + response(o);
            return new Command(Type.MOVE, move.source(), move.hotbar());
        }
        if (!s.gameplay()) { abandon(now, o); return Command.NONE; }
        if (phase == Phase.USE) {
            if (s.selected() != soupSlot || s.slots()[soupSlot] != Kind.SOUP) { abandon(now, o); return Command.NONE; }
            if (s.health() > threshold) {
                int slot = SoupInventory.swordSlot(s.slots(), original); abandon(now, o);
                return new Command(Type.SELECT, slot, -1);
            }
            if (now < due) return Command.NONE;
            used[soupSlot] = true; usedCount++; beforeHealth = s.health(); beforeCount = s.counts()[soupSlot];
            phase = Phase.RETURN; consumeAt = now; due = now + delay(o.returnMin(), o.returnMax());
            return new Command(Type.USE, soupSlot, -1);
        }
        if (phase == Phase.RETURN) {
            if (s.selected() != soupSlot) { abandon(now, o); return Command.NONE; }
            if (now < due) return Command.NONE;
            phase = Phase.CONFIRM; due = consumeAt + response(o);
            return new Command(Type.SELECT, SoupInventory.swordSlot(s.slots(), original), -1);
        }
        boolean confirmed = s.health() > beforeHealth || s.slots()[soupSlot] != Kind.SOUP || s.counts()[soupSlot] < beforeCount;
        if (!confirmed && now < due) return Command.NONE;
        int next = SoupInventory.randomSoup(s.slots(), used, random);
        if (confirmed && s.health() <= threshold && usedCount < Math.clamp(o.maxSoups(), 1, 9) && next >= 0)
            return selectSoup(now, next, o);
        abandon(now, o); return Command.NONE;
    }
    private Command selectSoup(long now, int slot, Options o) {
        soupSlot = slot; phase = Phase.USE; due = now + delay(o.consumeMin(), o.consumeMax());
        return new Command(Type.SELECT, slot, -1);
    }
    private long delay(int min, int max) {
        double low = Math.clamp(Math.min(min, max), 0, 1000), high = Math.clamp(Math.max(min, max), 0, 1000);
        return (long) ((low + random.getAsDouble() * (high - low)) * 1_000_000L);
    }
    private static long response(Options o) { return Math.clamp(o.responseMs(), 100, 3000) * 1_000_000L; }
    public void abandon(long now, Options o) { reset(); cooldown = now + Math.clamp(o.cooldownMs(), 0, 1000) * 1_000_000L; }
    public void reset() {
        phase = Phase.IDLE; due = cooldown = consumeAt = moveDue = 0;
        threshold = Double.NaN; refillAt = soupSlot = original = -1; usedCount = beforeCount = 0;
        pendingMove = null; Arrays.fill(used, false);
    }
}
