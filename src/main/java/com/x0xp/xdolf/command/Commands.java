package com.x0xp.xdolf.command;

import com.x0xp.xdolf.chat.ChatFormatter;
import com.x0xp.xdolf.core.ClientRuntime;
import com.x0xp.xdolf.core.OriginalQuotes;
import com.x0xp.xdolf.social.SocialState;
import com.x0xp.xdolf.ui.clickgui.ClientScreen;
import com.x0xp.xdolf.ui.hud.Hud;

import com.x0xp.xdolf.settings.ClientConfig;
import com.x0xp.xdolf.settings.KeyNames;

import com.x0xp.xdolf.module.ClientModule;

import com.x0xp.xdolf.module.player.SpammerModule;
import com.x0xp.xdolf.module.world.XRayModule;

import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.Musics;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraftforge.fml.loading.FMLPaths;
import java.nio.file.Files;
import java.util.*;

/** Xdolf dot-command parser and persistent command data. */
public final class Commands {
    public record Macro(int key,String command) {}
    public record Waypoint(String name,String dimension,int x,int y,int z) {}

    public static final List<Macro> macros=new ArrayList<>();
    public static final List<Waypoint> waypoints=new ArrayList<>();
    public static boolean showLogo=true;
    public static double deathX,deathY,deathZ;
    private static boolean wasDead;
    private static int macroDepth;
    private static final Map<String,String> SYNTAX=new LinkedHashMap<>();

    public static {
        String[] values={"help","toggle <name of hack>","timer <speed>","alloff","say <message>","modlist",
            "spam <mode/msg/delay> <args>","rotate <yaw> <pitch>","view <name/off>","bind add <hack> <key>, bind del <key>",
            "friend add <name> [alias], friend del <name>, friend list/clear","impersonate <chat/whisper> <name> <msg>",
            "xray add/del <block name>","waypoint add/del <name>, waypoint clear","praiseore","deathcoords","info <player>",
            "music","hide <logo/mods/potions>","follow <player>","macro add <key> <command>, macro del <key>","vclip <height>"};
        for(String syntax:values)SYNTAX.put(syntax.split(" ")[0],syntax);
    }

