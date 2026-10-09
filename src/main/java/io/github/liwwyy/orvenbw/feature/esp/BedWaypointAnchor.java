package io.github.liwwyy.orvenbw.feature.esp;
/** Six blocks over the base at range, smoothly landing on the supporting block's top nearby. */
public final class BedWaypointAnchor {
    private BedWaypointAnchor() {}
    public static double height(double distance) {
        double t=Math.clamp((distance-6)/42,0,1);
        return 6*t*t*(3-2*t);
    }
}
