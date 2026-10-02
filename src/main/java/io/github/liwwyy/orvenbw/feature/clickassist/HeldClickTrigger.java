package io.github.liwwyy.orvenbw.feature.clickassist;

/** A continuous eligible physical hold; generated binding state never enters this timer. */
public final class HeldClickTrigger {
    public record Conditions(boolean available, boolean entity, boolean weapon, boolean nearby) {
        public boolean allows(boolean requireEntity, boolean requireWeapon, boolean requireNearby) {
            return available && (!requireEntity || entity) && (!requireWeapon || weapon) && (!requireNearby || nearby);
        }
        public boolean instantReady() { return available && entity && weapon && nearby; }
    }
    private long started = -1;
    public boolean active(long now, boolean physicalDown, boolean eligible, long delayNanos, boolean instant) {
        if (!physicalDown || !eligible) { reset(); return false; }
        if (started < 0) started = now;
        return instant || now - started >= Math.max(0, delayNanos);
    }
    public void reset() { started = -1; }

    /** One scheduler per side. Explicit keybind spam takes priority even when its filters fail. */
    public static int source(boolean buttonHold, boolean explicitSpam, boolean heldSpam, boolean assist) {
        return buttonHold ? -1 : explicitSpam ? 1 : heldSpam ? 2 : assist ? 0 : -1;
    }
}
