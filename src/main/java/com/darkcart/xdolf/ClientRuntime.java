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
    private static net.minecraft.client.player.LocalPlayer previousPlayer;

    static void register() {
        LegacyWorldVisuals.register();
        RestoredVisuals.register();
        ClientConfig.load(MODULES);
        SocialState.load();
        LegacyCommands.load();
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
        updateSession(mc);
        LegacyCommands.recordDeath(mc);
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

    static void updateSession(Minecraft mc) {
        if (mc.level == previousLevel && mc.player == previousPlayer) return;
        // Release old player/camera/inventory references, retaining the selection.
        for (var module : MODULES) module.reset(mc);
        previousLevel = mc.level;
        previousPlayer = mc.player;
        LegacyCommands.worldChanged(mc);
        if (mc.level != null && mc.player != null)
            for (var module : MODULES) if (module.enabled()) module.activate(mc);
    }

    private static void key(InputEvent.Key event) { handleKey(event.getKey(),event.getAction()); }
    static void handleKey(int key,int action) {
        Minecraft mc = Minecraft.getInstance();
        if (action != GLFW.GLFW_PRESS || mc.screen != null || mc.player == null || key < 0) return;
        for (ClientModule module : MODULES) if (module.key == key) toggle(module);
        if (key == ClientConfig.guiKey || (ClientConfig.guiKey == GLFW.GLFW_KEY_GRAVE_ACCENT && key == GLFW.GLFW_KEY_RIGHT_SHIFT))
            mc.setScreen(new ClientScreen());
        else if (key == GLFW.GLFW_KEY_PERIOD) mc.setScreen(new net.minecraft.client.gui.screens.ChatScreen(".",false));
        LegacyCommands.runMacros(key);
    }

    private static boolean chat(ClientChatEvent event) { return LegacyCommands.execute(event.getMessage()); }

    static void configure(String[] parts) {
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
        return MODULES.stream().filter(module -> module.name.replace(" ", "").equalsIgnoreCase(name.replace(" ", "")) || ClientScreen.label(module).equalsIgnoreCase(name)).findFirst().orElse(null);
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
        if (player != null) player.displayClientMessage(LegacyChat.prefixed(text), false);
    }
}
