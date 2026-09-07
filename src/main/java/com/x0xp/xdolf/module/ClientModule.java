package com.x0xp.xdolf;

import net.minecraft.client.Minecraft;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Module state and declarative metadata are owned by the client thread. */
public abstract class ClientModule {
    public final String name;
    public final String description;
    public final String category;
    private boolean enabled;
    public int key = -1;
    public final List<ModuleSetting<?>> settings = new ArrayList<>();
    private final Set<String> conflicts = new LinkedHashSet<>();
    private final Set<String> dependencies = new LinkedHashSet<>();
    private boolean runsDuringFreecam;
    private boolean runsWhilePaused;

    protected ClientModule(String name, String description, String category) {
        this.name = name;
        this.description = description;
        this.category = category;
        this.runsDuringFreecam = category.equals("Render");
    }

    public final boolean enabled() { return enabled; }
    final void restoreEnabled(boolean value) { enabled = value; }

    protected final NumberSetting setting(String name, double value, double min, double max, double step) {
        return numberSetting(name, name, "", value, min, max, step);
    }

    protected final NumberSetting numberSetting(String name, String label, String description,
                                                double value, double min, double max, double step) {
        var setting = new NumberSetting(name, label, description, value, min, max, step);
        settings.add(setting);
        return setting;
    }

    protected final BooleanSetting toggle(String name, boolean value) {
        return booleanSetting(name, name, "", value);
    }

    protected final BooleanSetting booleanSetting(String name, String label, String description, boolean value) {
        var setting = new BooleanSetting(name, label, description, value);
        settings.add(setting);
        return setting;
    }

    protected final TextSetting textSetting(String name, String label, String description, String value, int maxLength) {
        var setting = new TextSetting(name, label, description, value, maxLength);
        settings.add(setting);
        return setting;
    }

    protected final ChoiceSetting choiceSetting(String name, String label, String description,
                                                String value, String... choices) {
        var setting = new ChoiceSetting(name, label, description, value, choices);
        settings.add(setting);
        return setting;
    }

    public final ModuleSetting<?> setting(String name) {
        return settings.stream().filter(setting -> setting.name.equalsIgnoreCase(name)).findFirst().orElse(null);
    }

    public final NumberSetting numberSetting(String name) {
        return setting(name) instanceof NumberSetting number ? number : null;
    }

    protected final void conflictsWith(String... moduleNames) { conflicts.addAll(List.of(moduleNames)); }
    protected final void dependsOn(String... moduleNames) { dependencies.addAll(List.of(moduleNames)); }
    protected final void runDuringFreecam() { runsDuringFreecam = true; }
    protected final void runWhilePaused() { runsWhilePaused = true; }
    final Set<String> conflicts() { return Set.copyOf(conflicts); }
    final Set<String> dependencies() { return Set.copyOf(dependencies); }
    final boolean runsDuringFreecam() { return runsDuringFreecam; }
    final boolean runsWhilePaused() { return runsWhilePaused; }

    public final void setEnabled(boolean value) {
        ModuleManager.setEnabled(this, value);
    }

    final void applyEnabled(boolean value) {
        if (enabled == value) return;
        enabled = value;
        if (value && Minecraft.getInstance().player != null && Minecraft.getInstance().level != null) activate(Minecraft.getInstance());
        else reset(Minecraft.getInstance());
        ClientConfig.save(ClientRuntime.MODULES);
    }

    public abstract void tick(Minecraft mc);
    public void activate(Minecraft mc) {}
    public void reset(Minecraft mc) {}
}
