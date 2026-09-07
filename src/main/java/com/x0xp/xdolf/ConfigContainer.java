package com.x0xp.xdolf;

import net.minecraft.client.gui.GuiGraphics;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static com.x0xp.xdolf.UiDraw.*;

/** Rendering and geometry for the typed option cards shown beneath a module. */
final class ConfigContainer {
    private static final float TOP_GAP = 2;
    static final float HEADER_HEIGHT = 15;
    static final float KEYBIND_HEIGHT = 15;
    static final float NUMBER_HEIGHT = 25;
    static final float TEXT_HEIGHT = 25;
    static final float CHOICE_HEIGHT = 15;
    static final float BOOLEAN_HEIGHT = 15;
    static final float BOOLEAN_WRAPPED_HEIGHT = 22;
    static final float FIELD_WIDTH = 38;
    static final float NUMBER_FIELD_WIDTH = 25;
    private static final float TOGGLE_LABEL_WIDTH = 68;
    private static final float CONTENT_LEFT_INSET = 6;
    private static final float CONTENT_RIGHT_INSET = 5;

    private ConfigContainer() {}

    static float height(ClientModule module) {
        float height = HEADER_HEIGHT + KEYBIND_HEIGHT + 3;
        for (ModuleSetting<?> setting : module.settings) height += rowHeight(setting);
        return height;
    }

    static float rowHeight(ModuleSetting<?> setting) {
        return switch (setting.kind()) {
            case BOOLEAN -> XdolfFont.compactWidth(label(setting)) > TOGGLE_LABEL_WIDTH
                ? BOOLEAN_WRAPPED_HEIGHT : BOOLEAN_HEIGHT;
            case NUMBER -> NUMBER_HEIGHT;
            case TEXT -> TEXT_HEIGHT;
            case CHOICE -> CHOICE_HEIGHT;
        };
    }

    static String label(ModuleSetting<?> setting) {
        if (!setting.label.equals(setting.name)) return setting.label;
        return titleCase(setting.name);
    }

    static void draw(GuiGraphics graphics, ClickGuiPanel panel, ClientModule module, float top, float progress,
                     int mouseX, int mouseY, ClientScreen screen) {
        if (progress <= 0.001f) return;
        float left = panel.x + 4;
        float right = panel.x + 96;
        float contentLeft = left + CONTENT_LEFT_INSET;
        float contentRight = right - CONTENT_RIGHT_INSET;
        float fullHeight = height(module);
        float cardTop = top + TOP_GAP;
        int scissorTop = (int) Math.floor(cardTop);
        int scissorBottom = (int) Math.ceil(top + fullHeight * progress);
        if (scissorBottom <= scissorTop) return;

        graphics.enableScissor((int) left, scissorTop, (int) Math.ceil(right), scissorBottom);
        // A distinct inset card: deep graphite body, red identity rail, blue interactive accent.
        rect(graphics, left + 1, cardTop + 1, right + 1, top + fullHeight + 1, fade(0x70000000, progress));
        rect(graphics, left, cardTop, right, top + fullHeight, fade(0xF20B0D12, progress));
        outline(graphics, left, cardTop, right, top + fullHeight, fade(0xFF343943, progress));
        rect(graphics, left, cardTop, left + 1, top + fullHeight, fade(0xFFFF2020, progress));
        rect(graphics, left + 1, cardTop, right, top + HEADER_HEIGHT, fade(0xF0181B22, progress));

        float headerMid = left + (right - left) / 2f;
        rect(graphics, left + 1, top + HEADER_HEIGHT - 1, headerMid, top + HEADER_HEIGHT, fade(0xFFFF2020, progress));
        rect(graphics, headerMid, top + HEADER_HEIGHT - 1, right, top + HEADER_HEIGHT, fade(0xFF329CFF, progress));
        float headerTextY = centered(cardTop, top + HEADER_HEIGHT - 1);
        String moduleName = XdolfFont.compactTrim(ClientScreen.label(module), 42);
        float leftHalfStart = left + 1;
        float leftHalfWidth = headerMid - leftHalfStart;
        XdolfFont.drawCompact(graphics, moduleName,
            leftHalfStart + (leftHalfWidth - XdolfFont.compactWidth(moduleName)) / 2f,
            headerTextY, fade(0xFFFFFFFF, progress));
        String options = "OPTIONS";
        float rightHalfWidth = right - headerMid;
        XdolfFont.drawCompact(graphics, options,
            headerMid + (rightHalfWidth - XdolfFont.compactWidth(options)) / 2f,
            headerTextY, fade(0xFF8C929D, progress));

        boolean interactive = panel.expansion(module).open && progress >= 0.95f && screen != null;
        float y = top + HEADER_HEIGHT + 2;
        drawKeybind(graphics, module, contentLeft, contentRight, y,
            interactive && hit(mouseX, mouseY, contentRight - FIELD_WIDTH, y + 1, FIELD_WIDTH, 10), progress, screen);
        y += KEYBIND_HEIGHT;

        for (ModuleSetting<?> setting : module.settings) {
            float rowHeight = rowHeight(setting);
            boolean hover = interactive && hit(mouseX, mouseY, left + 3, y, right - left - 5, rowHeight - 1);
            if (hover) screen.noteSettingHover(module, setting);
            card(graphics, left + 3, right - 2, y, y + rowHeight - 2, hover, progress);
            switch (setting.kind()) {
                case BOOLEAN -> drawBoolean(graphics, (BooleanSetting) setting, contentLeft, contentRight, y, hover, progress);
                case NUMBER -> drawNumber(graphics, (NumberSetting) setting, contentLeft, contentRight, y, hover, progress, screen);
                case TEXT -> drawText(graphics, (TextSetting) setting, contentLeft, contentRight, y, hover, progress, screen);
                case CHOICE -> drawChoice(graphics, (ChoiceSetting) setting, contentLeft, contentRight, y, hover, progress);
            }
            y += rowHeight;
        }
        graphics.disableScissor();
    }

