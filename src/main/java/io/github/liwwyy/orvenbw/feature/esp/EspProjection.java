package io.github.liwwyy.orvenbw.feature.esp;

/** OpenGL column-major projection and stable screen-edge placement, without a GL dependency. */
public final class EspProjection {
    private EspProjection() {}
    public record Point(double x, double y, double depth, boolean front) {}
    public static Point project(float[] view, float[] projection, double x, double y, double z, int width, int height) {
        double[] point = multiply(view, new double[]{x,y,z,1});
        point = multiply(projection, point);
        if (Math.abs(point[3]) < 1e-8) return null;
        double divisor = Math.abs(point[3]);
        return new Point(width * (.5 + point[0] / divisor * .5), height * (.5 - point[1] / divisor * .5), point[2] / point[3], point[3] > 0);
    }
    private static double[] multiply(float[] matrix, double[] vector) {
        double[] result = new double[4];
        for (int row = 0; row < 4; row++) for (int column = 0; column < 4; column++) result[row] += matrix[column * 4 + row] * vector[column];
        return result;
    }
    public static Point edge(Point point, int width, int height, double margin) {
        double dx = point.x - width / 2.0, dy = point.y - height / 2.0;
        double maxX = Math.max(1, width / 2.0 - margin), maxY = Math.max(1, height / 2.0 - margin);
        double amount = Math.max(Math.abs(dx) / maxX, Math.abs(dy) / maxY);
        if (amount < 1 && point.front) return point;
        if (amount < 1e-8) { dx = 0; dy = maxY; amount = 1; }
        // Behind-camera markers must stay on the edge even if their projected coordinate lies inside.
        return new Point(width / 2.0 + dx / amount, height / 2.0 + dy / amount, point.depth, point.front);
    }
}
