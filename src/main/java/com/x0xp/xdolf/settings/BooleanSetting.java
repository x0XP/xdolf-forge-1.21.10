package com.x0xp.xdolf;

import java.util.Locale;

public final class BooleanSetting extends ModuleSetting<Boolean> {
    private boolean value;

    BooleanSetting(String name, String label, String description, boolean value) {
        super(name, label, description);
        this.value = value;
    }

    @Override
    public Kind kind() { return Kind.BOOLEAN; }

    @Override
    public Boolean value() { return value; }

    public boolean on() { return value; }
    public void set(boolean value) { this.value = value; }
    public void toggle() { value = !value; }

    @Override
    public String serialize() { return Boolean.toString(value); }

    @Override
    public String display() { return value ? "ON" : "OFF"; }

    @Override
    public void parse(String raw) {
        String value = raw.trim().toLowerCase(Locale.ROOT);
        if (value.equals("true") || value.equals("on") || value.equals("1") || value.equals("1.0")) set(true);
        else if (value.equals("false") || value.equals("off") || value.equals("0") || value.equals("0.0")) set(false);
        else throw new IllegalArgumentException("Use ON or OFF");
    }
}
