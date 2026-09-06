from pathlib import Path
import re

root = Path('.')
screen_path = root / 'src/main/java/com/x0xp/xdolf/ClientScreen.java'
notif_path = root / 'src/main/java/com/x0xp/xdolf/NotificationCards.java'
screen = screen_path.read_text()


def replace_once(old, new, label):
    global screen
    if old not in screen:
        raise SystemExit(f'missing patch anchor: {label}')
    screen = screen.replace(old, new, 1)

# Shared UI drawing primitives extracted from ClientScreen / NotificationCards.
(root / 'src/main/java/com/x0xp/xdolf/UiDraw.java').write_text('''package com.x0xp.xdolf;

import net.minecraft.client.gui.GuiGraphics;

/** Shared pixel-aligned primitives for Xdolf HUD and click-GUI rendering. */
final class UiDraw {
    private UiDraw() {}

    static void rect(GuiGraphics graphics, float x, float y, float right, float bottom, int color) {
        graphics.pose().pushMatrix();
        graphics.pose().scale(0.5f, 0.5f);
        graphics.fill(Math.round(x * 2), Math.round(y * 2), Math.round(right * 2), Math.round(bottom * 2), color);
        graphics.pose().popMatrix();
    }

    static void outline(GuiGraphics graphics, float x, float y, float right, float bottom, int color) {
        rect(graphics, x, y, right, y + 0.5f, color);
        rect(graphics, x, bottom - 0.5f, right, bottom, color);
        rect(graphics, x, y, x + 0.5f, bottom, color);
        rect(graphics, right - 0.5f, y, right, bottom, color);
    }

    static void border(GuiGraphics graphics, float x, float y, float right, float bottom, int inside) {
        rect(graphics, x, y, right, bottom, inside);
        outline(graphics, x, y, right + 0.5f, bottom + 0.5f, 0xFF000000);
    }

    static int fade(int color, float alpha) {
        int originalAlpha = color >>> 24;
        int fadedAlpha = Math.max(0, Math.min(255, Math.round(originalAlpha * clamp01(alpha))));
        return (color & 0x00FFFFFF) | fadedAlpha << 24;
    }

    static boolean hit(double mouseX, double mouseY, double x, double y, double width, double height) {
        return mouseX >= x && mouseY >= y && mouseX <= x + width && mouseY <= y + height;
    }

    static float clamp01(float value) {
        return Math.max(0.0f, Math.min(1.0f, value));
    }

    static float easeOutCubic(float t) {
        float inverse = 1.0f - clamp01(t);
        return 1.0f - inverse * inverse * inverse;
    }

    static float easeInCubic(float t) {
        t = clamp01(t);
        return t * t * t;
    }
}
''')

(root / 'src/main/java/com/x0xp/xdolf/Keybinds.java').write_text('''package com.x0xp.xdolf;

import org.lwjgl.glfw.GLFW;
import java.util.List;
import java.util.stream.Collectors;

/** Key display and conflict rules shared by the click GUI and runtime configuration. */
final class Keybinds {
    private Keybinds() {}

    static String display(int key) {
        String name = KeyNames.name(key);
        return switch (name) {
            case "NONE" -> "None";
            case "LEFT_SHIFT" -> "LShift";
            case "RIGHT_SHIFT" -> "RShift";
            case "LEFT_CONTROL" -> "LCtrl";
            case "RIGHT_CONTROL" -> "RCtrl";
            case "LEFT_ALT" -> "LAlt";
            case "RIGHT_ALT" -> "RAlt";
            case "LEFT_SUPER" -> "LSuper";
            case "RIGHT_SUPER" -> "RSuper";
            case "GRAVE_ACCENT" -> "Grave";
            case "PAGE_UP" -> "PgUp";
            case "PAGE_DOWN" -> "PgDn";
            case "CAPS_LOCK" -> "Caps";
            case "SCROLL_LOCK" -> "Scroll";
            case "PRINT_SCREEN" -> "PrtSc";
            default -> name.replace('_', ' ');
        };
    }

    static boolean guiConflict(int key) {
        return key >= 0 && (key == ClientConfig.guiKey
            || (ClientConfig.guiKey == GLFW.GLFW_KEY_GRAVE_ACCENT && key == GLFW.GLFW_KEY_RIGHT_SHIFT));
    }

    static List<ClientModule> conflicts(ClientModule target, int key) {
        if (key < 0) return List.of();
        return ClientRuntime.MODULES.stream()
            .filter(module -> module != target && module.key == key)
            .toList();
    }

    static String conflictNames(ClientModule target, int key) {
        return conflicts(target, key).stream().map(ClientScreen::label).collect(Collectors.joining(", "));
    }

    static void replaceConflicts(ClientModule target, int key) {
        for (var conflict : conflicts(target, key)) conflict.key = -1;
        target.key = key;
    }
}
''')

