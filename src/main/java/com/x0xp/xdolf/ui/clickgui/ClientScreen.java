package com.x0xp.xdolf;

import com.x0xp.xdolf.module.ClientModule;
import com.x0xp.xdolf.module.ModuleManager;
import com.x0xp.xdolf.settings.*;

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
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import static com.x0xp.xdolf.UiDraw.*;

/** Xdolf's draggable click GUI. Typed option cards are rendered by {@link ConfigContainer}. */
public final class ClientScreen extends Screen {
    private static final List<ClickGuiPanel> PANELS = new ArrayList<>();
  private static boolean loaded;
    private static final long OPEN_FADE_NS = 180_000_000L;

    private ClickGuiPanel dragging;
    private NumberSetting sliding;
    private double sliderLeft;
    private double sliderWidth;
    private ModuleSetting<?> editing;
    private String editingText = "";
    private ClientModule binding;
    private int pendingBindingKey = -1;
    private List<ClientModule> pendingBindingConflicts = List.of();
    private double offsetX;
  private double offsetY;
    private final long openedAt = System.nanoTime();

    private ClientModule frameHoverModule;
    private ModuleSetting<?> frameHoverSetting;
    private ClientModule tooltipModule;
    private ModuleSetting<?> tooltipSetting;
    private long tooltipStarted;

    ClientScreen() {
        super(Component.literal("Xdolf"));
        setup();
    }

    private static void setup() {
        if (loaded) return;
        loaded = true;
        addModules("Player", 47, "AutoFish Flight Spammer Announcer AutoRespawn AutoWalk SafeWalk NoSlowdown HorseJump Sprint NoFall AntiHunger AutoEat Jesus EntitySpeed EntityStep ElytraFly ElytraPlus");
        addModules("Render", 62, "Tracers StorageESP EntityESP NoHurtCam Chams Trajectories Nametags Waypoints LogoutSpot");
        PANELS.add(new ClickGuiPanel("Info", 17));
        PANELS.add(new ClickGuiPanel("Radar", 92));
        addModules("Combat", 32, "AntiVelocity KillAura AutoArmor AutoTotem AutoLog CrystalAura Criticals CrystalLog");
        addModules("World", 77, "Fullbright Timer XRay FastPlace Freecam Speedmine");
        ClickGuiPersistence.load(PANELS);
    }

    private static void addModules(String title, int y, String names) {
        var panel = new ClickGuiPanel(title, y);
        for (String name : names.split(" ")) {
            var module = ClientRuntime.find(name);
            if (module == null) throw new IllegalStateException("Missing GUI module: " + name);
            panel.modules.add(module);
        }
        PANELS.add(panel);
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
        frameHoverModule = null;
        frameHoverSetting = null;
        float uiAlpha = easeOutCubic(clamp01((System.nanoTime() - openedAt) / (float) OPEN_FADE_NS));
        graphics.fill(0, 0, width, height, fade(0x8F000000, uiAlpha));
        UiDraw.setGlobalAlpha(uiAlpha);
        try {
            for (ClickGuiPanel panel : PANELS) drawPanel(graphics, panel, mouseX, mouseY, true, this);
            updateTooltip();
            if (tooltipModule != null && System.nanoTime() - tooltipStarted >= 300_000_000L)
                ClickGuiTooltip.draw(graphics, mouseX, mouseY, tooltipModule, tooltipSetting);
        } finally {
            UiDraw.setGlobalAlpha(1.0f);
        }
        NotificationCards.render(graphics);
        ClientSmoke.frame();
    }

    static void renderPinned(GuiGraphics graphics) {
        setup();
        if (Minecraft.getInstance().screen instanceof ClientScreen) return;
        for (ClickGuiPanel panel : PANELS)
            if (panel.pinned) drawPanel(graphics, panel, -1000, -1000, false, null);
    }

    private void updateTooltip() {
        if (frameHoverModule == null) {
            tooltipModule = null;
            tooltipSetting = null;
            return;
        }
        if (tooltipModule != frameHoverModule || tooltipSetting != frameHoverSetting) {
            tooltipModule = frameHoverModule;
            tooltipSetting = frameHoverSetting;
            tooltipStarted = System.nanoTime();
        }
    }

