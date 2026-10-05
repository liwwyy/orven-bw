package io.github.liwwyy.orvenbw.feature.autosoup;

import org.junit.jupiter.api.Test;
import java.util.Arrays;
import static org.junit.jupiter.api.Assertions.*;
import static io.github.liwwyy.orvenbw.feature.autosoup.SoupInventory.Kind;
import static io.github.liwwyy.orvenbw.feature.autosoup.AutoSoupState.Type;

class AutoSoupStateTest {
    private static final long MS = 1_000_000L;
    private static final AutoSoupState.Options OPTIONS = new AutoSoupState.Options(4, 14, 2, true, 30, 55, 33, 55, 33, 44, 750, 250, 2000);
    private static class Inventory {
        final Kind[] slots = new Kind[36]; final int[] counts = new int[36];
        int selected; double health = 2; boolean gameplay = true, ownInventory, cursor = true, usingItem;
        Inventory() { Arrays.fill(slots, Kind.OTHER); slots[0] = Kind.SWORD; }
        void set(int slot, Kind kind) { slots[slot] = kind; counts[slot] = kind == Kind.EMPTY ? 0 : 1; }
        AutoSoupState.Snapshot snapshot() { return new AutoSoupState.Snapshot(health, slots, counts, selected, gameplay, ownInventory, cursor, usingItem); }
        void apply(AutoSoupState.Command command) {
            switch (command.type()) {
                case SELECT -> selected = command.slot();
                case OPEN -> { gameplay = false; ownInventory = true; }
                case CLOSE -> { gameplay = true; ownInventory = false; }
                case MOVE -> {
                    Kind old = slots[command.hotbar()]; int count = counts[command.hotbar()];
                    set(command.hotbar(), slots[command.slot()]); counts[command.hotbar()] = counts[command.slot()];
                    set(command.slot(), old); counts[command.slot()] = count;
                }
                default -> { }
            }
        }
    }
    @Test void consumesAfterSampledDelayReturnsToSwordAndCapsCycleAtTwo() {
        var state = new AutoSoupState(() -> .5); var inv = new Inventory();
        inv.set(2, Kind.SOUP); inv.set(4, Kind.SOUP); inv.set(6, Kind.SOUP);
        var select = state.poll(0, inv.snapshot(), OPTIONS); assertEquals(Type.SELECT, select.type()); assertEquals(4, select.slot()); inv.apply(select);
        assertEquals(Type.NONE, state.poll(42 * MS, inv.snapshot(), OPTIONS).type());
        assertEquals(Type.USE, state.poll(43 * MS, inv.snapshot(), OPTIONS).type()); inv.set(4, Kind.BOWL);
        assertEquals(Type.NONE, state.poll(86 * MS, inv.snapshot(), OPTIONS).type());
        var back = state.poll(87 * MS, inv.snapshot(), OPTIONS); assertEquals(Type.SELECT, back.type()); assertEquals(0, back.slot()); inv.apply(back);
        var second = state.poll(88 * MS, inv.snapshot(), OPTIONS); assertEquals(Type.SELECT, second.type()); assertEquals(6, second.slot()); inv.apply(second);
        assertEquals(Type.USE, state.poll(131 * MS, inv.snapshot(), OPTIONS).type()); inv.set(6, Kind.BOWL);
        inv.apply(state.poll(175 * MS, inv.snapshot(), OPTIONS));
        assertEquals(Type.NONE, state.poll(176 * MS, inv.snapshot(), OPTIONS).type()); assertFalse(state.busy());
        assertEquals(Type.NONE, state.poll(300 * MS, inv.snapshot(), OPTIONS).type());
        assertEquals(Kind.SOUP, inv.slots[2], "Third soup is preserved until a new cycle");
    }
    @Test void healthRecoveryStopsFurtherSoupAndUserSlotChoiceIsRespected() {
        var state = new AutoSoupState(() -> .5); var inv = new Inventory(); inv.set(2, Kind.SOUP); inv.set(4, Kind.SOUP);
        inv.apply(state.poll(0, inv.snapshot(), OPTIONS));
        assertEquals(Type.USE, state.poll(43 * MS, inv.snapshot(), OPTIONS).type());
        inv.health = 18; inv.set(inv.selected, Kind.BOWL);
        inv.apply(state.poll(87 * MS, inv.snapshot(), OPTIONS));
        assertEquals(Type.NONE, state.poll(88 * MS, inv.snapshot(), OPTIONS).type()); assertFalse(state.busy());
        state.reset(); inv.health = 2;
        inv.apply(state.poll(200 * MS, inv.snapshot(), OPTIONS)); inv.selected = 7;
        assertEquals(Type.NONE, state.poll(250 * MS, inv.snapshot(), OPTIONS).type()); assertFalse(state.busy()); assertEquals(7, inv.selected);
    }
    @Test void noAcknowledgmentMeansNoSecondUseAndCursorOrScreenChangesCancel() {
        var state = new AutoSoupState(() -> .5); var inv = new Inventory(); inv.set(2, Kind.SOUP); inv.set(4, Kind.SOUP);
        inv.apply(state.poll(0, inv.snapshot(), OPTIONS)); state.poll(43 * MS, inv.snapshot(), OPTIONS);
        inv.apply(state.poll(87 * MS, inv.snapshot(), OPTIONS));
        assertEquals(Type.NONE, state.poll(400 * MS, inv.snapshot(), OPTIONS).type()); assertTrue(state.busy());
        assertEquals(Type.NONE, state.poll(793 * MS, inv.snapshot(), OPTIONS).type()); assertTrue(state.busy());
        inv.apply(state.poll(2043 * MS, inv.snapshot(), OPTIONS));
        assertEquals(Type.NONE, state.poll(2044 * MS, inv.snapshot(), OPTIONS).type()); assertFalse(state.busy());
        state.reset(); inv.cursor = false;
        assertEquals(Type.NONE, state.poll(1000 * MS, inv.snapshot(), OPTIONS).type()); assertFalse(state.busy());
        state.reset(); inv.cursor = true; inv.apply(state.poll(1200 * MS, inv.snapshot(), OPTIONS)); inv.gameplay = false;
        assertEquals(Type.NONE, state.poll(1300 * MS, inv.snapshot(), OPTIONS).type()); assertFalse(state.busy());
    }
    @Test void refillReplacesOnlyEmptySlotsAndBowlsAndWaitsBetweenMoves() {
        var state = new AutoSoupState(() -> .75); var inv = new Inventory(); inv.health = 20;
        inv.set(2, Kind.BOWL); inv.set(4, Kind.EMPTY); inv.set(10, Kind.SOUP); inv.set(11, Kind.SOUP);
        var open = state.poll(0, inv.snapshot(), OPTIONS); assertEquals(Type.OPEN, open.type()); inv.apply(open);
        assertEquals(Type.NONE, state.poll(40 * MS, inv.snapshot(), OPTIONS).type());
        var first = state.poll(42 * MS, inv.snapshot(), OPTIONS); assertEquals(Type.MOVE, first.type()); assertEquals(4, first.hotbar()); inv.apply(first);
        assertEquals(Type.NONE, state.poll(75 * MS, inv.snapshot(), OPTIONS).type());
        var second = state.poll(84 * MS, inv.snapshot(), OPTIONS); assertEquals(Type.MOVE, second.type()); assertEquals(2, second.hotbar()); inv.apply(second);
        assertEquals(Type.CLOSE, state.poll(126 * MS, inv.snapshot(), OPTIONS).type());
        assertEquals(Kind.SWORD, inv.slots[0]); assertEquals(Kind.OTHER, inv.slots[1]); assertEquals(Kind.BOWL, inv.slots[11]);
    }
    @Test void failedRefillTimesOutAndManualScreensAreNeverOpenedOrTouched() {
        var state = new AutoSoupState(() -> .75); var inv = new Inventory(); inv.health = 20; inv.set(4, Kind.EMPTY); inv.set(10, Kind.SOUP);
        inv.gameplay = false;
        assertEquals(Type.NONE, state.poll(0, inv.snapshot(), OPTIONS).type());
        inv.gameplay = true; inv.apply(state.poll(0, inv.snapshot(), OPTIONS));
        assertEquals(Type.MOVE, state.poll(42 * MS, inv.snapshot(), OPTIONS).type());
        assertEquals(Type.NONE, state.poll(100 * MS, inv.snapshot(), OPTIONS).type());
        assertEquals(Type.CLOSE, state.poll(792 * MS, inv.snapshot(), OPTIONS).type()); assertFalse(state.busy());
    }
    @Test void randomizedRefillThresholdCanWaitUntilZeroSoupsAndHealthThresholdIsStable() {
        var state = new AutoSoupState(() -> .25); var inv = new Inventory(); inv.health = 12;
        inv.set(2, Kind.SOUP); inv.set(4, Kind.EMPTY); inv.set(10, Kind.SOUP);
        for (int i = 0; i < 5; i++) assertEquals(Type.NONE, state.poll(i * 50 * MS, inv.snapshot(), OPTIONS).type());
        inv.health = 7; assertEquals(Type.NONE, state.poll(300 * MS, inv.snapshot(), OPTIONS).type());
        inv.health = 6.5; assertEquals(Type.SELECT, state.poll(350 * MS, inv.snapshot(), OPTIONS).type());
        state.reset(); inv.health = 20; inv.set(2, Kind.BOWL);
        assertEquals(Type.OPEN, state.poll(400 * MS, inv.snapshot(), OPTIONS).type());
    }
    @Test void holdsSoupAcrossFullFoodUseAndDoesNotSwitchEarlyOnUnrelatedHealing() {
        var options = new AutoSoupState.Options(4, 14, 2, true, 110, 135, 113, 135, 113, 124, 830, 330, 2000);
        var state = new AutoSoupState(() -> .5); var inv = new Inventory(); inv.set(2, Kind.SOUP);
        inv.apply(state.poll(0, inv.snapshot(), options));
        assertFalse(state.protectsUse(2));
        assertEquals(Type.NONE, state.poll(122 * MS, inv.snapshot(), options).type());
        assertEquals(Type.USE, state.poll(123 * MS, inv.snapshot(), options).type());
        inv.usingItem = true; inv.health = 3;
        for (int ms = 150; ms <= 1700; ms += 50) {
            assertEquals(Type.NONE, state.poll(ms * MS, inv.snapshot(), options).type());
            assertTrue(state.protectsUse(2)); assertEquals(2, inv.selected);
            assertFalse(state.protectsUse(0), "A manual slot switch must cancel the owned hold");
        }
        inv.set(2, Kind.BOWL); inv.usingItem = false;
        inv.apply(state.poll(1723 * MS, inv.snapshot(), options));
        assertEquals(0, inv.selected); assertFalse(state.protectsUse(2));
    }
    @Test void blockedConsumptionHasABoundedHoldAndManualChangesCancelIt() {
        var state = new AutoSoupState(() -> .5); var inv = new Inventory(); inv.set(2, Kind.SOUP);
        inv.apply(state.poll(0, inv.snapshot(), OPTIONS));
        assertEquals(Type.USE, state.poll(43 * MS, inv.snapshot(), OPTIONS).type());
        assertEquals(Type.NONE, state.poll(2042 * MS, inv.snapshot(), OPTIONS).type());
        inv.apply(state.poll(2043 * MS, inv.snapshot(), OPTIONS)); assertEquals(0, inv.selected);
        assertFalse(state.protectsUse(2));
        state.poll(2044 * MS, inv.snapshot(), OPTIONS); assertFalse(state.busy());
        state.reset(); inv.apply(state.poll(3000 * MS, inv.snapshot(), OPTIONS));
        state.poll(3043 * MS, inv.snapshot(), OPTIONS); inv.selected = 7;
        state.poll(3100 * MS, inv.snapshot(), OPTIONS);
        assertFalse(state.busy()); assertFalse(state.protectsUse(2)); assertEquals(7, inv.selected);
    }
}
