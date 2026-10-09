package io.github.liwwyy.orvenbw.feature.autoblock;
/** Raven's eye-to-nearest-hitbox range, independent of Minecraft/OpenGL. */
public final class CombatGeometry {
    private CombatGeometry() {}
    public static double distanceSquared(double x,double y,double z,double minX,double minY,double minZ,double maxX,double maxY,double maxZ) {
        return Math.pow(x-Math.clamp(x,minX,maxX),2)+Math.pow(y-Math.clamp(y,minY,maxY),2)+Math.pow(z-Math.clamp(z,minZ,maxZ),2);
    }
}