    void noteSettingHover(ClientModule module, ModuleSetting<?> setting) {
        frameHoverModule = module;
        frameHoverSetting = setting;
    }

    ClientModule binding() { return binding; }
    int pendingBindingKey() { return pendingBindingKey; }
    List<ClientModule> pendingBindingConflicts() { return pendingBindingConflicts; }
    ModuleSetting<?> editing() { return editing; }
    String editingText() { return editingText; }
    boolean cursorVisible() { return (System.currentTimeMillis() / 450L) % 2 == 0; }

    private static void drawPanel(GuiGraphics graphics, ClickGuiPanel panel, int mouseX, int mouseY,
                                  boolean controls, ClientScreen screen) {
        List<String> text = panel.text() ? lines(panel) : List.of();
        float displayHeight = panel.displayHeight(graphics.guiHeight(), text.size());
        border(graphics, panel.x, panel.y, panel.x + 100, panel.y + displayHeight, 0x80000000);
        XdolfFont.draw(graphics, panel.title, panel.x + 3, panel.y + 1, 0xFFFFFFFF);
        if (controls) {
            border(graphics, panel.x + 79, panel.y + 2, panel.x + 88, panel.y + 11,
                panel.pinned ? 0xFFFF0000 : 0xFF383B42);
            border(graphics, panel.x + 89, panel.y + 2, panel.x + 98, panel.y + 11,
                panel.open ? 0xFFFF0000 : 0xFF383B42);
        }
        if (panel.openProgress() <= .001f) return;

        if (panel.text()) {
            graphics.enableScissor(panel.x, panel.y + 12, panel.x + 100, (int) Math.ceil(panel.y + displayHeight));
            for (int i = 0; i < text.size(); i++)
                XdolfFont.draw(graphics, XdolfFont.trim(text.get(i), 97), panel.x + 3, panel.y + 13 + i * 10, 0xFFFFFFFF);
            graphics.disableScissor();
            return;
        }

        float bodyTop = panel.y + 12;
        float bodyBottom = panel.y + displayHeight - .5f;
        float scroll = panel.scrollValue(graphics.guiHeight());
        boolean mouseInBody = mouseY >= bodyTop && mouseY <= bodyBottom;
        graphics.enableScissor(panel.x, (int) bodyTop, panel.x + 100, (int) Math.ceil(bodyBottom));
        float moduleY = bodyTop - scroll;
        for (ClientModule module : panel.modules) {
            var expansion = panel.expansions.get(module);
            float progress = expansion == null ? 0 : expansion.value();
            boolean expanded = expansion != null && expansion.open;
            boolean hover = controls && mouseInBody && hit(mouseX, mouseY, panel.x + 2, moduleY, 96, 11);
            drawModuleRow(graphics, module, panel.x + 2, moduleY, hover, expanded);
            if (hover && screen != null) {
                screen.frameHoverModule = module;
                screen.frameHoverSetting = null;
            }
            moduleY += 12;
            if (progress > .001f) {
                ConfigContainer.draw(graphics, panel, module, moduleY, progress, mouseX,
                    mouseInBody ? mouseY : -10000, screen);
                moduleY += ConfigContainer.height(module) * progress;
            }
        }
        graphics.disableScissor();
        drawScrollbar(graphics, panel, displayHeight, bodyTop, bodyBottom, scroll);
    }

    private static void drawModuleRow(GuiGraphics graphics, ClientModule module, float x, float y,
                                      boolean hover, boolean expanded) {
        boolean enabled = module.enabled();
        if (expanded) rect(graphics, x, y, x + 95, y + 12, hover ? 0xCC252E3B : 0xB81B222D);
        int color = enabled ? hover ? 0xFF44AAFF : 0xFFFFFFFF : hover ? 0xFF888888 : 0x99FFFFFF;
        rect(graphics, x + 95, y, x + 96, y + 12,
            enabled ? hover ? 0xFF44AAFF : 0xFFFF0000 : hover ? 0xFF888888 : 0x0033363D);
        String name = label(module);
        XdolfFont.draw(graphics, name, x + 48 - XdolfFont.width(name) / 2f, y, color);
        if (hover) {
            // Use the same bundled Roboto TTF renderer and baseline as the module label.
            String arrow = expanded ? "v" : ">";
            float arrowX = x + 90.5f - XdolfFont.width(arrow) / 2f;
            XdolfFont.draw(graphics, arrow, arrowX, y, 0xFF62B5FF);
        }
    }

