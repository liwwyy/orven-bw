package io.github.liwwyy.orvenbw.feature.autotool;

import io.github.liwwyy.orvenbw.config.OrvenConfig;
import io.github.liwwyy.orvenbw.feature.ClientFeature;
import io.github.liwwyy.orvenbw.feature.ScoreboardGate;
import io.github.liwwyy.orvenbw.mixin.InteractionManagerAccessor;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.client.Minecraft;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.resource.Identifier;
import net.minecraft.world.HitResult;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import java.util.concurrent.ThreadLocalRandom;

/** Normal visible hotbar switches before vanilla mining; never spoofs the held item. */
public final class AutoToolFeature implements ClientFeature {
    private final OrvenConfig config;
    private final AutoToolState state = new AutoToolState();
    private Minecraft client;
    private Object world, player;
    public AutoToolFeature(OrvenConfig config) { this.config = config; }
    private boolean eligible(Minecraft mc) {
        return config.autoToolEnabled && soupInactive() && ScoreboardGate.allows(mc, config) && mc.player != null && mc.world != null
                && mc.interactionManager != null && mc.screen == null && mc.focused && !mc.isPaused()
                && mc.player.isAlive() && !mc.player.isSpectator() && mc.player.abilities.canModifyWorld;
    }
    private static boolean soupInactive() {
        var mod = io.github.liwwyy.orvenbw.OrvenBw.instance();
        return mod == null || !mod.autoSoup().busy();
    }
    @Override public void beforeInteractions(Minecraft mc) {
        if (world != mc.world || player != mc.player) { state.reset(); world = mc.world; player = mc.player; }
        client = mc;
        if (!eligible(mc) || config.autoToolOnlyCrouching && !mc.player.isSneaking()
                || down(mc.options.useKey.getKeyCode()) || mc.player.hasItemInUse()) { reset(); return; }
        boolean leftDown = Mouse.isCreated() && Mouse.isButtonDown(0);
        if (config.autoToolRequireLeftMouse && !leftDown) { reset(); return; }
        HitResult hit = mc.crosshairTarget;
        if (hit == null || hit.type != HitResult.Type.BLOCK) { reset(); return; }
        Block block = mc.world.getBlockState(hit.getPos()).getBlock();
        if (block == Blocks.AIR || config.autoToolBlacklistEnabled && BlockFamilies.matches(block, config.autoToolBlacklist)
                || config.autoToolWhitelistEnabled && !BlockFamilies.matches(block, config.autoToolWhitelist)
                || config.autoToolIgnoreHeldItems && ignored(mc.player.getItemInHand())) { reset(); return; }
        int current = mc.player.inventory.selectedSlot;
        // Respect slot changes made outside our vanilla input hooks; never restore over another mod.
        if (state.ownsSlot() && current != state.ownedSlot()) state.reset();
        int best = bestSlot(mc.player.inventory.items, block, current);
        if (best < 0) { reset(); return; }
        int next = state.update(System.nanoTime(), hit.getPos(), best, current, leftDown,
                config.autoToolRequireLeftMouse, config.autoToolSwitchDelayMs, config.autoToolVariationMs,
                config.autoToolHoverDelayMs, () -> ThreadLocalRandom.current().nextDouble());
        if (next >= 0) { state.switched(current, next); select(mc, next); }
    }
    /** Raven-style interception: keep the tool and optionally change the eventual restore slot. */
    public boolean requestSlot(int slot) {
        Minecraft mc = Minecraft.getInstance();
        if (!state.ownsSlot() || !eligible(mc) || mc.player != player || mc.world != world) return false;
        state.requestRestoreSlot(slot, config.autoToolOverrideSwitchBack); return true;
    }
    private boolean ignored(ItemStack stack) {
        if (stack == null || config.autoToolIgnoredItems == null) return false;
        for (String id : config.autoToolIgnoredItems) {
            if (id == null || id.isBlank()) continue;
            try { if (Item.REGISTRY.get(new Identifier(id)) == stack.getItem()) return true; }
            catch (IllegalArgumentException ignored) { }
        }
        return false;
    }
    static int bestSlot(ItemStack[] inventory, Block block, int current) {
        int best = -1; double score = 1;
        if (current >= 0 && current < Math.min(9, inventory.length)) {
            double value = score(inventory[current], block);
            if (value > score) { best = current; score = value; }
        }
        for (int slot = 0; slot < Math.min(9, inventory.length); slot++) {
            double value = score(inventory[slot], block);
            if (value > score + 0.0000001) { score = value; best = slot; }
        }
        return best;
    }
    static double score(ItemStack stack, Block block) {
        if (stack == null) return 0;
        double speed = stack.getMiningSpeed(block);
        if (speed <= 1) return 0;
        int efficiency = EnchantmentHelper.getLevel(Enchantment.EFFICIENCY.id, stack);
        if (efficiency > 0) speed += efficiency * efficiency + 1;
        if (!block.getMaterial().isToolNotRequired() && !stack.canMineBlock(block)) speed *= .3;
        return speed + (stack.isDamageable() && stack.getMaxDamage() > 0
                ? Math.clamp((stack.getMaxDamage() - stack.getDamage()) / (double) stack.getMaxDamage(), 0, 1) / 1000 : 0);
    }
    private static boolean down(int code) {
        return code < 0 ? Mouse.isCreated() && code + 100 >= 0 && code + 100 < Mouse.getButtonCount() && Mouse.isButtonDown(code + 100)
                : Keyboard.isCreated() && code > 0 && code < Keyboard.KEYBOARD_SIZE && Keyboard.isKeyDown(code);
    }
    private static void select(Minecraft mc, int slot) {
        if (slot < 0 || slot > 8 || slot == mc.player.inventory.selectedSlot) return;
        mc.player.inventory.selectedSlot = slot;
        ((InteractionManagerAccessor) mc.interactionManager).orven$updateSelectedHotbarSlot();
    }
    @Override public void reset() {
        boolean sameContext = client != null && client.player != null && client.player == player && client.world == world;
        int slot = state.finish(sameContext ? client.player.inventory.selectedSlot : -1, sameContext && config.autoToolSwitchBack);
        if (slot >= 0) select(client, slot);
    }
}
