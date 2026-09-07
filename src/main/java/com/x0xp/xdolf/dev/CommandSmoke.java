package com.x0xp.xdolf;

import com.x0xp.xdolf.module.player.SpammerModule;
import com.x0xp.xdolf.module.world.XRayModule;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.block.Blocks;
import org.lwjgl.glfw.GLFW;

/** Runs only in the opt-in development client; never connects to an external server. */
final class CommandSmoke {
    private static void check(boolean ok,String message) {if(!ok)throw new IllegalStateException(message);}

    static void run(Minecraft mc) {
        mc.setScreen(null);
        check(!Commands.execute("ordinary chat"),"Ordinary chat intercepted");
        check(Commands.execute(".invalid_command"),"Unknown dot command leaked");
        Commands.execute(".bind add AutoSprint RCONTROL");
        check(ClientRuntime.find("Sprint").key==GLFW.GLFW_KEY_RIGHT_CONTROL,"Original bind or label alias failed");
        ClientRuntime.handleKey(GLFW.GLFW_KEY_RIGHT_CONTROL,GLFW.GLFW_PRESS);
        check(ClientRuntime.find("Sprint").enabled(),"Bound key did not toggle");
        ClientRuntime.handleKey(GLFW.GLFW_KEY_RIGHT_CONTROL,GLFW.GLFW_REPEAT);
        check(ClientRuntime.find("Sprint").enabled(),"Key repeat toggled twice");
        Commands.execute(".bind del RCONTROL");
        check(ClientRuntime.find("Sprint").key==-1,"Original unbind failed");
        Commands.execute(".bind add GUI F7");ClientRuntime.handleKey(GLFW.GLFW_KEY_F7,GLFW.GLFW_PRESS);
        check(mc.screen instanceof ClientScreen,"GUI key cannot be rebound");mc.setScreen(null);
        Commands.execute(".bind add GUI GRAVE");
        Commands.execute(".bind add Fullbright NUMPAD1");
        Commands.execute(".macro add F8 .toggle Fullbright");
        Commands.execute(".macro add F8 .toggle NoHurtcam");
        ClientRuntime.handleKey(GLFW.GLFW_KEY_F8,GLFW.GLFW_PRESS);
        check(ClientRuntime.find("Fullbright").enabled()&&ClientRuntime.find("NoHurtCam").enabled(),"Shared-key macros failed");
        Commands.execute(".timer 2");check(ClientRuntime.find("Timer").numberSetting("speed").get()==2,"Timer command failed");
        Commands.execute(".spam mode antispam");Commands.execute(".spam delay 1800");Commands.execute(".spam msg Mixed Case: test");
        check(((SpammerModule)ClientRuntime.find("Spammer")).message.get().equals("Mixed Case: test"),"Message content changed");
        Commands.execute(".xray add stone");check(XRayModule.visible(Blocks.STONE.defaultBlockState()),"Xray add failed");
        Commands.execute(".xray del stone");check(!XRayModule.visible(Blocks.STONE.defaultBlockState()),"Xray del failed");
        Commands.execute(".waypoint add TestHome");check(Commands.waypoints.size()==1,"Waypoint add failed");
        Commands.save();Commands.load();
        check(Commands.macros.size()==2&&Commands.waypoints.getFirst().name().equals("TestHome"),"Macro/waypoint reload failed");
        Commands.execute(".friend add TestFriend");check(SocialState.isFriend("TestFriend"),"Friend add failed");
        Commands.execute(".friend del TestFriend");check(!SocialState.isFriend("TestFriend"),"Friend del failed");
        Commands.execute(".rotate 90 15");check(mc.player.getYRot()==90&&mc.player.getXRot()==15,"Rotate failed");
        Commands.execute(".view off");Commands.execute(".info "+mc.player.getName().getString());
        Commands.execute(".deathcoords");Commands.execute(".impersonate chat Test Local only");
        Commands.execute(".alloff");check(ClientRuntime.MODULES.stream().noneMatch(ClientModule::enabled),"Alloff failed");
        for(String name:new String[]{"Fullbright","Tracers","XRay","Freecam","Spammer"})ClientRuntime.find(name).setEnabled(true);
        ClientConfig.save(ClientRuntime.MODULES);
        for(var m:ClientRuntime.MODULES)m.restoreEnabled(false);
        ClientConfig.load(ClientRuntime.MODULES);
        check(ClientRuntime.find("Fullbright").enabled()&&ClientRuntime.find("Tracers").enabled()&&ClientRuntime.find("XRay").enabled(),"Enabled-state reload failed");
        check(!ClientRuntime.find("Freecam").enabled()&&!ClientRuntime.find("Spammer").enabled(),"Original startup exceptions lost");
        check(ClientRuntime.find("Fullbright").key==GLFW.GLFW_KEY_KP_1,"Key did not persist");
        ClientRuntime.find("Freecam").setEnabled(true);ClientRuntime.find("Spammer").setEnabled(true);
        com.mojang.logging.LogUtils.getLogger().info("XDOLF_COMMANDS_OK: original syntax, key dispatch, macros, commands and persisted state");
    }

    static void assertSelections() {
        for(String name:new String[]{"Fullbright","Tracers","XRay","Freecam","Spammer"})
            check(ClientRuntime.find(name).enabled(),name+" disabled across disconnect/join");
    }
}
