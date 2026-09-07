package com.x0xp.xdolf.dev;

import com.x0xp.xdolf.command.Commands;
import com.x0xp.xdolf.core.ClientRuntime;
import com.x0xp.xdolf.core.Hooks;
import com.x0xp.xdolf.render.WorldVisuals;
import com.x0xp.xdolf.ui.XdolfFont;
import com.x0xp.xdolf.ui.clickgui.ClientScreen;
import com.x0xp.xdolf.module.ClientModule;
import com.x0xp.xdolf.module.world.XRayModule;
import com.x0xp.xdolf.mixin.accessor.CreateWorldScreenAccess;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;

/** Explicit opt-in CI test. Normal launches never create a test world. */
public final class ClientSmoke {
    private static final boolean ACTIVE = Boolean.getBoolean("xdolf.smokeTest");
    private static int phase, frames, ticks, lowResTicks;
    private static volatile boolean captureDone;
    private static volatile boolean lowResCaptureDone;
    private static volatile boolean xrayCaptureDone;
    private static volatile boolean worldCaptureDone;
    private static boolean guiCaptureRequested;
    private static boolean lowResCaptureRequested;
    private static boolean worldFixtureStarted;
    private static BlockPos xrayFixtureBase;

    private static void placeXrayFixture(Level level, BlockPos base) {
        // Four solid stone layers completely occlude the targets in normal rendering.
        for (int x = -5; x <= 5; x++) {
            for (int y = 0; y <= 5; y++) {
                for (int z = 3; z <= 6; z++) {
                    level.setBlock(base.offset(x, y, z), Blocks.STONE.defaultBlockState(), 3);
                }
            }
        }
        // Stagger the coal and iron columns in depth so the shader frame exposes side faces
        // as well as front faces, making each ore texture visually identifiable under XRay.
        for (int y = 1; y <= 3; y++) {
            for (int i = 0; i < 3; i++) {
                level.setBlock(base.offset(-4 + i, y, 7 + i), Blocks.COAL_ORE.defaultBlockState(), 3);
                level.setBlock(base.offset(2 + i, y, 9 - i), Blocks.IRON_ORE.defaultBlockState(), 3);
            }
        }
    }

