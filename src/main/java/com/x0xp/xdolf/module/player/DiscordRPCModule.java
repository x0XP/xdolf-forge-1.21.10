package com.x0xp.xdolf.module.player;

import com.x0xp.xdolf.module.ClientModule;
import com.x0xp.xdolf.rpc.DiscordPresence;
import com.x0xp.xdolf.settings.BooleanSetting;
import com.x0xp.xdolf.settings.TextSetting;
import net.minecraft.client.Minecraft;

/** Publishes Xdolf Rich Presence through the local Discord desktop client. */
public final class DiscordRPCModule extends ClientModule {
    public final TextSetting details;
    public final BooleanSetting showServer;
    public final BooleanSetting showTime;
    private long startedAt;

    public DiscordRPCModule() {
        super("DiscordRPC", "Show Xdolf activity and its logo on your Discord profile. Requires the desktop Discord app.", "Player");
        details = textSetting("details", "Details", "Main line displayed in Discord.", "Playing with Xdolf", 128);
        showServer = booleanSetting("showServer", "Show server", "Publish the multiplayer server address to Discord.", false);
        showTime = booleanSetting("showTime", "Elapsed time", "Show time since presence was enabled.", true);
        enableByDefault();
        runWithoutWorld();
    }

    @Override
    public void tick(Minecraft mc) {
        refreshPresence(mc);
    }

    @Override
    public void activate(Minecraft mc) {
        if (startedAt == 0L) startedAt = System.currentTimeMillis() / 1000L;
        refreshPresence(mc);
        DiscordPresence.start();
    }

    @Override
    public void reset(Minecraft mc) {
        DiscordPresence.stop();
        startedAt = 0L;
    }

    private void refreshPresence(Minecraft mc) {
        if (startedAt == 0L) startedAt = System.currentTimeMillis() / 1000L;

        String detailText = details.get().isBlank() ? "Playing with Xdolf" : details.get();
        String state;
        if (mc.level == null) state = "In the menus";
        else if (mc.hasSingleplayerServer()) state = "Singleplayer";
        else state = "Multiplayer";

        if (showServer.on() && mc.getCurrentServer() != null)
            state = mc.getCurrentServer().ip;

        if (state.length() > 128) state = state.substring(0, 128);
        DiscordPresence.update(detailText, state, showTime.on() ? startedAt : 0L);
    }
}
