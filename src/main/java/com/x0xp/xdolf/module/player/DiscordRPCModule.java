package com.x0xp.xdolf.module.player;

import com.x0xp.xdolf.module.ClientModule;
import com.x0xp.xdolf.rpc.DiscordPresence;
import net.minecraft.client.Minecraft;

/** Publishes Xdolf Rich Presence through the local Discord desktop client. */
public final class DiscordRPCModule extends ClientModule {
    public DiscordRPCModule() {
        super("DiscordRPC", "Shows Xdolf as your Discord Rich Presence while the client is enabled.", "Player");
        enableByDefault();
        runWithoutWorld();
    }

    @Override
    public void tick(Minecraft mc) { }

    @Override
    public void activate(Minecraft mc) {
        DiscordPresence.start();
    }

    @Override
    public void reset(Minecraft mc) {
        DiscordPresence.stop();
    }
}
