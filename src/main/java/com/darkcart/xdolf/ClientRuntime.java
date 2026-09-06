package com.darkcart.xdolf;

import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.DeathScreen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.event.AddGuiOverlayLayersEvent;
import net.minecraftforge.client.event.ClientChatEvent;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.MovementInputUpdateEvent;
import com.darkcart.xdolf.mixin.ClientInputAccess;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.phys.Vec2;
import net.minecraftforge.event.TickEvent;
import org.lwjgl.glfw.GLFW;
import java.util.List;
import java.util.Locale;

final class ClientRuntime {
    static final List<ClientModule> MODULES = Modules.create();
    private static ClientLevel previousLevel;

    static void register() {
        LegacyWorldVisuals.register();
        ClientConfig.load(MODULES);
        SocialState.load();
        TickEvent.ClientTickEvent.Post.BUS.addListener(ClientRuntime::tick);
        InputEvent.Key.BUS.addListener(ClientRuntime::key);
        MovementInputUpdateEvent.BUS.addListener(event -> {
            if (Hooks.active("Freecam")) {
                event.getInput().keyPresses = Input.EMPTY;
                ((ClientInputAccess) event.getInput()).xdolf$setMoveVector(Vec2.ZERO);
            } else if (Hooks.active("AutoWalk")) {
                var input = event.getInput();
                var keys = input.keyPresses;
                input.keyPresses = new Input(true, false, keys.left(), keys.right(), keys.jump(), keys.shift(), keys.sprint());
                float sideways = (keys.left() ? 1 : 0) - (keys.right() ? 1 : 0);
                float length = (float) Math.sqrt(sideways * sideways + 1);
                ((ClientInputAccess) input).xdolf$setMoveVector(new Vec2(sideways / length, 1 / length));
            }
        });
        ClientChatEvent.BUS.addListener((java.util.function.Predicate<ClientChatEvent>) ClientRuntime::chat);
        AddGuiOverlayLayersEvent.BUS.addListener(event -> event.getLayeredDraw().add(
            ResourceLocation.fromNamespaceAndPath(Xdolf.ID, "hud"), (graphics, delta) -> {
                Minecraft mc = Minecraft.getInstance();
                if (mc.player == null || mc.options.hideGui) return;
                LegacyHud.render(graphics);
            }));
    }

    private static void tick(TickEvent.ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        ClientSmoke.tick(mc);
        if (mc.level != previousLevel) {
            for (ClientModule module : MODULES) {
                module.setEnabled(false);
                module.reset(mc);
            }
            previousLevel = mc.level;
        }
        if (mc.player == null || mc.level == null || mc.getConnection() == null) return;
        for (ClientModule module : MODULES) {
            if (!module.enabled()) continue;
            boolean respawnScreen = module.name.equals("AutoRespawn") && mc.screen instanceof DeathScreen;
            boolean visual = module.category.equals("Render") || module.name.equals("Fullbright") || module.name.equals("XRay");
            boolean freecamSuspended = Hooks.enabled("Freecam") && !visual && !module.name.equals("Freecam");
            if (freecamSuspended || (!visual && (mc.isPaused() || (mc.screen != null && !respawnScreen)))) {
                module.reset(mc);
                continue;
            }
            try {
                module.tick(mc);
            } catch (RuntimeException error) {
                if (Boolean.getBoolean("xdolf.smokeTest")) throw error;
                module.setEnabled(false);
                LogUtils.getLogger().error("Xdolf disabled failed module {}", module.name, error);
                message(module.name + " disabled after an error; check latest.log.");
            }
        }
    }

    private static void key(InputEvent.Key event) {
        Minecraft mc = Minecraft.getInstance();
        if (event.getAction() != GLFW.GLFW_PRESS || mc.screen != null || mc.player == null) return;
        if ((event.getKey() == GLFW.GLFW_KEY_RIGHT_SHIFT || event.getKey() == GLFW.GLFW_KEY_GRAVE_ACCENT)) {
            mc.setScreen(new ClientScreen());
            return;
        }
        for (ClientModule module : MODULES) if (module.key == event.getKey()) toggle(module);
    }

