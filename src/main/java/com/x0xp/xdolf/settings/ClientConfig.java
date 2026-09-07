package com.x0xp.xdolf;

import com.x0xp.xdolf.module.world.XRayModule;

import com.mojang.logging.LogUtils;
import net.minecraftforge.fml.loading.FMLPaths;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Properties;

final class ClientConfig {
    private static final Path FILE = FMLPaths.CONFIGDIR.get().resolve("xdolf.properties");
    static int guiKey = org.lwjgl.glfw.GLFW.GLFW_KEY_GRAVE_ACCENT;

    static void load(List<ClientModule> modules) {
        if (!Files.isRegularFile(FILE)) return;
        Properties properties = new Properties();
        Properties legacyCommands = legacyCommands();
        try (Reader reader = Files.newBufferedReader(FILE)) {
            properties.load(reader);
            guiKey = KeyNames.read(properties.getProperty("GUI.key"), guiKey);
            for (ClientModule module : modules) {
                module.restoreEnabled(!module.name.equals("Spammer") && !module.name.equals("Freecam")
                    && Boolean.parseBoolean(properties.getProperty(module.name + ".enabled", "false")));
                int defaultKey = module.key;
                String raw = properties.getProperty(module.name + ".key", Integer.toString(defaultKey));
                try {
                    int key = Integer.parseInt(raw);
                    module.key = key >= 32 && key <= 348 ? key : -1;
                } catch (NumberFormatException ignored) {
                    module.key = defaultKey;
                }
                for (var setting : module.settings) {
                    String value = properties.getProperty(module.name + "." + setting.name);
                    if (value == null && module.name.equals("Spammer") && setting.name.equals("mode"))
                        value = legacyCommands.getProperty("spam.mode");
                    if (value == null && module.name.equals("Spammer") && setting.name.equals("delay"))
                        value = legacyCommands.getProperty("spam.delay");
                    if (value != null) {
                        try { setting.parse(value); }
                        catch (IllegalArgumentException ignored) { }
                    }
                }
            }
        } catch (IOException | IllegalArgumentException error) {
            LogUtils.getLogger().warn("Could not load Xdolf configuration", error);
        }
    }

    static void save(List<ClientModule> modules) {
        Properties properties = new Properties();
        properties.setProperty("config.version", "2");
        properties.setProperty("GUI.key", Integer.toString(guiKey));
        for (ClientModule module : modules) {
            if (!module.name.equals("Spammer") && !module.name.equals("Freecam"))
                properties.setProperty(module.name + ".enabled", Boolean.toString(module.enabled()));
            properties.setProperty(module.name + ".key", Integer.toString(module.key));
            for (var setting : module.settings)
                properties.setProperty(module.name + "." + setting.name, setting.serialize());
        }
        Path temporary = FILE.resolveSibling(FILE.getFileName() + ".tmp");
        try {
            Files.createDirectories(FILE.getParent());
            try (Writer writer = Files.newBufferedWriter(temporary)) {
                properties.store(writer, "Xdolf settings, key bindings and persistent module states.");
            }
            try {
                Files.move(temporary, FILE, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException ignored) {
                Files.move(temporary, FILE, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException error) {
            LogUtils.getLogger().warn("Could not save Xdolf configuration", error);
            ClientRuntime.message("Could not save key bindings; check the log.");
        }
    }

    private static Properties legacyCommands() {
        var properties = new Properties();
        Path legacy = FMLPaths.CONFIGDIR.get().resolve("xdolf-commands.properties");
        if (!Files.isRegularFile(legacy)) return properties;
        try (Reader reader = Files.newBufferedReader(legacy)) {
            properties.load(reader);
        } catch (IOException ignored) { }
        return properties;
    }
}