# Notification polish: fixed geometry, generic warning cards, smooth reflow and lifetime bar.
notif_path.write_text('''package com.x0xp.xdolf;

import net.minecraft.client.gui.GuiGraphics;
import java.util.ArrayList;
import java.util.List;

import static com.x0xp.xdolf.UiDraw.*;

/** Animated centre-left HUD cards for module state and short client notices. */
final class NotificationCards {
    private static final int MAX_VISIBLE = 3;
    private static final float LEFT = 7.0f;
    private static final float CARD_WIDTH = 142.0f;
    private static final float CARD_HEIGHT = 22.0f;
    private static final float CARD_GAP = 4.0f;

    private static final long ENTER_NS = 170_000_000L;
    private static final long MOVE_NS = 150_000_000L;
    private static final long HOLD_NS = 2_700_000_000L;
    private static final long FADE_NS = 320_000_000L;

    private static final List<Card> ACTIVE = new ArrayList<>();
    private static final List<Card> EXITING = new ArrayList<>();

    private NotificationCards() {}

    static void module(ClientModule module, boolean enabled) {
        show(ClientScreen.label(module), enabled ? "Enabled" : "Disabled",
            enabled ? 0xFF35D07F : 0xFFFF4D5E,
            enabled ? 0xFF72E8A6 : 0xFFFF7B88);
    }

    static void warning(String title, String detail) {
        show(title, detail, 0xFFFFB347, 0xFFFFC66D);
    }

    private static void show(String title, String detail, int accent, int detailColor) {
        long now = System.nanoTime();
        expire(now);
        for (Card card : ACTIVE) card.moveTo(card.targetSlot + 1.0f, now);
        if (ACTIVE.size() >= MAX_VISIBLE) {
            Card oldest = ACTIVE.remove(ACTIVE.size() - 1);
            oldest.startExit(now, true);
            EXITING.add(oldest);
        }
        ACTIVE.add(0, new Card(title, detail, accent, detailColor, now));
        reflow(now);
    }

    static void render(GuiGraphics graphics) {
        long now = System.nanoTime();
        expire(now);
        int screenHeight = graphics.guiHeight();
        for (int i = EXITING.size() - 1; i >= 0; i--) {
            Card card = EXITING.get(i);
            if (card.finished(now)) {
                EXITING.remove(i);
                continue;
            }
            draw(graphics, card, screenHeight, now);
        }
        for (int i = ACTIVE.size() - 1; i >= 0; i--) draw(graphics, ACTIVE.get(i), screenHeight, now);
    }

    static int visibleCount() { return ACTIVE.size(); }

    static void clear() {
        ACTIVE.clear();
        EXITING.clear();
    }

    private static void expire(long now) {
        boolean changed = false;
        for (int i = ACTIVE.size() - 1; i >= 0; i--) {
            Card card = ACTIVE.get(i);
            if (now - card.created < ENTER_NS + HOLD_NS) continue;
            ACTIVE.remove(i);
            card.startExit(now, false);
            EXITING.add(card);
            changed = true;
        }
        if (changed) reflow(now);
    }

    private static void reflow(long now) {
        for (int i = 0; i < ACTIVE.size(); i++) ACTIVE.get(i).moveTo(i, now);
    }

    private static void draw(GuiGraphics graphics, Card card, int screenHeight, long now) {
        float enter = clamp01((now - card.created) / (float) ENTER_NS);
        float alpha = enter;
        float exit = 0.0f;
        if (card.exiting) {
            exit = clamp01((now - card.exitStarted) / (float) FADE_NS);
            alpha *= 1.0f - easeInCubic(exit);
        }
        if (alpha <= 0.01f) return;

        float slot = card.slot(now);
        float y = screenHeight / 2.0f - CARD_HEIGHT / 2.0f + slot * (CARD_HEIGHT + CARD_GAP);
        float hiddenX = -CARD_WIDTH - 6.0f;
        float x = hiddenX + (LEFT - hiddenX) * easeOutCubic(enter) - 10.0f * easeInCubic(exit);

        graphics.pose().pushMatrix();
        graphics.pose().translate(x, y);

        rect(graphics, 0, 0, CARD_WIDTH, CARD_HEIGHT, fade(0xE014171D, alpha));
        outline(graphics, 0, 0, CARD_WIDTH, CARD_HEIGHT, fade(0xF0000000, alpha));
        rect(graphics, 1, 1, 3.5f, CARD_HEIGHT - 1, fade(card.accent, alpha));

        int detailWidth = Math.min(63, XdolfFont.width(card.detail));
        String detail = XdolfFont.trim(card.detail, detailWidth);
        detailWidth = XdolfFont.width(detail);
        int titleAvailable = Math.max(24, Math.round(CARD_WIDTH) - detailWidth - 18);
        String title = XdolfFont.trim(card.title, titleAvailable);

        XdolfFont.draw(graphics, title, 8, 6, fade(0xFFFFFFFF, alpha));
        XdolfFont.draw(graphics, detail, CARD_WIDTH - detailWidth - 6, 6, fade(card.detailColor, alpha));

        if (!card.exiting) {
            float held = clamp01((now - card.created - ENTER_NS) / (float) HOLD_NS);
            float remaining = 1.0f - held;
            rect(graphics, 4, CARD_HEIGHT - 1.5f, 4 + (CARD_WIDTH - 8) * remaining, CARD_HEIGHT - 1,
                fade(card.accent, alpha * 0.65f));
        }
        graphics.pose().popMatrix();
    }

    private static final class Card {
        final String title;
        final String detail;
        final int accent;
        final int detailColor;
        final long created;
        float fromSlot;
        float targetSlot;
        long moveStarted;
        boolean exiting;
        long exitStarted;

        Card(String title, String detail, int accent, int detailColor, long created) {
            this.title = title;
            this.detail = detail;
            this.accent = accent;
            this.detailColor = detailColor;
            this.created = created;
            moveStarted = created;
        }

        float slot(long now) {
            float t = clamp01((now - moveStarted) / (float) MOVE_NS);
            return fromSlot + (targetSlot - fromSlot) * easeOutCubic(t);
        }

        void moveTo(float target, long now) {
            if (targetSlot == target && moveStarted != 0) return;
            fromSlot = slot(now);
            targetSlot = target;
            moveStarted = now;
        }

        void startExit(long now, boolean pushedOut) {
            if (exiting) return;
            exiting = true;
            exitStarted = now;
            if (pushedOut) moveTo(Math.max(targetSlot, MAX_VISIBLE), now);
        }

        boolean finished(long now) { return exiting && now - exitStarted >= FADE_NS; }
    }
}
''')