    private static boolean chat(ClientChatEvent event) {
        String text = event.getMessage();
        if (!text.startsWith(".")) return false;
        // Local commands must never leak into multiplayer chat, including invalid commands.
        String[] parts = text.substring(1).trim().split("\\s+");
        switch (parts[0].toLowerCase(Locale.ROOT)) {
            case "help" -> message(".gui | .mods | .toggle <module> | .bind <module> <key> | .set <module> <setting> <value> | .friend | .alloff");
            case "gui" -> Minecraft.getInstance().execute(() -> Minecraft.getInstance().setScreen(new ClientScreen()));
            case "mods", "modlist" -> MODULES.forEach(module -> message(module.name + (module.enabled() ? " ON" : " OFF")));
            case "alloff" -> {
                MODULES.forEach(module -> module.setEnabled(false));
                message("All modules disabled.");
            }
            case "toggle", "t" -> {
                ClientModule module = parts.length == 2 ? find(parts[1]) : null;
                if (module == null) message("Usage: .toggle <module>; use .mods for available modules.");
                else toggle(module);
            }
            case "bind" -> bind(parts);
            case "set" -> configure(parts);
            case "friend" -> SocialState.command(parts);
            case "hide" -> {
                if(parts.length!=2)message(".hide mods/potions");
                else if(parts[1].equalsIgnoreCase("mods"))LegacyHud.showModules=!LegacyHud.showModules;
                else if(parts[1].equalsIgnoreCase("potions"))LegacyHud.showPotions=!LegacyHud.showPotions;
                else message(".hide mods/potions");
            }
            case "spam" -> {
                String value = text.length() > 6 ? text.substring(6).trim() : "";
                if (value.length() > 256) message("Message must be 256 characters or fewer.");
                else {
                    NetworkModules.spamMessage = value; ClientConfig.save(MODULES);
                    message(value.isEmpty() ? "Repeated message cleared." : "Message saved; toggle Spammer to start.");
                }
            }
            default -> message("Unknown local command. Use .help.");
        }
        return true;
    }

    private static void bind(String[] parts) {
        ClientModule module = parts.length == 3 ? find(parts[1]) : null;
        if (module == null) { message("Usage: .bind <module> <A-Z/F1-F12/NONE>"); return; }
        String value = parts[2].toUpperCase(Locale.ROOT);
        int key = -1;
        if (value.matches("[A-Z0-9]")) key = value.charAt(0);
        else if (value.matches("F([1-9]|1[0-2])")) key = GLFW.GLFW_KEY_F1 + Integer.parseInt(value.substring(1)) - 1;
        else if (!value.equals("NONE")) { message("Choose A-Z, 0-9, F1-F12, or NONE."); return; }
        module.key = key;
        ClientConfig.save(MODULES);
        message(module.name + " binding: " + value);
    }

    private static void configure(String[] parts) {
        ClientModule module = parts.length >= 2 ? find(parts[1]) : null;
        if (module == null) { message(".set <module> <setting> <value>"); return; }
        if (parts.length == 2) {
            module.settings.forEach(s -> message(s.name + " = " + s.display() + " (" + s.min + " to " + s.max + ")"));
            return;
        }
        var setting = parts.length == 4 ? module.setting(parts[2]) : null;
        if (setting == null) { message("Use .set " + module.name + " to list settings."); return; }
        try {
            setting.set(Double.parseDouble(parts[3])); ClientConfig.save(MODULES);
            message(module.name + " " + setting.name + " = " + setting.display());
        } catch (IllegalArgumentException error) { message("Enter a number between " + setting.min + " and " + setting.max + "."); }
    }

    static ClientModule find(String name) {
        return MODULES.stream().filter(module -> module.name.equalsIgnoreCase(name)).findFirst().orElse(null);
    }

    static void toggle(ClientModule module) {
        if (!module.enabled() && List.of("Flight", "ElytraFly", "ElytraPlus").contains(module.name)) {
            for (var other : MODULES) if (other != module && List.of("Flight", "ElytraFly", "ElytraPlus").contains(other.name)) other.setEnabled(false);
        }
        module.setEnabled(!module.enabled());
        message(module.name + (module.enabled() ? " enabled" : " disabled"));
    }

    static void message(String text) {
        var player = Minecraft.getInstance().player;
        if (player != null) player.displayClientMessage(Component.literal("[Xdolf] " + text), false);
    }
}