    private static void card(GuiGraphics graphics, float left, float right, float top, float bottom,
                             boolean hover, float alpha) {
        rect(graphics, left, top, right, bottom, fade(hover ? 0xDC171C24 : 0xC811141A, alpha));
        outline(graphics, left, top, right, bottom, fade(hover ? 0xFF46515E : 0xFF252A32, alpha));
        if (hover) rect(graphics, left, top, left + 1, bottom, fade(0xFF329CFF, alpha));
    }

    private static void drawKeybind(GuiGraphics graphics, ClientModule module, float left, float right, float y,
                                    boolean hover, float alpha, ClientScreen screen) {
        boolean listening = screen != null && screen.binding() == module;
        boolean pending = listening && screen.pendingBindingKey() >= 0 && !screen.pendingBindingConflicts().isEmpty();
        boolean conflict = !listening && module.key >= 0 && !Keybinds.conflicts(module, module.key).isEmpty();
        XdolfFont.drawCompact(graphics, "Keybind", left, centered(y + 1, y + 11),
            fade(listening || hover ? 0xFFFFFFFF : 0xFFD0D4DA, alpha));
        float fieldRight = right;
        float fieldLeft = fieldRight - FIELD_WIDTH;
        rect(graphics, fieldLeft, y + 1, fieldRight, y + 11, fade(listening ? 0xEB151E29 : 0xE00D1015, alpha));
        outline(graphics, fieldLeft, y + 1, fieldRight, y + 11,
            fade(pending ? 0xFFFFB347 : conflict ? 0xFFFF5D6C : listening || hover ? 0xFF329CFF : 0xFF454B55, alpha));
        String value = pending ? Keybinds.display(screen.pendingBindingKey()) + " !"
            : listening ? "Press..." : Keybinds.display(module.key) + (conflict ? " !" : "");
        value = fit(value, (int) FIELD_WIDTH - 4, false);
        XdolfFont.drawCompact(graphics, value, fieldLeft + (FIELD_WIDTH - XdolfFont.compactWidth(value)) / 2,
            centered(y + 1, y + 11), fade(pending ? 0xFFFFC66D : 0xFFFFFFFF, alpha));
    }

