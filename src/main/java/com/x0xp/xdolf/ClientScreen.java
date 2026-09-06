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
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

/** Xdolf click GUI with draggable windows, animated inline module options and value sliders. */
public final class ClientScreen extends Screen {
    private static final List<Panel> PANELS = new ArrayList<>();
    private static final long OPTION_ANIMATION_NS = 135_000_000L;
    private static boolean loaded;
    private Panel dragging;
    private Slider sliding;
    private double offsetX, offsetY;

    private static final class Expansion {
        boolean open;
        float progress;
        float from;
        float target;
        long started;

        float value() {
            if (progress == target) return progress;
            float elapsed = Math.min(1.0f, (System.nanoTime() - started) / (float) OPTION_ANIMATION_NS);
            float eased = 1.0f - (float) Math.pow(1.0f - elapsed, 3.0);
            progress = from + (target - from) * eased;
            if (elapsed >= 1.0f) progress = target;
            return progress;
        }

        void setOpen(boolean value) {
            float current = value();
            open = value;
            from = current;
            target = value ? 1.0f : 0.0f;
            started = System.nanoTime();
        }

        void finish() {
            progress = target;
            from = target;
        }
    }

    private static final class Panel {
        final String title;
        final List<ClientModule> modules = new ArrayList<>();
        final List<Slider> sliders = new ArrayList<>();
        final Map<ClientModule, Expansion> expansions = new HashMap<>();
        int x, y;
        boolean open, pinned;

        Panel(String title, int y) {
            this.title = title;
            this.x = 2;
            this.y = y;
        }

        boolean text() {
            return title.equals("Info") || title.equals("Radar");
        }

        Expansion expansion(ClientModule module) {
            return expansions.computeIfAbsent(module, ignored -> new Expansion());
        }

        void toggleExpansion(ClientModule module) {
            Expansion selected = expansion(module);
            boolean opening = !selected.open;
            if (opening) {
                for (var entry : expansions.entrySet()) {
                    if (entry.getKey() != module && entry.getValue().open) entry.getValue().setOpen(false);
                }
            }
            selected.setOpen(opening);
        }

        float animatedOptionsHeight(ClientModule module) {
            var expansion = expansions.get(module);
            if (expansion == null) return 0.0f;
            int count = options(module).size();
            if (count == 0) return 0.0f;
            return optionContainerHeight(count) * expansion.value();
        }

        float height() {
            if (text()) return open ? lines(this).size() * 10 + 16 : 14;
            if (!open) return 13;
            if (!modules.isEmpty()) {
                float height = 13 + modules.size() * 12 + 0.5f;
                for (var module : modules) height += animatedOptionsHeight(module);
                return height;
            }
            return 13 + sliders.size() * 19 + (sliders.isEmpty() ? 0.5f : 3);
        }
    }

    private record Option(String label, ModuleSetting setting) {}
    private record Slider(String label, ModuleSetting setting, boolean integer) {}

    ClientScreen() {
        super(Component.literal("Xdolf"));
        setup();
    }

    private static void setup() {
        if (loaded) return;
        loaded = true;
        addModules("Player", 47, "AutoFish Flight Spammer AutoRespawn AutoWalk SafeWalk NoSlowdown HorseJump Sprint NoFall AntiHunger AutoEat Jesus EntitySpeed EntityStep ElytraFly ElytraPlus");
        addModules("Render", 62, "Tracers StorageESP EntityESP NoHurtCam Chams Trajectories Nametags Waypoints LogoutSpot");

        var values = new Panel("Values", 2);
        PANELS.add(values);
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

        PANELS.add(new Panel("Info", 17));
        PANELS.add(new Panel("Radar", 92));
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
        String[][] names = switch (module.name) {
            case "KillAura" -> new String[][] {{"Players", "players"}, {"Mobs", "mobs"}, {"Hit Through Walls", "walls"}, {"Can Be Seen", "seen"}};
            case "Tracers" -> new String[][] {{"Players", "players"}, {"Chests", "chests"}};
            case "EntityESP" -> new String[][] {{"Players", "players"}, {"Monsters", "monsters"}, {"Passive", "passive"}, {"Items", "items"}, {"Outline", "outline"}};
            case "LogoutSpot" -> new String[][] {{"Tracers", "tracers"}};
            case "ElytraPlus" -> new String[][] {{"Instant fly - easy takeoff", "takeoff"}, {"Stop in water", "stopwater"}};
            default -> new String[0][];
        };
        var result = new ArrayList<Option>();
        for (var pair : names) result.add(new Option(pair[0], module.setting(pair[1])));
        return result;
    }

