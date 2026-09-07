package com.x0xp.xdolf.settings;

import java.util.List;

public final class ChoiceSetting extends ModuleSetting<String> {
    public final List<String> choices;
    private String value;

    public ChoiceSetting(String name, String label, String description, String value, String... choices) {
        super(name, label, description);
        this.choices = List.of(choices);
        if (this.choices.isEmpty()) throw new IllegalArgumentException("Choice setting requires values");
        set(value);
    }

    @Override
    public Kind kind() { return Kind.CHOICE; }

    @Override
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

    @Override
    public String serialize() { return value; }

    @Override
    public String display() { return value; }

    @Override
    public void parse(String raw) { set(raw.trim()); }
}
