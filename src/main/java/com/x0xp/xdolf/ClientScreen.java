package com.x0xp.xdolf;

import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraftforge.fml.loading.FMLPaths;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Properties;

/** Xdolf click GUI with draggable windows, module rows, options and value sliders. */
public final class ClientScreen extends Screen {
    private static final List<Panel> PANELS = new ArrayList<>();
    private static boolean loaded;
    private Panel dragging;
    private Slider sliding;
    private double offsetX, offsetY;

    private static final class Panel {
        final String title;
        final List<ClientModule> modules = new ArrayList<>();
        final List<Slider> sliders = new ArrayList<>();
        final List<Option> options = new ArrayList<>();
        int x, y;
        boolean open, pinned, temporary;
        Panel(String title, int y) { this.title = title; this.x = 2; this.y = y; }
        boolean text() { return title.equals("Info") || title.equals("Radar"); }
        float height() { return text() ? open ? lines(this).size() * 10 + 16 : 14
            : 13 + (open ? modules.size() * 12 + options.size() * 12 + sliders.size() * 19 + (sliders.isEmpty() ? 0.5f : 3) : 0); }
    }

    private record Option(String label, ModuleSetting setting) {}
    private record Slider(String label, ModuleSetting setting, boolean integer) {}

    ClientScreen() { super(Component.literal("Xdolf")); setup(); }

    private static void setup() {
        if (loaded) return;
        loaded = true;
        addModules("Player", 47, "AutoFish Flight Spammer AutoRespawn AutoWalk SafeWalk NoSlowdown HorseJump Sprint NoFall AntiHunger AutoEat Jesus EntitySpeed EntityStep ElytraFly ElytraPlus");
        addModules("Render", 62, "Tracers StorageESP EntityESP NoHurtCam Chams Trajectories Nametags Waypoints LogoutSpot");
        var values = new Panel("Values", 2); PANELS.add(values);
        slider(values, "Flight Speed", "Flight", "speed", false);
        slider(values, "ElytraFlight Speed", "ElytraFly", "speed", false);
        slider(values, "Entity Speed", "EntitySpeed", "speed", false);
        slider(values, "Entity Step", "EntityStep", "height", true);
        slider(values, "Aura Range", "KillAura", "range", false);
        slider(values, "Crystal Speed", "CrystalAura", "speed", true);
        slider(values, "Crystal Range", "CrystalAura", "range", false);
        slider(values, "AutoLog Threshold", "AutoLog", "health", true);
        slider(values, "CrystalLog distance", "CrystalLog", "range", true);
        slider(values, "AutoEat Threshold", "AutoEat", "hunger", true);
        slider(values, "Mine Speed", "Speedmine", "progress", false);
        slider(values, "Auto Cast Delay", "AutoFish", "castdelay", true);
        slider(values, "Recast Delay", "AutoFish", "recast", true);
        PANELS.add(new Panel("Info", 17)); PANELS.add(new Panel("Radar", 92));
        addModules("Combat", 32, "AntiVelocity KillAura AutoArmor AutoTotem AutoLog CrystalAura Criticals CrystalLog");
        addModules("World", 77, "Fullbright Timer XRay FastPlace Freecam Speedmine");
        load();
    }

    private static void addModules(String title, int y, String names) {
        var panel = new Panel(title, y);
        for (String name : names.split(" ")) {
            var module = ClientRuntime.find(name);
            if (module == null) throw new IllegalStateException("Missing GUI module: " + name);
            panel.modules.add(module);
        }
        PANELS.add(panel);
    }

    private static void slider(Panel panel, String label, String module, String setting, boolean integer) {
        var value = ClientRuntime.find(module).setting(setting);
        if (value == null) throw new IllegalStateException("Missing GUI setting: " + module + "." + setting);
        panel.sliders.add(new Slider(label, value, integer));
    }

