package com.x0xp.xdolf;

import java.util.List;
import java.util.Locale;

/** A typed, self-validating value that can be persisted and rendered without module-specific GUI code. */
public abstract class ModuleSetting<T> {
    public enum Kind { BOOLEAN, NUMBER, TEXT, CHOICE }

    public final String name;
    public final String label;
    public final String description;

    ModuleSetting(String name, String label, String description) {
        this.name = name;
        this.label = label == null || label.isBlank() ? name : label;
        this.description = description == null ? "" : description;
    }

    public abstract Kind kind();
    public abstract T value();
    public abstract String serialize();
    public abstract String display();
    public abstract void parse(String raw);
}

final class NumberSetting extends ModuleSetting<Double> {
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

    public Kind kind() { return Kind.NUMBER; }
    public Double value() { return value; }
    public double get() { return value; }
    public void set(double value) {
        if (!Double.isFinite(value)) throw new IllegalArgumentException(name + " must be finite");
        this.value = Math.max(min, Math.min(max, value));
    }
    public void increment(int direction) { set(Math.round((value + direction * step) * 10000) / 10000d); }
    public boolean integer() {
        return step >= 1.0 && Math.rint(step) == step && Math.rint(min) == min && Math.rint(max) == max;
    }
    public String serialize() { return Double.toString(value); }
    public String display() {
        return String.format(Locale.ROOT, "%.2f", value).replaceAll("0+$", "").replaceAll("\\.$", "");
    }
    public void parse(String raw) { set(Double.parseDouble(raw.trim())); }
}

final class BooleanSetting extends ModuleSetting<Boolean> {
    private boolean value;

    BooleanSetting(String name, String label, String description, boolean value) {
        super(name, label, description);
        this.value = value;
    }

    public Kind kind() { return Kind.BOOLEAN; }
    public Boolean value() { return value; }
    public boolean on() { return value; }
    public void set(boolean value) { this.value = value; }
    public void toggle() { value = !value; }
    public String serialize() { return Boolean.toString(value); }
    public String display() { return value ? "ON" : "OFF"; }
    public void parse(String raw) {
        String value = raw.trim().toLowerCase(Locale.ROOT);
        if (value.equals("true") || value.equals("on") || value.equals("1") || value.equals("1.0")) set(true);
        else if (value.equals("false") || value.equals("off") || value.equals("0") || value.equals("0.0")) set(false);
        else throw new IllegalArgumentException("Use ON or OFF");
    }
}

final class TextSetting extends ModuleSetting<String> {
    public final int maxLength;
    private String value;

    TextSetting(String name, String label, String description, String value, int maxLength) {
        super(name, label, description);
        if (maxLength < 1) throw new IllegalArgumentException("Text limit must be positive");
        this.maxLength = maxLength;
        set(value);
    }

    public Kind kind() { return Kind.TEXT; }
    public String value() { return value; }
    public String get() { return value; }
    public void set(String value) {
        if (value == null) value = "";
        if (value.length() > maxLength) throw new IllegalArgumentException(label + " is too long");
        this.value = value;
    }
    public String serialize() { return value; }
    public String display() { return value.isBlank() ? "Not set" : value; }
    public void parse(String raw) { set(raw); }
}

final class ChoiceSetting extends ModuleSetting<String> {
    public final List<String> choices;
    private String value;

    ChoiceSetting(String name, String label, String description, String value, String... choices) {
        super(name, label, description);
        this.choices = List.of(choices);
        if (this.choices.isEmpty()) throw new IllegalArgumentException("Choice setting requires values");
        set(value);
    }

    public Kind kind() { return Kind.CHOICE; }
    public String value() { return value; }
    public String get() { return value; }
    public void set(String value) {
        this.value = choices.stream().filter(choice -> choice.equalsIgnoreCase(value)).findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Choose one of: " + String.join(", ", choices)));
    }
    public void cycle(int direction) {
        int index = choices.indexOf(value);
        set(choices.get(Math.floorMod(index + direction, choices.size())));
    }
    public String serialize() { return value; }
    public String display() { return value; }
    public void parse(String raw) { set(raw.trim()); }
}