# Static shared drawing helpers.
replace_once('import java.util.Properties;\n', 'import java.util.Properties;\n\nimport static com.x0xp.xdolf.UiDraw.*;\n', 'static UiDraw import')
replace_once('    private static final float KEYBIND_FIELD_WIDTH = 42.0f;\n', '    private static final float KEYBIND_FIELD_WIDTH = 48.0f;\n    private static final float CATEGORY_MAX_BODY_HEIGHT = 205.0f;\n    private static final long SCROLL_ANIMATION_NS = 120_000_000L;\n', 'scroll constants')
replace_once('    private ClientModule binding;\n    private double offsetX, offsetY;\n', '    private ClientModule binding;\n    private int pendingBindingKey = -1;\n    private List<ClientModule> pendingBindingConflicts = List.of();\n    private double offsetX, offsetY;\n', 'binding conflict state')
replace_once('        int x, y;\n        boolean open, pinned;\n', '        int x, y;\n        boolean open, pinned;\n        float scroll, scrollFrom, scrollTarget;\n        long scrollStarted;\n', 'panel scroll fields')

old_height = '''        float height() {
            if (text()) return open ? lines(this).size() * 10 + 16 : 14;
            if (!open) return 13;
            float height = 13 + modules.size() * 12 + 0.5f;
            for (var module : modules) height += animatedSettingsHeight(module);
            return height;
        }
'''
new_height = '''        float height() {
            if (text()) return open ? lines(this).size() * 10 + 16 : 14;
            if (!open) return 13;
            float height = 13 + modules.size() * 12 + 0.5f;
            for (var module : modules) height += animatedSettingsHeight(module);
            return height;
        }

        float displayHeight(int screenHeight) {
            if (text() || !open) return height();
            float availableBody = Math.max(48.0f, Math.min(CATEGORY_MAX_BODY_HEIGHT, screenHeight - y - 21.0f));
            return 13.0f + Math.min(height() - 13.0f, availableBody);
        }

        float maxScroll(int screenHeight) {
            return Math.max(0.0f, height() - displayHeight(screenHeight));
        }

        float scrollValue(int screenHeight) {
            float max = maxScroll(screenHeight);
            if (max <= 0.01f) {
                scroll = scrollFrom = scrollTarget = 0.0f;
                return 0.0f;
            }
            scrollTarget = Math.max(0.0f, Math.min(max, scrollTarget));
            if (scroll == scrollTarget) return scroll;
            float elapsed = Math.min(1.0f, (System.nanoTime() - scrollStarted) / (float) SCROLL_ANIMATION_NS);
            scroll = scrollFrom + (scrollTarget - scrollFrom) * easeOutCubic(elapsed);
            if (elapsed >= 1.0f) scroll = scrollTarget;
            return Math.max(0.0f, Math.min(max, scroll));
        }

        void scrollBy(float amount, int screenHeight) {
            float current = scrollValue(screenHeight);
            scrollFrom = current;
            scrollTarget = Math.max(0.0f, Math.min(maxScroll(screenHeight), scrollTarget + amount));
            scrollStarted = System.nanoTime();
        }

        void finishScroll(int screenHeight) {
            scroll = scrollTarget = Math.max(0.0f, Math.min(maxScroll(screenHeight), scrollTarget));
            scrollFrom = scroll;
        }
'''
replace_once(old_height, new_height, 'panel height scrolling')

