package com.x0xp.xdolf.module.player;

import com.x0xp.xdolf.module.support.MovementMath;
import com.x0xp.xdolf.settings.BooleanSetting;
import com.x0xp.xdolf.settings.NumberSetting;

import com.x0xp.xdolf.module.ClientModule;


import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;

public final class ElytraPlusModule extends ClientModule {
    private final NumberSetting boost = setting("boost", 0.05, 0.01, 0.3, 0.01);
    private final BooleanSetting takeoff = booleanSetting("takeoff", "Instant fly - easy takeoff",
        "Request elytra takeoff while jump is held in the air.", true);
    private final BooleanSetting stopWater = booleanSetting("stopwater", "Stop in water",
        "Pause the elytra boost while touching water.", false);
    private int delay;

    public ElytraPlusModule() {
        super("ElytraPlus", "Boost gliding; hold jump in the air to request takeoff.", "Player");
        conflictsWith("Flight", "ElytraFly");
    }

    @Override
    public void tick(Minecraft mc) {
        if (stopWater.on() && mc.player.isInWater()) return;
        if (delay > 0) delay--;
        if (mc.player.isFallFlying()) {
            mc.player.setDeltaMovement(mc.player.getDeltaMovement().add(MovementMath.direction(mc, boost.get()))
                .add(0, MovementMath.vertical(mc, boost.get()), 0));
        } else if (takeoff.on() && !mc.player.onGround() && !mc.player.isInWater()
            && mc.options.keyJump.isDown() && delay == 0) {
            mc.player.connection.send(new ServerboundPlayerCommandPacket(mc.player,
                ServerboundPlayerCommandPacket.Action.START_FALL_FLYING));
            delay = 20;
        }
    }

    @Override
    public void reset(Minecraft mc) {
        delay = 0;
    }
}