    private static List<Option> options(ClientModule module) {
        String[][] names = switch(module.name) {
            case "KillAura" -> new String[][] {{"Players", "players"}, {"Mobs", "mobs"}, {"Hit Through Walls", "walls"}, {"Can Be Seen", "seen"}};
            case "Tracers" -> new String[][] {{"Players", "players"}, {"Chests", "chests"}};
            case "EntityESP" -> new String[][] {{"Players", "players"}, {"Monsters", "monsters"}, {"Passive", "passive"}, {"Items", "items"}, {"Outline", "outline"}};
            case "ElytraPlus" -> new String[][] {{"Instant fly - easy takeoff", "takeoff"}, {"Stop in water", "stopwater"}};
            default -> new String[0][];
        };
        var result = new ArrayList<Option>();
        for (var pair : names) result.add(new Option(pair[0], module.setting(pair[1])));
        return result;
    }

    static String label(ClientModule m) {
        return switch(m.name) {
            case "Sprint" -> "AutoSprint";
            case "NoHurtCam" -> "NoHurtcam";
            case "XRay" -> "Xray";
            case "Speedmine" -> "SpeedMine";
            default -> m.name;
        };
    }

    @Override public void render(GuiGraphics g, int mx, int my, float partial) {
        g.fill(0, 0, width, height, 0x8F000000);
        for (var p : PANELS) draw(g, p, mx, my, true);
        ClientSmoke.frame();
    }

    static void renderPinned(GuiGraphics g) {
        setup();
        if (Minecraft.getInstance().screen instanceof ClientScreen) return;
        for (var p : PANELS) if (p.pinned) draw(g, p, -1000, -1000, false);
    }

    private static void rect(GuiGraphics g, float x, float y, float right, float bottom, int color) {
        g.pose().pushMatrix(); g.pose().scale(0.5f, 0.5f);
        g.fill(Math.round(x * 2), Math.round(y * 2), Math.round(right * 2), Math.round(bottom * 2), color);
        g.pose().popMatrix();
    }

    private static void border(GuiGraphics g, float x, float y, float right, float bottom, int inside) {
        rect(g, x, y, right, bottom, inside);
        rect(g, x, y, right, y + 0.5f, 0xFF000000); rect(g, x, bottom, right, bottom + 0.5f, 0xFF000000);
        rect(g, x, y, x + 0.5f, bottom + 0.5f, 0xFF000000); rect(g, right, y, right + 0.5f, bottom + 0.5f, 0xFF000000);
    }

    private static boolean hit(double mx, double my, double x, double y, double w, double h) {
        return mx >= x && my >= y && mx <= x + w && my <= y + h;
    }

    private static void row(GuiGraphics g, String name, int x, int y, boolean enabled, boolean hover, boolean plus) {
        int color = enabled ? hover ? 0xFF44AAFF : 0xFFFFFFFF : hover ? 0xFF888888 : 0x99FFFFFF;
        rect(g, x + 95, y, x + 96, y + 12, enabled ? hover ? 0xFF44AAFF : 0xFFFF0000 : hover ? 0xFF888888 : 0x0033363D);
        XdolfFont.draw(g, name, x + 48 - XdolfFont.width(name) / 2, y, color);
        if (plus) XdolfFont.draw(g, "+", x + 90, y, enabled && hover ? 0xFF44AAFF : hover ? 0xFF888888 : 0xFFFFFFFF);
    }

