package com.x0xp.xdolf;

import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.network.chat.Component;
import com.x0xp.xdolf.mixin.CreateWorldScreenAccess;

/** Explicit opt-in CI test. Normal launches never create a test world. */
final class ClientSmoke {
    private static final boolean ACTIVE = Boolean.getBoolean("xdolf.smokeTest");
    private static int phase, frames, ticks;
    private static volatile boolean captureDone;
    private static volatile boolean lowResCaptureDone;
    private static volatile boolean worldCaptureDone;
    private static boolean guiCaptureRequested;
    private static boolean lowResCaptureRequested;

    static void tick(Minecraft mc) {
        if (!ACTIVE || mc.getOverlay() != null) return;
        if(guiCaptureRequested) {
            guiCaptureRequested=false;
            net.minecraft.client.Screenshot.grab(new java.io.File("."),mc.getMainRenderTarget(),message -> captureDone=true);
        }
        if(lowResCaptureRequested) {
            lowResCaptureRequested=false;
            net.minecraft.client.Screenshot.grab(new java.io.File("."),mc.getMainRenderTarget(),message -> lowResCaptureDone=true);
        }

        if (phase == 0 && mc.screen != null && (mc.screen instanceof TitleScreen || mc.screen.getClass().getSimpleName().equals("AccessibilityOnboardingScreen"))) {
            phase = 1;
            mc.options.guiScale().set(2);
            org.lwjgl.glfw.GLFW.glfwSetWindowSize(org.lwjgl.glfw.GLFW.glfwGetCurrentContext(), 1280, 800);
            mc.options.renderDistance().set(3);
            mc.options.simulationDistance().set(3);
            mc.setScreen(new ClientScreen());
        } else if (phase == 2 && mc.screen instanceof CreateWorldScreen create) {
            create.getUiState().setName("Xdolf automated smoke");
            create.getUiState().setGameMode(WorldCreationUiState.SelectedGameMode.CREATIVE);
            phase = 3;
            ((CreateWorldScreenAccess) create).xdolf$create();
        } else if (phase == 3 && mc.player != null && mc.level != null && mc.screen == null) {
            if (++ticks == 30) {
                for (String name : new String[] {"Fullbright", "NoHurtCam", "Chams", "XRay", "EntityESP", "StorageESP", "Nametags", "Tracers", "Trajectories"}) ClientRuntime.find(name).setEnabled(true);
            }
            if (ticks == 60) mc.player.setXRot(-45);
            if (ticks == 90) WorldVisuals.smokeFixture=true;
            if (ticks == 180) {
                WorldVisuals.assertSmokeRendered();
                net.minecraft.client.Screenshot.grab(new java.io.File("."), mc.getMainRenderTarget(), message -> worldCaptureDone=true);
            }
            if (ticks >= 200 && worldCaptureDone) {
                WorldVisuals.smokeFixture=false;
                for (ClientModule module : ClientRuntime.MODULES) module.setEnabled(false);
                ClientRuntime.find("Fullbright").setEnabled(true);
                LogUtils.getLogger().info("XDOLF_SMOKE_WORLD_OK: singleplayer loaded and visual modules ran for 150 ticks");
                phase = 4; frames = 0; mc.setScreen(new ClientScreen());
            }
        }
    }

    static void frame() {
        if (!ACTIVE) return;
        frames++;
        if (phase == 4 && frames == 5) {
            var fullbright=ClientRuntime.find("Fullbright");
            if(!fullbright.enabled()||!Hooks.active("Fullbright"))
                throw new IllegalStateException("Opening a GUI suspended an enabled module");
            LogUtils.getLogger().info("XDOLF_SCREEN_MODULES_OK: enabled modules remain active while GUI screens are open");
            ClientScreen.smokeCheckAndArrange();
            return;
        }
        if (phase == 4 && frames == 15) {
            phase = 5;
            guiCaptureRequested=true;
            return;
        }
        if (phase == 5 && captureDone) {
            Minecraft mc=Minecraft.getInstance();
            mc.options.guiScale().set(1);
            org.lwjgl.glfw.GLFW.glfwSetWindowSize(org.lwjgl.glfw.GLFW.glfwGetCurrentContext(),640,360);
            mc.gui.getChat().addMessage(Component.literal("[Xdolf] Low-resolution TTF smoke test: lorem ipsum 0123456789"));
            mc.setScreen(new ChatScreen("lorem ipsum"));
            phase=6;
            frames=0;
            return;
        }
        if(phase==6&&frames==20) {
            lowResCaptureRequested=true;
            phase=7;
            return;
        }
        if (phase == 7 && lowResCaptureDone) {
            try(var files=java.nio.file.Files.list(java.nio.file.Path.of("screenshots"))) {
                long count=files.filter(p->p.toString().endsWith(".png")).count();
                if(count<2)throw new IllegalStateException("Expected normal and low-resolution screenshots");
            } catch(java.io.IOException error) { throw new IllegalStateException("Screenshots were not saved",error); }

            LogUtils.getLogger().info("XDOLF_LOW_RES_FONT_OK: rendered chat at 640x360 with GUI scale 1");
            ClientRuntime.find("Fullbright").setEnabled(false);
            Minecraft mc = Minecraft.getInstance();
            mc.setScreen(null);
            CommandSmoke.run(mc);

            // Exercise the same reset/reactivate lifecycle used when the client receives a new
            // world/player session, without deadlocking an integrated server inside CI.
            for(var module:ClientRuntime.MODULES)module.reset(mc);
            for(var module:ClientRuntime.MODULES)if(module.enabled())module.activate(mc);
            CommandSmoke.assertSelections();
            if(!XRayModule.rendering)throw new IllegalStateException("XRay did not reactivate after lifecycle reset");
            LogUtils.getLogger().info("XDOLF_LIFECYCLE_OK: enabled selections survived reset/reactivation");

            Commands.execute(".alloff");
            LogUtils.getLogger().info("XDOLF_SMOKE_OK: GUI, low-resolution TTF, screen-active modules, world rendering, commands, binds, persistence and lifecycle passed");
            phase=9;
            mc.stop();
            return;
        }
        if(frames != 5) return;
        Minecraft mc = Minecraft.getInstance();
        if (phase == 1) {
            LogUtils.getLogger().info("XDOLF_SMOKE_MENU_OK");
            phase = 2;
            mc.execute(() -> CreateWorldScreen.openFresh(mc, () -> { throw new IllegalStateException("World creation cancelled"); }));
        }
    }
}