    private static void drawBoolean(GuiGraphics graphics, BooleanSetting setting, float left, float right, float y,
                                    boolean hover, float alpha) {
        List<String> lines = wrap(label(setting), (int) (right - left - 23));
        float textY = lines.size() > 1 ? y + 1 : centered(y, y + rowHeight(setting) - 2);
        for (int i = 0; i < lines.size(); i++)
            XdolfFont.drawCompact(graphics, lines.get(i), left, textY + i * 8,
                fade(setting.on() ? 0xFFFFFFFF : 0xFFADB2BA, alpha));
        float switchRight = right - 2;
        float switchLeft = switchRight - 17;
        float switchTop = y + (rowHeight(setting) - 8) / 2;
        rect(graphics, switchLeft, switchTop, switchRight, switchTop + 7,
            fade(setting.on() ? 0xFFFF2020 : 0xFF30353D, alpha));
        outline(graphics, switchLeft, switchTop, switchRight, switchTop + 7,
            fade(hover ? 0xFF329CFF : setting.on() ? 0xFFFF6868 : 0xFF515863, alpha));
        float knob = setting.on() ? switchRight - 6 : switchLeft + 1;
        rect(graphics, knob, switchTop + 1, knob + 5, switchTop + 6, fade(0xFFFFFFFF, alpha));
    }

    private static void drawChoice(GuiGraphics graphics, ChoiceSetting setting, float left, float right, float y,
                                   boolean hover, float alpha) {
        XdolfFont.drawCompact(graphics, compactLabel(label(setting), (int) (right - left - FIELD_WIDTH - 3)), left,
            centered(y + 1, y + 11), fade(hover ? 0xFFFFFFFF : 0xFFD0D4DA, alpha));
        float fieldLeft = right - FIELD_WIDTH;
        rect(graphics, fieldLeft, y + 1, right, y + 11, fade(0xE00D1015, alpha));
        outline(graphics, fieldLeft, y + 1, right, y + 11, fade(hover ? 0xFF329CFF : 0xFF454B55, alpha));
        String value = setting.get().equals("antispam") ? "Anti-spam" : titleCase(setting.get());
        value = fit(value, (int) FIELD_WIDTH - 4, false);
        XdolfFont.drawCompact(graphics, value, fieldLeft + (FIELD_WIDTH - XdolfFont.compactWidth(value)) / 2,
            centered(y + 1, y + 11), fade(0xFFFFFFFF, alpha));
    }

    private static void drawText(GuiGraphics graphics, TextSetting setting, float left, float right, float y,
                                 boolean hover, float alpha, ClientScreen screen) {
        boolean editing = screen != null && screen.editing() == setting;
        XdolfFont.drawCompact(graphics, label(setting), left, y + 1,
            fade(editing || hover ? 0xFFFFFFFF : 0xFFD0D4DA, alpha));
        float top = y + 10;
        float bottom = y + 20;
        rect(graphics, left, top, right, bottom, fade(editing ? 0xEB151E29 : 0xE00D1015, alpha));
        outline(graphics, left, top, right, bottom, fade(editing || hover ? 0xFF329CFF : 0xFF454B55, alpha));
        String value = editing ? screen.editingText() : setting.display();
        if (editing && screen.cursorVisible()) value += "_";
        value = fit(value, (int) (right - left - 4), editing);
        XdolfFont.drawCompact(graphics, value, left + 2, centered(top, bottom),
            fade(setting.get().isBlank() && !editing ? 0xFF7E858F : 0xFFFFFFFF, alpha));
    }

