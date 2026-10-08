package io.github.liwwyy.orvenbw.feature.esp;

import java.util.*;

/** Pure coordinates: one canonical foot, one cardinal direction toward the head. */
public record BedGeometry(Pos foot, int dx, int dz) {
    public BedGeometry {
        Objects.requireNonNull(foot);
        if (Math.abs(dx) + Math.abs(dz) != 1) throw new IllegalArgumentException("Bed direction must be cardinal");
    }
    public record Pos(int x, int y, int z) {
        public Pos add(int x, int y, int z) { return new Pos(this.x + x, this.y + y, this.z + z); }
        public long chunkKey() { return ((long)(x >> 4) << 32) | ((z >> 4) & 0xffffffffL); }
    }
    public Pos head() { return foot.add(dx, 0, dz); }
    public boolean contains(Pos pos) { return foot.equals(pos) || head().equals(pos); }
    public double x() { return foot.x + .5 + dx * .5; }
    public double y() { return foot.y; } // Top face of the foundation block at bedY - 1.
    public double z() { return foot.z + .5 + dz * .5; }
    public double distanceSquared(double x, double y, double z) {
        return Math.pow(x - x(), 2) + Math.pow(y - y(), 2) + Math.pow(z - z(), 2);
    }
    public List<Pos> defence() {
        Pos head = head();
        return List.of(foot.add(-dx, 0, -dz), head.add(dx, 0, dz),
                foot.add(-dz, 0, dx), foot.add(dz, 0, -dx),
                head.add(-dz, 0, dx), head.add(dz, 0, -dx), foot.add(0, 1, 0), head.add(0, 1, 0));
    }
}
