package com.x0xp.xdolf.core;

import com.x0xp.xdolf.chat.ChatFormatter;
import com.x0xp.xdolf.chat.ChatQueue;
import com.x0xp.xdolf.command.Commands;
import com.x0xp.xdolf.dev.ClientSmoke;
import com.x0xp.xdolf.module.Activity;
import com.x0xp.xdolf.render.MarkerVisuals;
import com.x0xp.xdolf.render.WorldVisuals;
import com.x0xp.xdolf.settings.ChoiceSetting;
import com.x0xp.xdolf.settings.NumberSetting;
import com.x0xp.xdolf.social.SocialState;
import com.x0xp.xdolf.ui.clickgui.ClientScreen;
import com.x0xp.xdolf.ui.hud.Hud;
import com.x0xp.xdolf.ui.hud.NotificationCards;

import com.x0xp.xdolf.settings.ClientConfig;

import com.x0xp.xdolf.module.ClientModule;
import com.x0xp.xdolf.module.ModuleManager;
import com.x0xp.xdolf.settings.*;

import com.x0xp.xdolf.module.registry.Modules;

import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.DeathScreen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.event.AddGuiOverlayLayersEvent;
import net.minecraftforge.client.event.ClientChatEvent;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.MovementInputUpdateEvent;
import com.x0xp.xdolf.mixin.accessor.ClientInputAccess;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.phys.Vec2;
import net.minecraftforge.event.TickEvent;
import org.lwjgl.glfw.GLFW;
import java.util.List;

public final class ClientRuntime {
    public static final List<ClientModule> MODULES = Modules.create();
    private static ClientLevel previousLevel;
    private static net.minecraft.client.player.LocalPlayer previousPlayer;

    static void register() {
        WorldVisuals.register();
        MarkerVisuals.register();
        ClientConfig.load(MODULES);
        ModuleManager.reconcileRestoredSelections();
        SocialState.load();
        Commands.load();
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
                Hud.render(graphics);
            }));
    }

    private static void tick(TickEvent.ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        ClientSmoke.tick(mc);
        updateSession(mc);
        Commands.recordDeath(mc);
        if (mc.player == null || mc.level == null || mc.getConnection() == null) return;

        for (ClientModule module : MODULES) {
            if (!module.enabled()) continue;

            boolean respawnScreen = module.name.equals("AutoRespawn") && mc.screen instanceof DeathScreen;
            var status = ModuleManager.status(module, mc);
            if (status.activity() == ModuleManager.Activity.SUSPENDED
                || status.activity() == ModuleManager.Activity.MISSING_DEPENDENCY) {
                module.reset(mc);
                continue;
            }
            if (status.activity() == ModuleManager.Activity.PAUSED && !respawnScreen) continue;

            try {
                module.tick(mc);
            } catch (RuntimeException error) {
                if (Boolean.getBoolean("xdolf.smokeTest")) throw error;
                module.setEnabled(false);
                LogUtils.getLogger().error("Xdolf disabled failed module {}", module.name, error);
                message(module.name + " disabled after an error; check latest.log.");
            }
        }
        ChatQueue.tick(mc);
    }

    static void updateSession(Minecraft mc) {
        if (mc.level == previousLevel && mc.player == previousPlayer) return;
        ChatQueue.clear();
        for (var module : MODULES) module.reset(mc);
        previousLevel = mc.level;
        previousPlayer = mc.player;
        Commands.worldChanged(mc);
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
        else if (key == GLFW.GLFW_KEY_PERIOD)
            mc.setScreen(new net.minecraft.client.gui.screens.ChatScreen(".",false));
        Commands.runMacros(key);
    }

    private static boolean chat(ClientChatEvent event) { return Commands.execute(event.getMessage()); }

    static void configure(String[] parts) {
        ClientModule module = parts.length >= 2 ? find(parts[1]) : null;
        if (module == null) { message(".set <module> <setting> <value>"); return; }
        if (parts.length == 2) {
            module.settings.forEach(setting -> {
                String limits = setting instanceof NumberSetting number
                    ? " (" + number.min + " to " + number.max + ")"
                    : setting instanceof ChoiceSetting choice ? " (" + String.join("/", choice.choices) + ")" : "";
                message(setting.name + " = " + setting.display() + limits);
            });
            return;
        }
        var setting = parts.length >= 4 ? module.setting(parts[2]) : null;
        if (setting == null) { message("Use .set " + module.name + " to list settings."); return; }
        try {
            setting.parse(String.join(" ", java.util.Arrays.copyOfRange(parts, 3, parts.length)));
            ClientConfig.save(MODULES);
            message(module.name + " " + setting.name + " = " + setting.display());
        } catch (IllegalArgumentException error) {
            message(error.getMessage() == null ? "Invalid setting value." : error.getMessage());
        }
    }

    public static ClientModule find(String name) {
        return MODULES.stream().filter(module -> module.name.replace(" ", "").equalsIgnoreCase(name.replace(" ", ""))
            || ClientScreen.label(module).equalsIgnoreCase(name)).findFirst().orElse(null);
    }

    static void toggle(ClientModule module) {
        ModuleManager.toggle(module);
        NotificationCards.module(module, module.enabled());
    }

    public static void message(String text) {
        var player = Minecraft.getInstance().player;
        if (player != null) player.displayClientMessage(ChatFormatter.prefixed(text), false);
    }
}