    private static void draw(GuiGraphics g, Panel p, int mx, int my, boolean controls) {
        border(g, p.x, p.y, p.x + 100, p.y + p.height(), 0x80000000);
        XdolfFont.draw(g, p.title, p.x + 3, p.y + 1, 0xFFFFFFFF);
        if (controls) {
            border(g, p.x + 79, p.y + 2, p.x + 88, p.y + 11, p.pinned ? 0xFFFF0000 : 0xFF383B42);
            border(g, p.x + 89, p.y + 2, p.x + 98, p.y + 11, p.open ? 0xFFFF0000 : 0xFF383B42);
        }
        if (!p.open) return;

        for (int i = 0; i < p.modules.size(); i++) {
            var module = p.modules.get(i); int y = p.y + 12 + i * 12;
            row(g, label(module), p.x + 2, y, module.enabled(), hit(mx,my,p.x+2,y,96,11), !options(module).isEmpty());
        }
        for (int i = 0; i < p.options.size(); i++) {
            var option = p.options.get(i); int y = p.y + 12 + i * 12;
            row(g, option.label, p.x + 2, y, option.setting.on(), hit(mx,my,p.x+2,y,96,11), false);
        }
        for (int i = 0; i < p.sliders.size(); i++) {
            var slider = p.sliders.get(i); int y = p.y + 16 + i * 19, x = p.x + 2;
            String value = String.format(Locale.ROOT, slider.integer ? "%.0f" : "%.2f", slider.setting.get());
            XdolfFont.draw(g, slider.label + ": " + value, x + 1, y - 3, 0xFFFFFFFF);
            float drag = (float)((slider.setting.get() - slider.setting.min) / (slider.setting.max - slider.setting.min) * 90);
            border(g,x,y+9,x+96,y+17,0xFF383B42);
            border(g,x+1,y+10,x+5+(int)drag,y+16,0xFFFF0000);
            border(g,x+2+(int)drag,y+10,x+5+(int)drag,y+16,0xFFFF4C4C);
        }
        if (p.text()) {
            var textLines = lines(p);
            for (int i = 0; i < textLines.size(); i++)
                XdolfFont.draw(g, XdolfFont.trim(textLines.get(i),97), p.x+3,p.y+13+i*10,0xFFFFFFFF);
        }
    }

    private static List<? extends Player> radar() {
        var mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return List.of();
        return mc.level.players().stream().filter(p -> p != mc.player && p.isAlive())
            .sorted(Comparator.comparingDouble(p -> p.distanceToSqr(mc.player))).toList();
    }

    private static List<String> lines(Panel panel) {
        var mc = Minecraft.getInstance(); var player = mc.player;
        if (panel.title.equals("Radar")) {
            var players = radar();
            if (players.isEmpty()) return List.of("No players in range.");
            return players.stream().map(p -> (SocialState.isFriend(p.getName().getString()) ? "\u00a7a" : "\u00a7c")
                + p.getName().getString() + "\u00a7f: " + (int)p.distanceTo(player)).toList();
        }
        if (player == null) return List.of("No world loaded.");
        var direction = player.getDirection();
        String axis = switch(direction) { case NORTH -> "-Z"; case SOUTH -> "+Z"; case WEST -> "-X"; case EAST -> "+X"; default -> "Invalid"; };
        boolean nether = mc.level.dimension().equals(Level.NETHER);
        return List.of(mc.getFps()+" FPS", "X: "+coord(player.getX())+(nether ? " ["+coord(player.getX()*8)+"]" : ""),
            "Y: "+coord(player.getY()), "Z: "+coord(player.getZ())+(nether ? " ["+coord(player.getZ()*8)+"]" : ""),
            "Facing: "+direction.toString().toUpperCase(Locale.ROOT)+" ["+axis+"]",
            String.format(Locale.ROOT,"Yaw: %.1f Pitch: %.1f",Mth.wrapDegrees(player.getYRot()),Mth.wrapDegrees(player.getXRot())));
    }

    private static String coord(double value) { return String.format(Locale.ROOT,"%,.0f",Math.floor(value)); }

