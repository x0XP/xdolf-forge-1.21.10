package com.x0xp.xdolf;

import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraftforge.fml.loading.FMLPaths;
import org.lwjgl.glfw.GLFW;

import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

/** Xdolf click GUI with animated, inline module settings. */
public final class ClientScreen extends Screen {
    private static final List<Panel> PANELS = new ArrayList<>();
    private static final long OPTION_ANIMATION_NS = 135_000_000L;
    private static final float BOOLEAN_ROW_HEIGHT = 12.0f;
    private static final float BOOLEAN_WRAPPED_ROW_HEIGHT = 21.0f;
    private static final float NUMBER_ROW_HEIGHT = 25.0f;
    private static final float NUMBER_STACKED_ROW_HEIGHT = 36.0f;
    private static final float NUMBER_FIELD_WIDTH = 31.0f;
    private static final float NUMBER_LABEL_COMPACT_WIDTH = 45.0f;
    private static final float TOGGLE_LABEL_SINGLE_LINE_WIDTH = 76.0f;
    private static boolean loaded;

    private Panel dragging;
    private ModuleSetting sliding;
    private double sliderLeft;
    private double sliderWidth;
    private ModuleSetting editing;
    private String editingText = "";
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

        float animatedSettingsHeight(ClientModule module) {
            var expansion = expansions.get(module);
            return expansion == null ? 0.0f : settingContainerHeight(module) * expansion.value();
        }

