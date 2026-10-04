package io.github.liwwyy.orvenbw.feature.clickassist;

import com.google.gson.Gson;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.function.DoubleSupplier;

/** Aggregate-fitted tempo states and correlated interval categories. No recorded clicks are replayed. */
public final class ClickProfileSession {
    private static final double SECOND = 1_000_000_000.0;
    public record Options(int profile, boolean separateSides, double ceiling) {
        public Options {
            profile = profile == 1 ? 1 : 0;
            ceiling = Double.isFinite(ceiling) ? Math.clamp(ceiling, 1, 22) : 22;
        }
        public String name() { return profile == 1 ? "Performative" : "Humble"; }
    }
    private static final class State {
        double rate;
        double[] durations, transitions, categories;
        double[][] category_transitions, intervals;
    }
    private static final class Side {
        double median_cps;
        double[] initial, startup_weights;
        double[][] startup_rates;
        double[][][] startup_ratios;
        State[] states;
        boolean rare_peak;
    }
    private static final class Model {
        int schema;
        double[] probabilities;
        Map<String, Side> sides;
    }
    private static final Model MODEL = load();
    private static Model load() {
        try (var input = ClickProfileSession.class.getResourceAsStream("/assets/orvenbw/click-profile-model.json")) {
            if (input == null) throw new IllegalStateException("Click profile model is missing");
            Model model = new Gson().fromJson(new InputStreamReader(input, StandardCharsets.UTF_8), Model.class);
            if (model.schema != 1 || model.sides.size() != 2) throw new IllegalStateException("Unsupported click profile model");
            return model;
        } catch (java.io.IOException exception) { throw new ExceptionInInitializerError(exception); }
    }
    private final DoubleSupplier random;
    private final int button;
    private boolean active;
    private long started;
    private Options options;
    private Side model;
    private int state, category = -1;
    private double until, startRate, multiplier, peakAt, peakUntil, peakRate, transitionAt, transitionFrom;
    private final double[] startup = new double[7];
    public ClickProfileSession(DoubleSupplier random, int button) {
        if (button < 0 || button > 1) throw new IllegalArgumentException("Invalid click button");
        this.random = random; this.button = button;
    }
    public double target(long now, boolean eligible, Options requested) {
        if (!eligible) { reset(); return 0; }
        if (!active || !requested.equals(options)) start(now, requested);
        double elapsed = Math.max(0, (now - started) / SECOND);
        int skipped = 0;
        while (elapsed >= until) {
            transitionAt = until; transitionFrom = model.states[state].rate;
            state = choose(model.states[state].transitions);
            until += Math.max(.1, draw(model.states[state].durations));
            if (++skipped >= 256) { until = elapsed + draw(model.states[state].durations); break; }
        }
        double blend = Math.clamp((elapsed - transitionAt) / .1, 0, 1);
        double rate = transitionFrom + (model.states[state].rate - transitionFrom) * blend;
        if (elapsed < 1.5) {
            int index = Math.min(5, (int) (elapsed * 4));
            double fraction = elapsed * 4 - index;
            double endpoint = index == 5 ? rate : startRate * startup[index + 1];
            rate = startRate * startup[index] * (1 - fraction) + endpoint * fraction;
        }
        if (options.profile() == 1) rate = model.median_cps + .35 * (rate - model.median_cps);
        if (model.rare_peak && options.profile() == 0) {
            if (elapsed >= peakAt) {
                peakUntil = elapsed + .2 + unit() * .2;
                peakRate = 21 + unit();
                peakAt = peakUntil + 30 + exponential(90);
            }
            if (elapsed < peakUntil) rate = peakRate;
        }
        return Math.max(1, Math.min(rate, options.ceiling()) * multiplier);
    }
    private void start(long now, Options requested) {
        active = true; started = now; options = requested;
        model = MODEL.sides.get(button == 1 && options.separateSides() ? "right" : "left");
        multiplier = button == 1 && !options.separateSides() ? .9 : 1;
        state = choose(model.initial); category = -1;
        transitionAt = 0; transitionFrom = model.states[state].rate;
        until = draw(model.states[state].durations);
        int mode = choose(model.startup_weights);
        for (int i = 0; i < 6; i++) startup[i] = Math.clamp(draw(model.startup_ratios[mode][i]), .25, 2);
        startup[6] = 1;
        startRate = draw(model.startup_rates[mode]);
        peakAt = 30 + exponential(90); peakUntil = 0;
    }
    /** Dimensionless interval; dividing by generated CPS supplies only the missing physical rate. */
    public double intervalWeight() {
        if (!active) return 1;
        State current = model.states[state];
        category = choose(category < 0 ? current.categories : current.category_transitions[category]);
        return Math.max(.001, draw(current.intervals[category]) * current.rate);
    }
    public void reset() { active = false; category = -1; peakUntil = 0; }
    private double exponential(double mean) { return -mean * Math.log(Math.max(1e-12, 1 - unit())); }
    private double unit() {
        double value = random.getAsDouble();
        return Double.isFinite(value) ? Math.clamp(value, 0, Math.nextDown(1.0)) : .5;
    }
    private int choose(double[] weights) {
        double total = 0;
        for (double value : weights) total += value;
        double chosen = unit() * total;
        for (int i = 0; i < weights.length; i++) {
            chosen -= weights[i];
            if (weights[i] > 0 && chosen <= 0) return i;
        }
        return weights.length - 1;
    }
    private double draw(double[] values) {
        double u = unit();
        int i = 0;
        while (i < MODEL.probabilities.length - 2 && u > MODEL.probabilities[i + 1]) i++;
        double f = (u - MODEL.probabilities[i]) / (MODEL.probabilities[i + 1] - MODEL.probabilities[i]);
        return values[i] + f * (values[i + 1] - values[i]);
    }
}
