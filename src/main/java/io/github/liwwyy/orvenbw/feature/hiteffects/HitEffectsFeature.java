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
import java.util.ArrayDeque;
import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;

/** Reticle-relative health and floating combo text, inspired by the supplied hit_show fragment. */
public final class HitEffectsFeature implements ClientFeature {
    private final OrvenConfig config;
    private final HitTracker tracker = new HitTracker();
    private final ArrayDeque<HitPopup> popups = new ArrayDeque<>();
    private LivingEntity target;
    public HitEffectsFeature(OrvenConfig config) { this.config = config; }
    private boolean eligible(Minecraft mc) {
        return config.hitEffectsEnabled && ScoreboardGate.allows(mc, config) && mc.player != null && mc.world != null
                && mc.screen == null && mc.focused && !mc.isPaused() && mc.player.isAlive() && !mc.player.isSpectator();
    }
    public void attack(Minecraft mc, PlayerEntity player, Entity entity) {
        if (!eligible(mc) || player != mc.player || !(entity instanceof LivingEntity living) || living == player) return;
        if (target != living) popups.clear();
        target = living;
        boolean critical = player.fallDistance > 0 && !player.onGround && !player.isClimbing() && !player.isInWater()
                && !player.hasStatusEffect(StatusEffect.BLINDNESS) && player.vehicle == null;
        long previousHit = tracker.lastHit();
        tracker.attack(living, living.getHealth(), critical, System.nanoTime(), config.hitComboResetMs * 1_000_000L);
        if (tracker.lastHit() != 0 && tracker.lastHit() != previousHit) addPopup();
    }
    @Override public void beforeInteractions(Minecraft mc) {
        if (!eligible(mc)) { reset(); return; }
        if (target != null && tracker.observe(target.getHealth(), System.nanoTime(), config.hitComboResetMs * 1_000_000L)) addPopup();
    }
    private void addPopup() {
        var random = ThreadLocalRandom.current();
        if (popups.size() >= 12) popups.removeFirst();
        popups.addLast(new HitPopup(tracker.lastHit(), tracker.lastCombo(), tracker.damage(), tracker.critical(),
                tracker.criticalStreak(), random.nextBoolean() ? 1 : -1, random.nextDouble(0, Math.PI * 2)));
    }
    public void render(Minecraft mc) {
        if (!eligible(mc) || mc.options.hideGui) return;
        long now = System.nanoTime();
        LivingEntity shown = mc.crosshairTarget != null && mc.crosshairTarget.entity instanceof LivingEntity living ? living : null;
        long duration = Math.clamp(config.hitEffectDurationMs, 300, 3000) * 1_000_000L;
        while (!popups.isEmpty() && now - popups.peekFirst().at() >= duration) popups.removeFirst();
        if (!popups.isEmpty()) shown = target;
        if (shown == null || shown == mc.player || config.hitEffectsIgnoreNpcs && !listedPlayer(mc, shown)) return;
        Window window = new Window(mc);
        float x = window.getWidth() / 2f, y = window.getHeight() / 2f;
        if (config.hitEffectsShowHealth) {
            String health = "§c❤ " + number(shown.getHealth()) + "/" + number(shown.getMaxHealth());
            mc.textRenderer.drawWithShadow(health, x - mc.textRenderer.getWidth(health) / 2f,
                    y + config.hitEffectOffsetY, 0xFFFF5555);
        }
        for (HitPopup popup : popups) {
            int alpha = popup.alpha(now, duration);
            if (alpha < 8) continue; // Vanilla interprets near-zero alpha as fully opaque.
            String hit = popup.text();
            mc.textRenderer.drawWithShadow(hit, x - mc.textRenderer.getWidth(hit) / 2f + (float) popup.x(now, duration),
                    y + (float) popup.y(now, duration), (alpha << 24) | popup.color());
        }
    }
    private static boolean listedPlayer(Minecraft mc, LivingEntity entity) {
        if (!(entity instanceof PlayerEntity) || mc.getNetworkHandler() == null) return false;
        return mc.getNetworkHandler().getOnlinePlayers().stream()
                .anyMatch(info -> entity.getName().equalsIgnoreCase(info.getProfile().getName()));
    }
    private static String number(double value) { return String.format(Locale.ROOT, "%.1f", value).replaceAll("\\.0$", ""); }
    @Override public void reset() { target = null; tracker.reset(); popups.clear(); }
}
