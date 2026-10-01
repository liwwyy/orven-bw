package io.github.liwwyy.orvenbw.feature.clickassist;

import io.github.liwwyy.orvenbw.config.OrvenConfig;
import io.github.liwwyy.orvenbw.feature.ClientFeature;
import net.minecraft.client.Minecraft;
import net.minecraft.client.options.KeyBinding;
import net.minecraft.entity.living.player.PlayerEntity;
import net.minecraft.item.*;
import net.minecraft.world.HitResult;
import java.util.concurrent.ThreadLocalRandom;

public final class ClickAssistFeature implements ClientFeature {
    private final OrvenConfig config;
    private final ClickAssistEngine engine = new ClickAssistEngine(() -> ThreadLocalRandom.current().nextDouble());
    private int attackCode = Integer.MIN_VALUE;
    private int useCode = Integer.MIN_VALUE;

    public ClickAssistFeature(OrvenConfig config) { this.config = config; }

    @Override public void onInput(Minecraft mc, int keyCode) {
        syncBindings(mc);
        long now = System.nanoTime();
        if (keyCode == attackCode) engine.physicalClick(0, now, options(0), eligible(mc, 0));
        if (keyCode == useCode) engine.physicalClick(1, now, options(1), eligible(mc, 1));
    }

    @Override public void beforeInteractions(Minecraft mc) {
        syncBindings(mc);
        long now = System.nanoTime();
        for (int button = 0; button <= 1; button++) {
            if (engine.pollBoost(button, now, options(button), eligible(mc, button))) {
                // Queue through vanilla's click path. Never writes hardware mouse or held-key state.
                KeyBinding.click(button == 0 ? attackCode : useCode);
            }
        }
    }

    private void syncBindings(Minecraft mc) {
        int attack = mc.options.attackKey.getKeyCode();
        int use = mc.options.useKey.getKeyCode();
        if (attack != attackCode || use != useCode) {
            engine.reset();
            attackCode = attack;
            useCode = use;
        }
    }

    private ClickAssistEngine.Options options(int button) {
        return new ClickAssistEngine.Options(config.activationCps, config.totalCpsCap,
                button == 0 ? config.leftChance : config.rightChance,
                config.boostDelayMs * 1_000_000L, config.randomizeTiming);
    }

    private boolean eligible(Minecraft mc, int button) {
        if (!config.enabled || mc.player == null || mc.world == null || mc.screen != null || !mc.focused
                || !mc.player.isAlive() || mc.player.isSpectator()) return false;
        if (config.disableInCreative && mc.player.abilities.creativeMode) return false;
        // Vanilla discards all queued clicks while an item is in use. Do not count discarded boosts.
        if (mc.player.hasItemInUse()) return false;
        if (config.requiresPlayer && !nearbyPlayer(mc)) return false;
        ItemStack stack = mc.player.getItemInHand();
        Item held = stack == null ? null : stack.getItem();
        if (button == 0) {
            if (!config.leftClick) return false;
            if (config.weaponOnly && !allowedWeapon(held)) return false;
            if (config.onlyWhileTargeting && (mc.crosshairTarget == null || mc.crosshairTarget.entity == null)) return false;
            if (config.preserveMining && mc.crosshairTarget != null && mc.crosshairTarget.type == HitResult.Type.BLOCK) return false;
            return true;
        }
        return config.rightClick && (!config.blocksOnly || held instanceof BlockItem);
    }

    private boolean allowedWeapon(Item item) {
        return config.swords && item instanceof SwordItem
                || config.axes && item instanceof AxeItem
                || config.rods && item instanceof FishingRodItem
                || config.sticks && item == Items.STICK
                || config.hoes && item instanceof HoeItem
                || config.shovels && item instanceof ShovelItem;
    }

    private boolean nearbyPlayer(Minecraft mc) {
        for (PlayerEntity other : mc.world.players) {
            if (other == mc.player || !other.isAlive() || other.isSpectator()) continue;
            double dx = mc.player.x - other.x;
            double dy = mc.player.y - other.y;
            double dz = mc.player.z - other.z;
            if (dx * dx + dy * dy + dz * dz <= 16.0) return true;
        }
        return false;
    }

    public ClickAssistEngine.Cps cps(int button) { return engine.cps(button, System.nanoTime()); }
    @Override public void reset() { engine.reset(); }
}