    public static boolean execute(String text) {
        if(!text.startsWith("."))return false;
        String body=text.substring(1).trim();
        String[] p=body.isEmpty()?new String[]{""}:body.split("\\s+");
        String cmd=p[0].toLowerCase(Locale.ROOT);
        var mc=Minecraft.getInstance();
        try {
            switch(cmd) {
                case "help" -> {
                    SYNTAX.values().forEach(s->say(ChatFormatter.colourArguments("."+s)));
                    say(ChatFormatter.colourArguments("Aliases: .gui, .mods, .t, .set <mod> <setting> <value>, .bind <mod> <key/NONE>"));
                }
                case "gui" -> mc.execute(()->mc.setScreen(new ClientScreen()));
                case "toggle","t" -> {
                    need(p,2);
                    if(p[1].equalsIgnoreCase("GUI")){mc.execute(()->mc.setScreen(new ClientScreen()));break;}
                    ClientRuntime.toggle(module(p[1]));
                }
                case "alloff" -> {
                    int count=0;
                    for(var m:ClientRuntime.MODULES)if(m.enabled()){m.setEnabled(false);count++;}
                    say(count+(count==1?" hack":" hacks")+" turned off.");
                }
                case "mods","modlist" -> {
                    ClientRuntime.MODULES.forEach(m->say(ChatFormatter.colourArguments(ClientScreen.label(m))+" - "+m.description));
                    say("GUI - Open the click GUI.");
                }
                case "timer" -> {
                    need(p,2);
                    module("Timer").numberSetting("speed").set(number(p[1]));
                    ClientConfig.save(ClientRuntime.MODULES);
                    say("Timer updated.");
                }
                case "bind" -> bind(p);
                case "set" -> ClientRuntime.configure(p);
                case "friend" -> SocialState.command(p);
                case "say" -> {need(p,2);send(body.substring(body.indexOf(' ')+1));}
                case "spam" -> spam(body,p);
                case "rotate" -> {need(p,3);player();mc.player.setYRot((float)number(p[1]));mc.player.setXRot((float)number(p[2]));}
                case "view" -> {
                    need(p,2);player();
                    mc.setCameraEntity(p[1].equalsIgnoreCase("off")?mc.player:target(p[1]));
                    if(p[1].equalsIgnoreCase("off"))say("Now viewing normally.");
                }
                case "impersonate" -> {
                    need(p,4);player();
                    String msg=body.split("\\s+",4)[3];
                    String value;
                    if(p[1].equalsIgnoreCase("chat"))value="<"+p[2]+"> "+msg;
                    else if(p[1].equalsIgnoreCase("whisper"))value="\u00a7d"+p[2]+" whispers: "+msg;
                    else throw new IllegalArgumentException();
                    mc.player.displayClientMessage(ChatFormatter.parse(value),false);
                }
                case "xray" -> xray(p);
                case "waypoint" -> waypoint(p);
                case "praiseore" -> send(OriginalQuotes.VALUES[new Random().nextInt(OriginalQuotes.VALUES.length)]);
                case "deathcoords" -> say("You died at X: "+deathX+", Y: "+deathY+", Z: "+deathZ);
                case "info" -> {need(p,2);info(target(p[1]));}
                case "music" -> {mc.getMusicManager().startPlaying(new net.minecraft.client.sounds.MusicInfo(Musics.GAME));say("Music started.");}
                case "hide" -> {
                    need(p,2);
                    switch(p[1].toLowerCase(Locale.ROOT)) {
                        case "logo" -> showLogo=!showLogo;
                        case "mods" -> Hud.showModules=!Hud.showModules;
                        case "potions" -> Hud.showPotions=!Hud.showPotions;
                        default -> throw new IllegalArgumentException();
                    }
                }
                case "follow" -> {
                    need(p,2);
                    Player target=target(p[1]);var me=mc.player;
                    double x=target.getX()-me.getX(),z=target.getZ()-me.getZ();
                    double y=target.getY()+target.getEyeHeight()/1.4-me.getY()+me.getEyeHeight()/1.4;
                    me.setYRot((float)(Math.toDegrees(Math.atan2(z,x))-90));
                    me.setXRot((float)-Math.toDegrees(Math.atan2(y,Math.sqrt(x*x+z*z))));
                    ClientRuntime.toggle(module("AutoWalk"));
                }
                case "macro" -> macro(body,p);
                case "vclip" -> {
                    need(p,2);player();
                    int y=Integer.parseInt(p[1]);
                    var e=mc.player.getVehicle()==null?mc.player:mc.player.getVehicle();
                    e.setPos(e.getX(),e.getY()+y,e.getZ());
                }
                default -> say("Invalid command. Type .help for a list of commands.");
            }
        } catch(IllegalArgumentException e) {
            say("Usage: ."+ChatFormatter.colourArguments(SYNTAX.getOrDefault(cmd,cmd)));
            if(e.getMessage()!=null)say(e.getMessage());
        } catch(RuntimeException e) {
            LogUtils.getLogger().error("Xdolf command failed: {}",cmd,e);
            say("Command failed; check latest.log.");
            if(Boolean.getBoolean("xdolf.smokeTest"))throw e;
        }
        return true;
    }

    private static void need(String[] p,int count) {if(p.length<count)throw new IllegalArgumentException();}
    private static double number(String s) {double d=Double.parseDouble(s);if(!Double.isFinite(d))throw new IllegalArgumentException("Enter a finite number.");return d;}
    private static void player() {if(Minecraft.getInstance().player==null)throw new IllegalArgumentException("Join a world first.");}
    private static ClientModule module(String name) {var m=ClientRuntime.find(name);if(m==null)throw new IllegalArgumentException("Invalid mod.");return m;}
    private static Player target(String name) {
        player();
        return Minecraft.getInstance().level.players().stream()
            .filter(p->p.getName().getString().equalsIgnoreCase(name)).findFirst()
            .orElseThrow(()->new IllegalArgumentException("Player not loaded: "+name));
    }
    private static void say(String s) {ClientRuntime.message(s);}
    private static void send(String s) {
        player();
        if(s.length()>256)throw new IllegalArgumentException("Minecraft allows at most 256 chat characters.");
        if(s.startsWith("/"))Minecraft.getInstance().player.connection.sendCommand(s.substring(1));
        else Minecraft.getInstance().player.connection.sendChat(s);
    }