    static String label(ClientModule module) {
        return switch (module.name) {
            case "Sprint" -> "AutoSprint";
            case "NoHurtCam" -> "NoHurtcam";
            case "XRay" -> "Xray";
            case "Speedmine" -> "SpeedMine";
            default -> module.name;
        };
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, width, height, 0x8F000000);
        for (var panel : PANELS) draw(graphics, panel, mouseX, mouseY, true);
        ClientSmoke.frame();
    }

    static void renderPinned(GuiGraphics graphics) {
        setup();
        if (Minecraft.getInstance().screen instanceof ClientScreen) return;
        for (var panel : PANELS) if (panel.pinned) draw(graphics, panel, -1000, -1000, false);
    }

    private static void rect(GuiGraphics graphics, float x, float y, float right, float bottom, int color) {
        graphics.pose().pushMatrix();
        graphics.pose().scale(0.5f, 0.5f);
        graphics.fill(Math.round(x * 2), Math.round(y * 2), Math.round(right * 2), Math.round(bottom * 2), color);
        graphics.pose().popMatrix();
    }

    private static void border(GuiGraphics graphics, float x, float y, float right, float bottom, int inside) {
        rect(graphics, x, y, right, bottom, inside);
        rect(graphics, x, y, right, y + 0.5f, 0xFF000000);
        rect(graphics, x, bottom, right, bottom + 0.5f, 0xFF000000);
        rect(graphics, x, y, x + 0.5f, bottom + 0.5f, 0xFF000000);
        rect(graphics, right, y, right + 0.5f, bottom + 0.5f, 0xFF000000);
    }

    private static int fade(int color, float alpha) {
        int originalAlpha = color >>> 24;
        int fadedAlpha = Math.max(0, Math.min(255, Math.round(originalAlpha * alpha)));
        return (color & 0x00FFFFFF) | fadedAlpha << 24;
    }

    private static boolean hit(double mouseX, double mouseY, double x, double y, double width, double height) {
        return mouseX >= x && mouseY >= y && mouseX <= x + width && mouseY <= y + height;
    }

    private static float optionContainerHeight(int optionCount) {
        return optionCount * 12.0f + 4.0f;
    }

    private static void row(GuiGraphics graphics, String name, float x, float y, boolean enabled, boolean hover,
                            boolean configurable, boolean expanded) {
        int color = enabled ? hover ? 0xFF44AAFF : 0xFFFFFFFF : hover ? 0xFF888888 : 0x99FFFFFF;
        rect(graphics, x + 95, y, x + 96, y + 12,
            enabled ? hover ? 0xFF44AAFF : 0xFFFF0000 : hover ? 0xFF888888 : 0x0033363D);
        XdolfFont.draw(graphics, name, x + 48 - XdolfFont.width(name) / 2.0f, y, color);
        if (configurable) {
            XdolfFont.draw(graphics, expanded ? "-" : "+", x + 90, y,
                enabled && hover ? 0xFF44AAFF : hover ? 0xFF888888 : 0xFFFFFFFF);
        }
    }

    private static void optionRow(GuiGraphics graphics, Option option, float x, float y, float right,
                                  boolean hover, float alpha) {
        boolean enabled = option.setting.on();
        int color = enabled ? hover ? 0xFF44AAFF : 0xFFFFFFFF : hover ? 0xFF888888 : 0xB8FFFFFF;
        int stateColor = enabled ? hover ? 0xFF44AAFF : 0xFFFF0000 : hover ? 0xFF888888 : 0xFF454850;
        XdolfFont.draw(graphics, option.label, x, y, fade(color, alpha));
        rect(graphics, right - 2, y + 2, right - 1, y + 10, fade(stateColor, alpha));
    }

    private static void optionContainer(GuiGraphics graphics, Panel panel, ClientModule module, List<Option> moduleOptions,
                                        float top, float progress, int mouseX, int mouseY) {
        if (progress <= 0.001f) return;
        float left = panel.x + 5;
        float right = panel.x + 95;
        float fullHeight = optionContainerHeight(moduleOptions.size());
        float visibleHeight = fullHeight * progress;
        int scissorTop = (int) Math.floor(top);
        int scissorBottom = (int) Math.ceil(top + visibleHeight);
        if (scissorBottom <= scissorTop) return;

        graphics.enableScissor((int) Math.floor(left), scissorTop, (int) Math.ceil(right + 0.5f), scissorBottom);
        border(graphics, left, top, right, top + fullHeight - 0.5f, fade(0xB0181A20, progress));
        rect(graphics, left + 2, top + 2, left + 2.5f, top + fullHeight - 2,
            fade(0x665A5F6A, progress));

        float optionY = top + 2;
        boolean interactive = panel.expansion(module).open && progress >= 0.95f;
        for (int i = 0; i < moduleOptions.size(); i++) {
            var option = moduleOptions.get(i);
            boolean hover = interactive && hit(mouseX, mouseY, left + 5, optionY, right - left - 10, 11);
            optionRow(graphics, option, left + 5, optionY, right - 3, hover, progress);
            if (i + 1 < moduleOptions.size()) {
                rect(graphics, left + 4, optionY + 11.5f, right - 4, optionY + 12,
                    fade(0x28000000, progress));
            }
            optionY += 12;
        }
        graphics.disableScissor();
    }

    private static void draw(GuiGraphics graphics, Panel panel, int mouseX, int mouseY, boolean controls) {
        border(graphics, panel.x, panel.y, panel.x + 100, panel.y + panel.height(), 0x80000000);
        XdolfFont.draw(graphics, panel.title, panel.x + 3, panel.y + 1, 0xFFFFFFFF);
        if (controls) {
            border(graphics, panel.x + 79, panel.y + 2, panel.x + 88, panel.y + 11,
                panel.pinned ? 0xFFFF0000 : 0xFF383B42);
            border(graphics, panel.x + 89, panel.y + 2, panel.x + 98, panel.y + 11,
                panel.open ? 0xFFFF0000 : 0xFF383B42);
        }
        if (!panel.open) return;

        float moduleY = panel.y + 12;
        for (var module : panel.modules) {
            var moduleOptions = options(module);
            var expansion = panel.expansions.get(module);
            float progress = expansion == null ? 0.0f : expansion.value();
            boolean expanded = expansion != null && expansion.open;
            row(graphics, label(module), panel.x + 2, moduleY, module.enabled(),
                hit(mouseX, mouseY, panel.x + 2, moduleY, 96, 11), !moduleOptions.isEmpty(), expanded);
            moduleY += 12;

            if (!moduleOptions.isEmpty() && progress > 0.001f) {
                optionContainer(graphics, panel, module, moduleOptions, moduleY, progress, mouseX, mouseY);
                moduleY += optionContainerHeight(moduleOptions.size()) * progress;
            }
        }

        for (int i = 0; i < panel.sliders.size(); i++) {
            var slider = panel.sliders.get(i);
            int y = panel.y + 16 + i * 19;
            int x = panel.x + 2;
            String value = String.format(Locale.ROOT, slider.integer ? "%.0f" : "%.2f", slider.setting.get());
            XdolfFont.draw(graphics, slider.label + ": " + value, x + 1, y - 3, 0xFFFFFFFF);
            float drag = (float) ((slider.setting.get() - slider.setting.min) / (slider.setting.max - slider.setting.min) * 90);
            border(graphics, x, y + 9, x + 96, y + 17, 0xFF383B42);
            border(graphics, x + 1, y + 10, x + 5 + (int) drag, y + 16, 0xFFFF0000);
            border(graphics, x + 2 + (int) drag, y + 10, x + 5 + (int) drag, y + 16, 0xFFFF4C4C);
        }

        if (panel.text()) {
            var textLines = lines(panel);
            for (int i = 0; i < textLines.size(); i++) {
                XdolfFont.draw(graphics, XdolfFont.trim(textLines.get(i), 97), panel.x + 3, panel.y + 13 + i * 10, 0xFFFFFFFF);
            }
        }
    }

    private static List<? extends Player> radar() {
        var mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return List.of();
        return mc.level.players().stream()
            .filter(player -> player != mc.player && player.isAlive())
            .sorted(Comparator.comparingDouble(player -> player.distanceToSqr(mc.player)))
            .toList();
    }

    private static List<String> lines(Panel panel) {
        var mc = Minecraft.getInstance();
        var player = mc.player;
        if (panel.title.equals("Radar")) {
            var players = radar();
            if (players.isEmpty()) return List.of("No players in range.");
            return players.stream().map(other ->
                (SocialState.isFriend(other.getName().getString()) ? "\u00a7a" : "\u00a7c")
                    + other.getName().getString() + "\u00a7f: " + (int) other.distanceTo(player)).toList();
        }
        if (player == null) return List.of("No world loaded.");
        var direction = player.getDirection();
        String axis = switch (direction) {
            case NORTH -> "-Z";
            case SOUTH -> "+Z";
            case WEST -> "-X";
            case EAST -> "+X";
            default -> "Invalid";
        };
        boolean nether = mc.level.dimension().equals(Level.NETHER);
        return List.of(
            mc.getFps() + " FPS",
            "X: " + coord(player.getX()) + (nether ? " [" + coord(player.getX() * 8) + "]" : ""),
            "Y: " + coord(player.getY()),
            "Z: " + coord(player.getZ()) + (nether ? " [" + coord(player.getZ() * 8) + "]" : ""),
            "Facing: " + direction.toString().toUpperCase(Locale.ROOT) + " [" + axis + "]",
            String.format(Locale.ROOT, "Yaw: %.1f Pitch: %.1f", Mth.wrapDegrees(player.getYRot()), Mth.wrapDegrees(player.getXRot()))
        );
    }

    private static String coord(double value) {
        return String.format(Locale.ROOT, "%,.0f", Math.floor(value));
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        return click(event.x(), event.y(), event.button());
    }

    private boolean click(double mouseX, double mouseY, int button) {
        for (int index = PANELS.size() - 1; index >= 0; index--) {
            var panel = PANELS.get(index);
            if (!hit(mouseX, mouseY, panel.x, panel.y, 100, panel.height())) continue;
            PANELS.remove(index);
            PANELS.add(panel);

            if (hit(mouseX, mouseY, panel.x + 89, panel.y + 2, 9, 9)) {
                panel.open = !panel.open;
            } else if (hit(mouseX, mouseY, panel.x + 79, panel.y + 2, 9, 9)) {
                panel.pinned = !panel.pinned;
            } else if (hit(mouseX, mouseY, panel.x, panel.y, 79, 11)) {
                dragging = panel;
                offsetX = mouseX - panel.x;
                offsetY = mouseY - panel.y;
            } else if (panel.open) {
                float moduleY = panel.y + 12;
                for (var module : panel.modules) {
                    var moduleOptions = options(module);
                    if (hit(mouseX, mouseY, panel.x + 2, moduleY, 96, 11)) {
                        if (button == 0) {
                            ClientRuntime.toggle(module);
                        } else if (button == 1 && !moduleOptions.isEmpty()) {
                            panel.toggleExpansion(module);
                        }
                        return true;
                    }
                    moduleY += 12;

                    if (!moduleOptions.isEmpty()) {
                        var expansion = panel.expansions.get(module);
                        float progress = expansion == null ? 0.0f : expansion.value();
                        float fullHeight = optionContainerHeight(moduleOptions.size());
                        if (expansion != null && expansion.open && progress >= 0.95f) {
                            float optionY = moduleY + 2;
                            for (var option : moduleOptions) {
                                if (hit(mouseX, mouseY, panel.x + 10, optionY, 80, 11)) {
                                    if (button == 0) {
                                        var setting = option.setting;
                                        setting.set(setting.on() ? 0 : 1);
                                        ClientConfig.save(ClientRuntime.MODULES);
                                    }
                                    return true;
                                }
                                optionY += 12;
                            }
                        }
                        moduleY += fullHeight * progress;
                    }
                }

                for (int i = 0; i < panel.sliders.size(); i++) {
                    if (button == 0 && hit(mouseX, mouseY, panel.x + 2, panel.y + 25 + i * 19, 96, 8)) {
                        sliding = panel.sliders.get(i);
                        offsetX = panel.x + 2;
                        moveSlider(mouseX);
                        return true;
                    }
                }

                if (panel.title.equals("Radar")) {
                    int row = (int) ((mouseY - panel.y - 13) / 10);
                    var players = radar();
                    if (mouseY >= panel.y + 13 && row >= 0 && row < players.size()) {
                        String name = players.get(row).getName().getString();
                        SocialState.command(new String[] {"friend", SocialState.isFriend(name) ? "remove" : "add", name});
                    }
                }
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        if (dragging != null) {
            dragging.x = (int) (event.x() - offsetX);
            dragging.y = (int) (event.y() - offsetY);
            return true;
        }
        if (sliding != null) {
            moveSlider(event.x());
            return true;
        }
        return false;
    }

    private void moveSlider(double mouseX) {
        var setting = sliding.setting;
        double fraction = Math.max(0, Math.min(1, (mouseX - offsetX) / 90));
        double value = setting.min + fraction * (setting.max - setting.min);
        if (sliding.integer) value = Math.floor(value);
        setting.set(Math.max(setting.min, Math.min(setting.max, value)));
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        dragging = null;
        sliding = null;
        save();
        ClientConfig.save(ClientRuntime.MODULES);
        return true;
    }

    @Override
    public void removed() {
        dragging = null;
        sliding = null;
        save();
        ClientConfig.save(ClientRuntime.MODULES);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    static void smokeCheckAndArrange() {
        var screen = (ClientScreen) Minecraft.getInstance().screen;
        if (PANELS.size() != 7) throw new IllegalStateException("Expected seven GUI windows");

        var render = PANELS.stream().filter(panel -> panel.title.equals("Render")).findFirst().orElseThrow();
        var combat = PANELS.stream().filter(panel -> panel.title.equals("Combat")).findFirst().orElseThrow();
        if (render.modules.stream().noneMatch(module -> module.name.equals("Waypoints"))
            || render.modules.stream().noneMatch(module -> module.name.equals("LogoutSpot"))
            || combat.modules.stream().noneMatch(module -> module.name.equals("AutoTotem"))) {
            throw new IllegalStateException("Restored modules missing from click GUI");
        }

        var logout = render.modules.stream().filter(module -> module.name.equals("LogoutSpot")).findFirst().orElseThrow();
        var logoutOptions = options(logout);
        if (logoutOptions.size() != 1 || !logoutOptions.get(0).label.equals("Tracers") || logout.setting("tracers") == null) {
            throw new IllegalStateException("LogoutSpot tracer option missing from click GUI");
        }

        var player = PANELS.stream().filter(panel -> panel.title.equals("Player")).findFirst().orElseThrow();
        screen.click(player.x + 94, player.y + 6, 0);
        if (!player.open) throw new IllegalStateException("Open control failed");
        screen.click(player.x + 84, player.y + 6, 0);
        if (!player.pinned) throw new IllegalStateException("Pin control failed");
        screen.click(player.x + 10, player.y + 5, 0);
        if (screen.dragging != player) throw new IllegalStateException("Title drag failed");
        screen.mouseDragged(new MouseButtonEvent(player.x + 30, player.y + 25,
            new net.minecraft.client.input.MouseButtonInfo(0, 0)), 20, 20);
        if (player.x != 22 || player.y != 67) throw new IllegalStateException("Panel did not follow drag");
        screen.mouseReleased(new MouseButtonEvent(player.x + 10, player.y + 5,
            new net.minecraft.client.input.MouseButtonInfo(0, 0)));
        if (screen.dragging != null) throw new IllegalStateException("Panel drag did not stop");

        for (var panel : PANELS) {
            panel.open = true;
            panel.pinned = false;
            panel.expansions.clear();
            switch (panel.title) {
                case "Values" -> { panel.x = 2; panel.y = 2; }
                case "Player" -> { panel.x = 106; panel.y = 2; }
                case "Render" -> { panel.x = 210; panel.y = 2; }
                case "Combat" -> { panel.x = 314; panel.y = 2; }
                case "World" -> { panel.x = 314; panel.y = 105; }
                case "Info" -> { panel.x = 418; panel.y = 2; }
                case "Radar" -> { panel.x = 418; panel.y = 85; }
            }
        }

        var killAura = combat.modules.stream().filter(module -> module.name.equals("KillAura")).findFirst().orElseThrow();
        int killAuraY = combat.y + 12 + combat.modules.indexOf(killAura) * 12;
        screen.click(combat.x + 30, killAuraY + 5, 1);
        var animation = combat.expansion(killAura);
        if (!animation.open || PANELS.size() != 7) {
            throw new IllegalStateException("KillAura animated inline options failed");
        }
        animation.finish();
        if (combat.height() <= 13 + combat.modules.size() * 12) {
            throw new IllegalStateException("Inline option container did not expand panel height");
        }

        var setting = options(killAura).get(0).setting;
        boolean before = setting.on();
        screen.click(combat.x + 30, killAuraY + 19, 0);
        if (setting.on() == before) throw new IllegalStateException("Inline option toggle failed");
        setting.set(before ? 1 : 0);

        screen.click(combat.x + 30, killAuraY + 5, 1);
        if (animation.open) throw new IllegalStateException("Inline options did not begin collapsing");
        animation.finish();

        var values = PANELS.stream().filter(panel -> panel.title.equals("Values")).findFirst().orElseThrow();
        if (values.sliders.size() != 13) throw new IllegalStateException("Expected thirteen sliders");
        double old = values.sliders.get(0).setting.get();
        screen.click(values.x + 48, values.y + 28, 0);
        if (screen.sliding == null) throw new IllegalStateException("Slider drag failed");
        screen.moveSlider(values.x + 92);
        if (values.sliders.get(0).setting.get() == old) throw new IllegalStateException("Slider did not change value");
        values.sliders.get(0).setting.set(old);
        screen.sliding = null;

        save();
        int savedX = player.x;
        player.x += 100;
        player.open = false;
        load();
        if (player.x != savedX || !player.open) throw new IllegalStateException("Window state roundtrip failed");

        LogUtils.getLogger().info("XDOLF_GUI_OK: seven windows, restored modules, thirteen sliders, animated nested options, pin/open/drag controls");
    }

    private static void load() {
        var file = FMLPaths.CONFIGDIR.get().resolve("xdolf-gui.properties");
        if (!Files.isRegularFile(file)) return;
        try (var reader = Files.newBufferedReader(file)) {
            var properties = new Properties();
            properties.load(reader);
            for (var panel : PANELS) {
                try {
                    panel.x = Integer.parseInt(properties.getProperty(panel.title + ".x", "2"));
                    panel.y = Integer.parseInt(properties.getProperty(panel.title + ".y", Integer.toString(panel.y)));
                } catch (NumberFormatException ignored) {}
                panel.open = Boolean.parseBoolean(properties.getProperty(panel.title + ".open"));
                panel.pinned = Boolean.parseBoolean(properties.getProperty(panel.title + ".pinned"));
            }
        } catch (java.io.IOException error) {
            LogUtils.getLogger().warn("Could not load Xdolf GUI", error);
        }
    }

    private static void save() {
        var file = FMLPaths.CONFIGDIR.get().resolve("xdolf-gui.properties");
        var properties = new Properties();
        for (var panel : PANELS) {
            properties.setProperty(panel.title + ".x", Integer.toString(panel.x));
            properties.setProperty(panel.title + ".y", Integer.toString(panel.y));
            properties.setProperty(panel.title + ".open", Boolean.toString(panel.open));
            properties.setProperty(panel.title + ".pinned", Boolean.toString(panel.pinned));
        }
        try {
            Files.createDirectories(file.getParent());
            try (var writer = Files.newBufferedWriter(file)) {
                properties.store(writer, "Xdolf click GUI window state");
            }
        } catch (java.io.IOException error) {
            LogUtils.getLogger().warn("Could not save Xdolf GUI", error);
        }
    }
}