    private static void drawScrollbar(GuiGraphics graphics, ClickGuiPanel panel, float displayHeight,
                                      float bodyTop, float bodyBottom, float scroll) {
        float max = panel.maxScroll(graphics.guiHeight());
        if (max <= .5f) return;
        float trackTop = bodyTop + 2;
        float trackBottom = bodyBottom - 2;
        float trackHeight = Math.max(1, trackBottom - trackTop);
        float content = Math.max(1, panel.height(0) - 13);
        float visible = Math.max(1, displayHeight - 13);
        float thumbHeight = Math.max(16, trackHeight * Math.min(1, visible / content));
        float thumbY = trackTop + Math.max(0, trackHeight - thumbHeight) * (scroll / max);
        // Keep the scrollbar left of the module's x+97..x+98 enabled rail.
        rect(graphics, panel.x + 96, trackTop, panel.x + 96.5f, trackBottom, 0x553A3D44);
        rect(graphics, panel.x + 95.5f, thumbY, panel.x + 96.5f, thumbY + thumbHeight, 0xCC7B828F);
    }

    private static List<? extends Player> radar() {
        var mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return List.of();
        return mc.level.players().stream().filter(player -> player != mc.player && player.isAlive())
            .sorted(Comparator.comparingDouble(player -> player.distanceToSqr(mc.player))).toList();
    }