    private static void bind(String[] p) {
        need(p,3);
        if(p[1].equalsIgnoreCase("del")) {
            int key=KeyNames.parse(p[2]);
            if(key<0)throw new IllegalArgumentException("Invalid key.");
            for(var m:ClientRuntime.MODULES)if(m.key==key){m.key=-1;ClientConfig.save(ClientRuntime.MODULES);say("Unbound: "+p[2].toUpperCase(Locale.ROOT));return;}
            if(ClientConfig.guiKey==key){ClientConfig.guiKey=-1;ClientConfig.save(ClientRuntime.MODULES);say("Unbound: "+p[2].toUpperCase(Locale.ROOT));}
            return;
        }
        int index=p[1].equalsIgnoreCase("add")?2:1;
        need(p,index+2);
        int key=KeyNames.parse(p[index+1]);
        if(index==2&&key<0)throw new IllegalArgumentException("Invalid key.");
        String name=p[index];
        if(name.equalsIgnoreCase("GUI"))ClientConfig.guiKey=key;else module(name).key=key;
        ClientConfig.save(ClientRuntime.MODULES);
        say(name+" bound to: "+KeyNames.name(key));
    }

    private static void macro(String body,String[] p) {
        need(p,3);
        int key=KeyNames.parse(p[2]);
        if(key<0)throw new IllegalArgumentException("Invalid key.");
        if(p[1].equalsIgnoreCase("add")) {
            need(p,4);
            String value=body.split("\\s+",4)[3];
            macros.add(new Macro(key,value));
            say("Added \""+value+"\" on key: "+KeyNames.name(key));
        } else if(p[1].equalsIgnoreCase("del")) {
            for(int i=0;i<macros.size();i++)if(macros.get(i).key==key){macros.remove(i);say("Removed macro on key: "+KeyNames.name(key));break;}
        } else throw new IllegalArgumentException();
        save();
    }

    public static void runMacros(int key) {
        if(macroDepth>0)return;
        macroDepth++;
        try {for(var m:List.copyOf(macros))if(m.key==key)execute(m.command);}
        finally {macroDepth--;}
    }

    private static void spam(String body,String[] p) {
        need(p,2);
        SpammerModule spammer=(SpammerModule)ClientRuntime.find("Spammer");
        String response;
        switch(p[1].toLowerCase(Locale.ROOT)) {
            case "mode" -> {
                need(p,3);
                if(!Set.of("normal","antispam").contains(p[2].toLowerCase(Locale.ROOT)))throw new IllegalArgumentException("Use normal or antispam.");
                spammer.mode.set(p[2].toLowerCase(Locale.ROOT));
                response="Spam mode changed to \u00a7e"+spammer.mode.get();
            }
            case "delay" -> {
                need(p,3);int delay=Integer.parseInt(p[2]);
                if(delay<1)throw new IllegalArgumentException("Delay must be positive milliseconds.");
                spammer.delay.set(delay);response="Spam delay changed to \u00a7e"+spammer.delay.display();
            }
            case "msg" -> {need(p,3);spammer.message.set(body.split("\\s+",3)[2]);response="Spam message changed to \u00a7e"+spammer.message.get();}
            default -> {spammer.message.set(body.substring(body.indexOf(' ')+1));response="Spam message changed to \u00a7e"+spammer.message.get();}
        }
        spammer.reset(Minecraft.getInstance());
        ClientConfig.save(ClientRuntime.MODULES);
        say(response);
    }

    private static void xray(String[] p) {
        need(p,3);
        var id=ResourceLocation.tryParse(p[2]);
        if(id==null||!BuiltInRegistries.BLOCK.containsKey(id)){say("\u00a7e"+p[2]+"\u00a7f is not a recognized block.");return;}
        boolean add=p[1].equalsIgnoreCase("add");
        if(!add&&!p[1].equalsIgnoreCase("del"))throw new IllegalArgumentException();
        XRayModule.edit(id.toString(),add);
        save();
        say((add?"Added ":"Removed ")+"\u00a7e"+p[2]+"\u00a7f "+(add?"to":"from")+" xray list.");
    }

