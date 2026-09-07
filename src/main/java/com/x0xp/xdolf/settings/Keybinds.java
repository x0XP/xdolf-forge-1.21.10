package com.x0xp.xdolf.settings;

import com.x0xp.xdolf.ClientRuntime;
import com.x0xp.xdolf.ClientScreen;
import com.x0xp.xdolf.module.ClientModule;

import com.x0xp.xdolf.module.ClientModule;

import org.lwjgl.glfw.GLFW;
import java.util.List;
import java.util.stream.Collectors;

/** Key display and conflict rules shared by the click GUI and runtime configuration. */
public final class Keybinds {
    private Keybinds() {}

    public static String display(int key) {
        String name = KeyNames.name(key);
        return switch (name) {
            case "NONE" -> "None";
            case "LEFT_SHIFT" -> "LShift";
            case "RIGHT_SHIFT" -> "RShift";
            case "LEFT_CONTROL" -> "LCtrl";
            case "RIGHT_CONTROL" -> "RCtrl";
            case "LEFT_ALT" -> "LAlt";
            case "RIGHT_ALT" -> "RAlt";
            case "LEFT_SUPER" -> "LSuper";
            case "RIGHT_SUPER" -> "RSuper";
            case "GRAVE_ACCENT" -> "Grave";
            case "PAGE_UP" -> "PgUp";
            case "PAGE_DOWN" -> "PgDn";
            case "CAPS_LOCK" -> "Caps";
            case "SCROLL_LOCK" -> "Scroll";
            case "PRINT_SCREEN" -> "PrtSc";
            default -> name.replace('_', ' ');
        };
    }

    public static boolean guiConflict(int key) {
        return key >= 0 && (key == ClientConfig.guiKey
            || (ClientConfig.guiKey == GLFW.GLFW_KEY_GRAVE_ACCENT && key == GLFW.GLFW_KEY_RIGHT_SHIFT));
    }

    public static List<ClientModule> conflicts(ClientModule target, int key) {
        if (key < 0) return List.of();
        return ClientRuntime.MODULES.stream()
            .filter(module -> module != target && module.key == key)
            .toList();
    }

    public static String conflictNames(ClientModule target, int key) {
        return conflicts(target, key).stream().map(ClientScreen::label).collect(Collectors.joining(", "));
    }

    public static void replaceConflicts(ClientModule target, int key) {
        for (var conflict : conflicts(target, key)) conflict.key = -1;
        target.key = key;
    }
}
