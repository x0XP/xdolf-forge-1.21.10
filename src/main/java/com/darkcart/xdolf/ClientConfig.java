package com.darkcart.xdolf;

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
        try (Reader reader = Files.newBufferedReader(FILE)) {
            properties.load(reader);
            guiKey = LegacyKeys.read(properties.getProperty("GUI.key"), guiKey);
            NetworkModules.spamMessage = properties.getProperty("Spammer.message", "");
            if (NetworkModules.spamMessage.length() > 256) NetworkModules.spamMessage = "";
            for (ClientModule module : modules) {
                module.restoreEnabled(!module.name.equals("Spammer") && !module.name.equals("Freecam")
                    && Boolean.parseBoolean(properties.getProperty(module.name + ".enabled", "false")));
                String raw = properties.getProperty(module.name + ".key", "-1");
                try {
                    int key = Integer.parseInt(raw);
                    module.key = key >= 32 && key <= 348 ? key : -1;
                } catch (NumberFormatException ignored) {
                    module.key = -1;
                }
                for (var setting : module.settings) {
                    String value = properties.getProperty(module.name + "." + setting.name);
                    if (value != null) {
                        try { setting.set(Double.parseDouble(value)); }
                        catch (IllegalArgumentException ignored) { /* Keep the validated default. */ }
                    }
                }
            }
        } catch (IOException | IllegalArgumentException error) {
            LogUtils.getLogger().warn("Could not load Xdolf configuration", error);
        }
    }

    static void save(List<ClientModule> modules) {
        Properties properties = new Properties();
        properties.setProperty("GUI.key", Integer.toString(guiKey));
        properties.setProperty("Spammer.message", NetworkModules.spamMessage);
        for (ClientModule module : modules) {
            if (!module.name.equals("Spammer") && !module.name.equals("Freecam"))
                properties.setProperty(module.name + ".enabled", Boolean.toString(module.enabled()));
            properties.setProperty(module.name + ".key", Integer.toString(module.key));
            for (var setting : module.settings)
                properties.setProperty(module.name + "." + setting.name, Double.toString(setting.get()));
        }
        Path temporary = FILE.resolveSibling(FILE.getFileName() + ".tmp");
        try {
            Files.createDirectories(FILE.getParent());
            try (Writer writer = Files.newBufferedWriter(temporary)) {
                properties.store(writer, "Xdolf settings, key bindings and original persistent module states.");
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
}
