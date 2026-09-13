package com.x0xp.xdolf.module.render;

import com.google.gson.JsonObject;
import com.x0xp.xdolf.module.ClientModule;
import com.x0xp.xdolf.rpc.DiscordPresence;
import com.x0xp.xdolf.settings.BooleanSetting;
import com.x0xp.xdolf.settings.TextSetting;
import net.minecraft.client.Minecraft;

public final class DiscordRPCModule extends ClientModule {
    private static final String APPLICATION_ID = "1548709727276376184";
    private static final String LOGO_ASSET = "xdolf_logo";
    public final TextSetting details = textSetting("details", "Details", "Main line displayed in Discord.", "Playing with Xdolf", 128);
    public final BooleanSetting showServer = booleanSetting("showServer", "Show server", "Publish the multiplayer server address to Discord.", false);
    public final BooleanSetting showTime = booleanSetting("showTime", "Elapsed time", "Show time since presence was enabled.", true);
    private final DiscordPresence presence = new DiscordPresence();
    private long started;

    public DiscordRPCModule() {
        super("DiscordRPC", "Show Xdolf activity and its logo on your Discord profile. Requires the desktop Discord app.", "Render");
        runWithoutWorld(); runWhilePaused(); runDuringFreecam();
    }

    public void tick(Minecraft mc) {
        if (started == 0) started = System.currentTimeMillis() / 1000;
        JsonObject activity = new JsonObject();
        activity.addProperty("details", details.get().isBlank() ? "Playing with Xdolf" : details.get());
        String state = mc.level == null ? "In the menus" : mc.hasSingleplayerServer() ? "Singleplayer" : "Multiplayer";
        if (showServer.on() && mc.getCurrentServer() != null) state = mc.getCurrentServer().ip;
        activity.addProperty("state", state.substring(0, Math.min(128, state.length())));
        if (showTime.on()) {
            JsonObject timestamps = new JsonObject(); timestamps.addProperty("start", started);
            activity.add("timestamps", timestamps);
        }
        JsonObject assets = new JsonObject(); assets.addProperty("large_image", LOGO_ASSET);
        assets.addProperty("large_text", "Xdolf • Minecraft 1.21.10"); activity.add("assets", assets);
        presence.update(APPLICATION_ID, activity.toString());
    }

    public void reset(Minecraft mc) { presence.close(); started = 0; }
}
