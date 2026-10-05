package io.github.liwwyy.orvenbw.feature.hiteffects;

import java.util.Locale;

/** A confirmed hit; damage may arrive after the initial hurt notification. */
record HitPopup(long at, int combo, double damage, boolean critical, int criticalStreak,
                int direction, double phase) {
    String text() {
        String label = critical ? " CRIT" + (criticalStreak > 1 ? " x" + criticalStreak : "")
                : damage < 1.0 ? " WEAK" : "";
        return combo + (combo == 1 ? " hit" : " hits") + (Double.isFinite(damage) ? " -" + number(damage) : "") + label;
    }

    int color() {
        if (critical) return 0xFFD166;
        if (damage < 1.0) return 0xB8B8B8;
        return combo >= 5 ? 0xC792EA : combo >= 3 ? 0xFFAD66 : 0x7EE0A1;
    }

    double x(long now, long duration) {
        double progress = progress(now, duration);
        double flight = Math.min(1, progress / .4);
        double shake = progress >= .28 && progress <= .85
                ? Math.sin(progress * 65 + phase) * 1.7 * Math.sin(Math.PI * (progress - .28) / .57) : 0;
        return direction * (36 * (1 - Math.pow(1 - flight, 3)) + shake);
    }

    double y(long now, long duration) {
        double progress = progress(now, duration);
        double shake = progress >= .28 && progress <= .85
                ? Math.sin(progress * 79 + phase) * 1.2 * Math.sin(Math.PI * (progress - .28) / .57) : 0;
        return -17 * Math.min(1, progress / .8) + shake;
    }

    int alpha(long now, long duration) {
        double progress = progress(now, duration);
        if (progress < .12) return (int) Math.round(96 + 159 * progress / .12);
        return progress < .4 ? 255 : (int) Math.round(255 * (1 - progress) / .6);
    }

    double scale(long now, long duration) {
        double p = progress(now, duration);
        if (p < .18) return .72 + .5 * (1 - Math.pow(1 - p / .18, 3));
        return p < .4 ? 1.22 - .22 * (p - .18) / .22 : 1;
    }
    HitPopup withDamage(double amount) {
        return new HitPopup(at, combo, amount, critical, criticalStreak, direction, phase);
    }

    private double progress(long now, long duration) {
        return Math.clamp((now - at) / (double) duration, 0, 1);
    }

    private static String number(double value) {
        return String.format(Locale.ROOT, "%.1f", value).replaceAll("\\.0$", "");
    }
}
