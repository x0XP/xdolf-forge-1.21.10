package com.x0xp.xdolf.ui.clickgui;

import com.mojang.logging.LogUtils;
import net.minecraftforge.fml.loading.FMLPaths;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Properties;

/** Atomic persistence for click-GUI window position and state. */
final class ClickGuiPersistence {
    private ClickGuiPersistence() {}

    static void load(List<ClickGuiPanel> panels) {
        var file = FMLPaths.CONFIGDIR.get().resolve("xdolf-gui.properties");
        if (!Files.isRegularFile(file)) return;
        try (var reader = Files.newBufferedReader(file)) {
            var properties = new Properties();
            properties.load(reader);
            for (ClickGuiPanel panel : panels) {
                try {
                    panel.x = Integer.parseInt(properties.getProperty(panel.title + ".x", "2"));
                    panel.y = Integer.parseInt(properties.getProperty(panel.title + ".y", Integer.toString(panel.y)));
                } catch (NumberFormatException ignored) { }
                panel.restoreOpen(Boolean.parseBoolean(properties.getProperty(panel.title + ".open")));
                panel.pinned = Boolean.parseBoolean(properties.getProperty(panel.title + ".pinned"));
            }
        } catch (java.io.IOException error) {
            LogUtils.getLogger().warn("Could not load Xdolf GUI", error);
        }
    }

    static void save(List<ClickGuiPanel> panels) {
        var file = FMLPaths.CONFIGDIR.get().resolve("xdolf-gui.properties");
        var properties = new Properties();
        for (ClickGuiPanel panel : panels) {
            properties.setProperty(panel.title + ".x", Integer.toString(panel.x));
            properties.setProperty(panel.title + ".y", Integer.toString(panel.y));
            properties.setProperty(panel.title + ".open", Boolean.toString(panel.open));
            properties.setProperty(panel.title + ".pinned", Boolean.toString(panel.pinned));
        }
        var temporary = file.resolveSibling(file.getFileName() + ".tmp");
        try {
            Files.createDirectories(file.getParent());
            try (var writer = Files.newBufferedWriter(temporary)) {
                properties.store(writer, "Xdolf click GUI window state");
            }
            try {
                Files.move(temporary, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException ignored) {
                Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (java.io.IOException error) {
            LogUtils.getLogger().warn("Could not save Xdolf GUI", error);
        }
    }
}
