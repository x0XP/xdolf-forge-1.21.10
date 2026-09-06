package com.darkcart.xdolf;

import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import com.darkcart.xdolf.mixin.CreateWorldScreenAccess;

/** Explicit opt-in CI test. Normal launches never create a test world. */
final class ClientSmoke {
    private static final boolean ACTIVE = Boolean.getBoolean("xdolf.smokeTest");
    private static int phase, frames, ticks;
    private static volatile boolean captureDone;
    private static volatile boolean worldCaptureDone;
    private static boolean guiCaptureRequested;
    static void tick(Minecraft mc) {
        if (!ACTIVE || mc.getOverlay() != null) return;
        if(guiCaptureRequested) {
            guiCaptureRequested=false;
            net.minecraft.client.Screenshot.grab(new java.io.File("."),mc.getMainRenderTarget(),message -> captureDone=true);
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
            if (ticks == 90) LegacyWorldVisuals.smokeFixture=true;
            if (ticks == 180) {
                LegacyWorldVisuals.assertSmokeRendered();
                net.minecraft.client.Screenshot.grab(new java.io.File("."), mc.getMainRenderTarget(), message -> worldCaptureDone=true);
            }
            if (ticks >= 200 && worldCaptureDone) {
                LegacyWorldVisuals.smokeFixture=false;
                for (ClientModule module : ClientRuntime.MODULES) module.setEnabled(false);
                LogUtils.getLogger().info("XDOLF_SMOKE_WORLD_OK: singleplayer loaded and visual modules ran for 150 ticks");
                phase = 4; frames = 0; mc.setScreen(new ClientScreen());
            }
        }
    }
    static void frame() {
        if (!ACTIVE) return;
        frames++;
        if (phase == 4 && frames == 15) {
            phase = 5;
            // GUI commands are deferred until after Screen.render; capture the completed
            // framebuffer on the next tick, not while this screen is still submitting it.
            guiCaptureRequested=true;
            return;
        }
        if (phase == 5 && captureDone) {
            try(var files=java.nio.file.Files.list(java.nio.file.Path.of("screenshots"))) {
                if(files.noneMatch(p->p.toString().endsWith(".png")))throw new IllegalStateException("Screenshot was not saved");
            } catch(java.io.IOException error) { throw new IllegalStateException("Screenshot was not saved",error); }
            LogUtils.getLogger().info("XDOLF_SMOKE_OK: original GUI controls and singleplayer world passed");
            Minecraft.getInstance().stop();
            return;
        }
        if(frames != 5) return;
        Minecraft mc = Minecraft.getInstance();
        if (phase == 1) {
            LogUtils.getLogger().info("XDOLF_SMOKE_MENU_OK");
            phase = 2;
            mc.execute(() -> CreateWorldScreen.openFresh(mc, () -> { throw new IllegalStateException("World creation cancelled"); }));
        } else if (phase == 4) {
            ClientScreen.smokeCheckAndArrange();
        }
    }
}