# Remove local rendering primitives now extracted to UiDraw.
pattern = re.compile(r'''    private static void rect\(GuiGraphics graphics, float x, float y, float right, float bottom, int color\) \{.*?    private static boolean hit\(double mouseX, double mouseY, double x, double y, double width, double height\) \{\n        return mouseX >= x && mouseY >= y && mouseX <= x \+ width && mouseY <= y \+ height;\n    \}\n\n''', re.S)
screen, count = pattern.subn('', screen, count=1)
if count != 1: raise SystemExit('failed to extract UiDraw helpers')

# Remove local key display formatting now extracted to Keybinds.
pattern = re.compile(r'''    private static String keyDisplay\(int key\) \{.*?\n    \}\n\n    private static void keybindRow''', re.S)
screen, count = pattern.subn('    private static void keybindRow', screen, count=1)
if count != 1: raise SystemExit('failed to extract key display helper')

# Keybind row: show pending/current conflicts clearly.
pattern = re.compile(r'''    private static void keybindRow\(GuiGraphics graphics, ClientModule module, float left, float right, float y,\n                                   boolean hover, float alpha, ClientScreen screen\) \{.*?\n    \}\n\n    private static void row''', re.S)
keybind_row = '''    private static void keybindRow(GuiGraphics graphics, ClientModule module, float left, float right, float y,
                                   boolean hover, float alpha, ClientScreen screen) {
        boolean listening = screen != null && screen.binding == module;
        boolean pending = listening && screen.pendingBindingKey >= 0 && !screen.pendingBindingConflicts.isEmpty();
        boolean existingConflict = !listening && module.key >= 0 && !Keybinds.conflicts(module, module.key).isEmpty();
        int labelColor = pending ? 0xFFFFC66D : hover || listening ? 0xFFFFFFFF : 0xD8FFFFFF;
        XdolfFont.draw(graphics, "Keybind", left, y + 1, fade(labelColor, alpha));

        float fieldRight = right - 1;
        float fieldLeft = fieldRight - KEYBIND_FIELD_WIDTH;
        float fieldTop = y;
        float fieldBottom = y + 11.0f;
        int fieldFill = listening ? 0xE01B2029 : hover ? 0xD0191D24 : 0xC0101318;
        int fieldBorder = pending ? 0xFFFFB347 : existingConflict ? 0xFFFF5D6C
            : listening ? 0xFF44AAFF : hover ? 0xFF7B828F : 0xFF4D535D;
        rect(graphics, fieldLeft, fieldTop, fieldRight, fieldBottom, fade(fieldFill, alpha));
        outline(graphics, fieldLeft, fieldTop, fieldRight, fieldBottom, fade(fieldBorder, alpha));

        String value = pending ? Keybinds.display(screen.pendingBindingKey) + " !"
            : listening ? "Press..." : Keybinds.display(module.key) + (existingConflict ? " !" : "");
        value = XdolfFont.trim(value, Math.max(1, (int) (KEYBIND_FIELD_WIDTH - 4)));
        float valueX = fieldLeft + (KEYBIND_FIELD_WIDTH - XdolfFont.width(value)) / 2.0f;
        XdolfFont.draw(graphics, value, valueX, y + 1,
            fade(pending ? 0xFFFFC66D : listening ? 0xFF44AAFF : 0xFFFFFFFF, alpha));
    }

    private static void row'''
