package com.x0xp.xdolf.module.combat;

import com.x0xp.xdolf.*;
import com.x0xp.xdolf.module.support.*;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

public final class AutoLogModule extends ClientModule {
    private final NumberSetting health = setting("health", 6, 1, 19, 1);

    public AutoLogModule() {
        super("AutoLog", "Disconnect at the configured health threshold.", "Combat");
    }

    @Override
    public void tick(Minecraft mc) {
        if (mc.player.isAlive() && mc.player.getHealth() <= health.get()) {
            setEnabled(false);
            mc.getConnection().getConnection().disconnect(Component.literal("Xdolf AutoLog: low health"));
        }
    }
}