    private static void drawNumber(GuiGraphics graphics, NumberSetting setting, float left, float right, float y,
                                   boolean hover, float alpha, ClientScreen screen) {
        boolean editing = screen != null && screen.editing() == setting;
        float fieldLeft = right - NUMBER_FIELD_WIDTH;
        XdolfFont.drawCompact(graphics, compactLabel(label(setting), (int) (fieldLeft - left - 2)), left,
            centered(y + 1, y + 11), fade(editing || hover ? 0xFFFFFFFF : 0xFFD0D4DA, alpha));
        rect(graphics, fieldLeft, y + 1, right, y + 11, fade(editing ? 0xEB151E29 : 0xE00D1015, alpha));
        outline(graphics, fieldLeft, y + 1, right, y + 11, fade(editing || hover ? 0xFF329CFF : 0xFF454B55, alpha));
        String value = editing ? screen.editingText() : setting.display();
        if (editing && screen.cursorVisible()) value += "_";
        value = fit(value, (int) NUMBER_FIELD_WIDTH - 4, editing);
        XdolfFont.drawCompact(graphics, value, right - 2 - XdolfFont.compactWidth(value),
            centered(y + 1, y + 11), fade(0xFFFFFFFF, alpha));

        float trackTop = y + 16;
        float trackBottom = trackTop + 5;
        float fraction = (float) ((setting.get() - setting.min) / (setting.max - setting.min));
        fraction = clamp01(fraction);
        rect(graphics, left, trackTop, right, trackBottom, fade(0xFF262B33, alpha));
        outline(graphics, left, trackTop, right, trackBottom, fade(0xFF454B55, alpha));
        float fill = left + (right - left) * fraction;
        rect(graphics, left + .5f, trackTop + .5f, fill, trackBottom - .5f, fade(0xFFFF2020, alpha));
        rect(graphics, Math.max(left, fill - 1), trackTop - 1, Math.min(right, fill + 1), trackBottom + 1,
            fade(hover ? 0xFF62B5FF : 0xFFFF6A6A, alpha));
    }

    static float fieldLeft(float right, ModuleSetting<?> setting) {
        return right - (setting instanceof NumberSetting ? NUMBER_FIELD_WIDTH : FIELD_WIDTH);
    }

    static float sliderTop(float y) { return y + 16; }

    private static float centered(float top, float bottom) {
        int rounded = Math.round(top);
        return rounded + XdolfFont.compactCenteredYOffset(rounded, Math.max(1, Math.round(bottom - top)), rounded) + 1;
    }

    private static String compactLabel(String text, int width) {
        return XdolfFont.compactTrim(text, Math.max(1, width));
    }

    private static String fit(String text, int maxWidth, boolean keepEnd) {
        if (XdolfFont.compactWidth(text) <= maxWidth) return text;
        String ellipsis = "...";
        if (!keepEnd) return XdolfFont.compactTrim(text, maxWidth - XdolfFont.compactWidth(ellipsis)) + ellipsis;
        int start = 0;
        while (start < text.length() && XdolfFont.compactWidth(ellipsis + text.substring(start)) > maxWidth)
            start += Character.charCount(text.codePointAt(start));
        return ellipsis + text.substring(Math.min(start, text.length()));
    }

    private static List<String> wrap(String text, int maxWidth) {
        if (XdolfFont.compactWidth(text) <= maxWidth) return List.of(text);
        var lines = new ArrayList<String>(2);
        var current = new StringBuilder();
        for (String word : text.split(" ")) {
            String candidate = current.isEmpty() ? word : current + " " + word;
            if (!current.isEmpty() && XdolfFont.compactWidth(candidate) > maxWidth) {
                lines.add(current.toString());
                current.setLength(0);
                current.append(word);
            } else {
                if (!current.isEmpty()) current.append(' ');
                current.append(word);
            }
        }
        if (!current.isEmpty()) lines.add(current.toString());
        if (lines.size() <= 2) return lines;
        return List.of(lines.get(0), XdolfFont.compactTrim(String.join(" ", lines.subList(1, lines.size())), maxWidth));
    }

    private static String titleCase(String value) {
        if (value.isBlank()) return value;
        StringBuilder result = new StringBuilder();
        boolean upper = true;
        for (char c : value.toCharArray()) {
            if (c == '_' || c == '-') { result.append(' '); upper = true; }
            else { result.append(upper ? Character.toUpperCase(c) : c); upper = false; }
        }
        return result.toString();
    }
}