screen, count = pattern.subn(keybind_row, screen, count=1)
if count != 1: raise SystemExit('failed keybind row replacement')

# Draw category bodies through a fixed viewport with smooth scroll and a scrollbar.
pattern = re.compile(r'''    private static void draw\(GuiGraphics graphics, Panel panel, int mouseX, int mouseY, boolean controls, ClientScreen screen\) \{.*?\n    \}\n\n    private static List<\? extends Player> radar''', re.S)
draw = '''    private static void draw(GuiGraphics graphics, Panel panel, int mouseX, int mouseY, boolean controls, ClientScreen screen) {
        float displayHeight = panel.displayHeight(graphics.guiHeight());
        border(graphics, panel.x, panel.y, panel.x + 100, panel.y + displayHeight, 0x80000000);
        XdolfFont.draw(graphics, panel.title, panel.x + 3, panel.y + 1, 0xFFFFFFFF);
        if (controls) {
            border(graphics, panel.x + 79, panel.y + 2, panel.x + 88, panel.y + 11,
                panel.pinned ? 0xFFFF0000 : 0xFF383B42);
            border(graphics, panel.x + 89, panel.y + 2, panel.x + 98, panel.y + 11,
                panel.open ? 0xFFFF0000 : 0xFF383B42);
        }
        if (!panel.open) return;

        if (panel.text()) {
            var textLines = lines(panel);
            for (int i = 0; i < textLines.size(); i++) {
                XdolfFont.draw(graphics, XdolfFont.trim(textLines.get(i), 97), panel.x + 3, panel.y + 13 + i * 10, 0xFFFFFFFF);
            }
            return;
        }

        float bodyTop = panel.y + 12.0f;
        float bodyBottom = panel.y + displayHeight - 0.5f;
        if (bodyBottom <= bodyTop) return;
        float scroll = panel.scrollValue(graphics.guiHeight());
        boolean mouseInBody = mouseY >= bodyTop && mouseY <= bodyBottom;
        int bodyMouseY = mouseInBody ? mouseY : -10000;

        graphics.enableScissor(panel.x, (int) Math.floor(bodyTop), panel.x + 100, (int) Math.ceil(bodyBottom));
        float moduleY = bodyTop - scroll;
        for (var module : panel.modules) {
            var expansion = panel.expansions.get(module);
            float progress = expansion == null ? 0.0f : expansion.value();
            boolean expanded = expansion != null && expansion.open;
            row(graphics, label(module), panel.x + 2, moduleY, module.enabled(),
                mouseInBody && hit(mouseX, mouseY, panel.x + 2, moduleY, 96, 11), true, expanded);
            moduleY += 12;

            if (progress > 0.001f) {
                settingContainer(graphics, panel, module, moduleY, progress, mouseX, bodyMouseY, screen);
                moduleY += settingContainerHeight(module) * progress;
            }
        }
        graphics.disableScissor();

        float maxScroll = panel.maxScroll(graphics.guiHeight());
        if (maxScroll > 0.5f) {
            float trackTop = bodyTop + 2;
            float trackBottom = bodyBottom - 2;
            float trackHeight = Math.max(1, trackBottom - trackTop);
            float bodyContent = Math.max(1, panel.height() - 13.0f);
            float visibleBody = Math.max(1, displayHeight - 13.0f);
            float thumbHeight = Math.max(16.0f, trackHeight * Math.min(1.0f, visibleBody / bodyContent));
            float travel = Math.max(0, trackHeight - thumbHeight);
            float thumbY = trackTop + travel * (scroll / maxScroll);
            rect(graphics, panel.x + 97.0f, trackTop, panel.x + 98.0f, trackBottom, 0x553A3D44);
            rect(graphics, panel.x + 96.5f, thumbY, panel.x + 98.5f, thumbY + thumbHeight, 0xCC7B828F);
        }
    }

    private static List<? extends Player> radar'''
