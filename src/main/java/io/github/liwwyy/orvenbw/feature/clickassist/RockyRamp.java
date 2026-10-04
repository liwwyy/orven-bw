package io.github.liwwyy.orvenbw.feature.clickassist;

import java.util.function.DoubleSupplier;

/** Session-specific burst/plateau knots with eased rises, retaining the configured finish time. */
public final class RockyRamp {
    private final double[] times = new double[6], rates = new double[6];
    public RockyRamp(DoubleSupplier random, double high, double floor) {
        times[0] = 0; times[1] = .12 + unit(random) * .11;
        times[2] = .30 + unit(random) * .14; times[3] = .50 + unit(random) * .13;
        times[4] = .72 + unit(random) * .12; times[5] = 1;
        rates[0] = Math.min(high, Math.max(floor, 3.1 + unit(random) * 1.5));
        rates[1] = Math.min(high, rates[0] + .15 + unit(random) * .5);
        rates[2] = Math.min(high, Math.max(rates[1], 6.3 + unit(random) * 3.4));
        rates[3] = Math.min(high, Math.max(rates[2], Math.min(9.7, rates[2] + .1 + unit(random) * .5)));
        rates[4] = Math.max(rates[3], high - .6 - unit(random) * .9);
        rates[5] = high;
    }
    public double target(double progress) {
        double t = Math.clamp(progress, 0, 1);
        for (int i = 1; i < times.length; i++) {
            if (t <= times[i]) {
                double blend = (t - times[i - 1]) / (times[i] - times[i - 1]);
                blend = blend * blend * (3 - 2 * blend);
                return rates[i - 1] + (rates[i] - rates[i - 1]) * blend;
            }
        }
        return rates[5];
    }
    private static double unit(DoubleSupplier random) { return Math.clamp(random.getAsDouble(), 0, Math.nextDown(1.0)); }
}
