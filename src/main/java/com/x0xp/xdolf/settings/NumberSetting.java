package com.x0xp.xdolf;

import java.util.Locale;

public final class NumberSetting extends ModuleSetting<Double> {
    public final double min, max, step;
    private double value;

    NumberSetting(String name, String label, String description, double value, double min, double max, double step) {
        super(name, label, description);
        if (!Double.isFinite(min) || !Double.isFinite(max) || !Double.isFinite(step) || min > max || step <= 0)
            throw new IllegalArgumentException("Invalid numeric setting range: " + name);
        this.min = min;
        this.max = max;
        this.step = step;
        set(value);
    }

    @Override
    public Kind kind() { return Kind.NUMBER; }

    @Override
    public Double value() { return value; }

    public double get() { return value; }

    public void set(double value) {
        if (!Double.isFinite(value)) throw new IllegalArgumentException(name + " must be finite");
        this.value = Math.max(min, Math.min(max, value));
    }

    public void increment(int direction) {
        set(Math.round((value + direction * step) * 10000) / 10000d);
    }

    public boolean integer() {
        return step >= 1.0 && Math.rint(step) == step && Math.rint(min) == min && Math.rint(max) == max;
    }

    @Override
    public String serialize() { return Double.toString(value); }

    @Override
    public String display() {
        return String.format(Locale.ROOT, "%.2f", value).replaceAll("0+$", "").replaceAll("\\.$", "");
    }

    @Override
    public void parse(String raw) { set(Double.parseDouble(raw.trim())); }
}
