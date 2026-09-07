package com.x0xp.xdolf;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import org.lwjgl.glfw.GLFW;
import java.util.List;
import java.util.ArrayList;

final class Modules {
    static List<ClientModule> create() {
        var modules = new ArrayList<ClientModule>(List.of(
            new ClientModule("Sprint", "Sprint while moving forward and able to sprint.", "Player") {
                private LocalPlayer owner;
                private boolean applied;
                public void tick(Minecraft mc) {
                    var player = mc.player;
                    boolean eligible = (mc.options.keyUp.isDown() || Hooks.active("AutoWalk")) && !player.isShiftKeyDown()
                        && !player.horizontalCollision && !player.isUsingItem()
                        && (player.getFoodData().getFoodLevel() > 6 || player.getAbilities().mayfly);
                    if (eligible && !player.isSprinting()) {
                        owner = player;
                        applied = true;
                        player.setSprinting(true);
                    } else if (!eligible) reset(mc);
                }
                public void reset(Minecraft mc) {
                    if (applied && owner != null) owner.setSprinting(false);
                    applied = false;
                    owner = null;
                }
            },
            new ClientModule("AutoWalk", "Hold forward until disabled or a screen opens.", "Player") {
                public void tick(Minecraft mc) {}
            },
            new ClientModule("AutoRespawn", "Request respawn once after each death.", "Player") {
                private LocalPlayer lastDeath;
                public void tick(Minecraft mc) {
                    if (mc.player.isDeadOrDying()) {
                        if (lastDeath != mc.player) {
                            lastDeath = mc.player;
                            mc.player.respawn();
                        }
                    } else lastDeath = null;
                }
                public void reset(Minecraft mc) { lastDeath = null; }
            },
            new ClientModule("AutoLog", "Disconnect at the configured health threshold.", "Combat") {
                final NumberSetting health = setting("health", 6, 1, 19, 1);
                public void tick(Minecraft mc) {
                    if (mc.player.isAlive() && mc.player.getHealth() <= health.get()) {
                        setEnabled(false);
                        mc.getConnection().getConnection().disconnect(Component.literal("Xdolf AutoLog: low health"));
                    }
                }
            },
            new ClientModule("CrystalLog", "Disconnect when an end crystal is within the configured range.", "Combat") {
                final NumberSetting range = setting("range", 2, 1, 10, 1);
                public void tick(Minecraft mc) {
                    if (!mc.level.getEntitiesOfClass(EndCrystal.class, mc.player.getBoundingBox().inflate(range.get()),
                        crystal -> crystal.distanceToSqr(mc.player) <= range.get() * range.get()).isEmpty()) {
                        setEnabled(false);
                        mc.getConnection().getConnection().disconnect(Component.literal("Xdolf CrystalLog: nearby crystal"));
                    }
                }
            }
        ));
        MovementModules.addTo(modules);
        CombatModules.addTo(modules);
        InventoryModules.addTo(modules);
        modules.add(new AutoFishModule());
        HookModules.addTo(modules);
        RenderOverlays.addTo(modules);
        NetworkModules.addTo(modules);
        modules.add(new FreecamModule());
        modules.add(new XRayModule());
        restoreOriginalDefaultKeys(modules);
        return List.copyOf(modules);
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
