package com.x0xp.xdolf;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import java.util.ArrayList;
import java.util.List;

import static com.x0xp.xdolf.UiDraw.*;

/** Original-style information card used for module and setting hover descriptions. */
final class ClickGuiTooltip {
    private static final int WIDTH = 166;

    private ClickGuiTooltip() {}

    static void draw(GuiGraphics graphics, int mouseX, int mouseY, ClientModule module, ModuleSetting<?> setting) {
        if (module == null) return;
        var body = new ArrayList<String>();
        String title;
        String status;
        int statusColor;
        if (setting == null) {
            title = ClientScreen.label(module);
            var moduleStatus = ModuleManager.status(module, Minecraft.getInstance());
            status = moduleStatus.detail();
            statusColor = switch (moduleStatus.activity()) {
                case ACTIVE -> 0xFF62B5FF;
                case DISABLED -> 0xFF8D939D;
                case WAITING_FOR_WORLD, PAUSED, SUSPENDED -> 0xFFFFB347;
                case MISSING_DEPENDENCY -> 0xFFFF5D6C;
            };
            body.addAll(wrap(module.description, WIDTH - 12));
            var conflicts = ModuleManager.conflicts(module);
            if (!conflicts.isEmpty()) body.add("Conflicts: " + String.join(", ", conflicts));
            if (!module.dependencies().isEmpty()) body.add("Requires: " + String.join(", ", module.dependencies()));
            body.add("Left-click toggles | Right-click options");
        } else {
            title = ConfigContainer.label(setting);
            status = setting.display();
            statusColor = 0xFF62B5FF;
            if (!setting.description.isBlank()) body.addAll(wrap(setting.description, WIDTH - 12));
            if (setting instanceof NumberSetting number)
                body.add("Range: " + plain(number.min) + " - " + plain(number.max));
            else if (setting instanceof ChoiceSetting choice)
                body.add("Choices: " + String.join(", ", choice.choices));
            body.add(setting.kind() == ModuleSetting.Kind.TEXT ? "Click to edit" :
                setting.kind() == ModuleSetting.Kind.NUMBER ? "Click value or drag slider" : "Click to change");
        }

        int height = 22 + body.size() * 8 + 5;
        int left = Math.min(graphics.guiWidth() - WIDTH - 4, mouseX + 10);
        int top = Math.min(graphics.guiHeight() - height - 4, mouseY + 10);
        left = Math.max(4, left);
        top = Math.max(4, top);

        rect(graphics, left + 2, top + 2, left + WIDTH + 2, top + height + 2, 0x88000000);
        rect(graphics, left, top, left + WIDTH, top + height, 0xF20A0C10);
        outline(graphics, left, top, left + WIDTH, top + height, 0xFF3B414B);
        rect(graphics, left, top, left + 1, top + height, 0xFFFF2020);
        rect(graphics, left + 1, top, left + WIDTH, top + 1, 0xFF329CFF);
        XdolfFont.drawCompact(graphics, title, left + 5, top + 4, 0xFFFFFFFF);
        String fittedStatus = XdolfFont.compactTrim(status, WIDTH - 12 - XdolfFont.compactWidth(title));
        XdolfFont.drawCompact(graphics, fittedStatus, left + WIDTH - 5 - XdolfFont.compactWidth(fittedStatus),
            top + 4, statusColor);
        rect(graphics, left + 5, top + 14, left + WIDTH - 5, top + 14.5f, 0x553B414B);
        for (int i = 0; i < body.size(); i++) {
            int color = i == body.size() - 1 ? 0xFF8D939D : 0xFFD4D7DC;
            XdolfFont.drawCompact(graphics, XdolfFont.compactTrim(body.get(i), WIDTH - 10),
                left + 5, top + 17 + i * 8, color);
        }
    }

    private static List<String> wrap(String text, int width) {
        var lines = new ArrayList<String>();
        var current = new StringBuilder();
        for (String word : text.split(" ")) {
            String candidate = current.isEmpty() ? word : current + " " + word;
            if (!current.isEmpty() && XdolfFont.compactWidth(candidate) > width) {
                lines.add(current.toString());
                current.setLength(0);
                current.append(word);
            } else {
                if (!current.isEmpty()) current.append(' ');
                current.append(word);
            }
        }
        if (!current.isEmpty()) lines.add(current.toString());
        return lines;
    }

    private static String plain(double value) {
        return value == Math.rint(value) ? Long.toString(Math.round(value)) : Double.toString(value);
    }
}
