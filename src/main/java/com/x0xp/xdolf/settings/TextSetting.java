package com.x0xp.xdolf.settings;

public final class TextSetting extends ModuleSetting<String> {
    public final int maxLength;
    private String value;

    public TextSetting(String name, String label, String description, String value, int maxLength) {
        super(name, label, description);
        if (maxLength < 1) throw new IllegalArgumentException("Text limit must be positive");
        this.maxLength = maxLength;
        set(value);
    }

    @Override
    public Kind kind() { return Kind.TEXT; }

    @Override
    public String value() { return value; }

    public String get() { return value; }

    public void set(String value) {
        if (value == null) value = "";
        if (value.length() > maxLength) throw new IllegalArgumentException(label + " is too long");
        this.value = value;
    }

    @Override
    public String serialize() { return value; }

    @Override
    public String display() { return value.isBlank() ? "Not set" : value; }

    @Override
    public void parse(String raw) { set(raw); }
}