    @Override public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) { return click(event.x(),event.y(),event.button()); }

    private boolean click(double mx,double my,int button) {
        for (int index = PANELS.size()-1; index >= 0; index--) {
            var p = PANELS.get(index);
            if (!hit(mx,my,p.x,p.y,100,p.height())) continue;
            PANELS.remove(index); PANELS.add(p);
            if (hit(mx,my,p.x+89,p.y+2,9,9)) p.open = !p.open;
            else if (hit(mx,my,p.x+79,p.y+2,9,9)) p.pinned = !p.pinned;
            else if (hit(mx,my,p.x,p.y,79,11)) { dragging=p;offsetX=mx-p.x;offsetY=my-p.y; }
            else if (p.open) {
                for (int i=0;i<p.modules.size();i++) if (hit(mx,my,p.x+2,p.y+12+i*12,96,11)) {
                    var module=p.modules.get(i);
                    if (button==0) ClientRuntime.toggle(module);
                    else if (!options(module).isEmpty()) {
                        boolean close=PANELS.stream().anyMatch(w->w.temporary&&w.title.equals(label(module)));
                        PANELS.removeIf(w->w.temporary);
                        if (!close) {
                            var optionPanel=new Panel(label(module),0);
                            optionPanel.x=0;optionPanel.open=true;optionPanel.temporary=true;
                            optionPanel.options.addAll(options(module));PANELS.add(optionPanel);
                        }
                    }
                    return true;
                }
                for (int i=0;i<p.options.size();i++) if (hit(mx,my,p.x+2,p.y+12+i*12,96,11)) {
                    var setting=p.options.get(i).setting;setting.set(setting.on()?0:1);return true;
                }
                for (int i=0;i<p.sliders.size();i++) if (button==0 && hit(mx,my,p.x+2,p.y+25+i*19,96,8)) {
                    sliding=p.sliders.get(i);offsetX=p.x+2;moveSlider(mx);return true;
                }
                if (p.title.equals("Radar")) {
                    int row=(int)((my-p.y-13)/10); var players=radar();
                    if (my>=p.y+13&&row>=0&&row<players.size()) {
                        String name=players.get(row).getName().getString();
                        SocialState.command(new String[]{"friend",SocialState.isFriend(name)?"remove":"add",name});
                    }
                }
            }
            return true;
        }
        return false;
    }

    @Override public boolean mouseDragged(MouseButtonEvent event,double dx,double dy) {
        if (dragging!=null) { dragging.x=(int)(event.x()-offsetX);dragging.y=(int)(event.y()-offsetY);return true; }
        if (sliding!=null) { moveSlider(event.x());return true; }
        return false;
    }

    private void moveSlider(double mx) {
        var s=sliding.setting;
        double fraction=Math.max(0,Math.min(1,(mx-offsetX)/90));
        double value=s.min+fraction*(s.max-s.min);
        if(sliding.integer)value=Math.floor(value);
        s.set(Math.max(s.min,Math.min(s.max,value)));
    }

    @Override public boolean mouseReleased(MouseButtonEvent event) {
        dragging=null;sliding=null;save();ClientConfig.save(ClientRuntime.MODULES);return true;
    }
    @Override public void removed() { dragging=null;sliding=null;save();ClientConfig.save(ClientRuntime.MODULES); }
    @Override public boolean isPauseScreen() { return false; }

    static void smokeCheckAndArrange() {
        var screen = (ClientScreen)Minecraft.getInstance().screen;
        if(PANELS.size()!=7) throw new IllegalStateException("Expected seven GUI windows");
        var render=PANELS.stream().filter(p->p.title.equals("Render")).findFirst().orElseThrow();
        var combat=PANELS.stream().filter(p->p.title.equals("Combat")).findFirst().orElseThrow();
        if(render.modules.stream().noneMatch(m->m.name.equals("Waypoints")) || render.modules.stream().noneMatch(m->m.name.equals("LogoutSpot"))
            || combat.modules.stream().noneMatch(m->m.name.equals("AutoTotem")))
            throw new IllegalStateException("Restored modules missing from click GUI");
        var player=PANELS.stream().filter(p->p.title.equals("Player")).findFirst().orElseThrow();
        screen.click(player.x+94,player.y+6,0);
        if(!player.open) throw new IllegalStateException("Open control failed");
        screen.click(player.x+84,player.y+6,0);
        if(!player.pinned) throw new IllegalStateException("Pin control failed");
        screen.click(player.x+10,player.y+5,0);
        if(screen.dragging!=player) throw new IllegalStateException("Title drag failed");
        screen.mouseDragged(new MouseButtonEvent(player.x+30,player.y+25,new net.minecraft.client.input.MouseButtonInfo(0,0)),20,20);
        if(player.x!=22||player.y!=67)throw new IllegalStateException("Panel did not follow drag");
        screen.mouseReleased(new MouseButtonEvent(player.x+10,player.y+5,new net.minecraft.client.input.MouseButtonInfo(0,0)));
        if(screen.dragging!=null)throw new IllegalStateException("Panel drag did not stop");
        for(var p:PANELS) {
            p.open=true;p.pinned=false;
            switch(p.title) {
                case "Values" -> {p.x=2;p.y=2;}
                case "Player" -> {p.x=106;p.y=2;}
                case "Render" -> {p.x=210;p.y=2;}
                case "Combat" -> {p.x=314;p.y=2;}
                case "World" -> {p.x=314;p.y=105;}
                case "Info" -> {p.x=418;p.y=2;}
                case "Radar" -> {p.x=418;p.y=85;}
            }
        }
        screen.click(combat.x+30,combat.y+12+12+5,1);
        var options=PANELS.get(PANELS.size()-1);
        if(!options.temporary||options.options.size()!=4)throw new IllegalStateException("KillAura options failed");
        options.x=418;options.y=125;
        var setting=options.options.get(0).setting;boolean before=setting.on();
        screen.click(options.x+30,options.y+17,0);
        if(setting.on()==before)throw new IllegalStateException("Option toggle failed");
        setting.set(before?1:0);
        var values=PANELS.stream().filter(p->p.title.equals("Values")).findFirst().orElseThrow();
        if(values.sliders.size()!=13)throw new IllegalStateException("Expected thirteen sliders");
        double old=values.sliders.get(0).setting.get();
        screen.click(values.x+48,values.y+28,0);
        if(screen.sliding==null)throw new IllegalStateException("Slider drag failed");
        screen.moveSlider(values.x+92);
        if(values.sliders.get(0).setting.get()==old)throw new IllegalStateException("Slider did not change value");
        values.sliders.get(0).setting.set(old);screen.sliding=null;
        save();int savedX=player.x;player.x+=100;player.open=false;load();
        if(player.x!=savedX||!player.open)throw new IllegalStateException("Window state roundtrip failed");
        LogUtils.getLogger().info("XDOLF_GUI_OK: seven windows, restored modules, thirteen sliders, pin/open/drag/options controls");
    }

    private static void load() {
        var file=FMLPaths.CONFIGDIR.get().resolve("xdolf-gui.properties");
        if (!Files.isRegularFile(file)) return;
        try(var reader=Files.newBufferedReader(file)) {
            var props=new Properties();props.load(reader);
            for(var p:PANELS) {
                if(p.temporary)continue;
                try {
                    p.x=Integer.parseInt(props.getProperty(p.title+".x","2"));
                    p.y=Integer.parseInt(props.getProperty(p.title+".y",Integer.toString(p.y)));
                } catch(NumberFormatException ignored) {}
                p.open=Boolean.parseBoolean(props.getProperty(p.title+".open"));
                p.pinned=Boolean.parseBoolean(props.getProperty(p.title+".pinned"));
            }
        } catch(java.io.IOException error) {
            LogUtils.getLogger().warn("Could not load Xdolf GUI",error);
        }
    }

    private static void save() {
        var file=FMLPaths.CONFIGDIR.get().resolve("xdolf-gui.properties");
        var props=new Properties();
        for(var p:PANELS) if(!p.temporary) {
            props.setProperty(p.title+".x",""+p.x);props.setProperty(p.title+".y",""+p.y);
            props.setProperty(p.title+".open",""+p.open);props.setProperty(p.title+".pinned",""+p.pinned);
        }
        try {
            Files.createDirectories(file.getParent());
            try(var writer=Files.newBufferedWriter(file)) {props.store(writer,"Xdolf click GUI window state");}
        } catch(java.io.IOException error) {
            LogUtils.getLogger().warn("Could not save Xdolf GUI",error);
        }
    }
}
