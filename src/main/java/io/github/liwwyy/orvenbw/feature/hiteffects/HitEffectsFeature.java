package io.github.liwwyy.orvenbw.feature.hiteffects;

import io.github.liwwyy.orvenbw.config.OrvenConfig;
import io.github.liwwyy.orvenbw.feature.ClientFeature;
import io.github.liwwyy.orvenbw.feature.ScoreboardGate;
import net.minecraft.client.Minecraft;
import net.minecraft.client.render.Window;
import net.minecraft.entity.Entity;
import net.minecraft.entity.living.LivingEntity;
import net.minecraft.entity.living.player.PlayerEntity;
import net.minecraft.entity.living.effect.StatusEffect;
import java.util.Locale;

/** Reticle-relative health and floating combo text, inspired by the supplied hit_show fragment. */
public final class HitEffectsFeature implements ClientFeature {
    private final OrvenConfig config;
    private final HitTracker tracker = new HitTracker();
    private LivingEntity target;
    public HitEffectsFeature(OrvenConfig config) { this.config = config; }
    private boolean eligible(Minecraft mc) {
        return config.hitEffectsEnabled && ScoreboardGate.allows(mc, config) && mc.player != null && mc.world != null
                && mc.screen == null && mc.focused && !mc.isPaused() && mc.player.isAlive() && !mc.player.isSpectator();
    }
    public void attack(Minecraft mc, PlayerEntity player, Entity entity) {
        if (!eligible(mc) || player != mc.player || !(entity instanceof LivingEntity living) || living == player) return;
        target = living;
        boolean critical = player.fallDistance > 0 && !player.onGround && !player.isClimbing() && !player.isInWater()
                && !player.hasStatusEffect(StatusEffect.BLINDNESS) && player.vehicle == null;
        tracker.attack(living, living.getHealth(), critical, System.nanoTime(), config.hitComboResetMs * 1_000_000L);
    }
    @Override public void beforeInteractions(Minecraft mc) {
        if (!eligible(mc)) { reset(); return; }
        if (target != null) tracker.observe(target.getHealth(), System.nanoTime(), config.hitComboResetMs * 1_000_000L);
    }
    public void render(Minecraft mc) {
        if (!eligible(mc) || mc.options.hideGui) return;
        long now = System.nanoTime();
        LivingEntity shown = mc.crosshairTarget != null && mc.crosshairTarget.entity instanceof LivingEntity living ? living : null;
        long duration = Math.clamp(config.hitEffectDurationMs, 300, 3000) * 1_000_000L;
        boolean animated = tracker.lastCombo() > 0 && now - tracker.lastHit() < duration;
        if (animated) shown = target;
        if (shown == null || shown == mc.player) return;
        Window window = new Window(mc);
        float x = window.getWidth() / 2f, y = window.getHeight() / 2f + config.hitEffectOffsetY;
        String health = "Health " + number(shown.getHealth()) + " / " + number(shown.getMaxHealth());
        mc.textRenderer.drawWithShadow(health, x - mc.textRenderer.getWidth(health) / 2f, y, 0xFFFFFFFF);
        if (animated) {
            double progress = Math.clamp((now - tracker.lastHit()) / (double) duration, 0, 1);
            int alpha = (int) (255 * Math.min(1, (1 - progress) * 4));
            if (alpha < 8) return; // Vanilla interprets a near-zero alpha as fully opaque.
            String hit = tracker.lastCombo() + (tracker.lastCombo() == 1 ? " hit" : " hits") + "  -" + number(tracker.damage())
                    + (tracker.critical() ? "  CRIT" : "");
            float slide = (float) (Math.pow(1 - Math.min(1, progress * 5), 3) * 20);
            float rise = (float) (progress * 8);
            mc.textRenderer.drawWithShadow(hit, x - mc.textRenderer.getWidth(hit) / 2f + slide,
                    y + 13 - rise, (alpha << 24) | tracker.color());
        }
    }
    private static String number(double value) { return String.format(Locale.ROOT, "%.1f", value).replaceAll("\\.0$", ""); }
    @Override public void reset() { target = null; tracker.reset(); }
}
