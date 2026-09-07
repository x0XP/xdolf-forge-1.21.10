package com.x0xp.xdolf;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;

final class CrystalLogModule extends ClientModule {
    private final NumberSetting range = setting("range", 2, 1, 10, 1);

    CrystalLogModule() {
        super("CrystalLog", "Disconnect when an end crystal is within the configured range.", "Combat");
    }

    @Override
    public void tick(Minecraft mc) {
        if (!mc.level.getEntitiesOfClass(EndCrystal.class, mc.player.getBoundingBox().inflate(range.get()),
            crystal -> crystal.distanceToSqr(mc.player) <= range.get() * range.get()).isEmpty()) {
            setEnabled(false);
            mc.getConnection().getConnection().disconnect(Component.literal("Xdolf CrystalLog: nearby crystal"));
        }
    }
}