        float height() {
            if (text()) return open ? lines(this).size() * 10 + 16 : 14;
            if (!open) return 13;
            float height = 13 + modules.size() * 12 + 0.5f;
            for (var module : modules) height += animatedSettingsHeight(module);
            return height;
        }
    }

    private record SettingRow(String label, ModuleSetting setting, boolean toggle, boolean integer) {}

    ClientScreen() {
        super(Component.literal("Xdolf"));
        setup();
    }

    private static void setup() {
        if (loaded) return;
        loaded = true;
        addModules("Player", 47, "AutoFish Flight Spammer AutoRespawn AutoWalk SafeWalk NoSlowdown HorseJump Sprint NoFall AntiHunger AutoEat Jesus EntitySpeed EntityStep ElytraFly ElytraPlus");
        addModules("Render", 62, "Tracers StorageESP EntityESP NoHurtCam Chams Trajectories Nametags Waypoints LogoutSpot");
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

    private static boolean toggleSetting(ModuleSetting setting) {
        return setting.min == 0.0 && setting.max == 1.0 && setting.step == 1.0;
    }

    private static boolean integerSetting(ModuleSetting setting) {
        return !toggleSetting(setting)
            && setting.step >= 1.0
            && Math.rint(setting.step) == setting.step
            && Math.rint(setting.min) == setting.min
            && Math.rint(setting.max) == setting.max;
    }

    private static String settingLabel(ClientModule module, ModuleSetting setting) {
        String key = module.name + "." + setting.name;
        return switch (key) {
            case "ElytraPlus.takeoff" -> "Instant fly - easy takeoff";
            case "ElytraPlus.stopwater" -> "Stop in water";
            case "AutoFish.autocast" -> "Auto Cast";
            case "AutoFish.castdelay" -> "Auto Cast Delay";
            case "AutoFish.recast" -> "Recast Delay";
            case "AutoFish.recaster" -> "Recaster";
            case "AutoLog.health" -> "Health Threshold";
            case "CrystalLog.range" -> "Distance";
            case "AutoEat.hunger" -> "Hunger Threshold";
            case "EntityStep.height" -> "Step Height";
            case "KillAura.walls" -> "Hit Through Walls";
            case "KillAura.seen" -> "Can Be Seen";
            case "KillAura.mobs" -> "Other Mobs";
            case "Speedmine.progress" -> "Mine Progress";
            default -> titleCase(setting.name);
        };
    }

    private static String titleCase(String value) {
        if (value.isBlank()) return value;
        StringBuilder result = new StringBuilder();
        boolean upper = true;
        for (char c : value.toCharArray()) {
            if (c == '_' || c == '-') {
                result.append(' ');
                upper = true;
            } else {
                result.append(upper ? Character.toUpperCase(c) : c);
                upper = false;
            }
        }
        return result.toString();
    }

    private static List<SettingRow> settings(ClientModule module) {
        var result = new ArrayList<SettingRow>();
        for (var setting : module.settings) {
            result.add(new SettingRow(settingLabel(module, setting), setting,
                toggleSetting(setting), integerSetting(setting)));
        }
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
        for (var panel : PANELS) draw(graphics, panel, mouseX, mouseY, true, this);
        ClientSmoke.frame();
    }

    static void renderPinned(GuiGraphics graphics) {
        setup();
        if (Minecraft.getInstance().screen instanceof ClientScreen) return;
        for (var panel : PANELS) if (panel.pinned) draw(graphics, panel, -1000, -1000, false, null);
    }

    private static void rect(GuiGraphics graphics, float x, float y, float right, float bottom, int color) {
        graphics.pose().pushMatrix();
        graphics.pose().scale(0.5f, 0.5f);
        graphics.fill(Math.round(x * 2), Math.round(y * 2), Math.round(right * 2), Math.round(bottom * 2), color);
        graphics.pose().popMatrix();
    }

    private static void outline(GuiGraphics graphics, float x, float y, float right, float bottom, int color) {
        rect(graphics, x, y, right, y + 0.5f, color);
        rect(graphics, x, bottom - 0.5f, right, bottom, color);
        rect(graphics, x, y, x + 0.5f, bottom, color);
        rect(graphics, right - 0.5f, y, right, bottom, color);
    }

    private static void border(GuiGraphics graphics, float x, float y, float right, float bottom, int inside) {
        rect(graphics, x, y, right, bottom, inside);
        outline(graphics, x, y, right + 0.5f, bottom + 0.5f, 0xFF000000);
    }

    private static int fade(int color, float alpha) {
        int originalAlpha = color >>> 24;
        int fadedAlpha = Math.max(0, Math.min(255, Math.round(originalAlpha * alpha)));
        return (color & 0x00FFFFFF) | fadedAlpha << 24;
    }

    private static boolean hit(double mouseX, double mouseY, double x, double y, double width, double height) {
        return mouseX >= x && mouseY >= y && mouseX <= x + width && mouseY <= y + height;
    }

    private static boolean stackedNumber(SettingRow row) {
        return !row.toggle && XdolfFont.width(row.label) > NUMBER_LABEL_COMPACT_WIDTH;
    }

    private static boolean wrappedToggle(SettingRow row) {
        return row.toggle && XdolfFont.width(row.label) > TOGGLE_LABEL_SINGLE_LINE_WIDTH;
    }

    private static float settingRowHeight(SettingRow row) {
        if (row.toggle) return wrappedToggle(row) ? BOOLEAN_WRAPPED_ROW_HEIGHT : BOOLEAN_ROW_HEIGHT;
        return stackedNumber(row) ? NUMBER_STACKED_ROW_HEIGHT : NUMBER_ROW_HEIGHT;
    }

    private static float settingContainerHeight(ClientModule module) {
        float height = 4.0f;
        for (var row : settings(module)) height += settingRowHeight(row);
        return height;
    }

    private static List<String> wrapLabel(String text, int maxWidth) {
        if (XdolfFont.width(text) <= maxWidth) return List.of(text);
        var words = text.split(" ");
        var lines = new ArrayList<String>(2);
        var current = new StringBuilder();
        for (String word : words) {
            String candidate = current.isEmpty() ? word : current + " " + word;
            if (!current.isEmpty() && XdolfFont.width(candidate) > maxWidth) {
                lines.add(current.toString());
                current.setLength(0);
                current.append(word);
                if (lines.size() == 1) continue;
            } else {
                if (!current.isEmpty()) current.append(' ');
                current.append(word);
            }
        }
        if (!current.isEmpty()) lines.add(current.toString());
        if (lines.size() <= 2) return lines;
        return List.of(lines.get(0), XdolfFont.trim(String.join(" ", lines.subList(1, lines.size())), maxWidth));
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

    private static void toggleRow(GuiGraphics graphics, SettingRow row, float left, float right, float y,
                                  boolean hover, float alpha) {
        boolean enabled = row.setting.on();
        int color = enabled ? hover ? 0xFF44AAFF : 0xFFFFFFFF : hover ? 0xFF888888 : 0xB8FFFFFF;
        int stateColor = enabled ? hover ? 0xFF44AAFF : 0xFFFF0000 : hover ? 0xFF888888 : 0xFF454850;
        float rowHeight = settingRowHeight(row);
        var labelLines = wrapLabel(row.label, (int) (right - left - 8));
        if (labelLines.size() == 1) {
            XdolfFont.draw(graphics, labelLines.get(0), left, y, fade(color, alpha));
        } else {
            XdolfFont.draw(graphics, labelLines.get(0), left, y, fade(color, alpha));
            XdolfFont.draw(graphics, labelLines.get(1), left, y + 9, fade(color, alpha));
        }
        float center = y + rowHeight / 2.0f;
        rect(graphics, right - 2, center - 4, right - 1, center + 4, fade(stateColor, alpha));
    }

    private static float numberFieldTop(SettingRow row, float y) {
        return y + (stackedNumber(row) ? 10.5f : -0.5f);
    }

    private static float numberTrackTop(SettingRow row, float y) {
        return y + (stackedNumber(row) ? 25.0f : 14.0f);
    }

    private static void numberRow(GuiGraphics graphics, SettingRow row, float left, float right, float y,
                                  boolean hover, float alpha, ClientScreen screen) {
        boolean editing = screen != null && screen.editing == row.setting;
        boolean stacked = stackedNumber(row);
        XdolfFont.draw(graphics, row.label, left, y, fade(hover ? 0xFFFFFFFF : 0xD8FFFFFF, alpha));

        float fieldRight = right - 3;
        float fieldLeft = fieldRight - NUMBER_FIELD_WIDTH;
        float fieldTop = numberFieldTop(row, y);
        float fieldBottom = fieldTop + 11.0f;
        int fieldFill = editing ? 0xE01B2029 : hover ? 0xD0191D24 : 0xC0101318;
        int fieldBorder = editing ? 0xFF44AAFF : hover ? 0xFF7B828F : 0xFF4D535D;
        rect(graphics, fieldLeft, fieldTop, fieldRight, fieldBottom, fade(fieldFill, alpha));
        outline(graphics, fieldLeft, fieldTop, fieldRight, fieldBottom, fade(fieldBorder, alpha));

        String value = editing ? screen.editingText : row.setting.display();
        if (editing && (System.currentTimeMillis() / 450L) % 2 == 0) value += "_";
        value = XdolfFont.trim(value, (int) (fieldRight - fieldLeft - 4));
        float valueX = fieldRight - 2 - XdolfFont.width(value);
        XdolfFont.draw(graphics, value, Math.max(fieldLeft + 2, valueX), fieldTop + 0.5f, fade(0xFFFFFFFF, alpha));
        float trackLeft = left;
        float trackRight = right - 3;
        float trackTop = numberTrackTop(row, y);
        float trackBottom = trackTop + 6;
        double span = row.setting.max - row.setting.min;
        float fraction = span <= 0 ? 0 : (float) ((row.setting.get() - row.setting.min) / span);
        fraction = Math.max(0.0f, Math.min(1.0f, fraction));
        border(graphics, trackLeft, trackTop, trackRight, trackBottom, fade(0xFF383B42, alpha));
        float fillRight = trackLeft + 1 + (trackRight - trackLeft - 2) * fraction;
        rect(graphics, trackLeft + 1, trackTop + 1, fillRight, trackBottom - 1, fade(0xFFFF0000, alpha));
        float knob = Math.max(trackLeft + 1, Math.min(trackRight - 2, fillRight - 1));
        rect(graphics, knob, trackTop, knob + 2, trackBottom, fade(hover ? 0xFF44AAFF : 0xFFFF4C4C, alpha));
    }

    private static void settingContainer(GuiGraphics graphics, Panel panel, ClientModule module, float top,
                                         float progress, int mouseX, int mouseY, ClientScreen screen) {
        if (progress <= 0.001f) return;
        var rows = settings(module);
        if (rows.isEmpty()) return;
        float left = panel.x + 5;
        float right = panel.x + 95;
        float fullHeight = settingContainerHeight(module);
        float visibleHeight = fullHeight * progress;
        int scissorTop = (int) Math.floor(top);
        int scissorBottom = (int) Math.ceil(top + visibleHeight);
        if (scissorBottom <= scissorTop) return;

        graphics.enableScissor((int) Math.floor(left), scissorTop, (int) Math.ceil(right + 0.5f), scissorBottom);
        border(graphics, left, top, right, top + fullHeight - 0.5f, fade(0xB0181A20, progress));
        rect(graphics, left + 2, top + 2, left + 2.5f, top + fullHeight - 2, fade(0x665A5F6A, progress));

        float y = top + 2;
        boolean interactive = panel.expansion(module).open && progress >= 0.95f && screen != null;
        for (int i = 0; i < rows.size(); i++) {
            var settingRow = rows.get(i);
            boolean hover = interactive && hit(mouseX, mouseY, left + 5, y, right - left - 10, settingRowHeight(settingRow) - 1);
            if (settingRow.toggle) {
                toggleRow(graphics, settingRow, left + 5, right - 3, y, hover, progress);
            } else {
                numberRow(graphics, settingRow, left + 5, right - 3, y, hover, progress, screen);
            }
            y += settingRowHeight(settingRow);
            if (i + 1 < rows.size()) {
                rect(graphics, left + 4, y - 0.5f, right - 4, y, fade(0x28000000, progress));
            }
        }
        graphics.disableScissor();
    }

    private static void draw(GuiGraphics graphics, Panel panel, int mouseX, int mouseY, boolean controls, ClientScreen screen) {
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
            var moduleSettings = settings(module);
            var expansion = panel.expansions.get(module);
            float progress = expansion == null ? 0.0f : expansion.value();
            boolean expanded = expansion != null && expansion.open;
            row(graphics, label(module), panel.x + 2, moduleY, module.enabled(),
                hit(mouseX, mouseY, panel.x + 2, moduleY, 96, 11), !moduleSettings.isEmpty(), expanded);
            moduleY += 12;

            if (!moduleSettings.isEmpty() && progress > 0.001f) {
                settingContainer(graphics, panel, module, moduleY, progress, mouseX, mouseY, screen);
                moduleY += settingContainerHeight(module) * progress;
            }
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
                commitEditing();
                panel.open = !panel.open;
            } else if (hit(mouseX, mouseY, panel.x + 79, panel.y + 2, 9, 9)) {
                commitEditing();
                panel.pinned = !panel.pinned;
            } else if (hit(mouseX, mouseY, panel.x, panel.y, 79, 11)) {
                commitEditing();
                dragging = panel;
                offsetX = mouseX - panel.x;
                offsetY = mouseY - panel.y;
            } else if (panel.open) {
                float moduleY = panel.y + 12;
                for (var module : panel.modules) {
                    var rows = settings(module);
                    if (hit(mouseX, mouseY, panel.x + 2, moduleY, 96, 11)) {
                        commitEditing();
                        if (button == 0) {
                            ClientRuntime.toggle(module);
                        } else if (button == 1 && !rows.isEmpty()) {
                            panel.toggleExpansion(module);
                        }
                        return true;
                    }
                    moduleY += 12;

                    if (!rows.isEmpty()) {
                        var expansion = panel.expansions.get(module);
                        float progress = expansion == null ? 0.0f : expansion.value();
                        float fullHeight = settingContainerHeight(module);
                        if (expansion != null && expansion.open && progress >= 0.95f) {
                            float left = panel.x + 5;
                            float right = panel.x + 95;
                            float settingY = moduleY + 2;
                            for (var settingRow : rows) {
                                if (settingRow.toggle) {
                                    if (hit(mouseX, mouseY, left + 5, settingY, right - left - 10, settingRowHeight(settingRow) - 1)) {
                                        commitEditing();
                                        if (button == 0) {
                                            settingRow.setting.set(settingRow.setting.on() ? 0 : 1);
                                            ClientConfig.save(ClientRuntime.MODULES);
                                        }
                                        return true;
                                    }
                                } else {
                                    float rowLeft = left + 5;
                                    float rowRight = right - 3;
                                    float fieldRight = rowRight - 3;
                                    float fieldLeft = fieldRight - NUMBER_FIELD_WIDTH;
                                    float fieldTop = numberFieldTop(settingRow, settingY);
                                    if (button == 0 && hit(mouseX, mouseY, fieldLeft, fieldTop, NUMBER_FIELD_WIDTH, 11)) {
                                        beginEditing(settingRow.setting);
                                        return true;
                                    }
                                    float trackTop = numberTrackTop(settingRow, settingY);
                                    if (button == 0 && hit(mouseX, mouseY, rowLeft, trackTop, rowRight - rowLeft - 3, 7)) {
                                        commitEditing();
                                        sliding = settingRow.setting;
                                        sliderLeft = rowLeft;
                                        sliderWidth = rowRight - rowLeft - 3;
                                        moveSlider(mouseX);
                                        return true;
                                    }
                                }
                                settingY += settingRowHeight(settingRow);
                            }
                        }
                        moduleY += fullHeight * progress;
                    }
                }

                if (panel.title.equals("Radar")) {
                    commitEditing();
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
        commitEditing();
        return false;
    }

    private void beginEditing(ModuleSetting setting) {
        if (editing != setting) commitEditing();
        editing = setting;
        editingText = "";
    }

    private void cancelEditing() {
        editing = null;
        editingText = "";
    }

    private void commitEditing() {
        if (editing == null) return;
        try {
            if (!editingText.isBlank() && !editingText.equals("-") && !editingText.equals(".") && !editingText.equals("-.")) {
                double value = Double.parseDouble(editingText);
                value = Math.max(editing.min, Math.min(editing.max, value));
                if (integerSetting(editing)) value = Math.rint(value);
                editing.set(value);
                ClientConfig.save(ClientRuntime.MODULES);
            }
        } catch (IllegalArgumentException ignored) {
            // Invalid partial input simply reverts to the previous setting value.
        }
        cancelEditing();
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (editing == null) return super.keyPressed(event);
        int key = event.key();
        if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) {
            commitEditing();
            return true;
        }
        if (key == GLFW.GLFW_KEY_ESCAPE) {
            cancelEditing();
            return true;
        }
        if (key == GLFW.GLFW_KEY_BACKSPACE) {
            if (!editingText.isEmpty()) editingText = editingText.substring(0, editingText.length() - 1);
            return true;
        }
        if (key == GLFW.GLFW_KEY_DELETE) {
            editingText = "";
            return true;
        }
        return true;
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        if (editing == null) return super.charTyped(event);
        int codepoint = event.codepoint();
        if (!Character.isValidCodePoint(codepoint)) return true;
        char c = (char) codepoint;
        if (Character.isDigit(c)) {
            if (editingText.length() < 14) editingText += c;
        } else if (c == '.' && !integerSetting(editing) && !editingText.contains(".")) {
            editingText += editingText.isEmpty() ? "0." : ".";
        } else if (c == '-' && editing.min < 0 && editingText.isEmpty()) {
            editingText = "-";
        }
        return true;
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
        if (sliding == null || sliderWidth <= 0) return;
        double fraction = Math.max(0, Math.min(1, (mouseX - sliderLeft) / sliderWidth));
        double raw = sliding.min + fraction * (sliding.max - sliding.min);
        double steps = Math.round((raw - sliding.min) / sliding.step);
        double value = sliding.min + steps * sliding.step;
        value = Math.max(sliding.min, Math.min(sliding.max, value));
        value = Math.round(value * 10000.0) / 10000.0;
        sliding.set(value);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        dragging = null;
        if (sliding != null) ClientConfig.save(ClientRuntime.MODULES);
        sliding = null;
        save();
        return true;
    }

    @Override
    public void removed() {
        commitEditing();
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
        if (PANELS.size() != 6) throw new IllegalStateException("Expected six GUI windows after removing Values");

        var render = PANELS.stream().filter(panel -> panel.title.equals("Render")).findFirst().orElseThrow();
        var combat = PANELS.stream().filter(panel -> panel.title.equals("Combat")).findFirst().orElseThrow();
        var world = PANELS.stream().filter(panel -> panel.title.equals("World")).findFirst().orElseThrow();
        if (render.modules.stream().noneMatch(module -> module.name.equals("Waypoints"))
            || render.modules.stream().noneMatch(module -> module.name.equals("LogoutSpot"))
            || combat.modules.stream().noneMatch(module -> module.name.equals("AutoTotem"))) {
            throw new IllegalStateException("Restored modules missing from click GUI");
        }

        var logout = render.modules.stream().filter(module -> module.name.equals("LogoutSpot")).findFirst().orElseThrow();
        if (settings(logout).size() != 1 || !settings(logout).get(0).label.equals("Tracers")) {
            throw new IllegalStateException("LogoutSpot tracer setting missing from click GUI");
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
                case "Player" -> { panel.x = 2; panel.y = 2; }
                case "Render" -> { panel.x = 106; panel.y = 2; }
                case "Combat" -> { panel.x = 210; panel.y = 2; }
                case "World" -> { panel.x = 314; panel.y = 2; }
                case "Info" -> { panel.x = 418; panel.y = 2; }
                case "Radar" -> { panel.x = 418; panel.y = 85; }
            }
        }

        var killAura = combat.modules.stream().filter(module -> module.name.equals("KillAura")).findFirst().orElseThrow();
        int killAuraY = combat.y + 12 + combat.modules.indexOf(killAura) * 12;
        screen.click(combat.x + 30, killAuraY + 5, 1);
        var animation = combat.expansion(killAura);
        if (!animation.open || PANELS.size() != 6) {
            throw new IllegalStateException("KillAura animated inline settings failed");
        }
        animation.finish();
        if (combat.height() <= 13 + combat.modules.size() * 12) {
            throw new IllegalStateException("Inline settings container did not expand panel height");
        }

        var playerToggle = settings(killAura).stream().filter(row -> row.setting.name.equals("players")).findFirst().orElseThrow();
        boolean before = playerToggle.setting.on();
        var rangeRow = settings(killAura).stream().filter(row -> row.setting.name.equals("range")).findFirst().orElseThrow();
        float settingY = killAuraY + 14 + settingRowHeight(rangeRow);
        screen.click(combat.x + 20, settingY + 2, 0);
        if (playerToggle.setting.on() == before) throw new IllegalStateException("Inline toggle failed");
        playerToggle.setting.set(before ? 1 : 0);

        var range = killAura.setting("range");
        double oldRange = range.get();
        screen.beginEditing(range);
        screen.editingText = "999";
        screen.commitEditing();
        if (range.get() != range.max) throw new IllegalStateException("Typed setting did not clamp to max");
        range.set(oldRange);

        var timer = world.modules.stream().filter(module -> module.name.equals("Timer")).findFirst().orElseThrow();
        if (settings(timer).stream().noneMatch(row -> row.setting.name.equals("speed") && !row.toggle)) {
            throw new IllegalStateException("Numeric module settings were not moved inline");
        }

        var autoLog = combat.modules.stream().filter(module -> module.name.equals("AutoLog")).findFirst().orElseThrow();
        var health = settings(autoLog).stream().filter(row -> row.setting.name.equals("health")).findFirst().orElseThrow();
        if (!stackedNumber(health) || settingRowHeight(health) <= NUMBER_ROW_HEIGHT) {
            throw new IllegalStateException("Long numeric labels are not using stacked layout");
        }

        var elytraPlus = player.modules.stream().filter(module -> module.name.equals("ElytraPlus")).findFirst().orElseThrow();
        var takeoff = settings(elytraPlus).stream().filter(row -> row.setting.name.equals("takeoff")).findFirst().orElseThrow();
        if (!wrappedToggle(takeoff) || settingRowHeight(takeoff) <= BOOLEAN_ROW_HEIGHT) {
            throw new IllegalStateException("Long toggle labels are not wrapping");
        }

        screen.click(combat.x + 30, killAuraY + 5, 1);
        if (animation.open) throw new IllegalStateException("Inline settings did not begin collapsing");
        animation.finish();

        save();
        int savedX = player.x;
        player.x += 100;
        player.open = false;
        load();
        if (player.x != savedX || !player.open) throw new IllegalStateException("Window state roundtrip failed");

        LogUtils.getLogger().info("XDOLF_GUI_OK: six windows, adaptive inline settings, typed values, animation, pin/open/drag controls");
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
