package com.x0xp.xdolf;

import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.phys.Vec3;
import java.util.List;

final class MovementModules {
    static Vec3 direction(Minecraft mc, double speed) {
        double forward = (mc.options.keyUp.isDown() ? 1 : 0) - (mc.options.keyDown.isDown() ? 1 : 0);
        double strafe = (mc.options.keyLeft.isDown() ? 1 : 0) - (mc.options.keyRight.isDown() ? 1 : 0);
        double length = Math.hypot(forward, strafe);
        if (length == 0) return Vec3.ZERO;
        double yaw = Math.toRadians(mc.player.getYRot());
        return new Vec3((strafe * Math.cos(yaw) - forward * Math.sin(yaw)) * speed / length,
            0, (forward * Math.cos(yaw) + strafe * Math.sin(yaw)) * speed / length);
    }

    static double vertical(Minecraft mc, double speed) {
        return ((mc.options.keyJump.isDown() ? 1 : 0) - (mc.options.keyShift.isDown() ? 1 : 0)) * speed;
    }

    static void addTo(List<ClientModule> modules) {
        modules.add(new ClientModule("Flight", "Controlled flight; servers may reject this movement.", "Player") {
            final NumberSetting speed = setting("speed", 1, 0.1, 10, 0.05);
            { conflictsWith("ElytraFly", "ElytraPlus"); }
            public void tick(Minecraft mc) {
                if (!mc.player.isPassenger()) mc.player.setDeltaMovement(direction(mc, speed.get()).add(0, vertical(mc, speed.get()), 0));
            }
        });
        modules.add(new ClientModule("ElytraFly", "Control horizontal and vertical speed while gliding.", "Player") {
            final NumberSetting speed = setting("speed", 1.41, 0.1, 1.45, 0.01);
            { conflictsWith("Flight", "ElytraPlus"); }
            public void tick(Minecraft mc) {
                if (mc.player.isFallFlying()) mc.player.setDeltaMovement(direction(mc, speed.get()).add(0, vertical(mc, speed.get()), 0));
            }
        });
        modules.add(new ClientModule("ElytraPlus", "Boost gliding; hold jump in the air to request takeoff.", "Player") {
            final NumberSetting boost = setting("boost", 0.05, 0.01, 0.3, 0.01);
            final BooleanSetting takeoff = booleanSetting("takeoff", "Instant fly - easy takeoff",
                "Request elytra takeoff while jump is held in the air.", true);
            final BooleanSetting stopWater = booleanSetting("stopwater", "Stop in water",
                "Pause the elytra boost while touching water.", false);
            { conflictsWith("Flight", "ElytraFly"); }
            int delay;
            public void tick(Minecraft mc) {
                if (stopWater.on() && mc.player.isInWater()) return;
                if (delay > 0) delay--;
                if (mc.player.isFallFlying()) {
                    mc.player.setDeltaMovement(mc.player.getDeltaMovement().add(direction(mc, boost.get()))
                        .add(0, vertical(mc, boost.get()), 0));
                } else if (takeoff.on() && !mc.player.onGround() && !mc.player.isInWater()
                    && mc.options.keyJump.isDown() && delay == 0) {
                    mc.player.connection.send(new ServerboundPlayerCommandPacket(mc.player, ServerboundPlayerCommandPacket.Action.START_FALL_FLYING));
                    delay = 20;
                }
            }
            public void reset(Minecraft mc) { delay = 0; }
        });
        modules.add(new ClientModule("EntitySpeed", "Change the speed of the vehicle you control.", "Player") {
            final NumberSetting speed = setting("speed", 3, 0.1, 3.86, 0.01);
            public void tick(Minecraft mc) {
                var vehicle = mc.player.getVehicle();
                if (vehicle != null && vehicle.getControllingPassenger() == mc.player) {
                    var move = direction(mc, speed.get());
                    vehicle.setDeltaMovement(move.x, vehicle.getDeltaMovement().y, move.z);
                }
            }
        });
        modules.add(new ClientModule("EntityStep", "Increase step height for a controlled living mount.", "Player") {
            final NumberSetting height = setting("height", 2, 1, 256, 1);
            final ResourceLocation id = ResourceLocation.fromNamespaceAndPath(Xdolf.ID, "entity_step");
            LivingEntity owner;
            public void tick(Minecraft mc) {
                var vehicle = mc.player.getVehicle();
                if (!(vehicle instanceof LivingEntity living) || vehicle.getControllingPassenger() != mc.player) { reset(mc); return; }
                if (owner != living) { reset(mc); owner = living; }
                var attribute = owner.getAttribute(Attributes.STEP_HEIGHT);
                if (attribute == null) return;
                attribute.removeModifier(id);
                attribute.addTransientModifier(new AttributeModifier(id, Math.max(0, height.get() - attribute.getValue()), AttributeModifier.Operation.ADD_VALUE));
            }
            public void reset(Minecraft mc) {
                if (owner != null && owner.getAttribute(Attributes.STEP_HEIGHT) != null) owner.getAttribute(Attributes.STEP_HEIGHT).removeModifier(id);
                owner = null;
            }
        });
        modules.add(new ClientModule("NoFall", "Send grounded status while falling; server-dependent.", "Player") {
            int delay;
            public void tick(Minecraft mc) {
                if (delay > 0) delay--;
                if (delay == 0 && mc.player.fallDistance > 2 && !mc.player.isFallFlying() && !mc.player.isPassenger()) {
                    mc.player.connection.send(new ServerboundMovePlayerPacket.StatusOnly(true, mc.player.horizontalCollision));
                    delay = 5;
                }
            }
            public void reset(Minecraft mc) { delay = 0; }
        });
    }
}