screen, count = pattern.subn(draw, screen, count=1)
if count != 1: raise SystemExit('failed draw scrolling replacement')

# Click handling must use displayed height and scrolled module coordinates.
pattern = re.compile(r'''    private boolean click\(double mouseX, double mouseY, int button\) \{.*?\n    \}\n\n    private void beginBinding''', re.S)
click = '''    private boolean click(double mouseX, double mouseY, int button) {
        if (binding != null) cancelBinding();
        for (int index = PANELS.size() - 1; index >= 0; index--) {
            var panel = PANELS.get(index);
            float displayHeight = panel.displayHeight(height);
            if (!hit(mouseX, mouseY, panel.x, panel.y, 100, displayHeight)) continue;
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
            } else if (panel.open && !panel.text() && mouseY >= panel.y + 12) {
                float moduleY = panel.y + 12 - panel.scrollValue(height);
                for (var module : panel.modules) {
                    var rows = settings(module);
                    if (hit(mouseX, mouseY, panel.x + 2, moduleY, 96, 11)) {
                        commitEditing();
                        if (button == 0) ClientRuntime.toggle(module);
                        else if (button == 1) panel.toggleExpansion(module);
                        return true;
                    }
                    moduleY += 12;

                    var expansion = panel.expansions.get(module);
                    float progress = expansion == null ? 0.0f : expansion.value();
                    float fullHeight = settingContainerHeight(module);
                    if (expansion != null && expansion.open && progress >= 0.95f) {
                        float left = panel.x + 4;
                        float right = panel.x + 96;
                        float settingY = moduleY + 2;
                        float rowLeft = left + 4;
                        float rowRight = right - 2;
                        float keyFieldRight = rowRight - 1;
                        float keyFieldLeft = keyFieldRight - KEYBIND_FIELD_WIDTH;
                        if (button == 0 && hit(mouseX, mouseY, keyFieldLeft, settingY, KEYBIND_FIELD_WIDTH, 11)) {
                            commitEditing();
                            beginBinding(module);
                            return true;
                        }
                        settingY += KEYBIND_ROW_HEIGHT;

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
                                float fieldRight = rowRight - 1;
                                float fieldLeft = fieldRight - NUMBER_FIELD_WIDTH;
                                if (button == 0 && hit(mouseX, mouseY, fieldLeft, settingY, NUMBER_FIELD_WIDTH, 11)) {
                                    beginEditing(settingRow.setting);
                                    return true;
                                }
                                float trackTop = settingY + 14.0f;
                                if (button == 0 && hit(mouseX, mouseY, rowLeft, trackTop, rowRight - rowLeft - 1, 7)) {
                                    commitEditing();
                                    sliding = settingRow.setting;
                                    sliderLeft = rowLeft;
                                    sliderWidth = rowRight - rowLeft - 1;
                                    moveSlider(mouseX);
                                    return true;
                                }
                            }
                            settingY += settingRowHeight(settingRow);
                        }
                    }
                    moduleY += fullHeight * progress;
                }
            } else if (panel.open && panel.title.equals("Radar")) {
                commitEditing();
                int row = (int) ((mouseY - panel.y - 13) / 10);
                var players = radar();
                if (mouseY >= panel.y + 13 && row >= 0 && row < players.size()) {
                    String name = players.get(row).getName().getString();
                    SocialState.command(new String[] {"friend", SocialState.isFriend(name) ? "remove" : "add", name});
                }
            }
            return true;
        }
        commitEditing();
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        for (int index = PANELS.size() - 1; index >= 0; index--) {
            var panel = PANELS.get(index);
            if (!panel.open || panel.text() || panel.maxScroll(height) <= 0.5f) continue;
            float displayHeight = panel.displayHeight(height);
            if (!hit(mouseX, mouseY, panel.x, panel.y + 12, 100, displayHeight - 12)) continue;
            panel.scrollBy((float) (-scrollY * 32.0), height);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    private void beginBinding'''
