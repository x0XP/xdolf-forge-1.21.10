package com.x0xp.xdolf.module.registry;

import com.x0xp.xdolf.module.combat.AntiVelocityModule;
import com.x0xp.xdolf.module.combat.AutoArmorModule;
import com.x0xp.xdolf.module.combat.AutoLogModule;
import com.x0xp.xdolf.module.combat.AutoTotemModule;
import com.x0xp.xdolf.module.combat.CriticalsModule;
import com.x0xp.xdolf.module.combat.CrystalAuraModule;
import com.x0xp.xdolf.module.combat.CrystalLogModule;
import com.x0xp.xdolf.module.combat.KillAuraModule;
import com.x0xp.xdolf.module.player.AnnouncerModule;
import com.x0xp.xdolf.module.player.AntiHungerModule;
import com.x0xp.xdolf.module.player.AutoEatModule;
import com.x0xp.xdolf.module.player.AutoFishModule;
import com.x0xp.xdolf.module.player.AutoRespawnModule;
import com.x0xp.xdolf.module.player.AutoWalkModule;
import com.x0xp.xdolf.module.player.ElytraFlyModule;
import com.x0xp.xdolf.module.player.ElytraPlusModule;
import com.x0xp.xdolf.module.player.EntitySpeedModule;
import com.x0xp.xdolf.module.player.EntityStepModule;
import com.x0xp.xdolf.module.player.FlightModule;
import com.x0xp.xdolf.module.player.HorseJumpModule;
import com.x0xp.xdolf.module.player.JesusModule;
import com.x0xp.xdolf.module.player.NoFallModule;
import com.x0xp.xdolf.module.player.NoSlowdownModule;
import com.x0xp.xdolf.module.player.SafeWalkModule;
import com.x0xp.xdolf.module.player.SpammerModule;
import com.x0xp.xdolf.module.player.SprintModule;
import com.x0xp.xdolf.module.render.ChamsModule;
import com.x0xp.xdolf.module.render.EntityESPModule;
import com.x0xp.xdolf.module.render.LogoutSpotModule;
import com.x0xp.xdolf.module.render.NametagsModule;
import com.x0xp.xdolf.module.render.NoHurtCamModule;
import com.x0xp.xdolf.module.render.StorageESPModule;
import com.x0xp.xdolf.module.render.TracersModule;
import com.x0xp.xdolf.module.render.TrajectoriesModule;
import com.x0xp.xdolf.module.render.WaypointsModule;
import com.x0xp.xdolf.module.world.FastPlaceModule;
import com.x0xp.xdolf.module.world.FreecamModule;
import com.x0xp.xdolf.module.world.FullbrightModule;
import com.x0xp.xdolf.module.world.SpeedMineModule;
import com.x0xp.xdolf.module.world.TimerModule;
import com.x0xp.xdolf.module.world.XRayModule;

import com.x0xp.xdolf.module.ClientModule;


import org.lwjgl.glfw.GLFW;
import java.util.ArrayList;
import java.util.List;

/** Central module registry. Implementations are kept one-class-per-file in their GUI category. */
public final class Modules {
    private Modules() {}

    public static List<ClientModule> create() {
        var modules = new ArrayList<ClientModule>();
        registerPlayer(modules);
        registerRender(modules);
        registerCombat(modules);
        registerWorld(modules);
        restoreOriginalDefaultKeys(modules);
        return List.copyOf(modules);
    }

    private static void registerPlayer(List<ClientModule> modules) {
        modules.add(new AutoFishModule());
        modules.add(new FlightModule());
        modules.add(new SpammerModule());
        modules.add(new AnnouncerModule());
        modules.add(new AutoRespawnModule());
        modules.add(new AutoWalkModule());
        modules.add(new SafeWalkModule());
        modules.add(new NoSlowdownModule());
        modules.add(new HorseJumpModule());
        modules.add(new SprintModule());
        modules.add(new NoFallModule());
        modules.add(new AntiHungerModule());
        modules.add(new AutoEatModule());
        modules.add(new JesusModule());
        modules.add(new EntitySpeedModule());
        modules.add(new EntityStepModule());
        modules.add(new ElytraFlyModule());
        modules.add(new ElytraPlusModule());
    }

    private static void registerRender(List<ClientModule> modules) {
        modules.add(new TracersModule());
        modules.add(new StorageESPModule());
        modules.add(new EntityESPModule());
        modules.add(new NoHurtCamModule());
        modules.add(new ChamsModule());
        modules.add(new TrajectoriesModule());
        modules.add(new NametagsModule());
        modules.add(new WaypointsModule());
        modules.add(new LogoutSpotModule());
    }

    private static void registerCombat(List<ClientModule> modules) {
        modules.add(new AntiVelocityModule());
        modules.add(new KillAuraModule());
        modules.add(new AutoArmorModule());
        modules.add(new AutoTotemModule());
        modules.add(new AutoLogModule());
        modules.add(new CrystalAuraModule());
        modules.add(new CriticalsModule());
        modules.add(new CrystalLogModule());
    }

    private static void registerWorld(List<ClientModule> modules) {
        modules.add(new FullbrightModule());
        modules.add(new TimerModule());
        modules.add(new XRayModule());
        modules.add(new FastPlaceModule());
        modules.add(new FreecamModule());
        modules.add(new SpeedMineModule());
    }

    /** Original 1.12.2 defaults translated from LWJGL 2 key names to GLFW codes. */
    private static void restoreOriginalDefaultKeys(List<ClientModule> modules) {
        setKey(modules, "Flight", GLFW.GLFW_KEY_V);
        setKey(modules, "SafeWalk", GLFW.GLFW_KEY_F4);
        setKey(modules, "Jesus", GLFW.GLFW_KEY_J);
        setKey(modules, "AntiVelocity", GLFW.GLFW_KEY_L);
        setKey(modules, "KillAura", GLFW.GLFW_KEY_R);
        setKey(modules, "Trajectories", GLFW.GLFW_KEY_F6);
        setKey(modules, "Fullbright", GLFW.GLFW_KEY_C);
        setKey(modules, "Freecam", GLFW.GLFW_KEY_B);
        setKey(modules, "XRay", GLFW.GLFW_KEY_X);
        setKey(modules, "Waypoints", GLFW.GLFW_KEY_EQUAL);
    }

    private static void setKey(List<ClientModule> modules, String name, int key) {
        modules.stream().filter(module -> module.name.equals(name)).findFirst().orElseThrow().key = key;
    }
}
