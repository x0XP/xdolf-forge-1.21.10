package com.x0xp.xdolf;

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