screen, count = pattern.subn(click, screen, count=1)
if count != 1: raise SystemExit('failed click scrolling replacement')

old_binding_methods = '''    private void beginBinding(ClientModule module) {
        binding = module;
    }

    private void assignBinding(ClientModule module, int key) {
        module.key = key;
        binding = null;
        ClientConfig.save(ClientRuntime.MODULES);
    }

    private void cancelBinding() {
        binding = null;
    }
'''
new_binding_methods = '''    private void beginBinding(ClientModule module) {
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
'''
replace_once(old_binding_methods, new_binding_methods, 'binding methods')

old_key_block = '''        if (binding != null) {
            ClientModule target = binding;
            if (key == GLFW.GLFW_KEY_ESCAPE) {
                cancelBinding();
                return true;
            }
            if (key == GLFW.GLFW_KEY_BACKSPACE || key == GLFW.GLFW_KEY_DELETE) {
                assignBinding(target, -1);
                return true;
            }
            if (key >= GLFW.GLFW_KEY_SPACE && key <= GLFW.GLFW_KEY_LAST) {
                assignBinding(target, key);
                return true;
            }
            return true;
        }
'''
new_key_block = '''        if (binding != null) {
            ClientModule target = binding;
            if (key == GLFW.GLFW_KEY_ESCAPE) {
                cancelBinding();
                return true;
            }
            if (key == GLFW.GLFW_KEY_BACKSPACE || key == GLFW.GLFW_KEY_DELETE) {
                assignBinding(target, -1);
                return true;
            }
            if (key >= GLFW.GLFW_KEY_SPACE && key <= GLFW.GLFW_KEY_LAST) {
                if (Keybinds.guiConflict(key)) {
                    pendingBindingKey = -1;
                    pendingBindingConflicts = List.of();
                    NotificationCards.warning("Keybind blocked", Keybinds.display(key) + " opens GUI");
                    return true;
                }
                var conflicts = Keybinds.conflicts(target, key);
                if (conflicts.isEmpty()) {
                    assignBinding(target, key);
                    return true;
                }
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
'''
replace_once(old_key_block, new_key_block, 'key conflict handling')