    private static List<String> lines(ClickGuiPanel panel) {
        var mc = Minecraft.getInstance();
        var player = mc.player;
        if (panel.title.equals("Radar")) {
            var players = radar();
            if (players.isEmpty()) return List.of("No players in range.");
            return players.stream().map(other -> (SocialState.isFriend(other.getName().getString()) ? "\u00a7a" : "\u00a7c")
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
        if (binding != null) cancelBinding();
        for (int index = PANELS.size() - 1; index >= 0; index--) {
            ClickGuiPanel panel = PANELS.get(index);
            int textLines = panel.text() ? lines(panel).size() : 0;
            float displayHeight = panel.displayHeight(height, textLines);
            if (!hit(mouseX, mouseY, panel.x, panel.y, 100, displayHeight)) continue;
            PANELS.remove(index);
            PANELS.add(panel);

            if (hit(mouseX, mouseY, panel.x + 89, panel.y + 2, 9, 9)) {
                commitEditing();
                panel.setOpen(!panel.open);
            } else if (hit(mouseX, mouseY, panel.x + 79, panel.y + 2, 9, 9)) {
                commitEditing();
                panel.pinned = !panel.pinned;
            } else if (hit(mouseX, mouseY, panel.x, panel.y, 79, 11)) {
                commitEditing();
                dragging = panel;
                offsetX = mouseX - panel.x;
                offsetY = mouseY - panel.y;
            } else if (panel.open && !panel.text() && mouseY >= panel.y + 12) {
                if (clickModules(panel, mouseX, mouseY, button)) return true;
            } else if (panel.open && panel.title.equals("Radar")) {
                commitEditing();
                int row = (int) ((mouseY - panel.y - 13) / 10);
                var players = radar();
                if (mouseY >= panel.y + 13 && row >= 0 && row < players.size()) {
                    String name = players.get(row).getName().getString();
                    SocialState.command(new String[]{"friend", SocialState.isFriend(name) ? "remove" : "add", name});
                }
            }
            return true;
        }
        commitEditing();
        return false;
    }

    private boolean clickModules(ClickGuiPanel panel, double mouseX, double mouseY, int button) {
        float moduleY = panel.y + 12 - panel.scrollValue(height);
        for (ClientModule module : panel.modules) {
            if (hit(mouseX, mouseY, panel.x + 2, moduleY, 96, 11)) {
                commitEditing();
                if (button == 0) ClientRuntime.toggle(module);
                else if (button == 1) panel.toggleExpansion(module);
                return true;
            }
            moduleY += 12;
            var expansion = panel.expansions.get(module);
            float progress = expansion == null ? 0 : expansion.value();
            if (expansion != null && expansion.open && progress >= .95f
                && clickOptions(panel, module, moduleY, mouseX, mouseY, button)) return true;
            moduleY += ConfigContainer.height(module) * progress;
        }
        return false;
    }

    private boolean clickOptions(ClickGuiPanel panel, ClientModule module, float top,
                                 double mouseX, double mouseY, int button) {
        float left = panel.x + 4;
        float right = panel.x + 96;
        float rowLeft = left + 6;
        float rowRight = right - 5;
        float y = top + ConfigContainer.HEADER_HEIGHT + 2;
        float keyFieldLeft = rowRight - ConfigContainer.FIELD_WIDTH;
        if (button == 0 && hit(mouseX, mouseY, keyFieldLeft, y + 1, ConfigContainer.FIELD_WIDTH, 10)) {
            commitEditing();
            beginBinding(module);
            return true;
        }
        y += ConfigContainer.KEYBIND_HEIGHT;

        for (ModuleSetting<?> setting : module.settings) {
            float rowHeight = ConfigContainer.rowHeight(setting);
            if (hit(mouseX, mouseY, left + 3, y, right - left - 5, rowHeight - 1)) {
                if (setting instanceof BooleanSetting toggle && button == 0) {
                    commitEditing();
                    toggle.toggle();
                    settingChanged(module);
                    return true;
                }
                if (setting instanceof ChoiceSetting choice && (button == 0 || button == 1)) {
                    commitEditing();
                    choice.cycle(button == 0 ? 1 : -1);
                    settingChanged(module);
                    return true;
                }
                if (setting instanceof TextSetting && button == 0 && hit(mouseX, mouseY, rowLeft, y + 10, rowRight - rowLeft, 10)) {
                    beginEditing(setting);
                    return true;
                }
                if (setting instanceof NumberSetting number && button == 0) {
                    float fieldLeft = rowRight - ConfigContainer.NUMBER_FIELD_WIDTH;
                    if (hit(mouseX, mouseY, fieldLeft, y + 1, ConfigContainer.NUMBER_FIELD_WIDTH, 10)) {
                        beginEditing(setting);
                        return true;
                    }
                    if (hit(mouseX, mouseY, rowLeft, ConfigContainer.sliderTop(y), rowRight - rowLeft, 7)) {
                        commitEditing();
                        sliding = number;
                        sliderLeft = rowLeft;
                        sliderWidth = rowRight - rowLeft;
                        moveSlider(mouseX);
                        return true;
                    }
                }
            }
            y += rowHeight;
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        for (int index = PANELS.size() - 1; index >= 0; index--) {
            ClickGuiPanel panel = PANELS.get(index);
            if (!panel.open || panel.text() || panel.maxScroll(height) <= .5f) continue;
            float displayHeight = panel.displayHeight(height, 0);
            if (!hit(mouseX, mouseY, panel.x, panel.y + 12, 100, displayHeight - 12)) continue;
            panel.scrollBy((float) (-scrollY * 32), height);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    private void beginBinding(ClientModule module) {
        binding = module;
        pendingBindingKey = -1;
        pendingBindingConflicts = List.of();
    }

    private void assignBinding(ClientModule module, int key) {
        module.key = key;
        cancelBinding();
        ClientConfig.save(ClientRuntime.MODULES);
    }

    private void replaceBinding(ClientModule module, int key) {
        String oldOwners = Keybinds.conflictNames(module, key);
        Keybinds.replaceConflicts(module, key);
        cancelBinding();
        ClientConfig.save(ClientRuntime.MODULES);
        NotificationCards.warning("Keybind moved", Keybinds.display(key) + " from " + oldOwners);
    }

    private void cancelBinding() {
        binding = null;
        pendingBindingKey = -1;
        pendingBindingConflicts = List.of();
    }

    private void beginEditing(ModuleSetting<?> setting) {
        if (editing == setting) return;
        commitEditing();
        editing = setting;
        editingText = setting instanceof TextSetting text ? text.get() : "";
    }

    private void cancelEditing() {
        editing = null;
        editingText = "";
    }

    private void commitEditing() {
        if (editing == null) return;
        try {
            if (!editingText.isBlank() || editing instanceof TextSetting) {
                if (editing instanceof NumberSetting number && number.integer() && !editingText.isBlank())
                    editingText = Long.toString(Math.round(Double.parseDouble(editingText)));
                editing.parse(editingText);
                settingChanged(ownerOf(editing));
            }
        } catch (IllegalArgumentException ignored) {
            // Invalid partial input reverts to the setting's previous value.
        }
        cancelEditing();
    }

    private static ClientModule ownerOf(ModuleSetting<?> setting) {
        return ClientRuntime.MODULES.stream().filter(module -> module.settings.contains(setting)).findFirst().orElse(null);
    }

    private static void settingChanged(ClientModule module) {
        if (module != null && (module.name.equals("Spammer") || module.name.equals("Announcer"))) {
            module.reset(Minecraft.getInstance());
            if (module.enabled() && Minecraft.getInstance().player != null) module.activate(Minecraft.getInstance());
        }
        ClientConfig.save(ClientRuntime.MODULES);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        int key = event.key();
        if (binding != null) {
            ClientModule target = binding;
            if (key == GLFW.GLFW_KEY_ESCAPE) { cancelBinding(); return true; }
            if (key == GLFW.GLFW_KEY_BACKSPACE || key == GLFW.GLFW_KEY_DELETE) { assignBinding(target, -1); return true; }
            if (key >= GLFW.GLFW_KEY_SPACE && key <= GLFW.GLFW_KEY_LAST) {
                if (Keybinds.guiConflict(key)) {
                    pendingBindingKey = -1;
                    pendingBindingConflicts = List.of();
                    NotificationCards.warning("Keybind blocked", Keybinds.display(key) + " opens GUI");
                    return true;
                }
                var conflicts = Keybinds.conflicts(target, key);
                if (conflicts.isEmpty()) { assignBinding(target, key); return true; }
                if (pendingBindingKey == key && pendingBindingConflicts.equals(conflicts)) {
                    replaceBinding(target, key);
                    return true;
                }
                pendingBindingKey = key;
                pendingBindingConflicts = conflicts;
                NotificationCards.warning("Key conflict",
                    Keybinds.display(key) + ": " + Keybinds.conflictNames(target, key) + " - press again");
                return true;
            }
            return true;
        }
        if (editing == null) return super.keyPressed(event);
        if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) { commitEditing(); return true; }
        if (key == GLFW.GLFW_KEY_ESCAPE) { cancelEditing(); return true; }
        if (key == GLFW.GLFW_KEY_BACKSPACE) {
            if (!editingText.isEmpty()) {
                int start = editingText.offsetByCodePoints(editingText.length(), -1);
                editingText = editingText.substring(0, start);
            }
            return true;
        }
        if (key == GLFW.GLFW_KEY_DELETE) { editingText = ""; return true; }
        return true;
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        if (editing == null) return super.charTyped(event);
        int codepoint = event.codepoint();
        if (!Character.isValidCodePoint(codepoint)) return true;
        if (editing instanceof TextSetting text) {
            if (!Character.isISOControl(codepoint)) {
                String typed = new String(Character.toChars(codepoint));
                if (editingText.length() + typed.length() <= text.maxLength) editingText += typed;
            }
            return true;
        }
        if (!(editing instanceof NumberSetting number)) return true;
        char c = (char) codepoint;
        if (Character.isDigit(c) && editingText.length() < 14) editingText += c;
        else if (c == '.' && !number.integer() && !editingText.contains("."))
            editingText += editingText.isEmpty() ? "0." : ".";
        else if (c == '-' && number.min < 0 && editingText.isEmpty()) editingText = "-";
        return true;
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        if (dragging != null) {
            dragging.x = (int) (event.x() - offsetX);
            dragging.y = (int) (event.y() - offsetY);
            return true;
        }
        if (sliding != null) { moveSlider(event.x()); return true; }
        return false;
    }

    private void moveSlider(double mouseX) {
        if (sliding == null || sliderWidth <= 0) return;
        double fraction = Math.max(0, Math.min(1, (mouseX - sliderLeft) / sliderWidth));
        double raw = sliding.min + fraction * (sliding.max - sliding.min);
        double value = sliding.min + Math.round((raw - sliding.min) / sliding.step) * sliding.step;
        sliding.set(Math.round(value * 10000) / 10000d);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        dragging = null;
        if (sliding != null) {
            settingChanged(ownerOf(sliding));
            sliding = null;
        }
        ClickGuiPersistence.save(PANELS);
        return true;
    }

    @Override
    public void removed() {
        commitEditing();
        cancelBinding();
        dragging = null;
        sliding = null;
        ClickGuiPersistence.save(PANELS);
        ClientConfig.save(ClientRuntime.MODULES);
    }

    @Override
    public boolean isPauseScreen() { return false; }

    static void smokeCheckAndArrange() {
        var screen = (ClientScreen) Minecraft.getInstance().screen;
        if (PANELS.size() != 6) throw new IllegalStateException("Expected six GUI windows");
        var player = panel("Player");
        var render = panel("Render");
        var combat = panel("Combat");
        var world = panel("World");
        var announcer = ClientRuntime.find("Announcer");
        var spammer = ClientRuntime.find("Spammer");
        if (!player.modules.contains(announcer) || !player.modules.contains(spammer))
            throw new IllegalStateException("Typed network modules missing from click GUI");
        if (spammer.settings.stream().noneMatch(setting -> setting.kind() == ModuleSetting.Kind.TEXT)
            || spammer.settings.stream().noneMatch(setting -> setting.kind() == ModuleSetting.Kind.CHOICE)
            || announcer.settings.stream().filter(setting -> setting.kind() == ModuleSetting.Kind.BOOLEAN).count() < 4)
            throw new IllegalStateException("Typed settings are incomplete");

        var flight = ClientRuntime.find("Flight");
        var elytra = ClientRuntime.find("ElytraFly");
        flight.setEnabled(true);
        elytra.setEnabled(true);
        if (flight.enabled() || !elytra.enabled() || !ModuleManager.conflict(flight, elytra))
            throw new IllegalStateException("Central conflict handling failed");
        elytra.setEnabled(false);

        for (ClickGuiPanel panel : PANELS) {
            panel.restoreOpen(true);
            panel.pinned = false;
            panel.expansions.clear();
            panel.scroll = panel.scrollFrom = panel.scrollTarget = 0;
            switch (panel.title) {
                case "Player" -> { panel.x = 2; panel.y = 2; }
                case "Render" -> { panel.x = 106; panel.y = 2; }
                case "Combat" -> { panel.x = 210; panel.y = 2; }
                case "World" -> { panel.x = 314; panel.y = 2; }
                case "Info" -> { panel.x = 418; panel.y = 2; }
                case "Radar" -> { panel.x = 418; panel.y = 85; }
            }
        }

        var expansion = player.expansion(announcer);
        expansion.setOpen(true);
        expansion.finish();
        if (ConfigContainer.height(announcer) < 100 || player.maxScroll(screen.height) <= .5f)
            throw new IllegalStateException("Announcer option container or scrolling failed");
        var delay = (NumberSetting) announcer.setting("delay");
        double oldDelay = delay.get();
        screen.beginEditing(delay);
        screen.editingText = "2400";
        screen.commitEditing();
        if (delay.get() != 2400) throw new IllegalStateException("Typed number editor failed");
        delay.set(oldDelay);

        var walking = (BooleanSetting) announcer.setting("walking");
        boolean oldWalking = walking.on();
        walking.toggle();
        if (walking.on() == oldWalking) throw new IllegalStateException("Typed boolean failed");
        walking.set(oldWalking);
        var mode = (ChoiceSetting) spammer.setting("mode");
        String oldMode = mode.get();
        mode.cycle(1);
        if (mode.get().equals(oldMode)) throw new IllegalStateException("Typed choice failed");
        mode.set(oldMode);

        int oldX = player.x;
        ClickGuiPersistence.save(PANELS);
        player.x += 50;
        ClickGuiPersistence.load(PANELS);
        if (player.x != oldX) throw new IllegalStateException("Window state roundtrip failed");
        ClientConfig.save(ClientRuntime.MODULES);
        LogUtils.getLogger().info("XDOLF_GUI_OK: redesigned typed option cards, hover descriptions, scrolling, conflicts and Announcer controls");
    }

    private static ClickGuiPanel panel(String title) {
        return PANELS.stream().filter(panel -> panel.title.equals(title)).findFirst().orElseThrow();
    }
}
