package com.darkcart.xdolf;

import net.minecraft.client.Minecraft;
import net.minecraft.world.level.block.Blocks;
import org.lwjgl.glfw.GLFW;

/** Runs only in the opt-in development client; never connects to an external server. */
final class CommandSmoke {
    private static void check(boolean ok,String message) {if(!ok)throw new IllegalStateException(message);}
    static void run(Minecraft mc) {
        mc.setScreen(null);
        check(!LegacyCommands.execute("ordinary chat"),"Ordinary chat intercepted");
        check(LegacyCommands.execute(".invalid_command"),"Unknown dot command leaked");
        LegacyCommands.execute(".bind add AutoSprint RCONTROL");
        check(ClientRuntime.find("Sprint").key==GLFW.GLFW_KEY_RIGHT_CONTROL,"Original bind or label alias failed");
        ClientRuntime.handleKey(GLFW.GLFW_KEY_RIGHT_CONTROL,GLFW.GLFW_PRESS);
        check(ClientRuntime.find("Sprint").enabled(),"Bound key did not toggle");
        ClientRuntime.handleKey(GLFW.GLFW_KEY_RIGHT_CONTROL,GLFW.GLFW_REPEAT);
        check(ClientRuntime.find("Sprint").enabled(),"Key repeat toggled twice");
        LegacyCommands.execute(".bind del RCONTROL");
        check(ClientRuntime.find("Sprint").key==-1,"Original unbind failed");
        LegacyCommands.execute(".bind add GUI F7");ClientRuntime.handleKey(GLFW.GLFW_KEY_F7,GLFW.GLFW_PRESS);
        check(mc.screen instanceof ClientScreen,"GUI key cannot be rebound");mc.setScreen(null);
        LegacyCommands.execute(".bind add GUI GRAVE");
        LegacyCommands.execute(".bind add Fullbright NUMPAD1");
        LegacyCommands.execute(".macro add F8 .toggle Fullbright");
        LegacyCommands.execute(".macro add F8 .toggle NoHurtcam");
        ClientRuntime.handleKey(GLFW.GLFW_KEY_F8,GLFW.GLFW_PRESS);
        check(ClientRuntime.find("Fullbright").enabled()&&ClientRuntime.find("NoHurtCam").enabled(),"Shared-key macros failed");
        LegacyCommands.execute(".timer 2");check(ClientRuntime.find("Timer").setting("speed").get()==2,"Timer command failed");
        LegacyCommands.execute(".spam mode antispam");LegacyCommands.execute(".spam delay 1800");LegacyCommands.execute(".spam msg Mixed Case: test");
        check(NetworkModules.spamMessage.equals("Mixed Case: test"),"Message content changed");
        LegacyCommands.execute(".xray add stone");check(XRayModule.visible(Blocks.STONE.defaultBlockState()),"Xray add failed");
        LegacyCommands.execute(".xray del stone");check(!XRayModule.visible(Blocks.STONE.defaultBlockState()),"Xray del failed");
        LegacyCommands.execute(".waypoint add TestHome");check(LegacyCommands.waypoints.size()==1,"Waypoint add failed");
        LegacyCommands.save();LegacyCommands.load();
        check(LegacyCommands.macros.size()==2&&LegacyCommands.waypoints.getFirst().name().equals("TestHome"),"Macro/waypoint reload failed");
        LegacyCommands.execute(".friend add TestFriend");check(SocialState.isFriend("TestFriend"),"Friend add failed");
        LegacyCommands.execute(".friend del TestFriend");check(!SocialState.isFriend("TestFriend"),"Friend del failed");
        LegacyCommands.execute(".rotate 90 15");check(mc.player.getYRot()==90&&mc.player.getXRot()==15,"Rotate failed");
        LegacyCommands.execute(".view off");LegacyCommands.execute(".info "+mc.player.getName().getString());
        LegacyCommands.execute(".deathcoords");LegacyCommands.execute(".impersonate chat Test Local only");
        LegacyCommands.execute(".alloff");check(ClientRuntime.MODULES.stream().noneMatch(ClientModule::enabled),"Alloff failed");
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
        for(String name:new String[]{"Fullbright","Tracers","XRay","Freecam","Spammer"})check(ClientRuntime.find(name).enabled(),name+" disabled across disconnect/join");
    }
}