# Smoke coverage for conflicts + category scrolling.
old_storage = '''        int originalStorageKey = storageEsp.key;
        screen.beginBinding(storageEsp);
        if (screen.binding != storageEsp) throw new IllegalStateException("Keybind capture did not start");
        screen.assignBinding(storageEsp, GLFW.GLFW_KEY_F8);
        if (storageEsp.key != GLFW.GLFW_KEY_F8 || screen.binding != null) {
            throw new IllegalStateException("Keybind capture did not assign F8");
        }
        storageEsp.key = originalStorageKey;
        ClientConfig.save(ClientRuntime.MODULES);
'''
new_storage = '''        int originalStorageKey = storageEsp.key;
        var tracers = render.modules.stream().filter(module -> module.name.equals("Tracers")).findFirst().orElseThrow();
        int originalTracerKey = tracers.key;
        storageEsp.key = GLFW.GLFW_KEY_F8;
        tracers.key = GLFW.GLFW_KEY_F8;
        if (Keybinds.conflicts(storageEsp, GLFW.GLFW_KEY_F8).size() != 1 || !Keybinds.guiConflict(ClientConfig.guiKey)) {
            throw new IllegalStateException("Keybind conflict detection failed");
        }
        Keybinds.replaceConflicts(storageEsp, GLFW.GLFW_KEY_F8);
        if (tracers.key != -1 || storageEsp.key != GLFW.GLFW_KEY_F8) {
            throw new IllegalStateException("Exclusive keybind replacement failed");
        }
        screen.beginBinding(storageEsp);
        if (screen.binding != storageEsp) throw new IllegalStateException("Keybind capture did not start");
        screen.assignBinding(storageEsp, GLFW.GLFW_KEY_F9);
        if (storageEsp.key != GLFW.GLFW_KEY_F9 || screen.binding != null) {
            throw new IllegalStateException("Keybind capture did not assign F9");
        }
        storageEsp.key = originalStorageKey;
        tracers.key = originalTracerKey;
        ClientConfig.save(ClientRuntime.MODULES);
'''
replace_once(old_storage, new_storage, 'keybind smoke')

# Reset scroll when arranging panels and validate smooth scroll on a tall expanded Player panel.
replace_once('''            panel.open = true;
            panel.pinned = false;
            panel.expansions.clear();
''', '''            panel.open = true;
            panel.pinned = false;
            panel.expansions.clear();
            panel.scroll = panel.scrollFrom = panel.scrollTarget = 0.0f;
''', 'arrange scroll reset')

insert_anchor = '''        var killAura = combat.modules.stream().filter(module -> module.name.equals("KillAura")).findFirst().orElseThrow();
'''
insert = '''        var autoFish = player.modules.stream().filter(module -> module.name.equals("AutoFish")).findFirst().orElseThrow();
        var autoFishExpansion = player.expansion(autoFish);
        autoFishExpansion.setOpen(true);
        autoFishExpansion.finish();
        if (player.maxScroll(screen.height) <= 0.5f || player.displayHeight(screen.height) >= player.height()) {
            throw new IllegalStateException("Category panel did not become scrollable");
        }
        player.scrollBy(40.0f, screen.height);
        player.finishScroll(screen.height);
        if (player.scroll <= 0.0f) throw new IllegalStateException("Category panel scroll did not advance");
        autoFishExpansion.setOpen(false);
        autoFishExpansion.finish();
        player.scroll = player.scrollFrom = player.scrollTarget = 0.0f;

        var killAura = combat.modules.stream().filter(module -> module.name.equals("KillAura")).findFirst().orElseThrow();
'''
replace_once(insert_anchor, insert, 'scroll smoke')
replace_once('''        LogUtils.getLogger().info("XDOLF_GUI_OK: six windows, inline keybind capture, compact settings, typed values, animation, pin/open/drag controls");
''', '''        LogUtils.getLogger().info("XDOLF_GUI_OK: six windows, smooth category scrolling, keybind conflicts, compact settings, typed values, animation, pin/open/drag controls");
''', 'smoke log')

screen_path.write_text(screen)
print('Applied category scrolling, keybind conflict handling, notification polish and UI cleanup')
