package com.darkcart.xdolf;

import net.minecraft.client.Minecraft;
import java.util.ArrayList;
import java.util.List;

/** Module state is owned by the client thread; no background game mutations. */
public abstract class ClientModule {
    public final String name;
    public final String description;
    public final String category;
    private boolean enabled;
    public int key = -1;
    public final List<ModuleSetting> settings = new ArrayList<>();

    protected ClientModule(String name, String description, String category) {
        this.name = name;
        this.description = description;
        this.category = category;
    }

    public final boolean enabled() { return enabled; }

    protected final ModuleSetting setting(String name, double value, double min, double max, double step) {
        var setting = new ModuleSetting(name, value, min, max, step);
        settings.add(setting);
        return setting;
    }

    public final ModuleSetting setting(String name) {
        return settings.stream().filter(s -> s.name.equalsIgnoreCase(name)).findFirst().orElse(null);
    }

    public final void setEnabled(boolean value) {
        if (enabled == value) return;
        enabled = value;
        if (value) activate(Minecraft.getInstance());
        else reset(Minecraft.getInstance());
    }

    public abstract void tick(Minecraft mc);
    public void activate(Minecraft mc) {}

    /** Also called on world changes and when opening a screen. Must be idempotent. */
    public void reset(Minecraft mc) {}
}
