package io.github.liwwyy.orvenbw.feature.autosoup;

import io.github.liwwyy.orvenbw.OrvenBw;
import io.github.liwwyy.orvenbw.config.OrvenConfig;
import io.github.liwwyy.orvenbw.feature.ClientFeature;
import io.github.liwwyy.orvenbw.feature.ScoreboardGate;
import io.github.liwwyy.orvenbw.mixin.InteractionManagerAccessor;
import io.github.liwwyy.orvenbw.mixin.SoupUseAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.game.inventory.SurvivalInventoryScreen;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.SwordItem;
import net.minecraft.network.packet.c2s.play.ClientStatusC2SPacket;
import java.util.concurrent.ThreadLocalRandom;
import static io.github.liwwyy.orvenbw.feature.autosoup.SoupInventory.Kind;

/** Client-thread soup use and owned-inventory refill, with no movement-key injection. */
public final class AutoSoupFeature implements ClientFeature {
    private final OrvenConfig config;
    private final AutoSoupState state = new AutoSoupState(() -> ThreadLocalRandom.current().nextDouble());
    private Minecraft client;
    private Object world, player;
    private SurvivalInventoryScreen ownedScreen;
    private boolean issuingUse;
    public AutoSoupFeature(OrvenConfig config) { this.config = config; }
    @Override public boolean ownsScreen(Minecraft mc) {
        return ownedScreen != null && mc.screen == ownedScreen && mc.world == world && mc.player == player;
    }
    private boolean eligible(Minecraft mc) {
        return config.autoSoupEnabled && ScoreboardGate.allows(mc, config) && mc.world != null && mc.player != null
                && mc.interactionManager != null && mc.getNetworkHandler() != null && mc.player.isAlive()
                && !mc.player.isSpectator() && !mc.player.abilities.creativeMode && !mc.isPaused()
                && mc.player.menu == mc.player.playerMenu
                && (mc.screen == null && mc.focused || ownsScreen(mc))
                && (!config.autoSoupScoreboardOnly || ScoreboardGate.sidebarMatches(mc, config.autoSoupScoreboardWord));
    }
    public boolean issuingUse() { return issuingUse; }
    public boolean busy() { return config.autoSoupEnabled && state.busy(); }
    public boolean protectsUse(Minecraft mc) {
        return mc.player == player && mc.world == world && eligible(mc)
                && state.protectsUse(mc.player.inventory.selectedSlot)
                && kind(mc.player.getItemInHand()) == Kind.SOUP;
    }
    public boolean blocksLeft() { return busy() && config.autoSoupDisableLeft; }
    @Override public void tick(Minecraft mc) { poll(mc); }
    public void frame(Minecraft mc) { poll(mc); }
    private void poll(Minecraft mc) {
        if (world != mc.world || player != mc.player) {
            state.reset(); ownedScreen = null; world = mc.world; player = mc.player;
        }
        client = mc;
        if (!eligible(mc)) { reset(); return; }
        Kind[] kinds = new Kind[36]; int[] counts = new int[36];
        for (int i = 0; i < 36; i++) {
            ItemStack stack = mc.player.inventory.items[i]; kinds[i] = kind(stack); counts[i] = stack == null ? 0 : stack.size;
        }
        boolean wasBusy = state.busy();
        int previousSoup = state.soupSlot(), original = state.originalSlot();
        AutoSoupState.Phase previousPhase = state.phase();
        var snapshot = new AutoSoupState.Snapshot(mc.player.getHealth(), kinds, counts, mc.player.inventory.selectedSlot,
                mc.screen == null && mc.focused, ownsScreen(mc), cursorEmpty(mc), mc.player.hasItemInUse());
        var command = state.poll(System.nanoTime(), snapshot, options());
        if (!wasBusy && state.busy()) {
            OrvenBw.instance().autoTool().reset();
        }
        switch (command.type()) {
            case SELECT -> {
                if (previousPhase == AutoSoupState.Phase.RETURN && mc.player.hasItemInUse()) mc.interactionManager.stopUsingHand(mc.player);
                select(mc, command.slot());
            }
            case USE -> {
                if (mc.player.inventory.selectedSlot == command.slot() && kind(mc.player.getItemInHand()) == Kind.SOUP) use(mc);
                else reset();
            }
            case OPEN -> {
                if (mc.screen != null || !cursorEmpty(mc)) { reset(); break; }
                ownedScreen = new SurvivalInventoryScreen(mc.player);
                mc.getNetworkHandler().sendPacket(new ClientStatusC2SPacket(ClientStatusC2SPacket.Status.OPEN_INVENTORY_ACHIEVEMENT));
                mc.openScreen(ownedScreen);
                ownedScreen.passEvents = false; // Ordinary inventory controls; no gameplay input passthrough.
            }
            case MOVE -> {
                if (!ownsScreen(mc) || !cursorEmpty(mc) || mc.player.menu != mc.player.playerMenu
                        || kind(mc.player.inventory.items[command.slot()]) != Kind.SOUP
                        || !(kind(mc.player.inventory.items[command.hotbar()]) == Kind.EMPTY || kind(mc.player.inventory.items[command.hotbar()]) == Kind.BOWL)) {
                    reset(); break;
                }
                // Player-menu main-inventory indexes 9..35 equal menu IDs. Mode 2 swaps with a hotbar index.
                mc.interactionManager.clickSlot(mc.player.menu.networkId, command.slot(), command.hotbar(), 2, mc.player);
            }
            case CLOSE -> closeOwned(mc);
            case NONE -> {
                if (wasBusy && !state.busy() && (previousPhase == AutoSoupState.Phase.USE || previousPhase == AutoSoupState.Phase.RETURN)
                        && mc.player.inventory.selectedSlot == previousSoup) select(mc, SoupInventory.swordSlot(kinds, original));
                if (ownedScreen != null && !state.busy()) closeOwned(mc);
            }
        }
    }
    private void use(Minecraft mc) {
        var mod = OrvenBw.instance();
        if (config.debugEnabled) mod.clickOrigins().beginDirectAction("auto_soup", 1, mc.options.useKey.getKeyCode(), System.nanoTime());
        issuingUse = true;
        try {
            // Follow the same block/entity/air interaction path as a real right click.
            // Vanilla skips doUse while mining, so release mining before our soup action.
            mc.interactionManager.stopMiningBlock();
            ((SoupUseAccessor) mc).orven$useSoup();
        } finally {
            issuingUse = false;
            if (config.debugEnabled) mod.clickOrigins().endAction();
        }
    }
    private AutoSoupState.Options options() {
        return new AutoSoupState.Options(config.autoSoupHealthMin, config.autoSoupHealthMax, config.autoSoupMaxPerCycle, config.autoSoupRefill,
                config.autoSoupConsumeMinMs, config.autoSoupConsumeMaxMs, config.autoSoupReturnMinMs, config.autoSoupReturnMaxMs,
                config.autoSoupMoveMinMs, config.autoSoupMoveMaxMs, config.autoSoupResponseTimeoutMs, config.autoSoupCycleCooldownMs);
    }
    private static Kind kind(ItemStack stack) {
        if (stack == null || stack.size <= 0) return Kind.EMPTY;
        if (stack.getItem() instanceof SwordItem) return Kind.SWORD;
        var id = Item.REGISTRY.getKey(stack.getItem());
        return id != null && id.toString().equals("minecraft:mushroom_stew") ? Kind.SOUP
                : id != null && id.toString().equals("minecraft:bowl") ? Kind.BOWL : Kind.OTHER;
    }
    private static boolean cursorEmpty(Minecraft mc) { return kind(mc.player.inventory.getCursorItem()) == Kind.EMPTY; }
    private static void select(Minecraft mc, int slot) {
        if (slot < 0 || slot > 8 || mc.player.inventory.selectedSlot == slot) return;
        mc.player.inventory.selectedSlot = slot;
        ((InteractionManagerAccessor) mc.interactionManager).orven$updateSelectedHotbarSlot();
    }
    private void closeOwned(Minecraft mc) {
        if (ownsScreen(mc) && cursorEmpty(mc)) mc.player.closeMenu();
        // A user-held cursor item makes this a manual inventory session. Never clear/drop it.
        ownedScreen = null;
    }
    @Override public void reset() {
        if (!state.busy() && ownedScreen == null) { state.reset(); return; }
        if (client != null && client.player == player && client.world == world && client.player != null) {
            int soup = state.soupSlot(), original = state.originalSlot();
            if ((state.phase() == AutoSoupState.Phase.USE || state.phase() == AutoSoupState.Phase.RETURN)
                    && client.player.inventory.selectedSlot == soup && client.interactionManager != null) {
                Kind[] kinds = new Kind[9];
                for (int i = 0; i < 9; i++) kinds[i] = kind(client.player.inventory.items[i]);
                if (client.player.hasItemInUse()) client.interactionManager.stopUsingHand(client.player);
                select(client, SoupInventory.swordSlot(kinds, original));
            }
            closeOwned(client);
        } else ownedScreen = null;
        state.abandon(System.nanoTime(), options());
    }
}
