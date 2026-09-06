package com.darkcart.xdolf;

import java.util.Locale;

public final class ModuleSetting {
    public final String name;
    public final double min, max, step;
    private double value;

    public ModuleSetting(String name, double value, double min, double max, double step) {
        this.name = name;
        this.min = min;
        this.max = max;
        this.step = step;
        set(value);
    }

    public double get() { return value; }
    public boolean on() { return value != 0; }
    public void set(double value) {
        if (!Double.isFinite(value) || value < min || value > max)
            throw new IllegalArgumentException(name + " must be between " + min + " and " + max);
        this.value = value;
    }
    public void increment(int direction) {
        set(Math.max(min, Math.min(max, Math.round((value + direction * step) * 10000) / 10000d)));
    }
    public String display() {
        if (min == 0 && max == 1 && step == 1) return on() ? "ON" : "OFF";
        return String.format(Locale.ROOT, "%.2f", value).replaceAll("0+$", "").replaceAll("\\.$", "");
    }
}