    private static void waypoint(String[] p) {
        need(p,2);
        if(p[1].equalsIgnoreCase("clear")){waypoints.clear();save();say("Cleared waypoints.");return;}
        need(p,3);
        if(p[1].equalsIgnoreCase("add")) {
            player();
            if(waypoints.stream().anyMatch(w->w.name.equals(p[2])))throw new IllegalArgumentException("Waypoint already exists.");
            var mc=Minecraft.getInstance();
            waypoints.add(new Waypoint(p[2],mc.level.dimension().location().toString(),(int)mc.player.getX(),(int)mc.player.getY(),(int)mc.player.getZ()));
            say("Added waypoint \""+p[2]+"\"");
        } else if(p[1].equalsIgnoreCase("del")) {
            boolean removed=waypoints.removeIf(w->w.name.equals(p[2]));
            say(removed?"Removed waypoint: "+p[2]:p[2]+" does not exist.");
        } else throw new IllegalArgumentException();
        save();
    }

    private static void info(Player p) {
        say("Username: "+p.getName().getString());
        say("Health: "+(int)(p.getHealth()/20*100)+"%");
        say("Distance: "+p.distanceTo(Minecraft.getInstance().player));
        for(var slot:List.of(EquipmentSlot.HEAD,EquipmentSlot.CHEST,EquipmentSlot.LEGS,EquipmentSlot.FEET,EquipmentSlot.MAINHAND)) {
            var item=p.getItemBySlot(slot);
            var enchants=item.getOrDefault(DataComponents.ENCHANTMENTS,ItemEnchantments.EMPTY);
            if(enchants.isEmpty())continue;
            var entries=new ArrayList<String>();entries.add(item.getHoverName().getString());
            for(var entry:enchants.entrySet())entries.add(Enchantment.getFullname(entry.getKey(),entry.getIntValue()).getString());
            say("\u00a7f"+entries);
        }
    }

    public static void recordDeath(Minecraft mc) {
        if(mc.player==null)return;
        boolean dead=mc.player.isDeadOrDying();
        if(dead&&!wasDead){deathX=mc.player.getX();deathY=mc.player.getY();deathZ=mc.player.getZ();}
        wasDead=dead;
    }

    public static void worldChanged(Minecraft mc) {
        wasDead=false;
        if(mc.player!=null)mc.setCameraEntity(mc.player);
    }

    public static void load() {
        var path=FMLPaths.CONFIGDIR.get().resolve("xdolf-commands.properties");
        if(!Files.isRegularFile(path))return;
        var p=new Properties();
        try(var r=Files.newBufferedReader(path)){p.load(r);}
        catch(Exception e){LogUtils.getLogger().warn("Could not load Xdolf commands",e);return;}
        macros.clear();waypoints.clear();
        for(int i=0;i<10000;i++) {
            String command=p.getProperty("macro."+i+".command");if(command==null)break;
            int key=KeyNames.read(p.getProperty("macro."+i+".key"),-1);
            if(key>=0)macros.add(new Macro(key,command));
        }
        for(int i=0;i<10000;i++) {
            String name=p.getProperty("waypoint."+i+".name");if(name==null)break;
            try {
                String b="waypoint."+i;
                waypoints.add(new Waypoint(name,p.getProperty(b+".dimension","minecraft:overworld"),Integer.parseInt(p.getProperty(b+".x")),Integer.parseInt(p.getProperty(b+".y")),Integer.parseInt(p.getProperty(b+".z"))));
            } catch(RuntimeException ignored) {}
        }
        XRayModule.loadSelection(p);
    }

    public static void save() {
        var p=new Properties();
        for(int i=0;i<macros.size();i++) {
            var m=macros.get(i);
            p.setProperty("macro."+i+".key",Integer.toString(m.key));
            p.setProperty("macro."+i+".command",m.command);
        }
        for(int i=0;i<waypoints.size();i++) {
            var w=waypoints.get(i);String b="waypoint."+i;
            p.setProperty(b+".name",w.name);p.setProperty(b+".dimension",w.dimension);
            p.setProperty(b+".x",Integer.toString(w.x));p.setProperty(b+".y",Integer.toString(w.y));p.setProperty(b+".z",Integer.toString(w.z));
        }
        XRayModule.saveSelection(p);
        var path=FMLPaths.CONFIGDIR.get().resolve("xdolf-commands.properties");
        try {
            Files.createDirectories(path.getParent());
            var temp=path.resolveSibling(path.getFileName()+".tmp");
            try(var w=Files.newBufferedWriter(temp)){p.store(w,"Xdolf command configuration");}
            Files.move(temp,path,java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        } catch(Exception e) {
            LogUtils.getLogger().warn("Could not save Xdolf commands",e);
            say("Could not save command configuration; check latest.log.");
        }
    }
}
