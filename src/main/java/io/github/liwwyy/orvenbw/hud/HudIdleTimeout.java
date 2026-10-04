package io.github.liwwyy.orvenbw.hud;

/** Idle duration starts at the last observed click, independent of the one-second CPS window. */
public final class HudIdleTimeout {
    private HudIdleTimeout() {}
    public static boolean visible(boolean hide, long now, long lastClick, int timeoutMs) {
        return !hide || lastClick != Long.MIN_VALUE && now - lastClick < Math.max(0L, timeoutMs) * 1_000_000L;
    }
}