    public static void tick(Minecraft mc) {
        if (!ACTIVE || mc.getOverlay() != null) return;
        if(guiCaptureRequested) {
            guiCaptureRequested=false;
            net.minecraft.client.Screenshot.grab(new java.io.File("."),mc.getMainRenderTarget(),message -> captureDone=true);
        }
        if(lowResCaptureRequested) {
            lowResCaptureRequested=false;
            net.minecraft.client.Screenshot.grab(new java.io.File("."),mc.getMainRenderTarget(),message -> lowResCaptureDone=true);
        }

        // Once the low-raster ChatScreen is open, progression must not depend on
        // ClientScreen.render(). Tick from here so any normal Minecraft screen can be tested.
        if(phase==6) {
            if(++lowResTicks>=10) {
                lowResCaptureRequested=true;
                phase=7;
            }
            return;
        }
        if(phase==7&&lowResCaptureDone) {
            finishLowResTest(mc);
            return;
        }

        if (phase == 0 && mc.screen != null && (mc.screen instanceof TitleScreen || mc.screen.getClass().getSimpleName().equals("AccessibilityOnboardingScreen"))) {
            phase = 1;
            mc.options.guiScale().set(2);
            org.lwjgl.glfw.GLFW.glfwSetWindowSize(org.lwjgl.glfw.GLFW.glfwGetCurrentContext(), 1280, 800);
            mc.options.renderDistance().set(3);
            mc.options.simulationDistance().set(5);
            mc.setScreen(new ClientScreen());
        } else if (phase == 2 && mc.screen instanceof CreateWorldScreen create) {
            create.getUiState().setName("Xdolf automated smoke");
            create.getUiState().setGameMode(WorldCreationUiState.SelectedGameMode.CREATIVE);
            phase = 3;
            ((CreateWorldScreenAccess) create).xdolf$create();
        } else if (phase == 3 && mc.player != null && mc.level != null && mc.screen == null) {
            if (++ticks == 20) {
                if (!Blocks.STONE.defaultBlockState().isSolidRender())
                    throw new IllegalStateException("Stone was non-solid before XRay activation");
                xrayFixtureBase = mc.player.blockPosition();
                placeXrayFixture(mc.level, xrayFixtureBase);
                var server = mc.getSingleplayerServer();
                if (server != null) {
                    BlockPos serverBase = xrayFixtureBase;
                    server.execute(() -> placeXrayFixture(server.overworld(), serverBase));
                }
            }
            if (ticks == 24) {
                // Face directly through the four-block stone wall toward the ore clusters.
                mc.player.setYRot(0);
                mc.player.setXRot(0);
            }
            if (ticks == 30) {
                for (String name : new String[] {"Fullbright", "NoHurtCam", "Chams", "XRay", "EntityESP", "StorageESP", "Nametags", "Tracers", "Trajectories"}) ClientRuntime.find(name).setEnabled(true);
            }
            if (ticks == 40) {
                if (!XRayModule.rendering) throw new IllegalStateException("XRay did not activate its render hook");
                if (Blocks.STONE.defaultBlockState().isSolidRender())
                    throw new IllegalStateException("XRay solidity hook did not make terrain non-solid");
                if (!XRayModule.visible(Blocks.COAL_ORE.defaultBlockState()))
                    throw new IllegalStateException("XRay target selection rejected coal ore");
                if (!XRayModule.visible(Blocks.DEEPSLATE_COAL_ORE.defaultBlockState()))
                    throw new IllegalStateException("XRay target selection rejected deepslate coal ore");
                if (!XRayModule.visible(Blocks.IRON_ORE.defaultBlockState()))
                    throw new IllegalStateException("XRay target selection rejected iron ore");
                if (!XRayModule.visible(Blocks.DEEPSLATE_IRON_ORE.defaultBlockState()))
                    throw new IllegalStateException("XRay target selection rejected deepslate iron ore");
                if (!XRayModule.visible(Blocks.ANCIENT_DEBRIS.defaultBlockState()))
                    throw new IllegalStateException("XRay target selection rejected ancient debris");
                if (xrayFixtureBase == null
                    || !mc.level.getBlockState(xrayFixtureBase.offset(0, 2, 4)).is(Blocks.STONE)
                    || !mc.level.getBlockState(xrayFixtureBase.offset(-3, 2, 8)).is(Blocks.COAL_ORE)
                    || !mc.level.getBlockState(xrayFixtureBase.offset(3, 2, 8)).is(Blocks.IRON_ORE))
                    throw new IllegalStateException("Coal/iron XRay fixture was not present behind the stone wall");
                LogUtils.getLogger().info("XDOLF_XRAY_OK: dev.16 OptiFine-safe solidity hook active; coal and iron selected behind four stone layers");
            }
            if (ticks == 150) {
                // Remove unrelated overlays from the proof frame while leaving Fullbright/XRay on.
                for (String name : new String[] {"NoHurtCam", "Chams", "EntityESP", "StorageESP", "Nametags", "Tracers", "Trajectories"})
                    ClientRuntime.find(name).setEnabled(false);
            }
            if (ticks == 160) {
                net.minecraft.client.Screenshot.grab(new java.io.File("."), mc.getMainRenderTarget(), message -> xrayCaptureDone=true);
            }
            if (ticks >= 180 && xrayCaptureDone && !worldFixtureStarted) {
                for (String name : new String[] {"NoHurtCam", "Chams", "EntityESP", "StorageESP", "Nametags", "Tracers", "Trajectories"})
                    ClientRuntime.find(name).setEnabled(true);
                worldFixtureStarted = true;
                WorldVisuals.smokeFixture=true;
                LogUtils.getLogger().info("XDOLF_XRAY_CAPTURE_OK: dev.16 clean coal/iron XRay shader frame captured before synthetic world visuals");
            }
            if (ticks == 200 && !worldFixtureStarted)
                throw new IllegalStateException("Dev.16 XRay screenshot did not complete before world visual smoke");
            if (ticks == 280) {
                WorldVisuals.assertSmokeRendered();
                net.minecraft.client.Screenshot.grab(new java.io.File("."), mc.getMainRenderTarget(), message -> worldCaptureDone=true);
            }
            if (ticks >= 300 && worldCaptureDone) {
                WorldVisuals.smokeFixture=false;
                for (ClientModule module : ClientRuntime.MODULES) module.setEnabled(false);
                if (XRayModule.rendering) throw new IllegalStateException("XRay render hook remained active after disable");
                if (!Blocks.STONE.defaultBlockState().isSolidRender())
                    throw new IllegalStateException("Stone solidity was not restored after XRay disable");
                LogUtils.getLogger().info("XDOLF_XRAY_RESTORE_OK: dev.16 restored normal solidity after XRay disable");
                ClientRuntime.find("Fullbright").setEnabled(true);
                LogUtils.getLogger().info("XDOLF_SMOKE_WORLD_OK: singleplayer loaded and visual modules ran through the extended XRay proof");
                mc.player.setXRot(-45);
                phase = 4; frames = 0; mc.setScreen(new ClientScreen());
            }
        }
    }

    public static void frame() {
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
            // Exercise the scale-1 font atlas directly. Mutating Minecraft's live GUI-scale option
            // from inside the render loop can stall the framebuffer in CI and is not representative
            // of starting the game normally at a low GUI scale.
            XdolfFont.smokeRasterScale(1);
            mc.gui.getChat().addMessage(Component.literal("[Xdolf] Low-resolution TTF smoke test: lorem ipsum 0123456789"));
            mc.setScreen(new ChatScreen("lorem ipsum",false));
            phase=6;
            lowResTicks=0;
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

    private static void finishLowResTest(Minecraft mc) {
        try(var files=java.nio.file.Files.list(java.nio.file.Path.of("screenshots"))) {
            long count=files.filter(p->p.toString().endsWith(".png")).count();
            if(count<4)throw new IllegalStateException("Expected XRay, world, dev.16 GUI and low-raster chat screenshots");
        } catch(java.io.IOException error) {
            throw new IllegalStateException("Screenshots were not saved",error);
        }

        LogUtils.getLogger().info("XDOLF_LOW_RES_FONT_OK: rendered chat with the GUI-scale-1 TTF atlas");
        XdolfFont.smokeRasterScale(0);
        ClientRuntime.find("Fullbright").setEnabled(false);
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
        LogUtils.getLogger().info("XDOLF_SMOKE_OK: dev.16 GUI, low-resolution TTF, screen-active modules, world rendering, commands, binds, persistence and lifecycle passed");
        phase=9;
        mc.stop();
    }
}
