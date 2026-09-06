from pathlib import Path
import re

path = Path('src/main/java/com/x0xp/xdolf/ClientScreen.java')
text = path.read_text()


def replace_once(old, new, label):
    global text
    if old not in text:
        raise SystemExit(f'Missing patch target: {label}')
    text = text.replace(old, new, 1)

replace_once(
    '    private static final float NUMBER_FIELD_WIDTH = 23.0f;\n    private static final float TOGGLE_LABEL_SINGLE_LINE_WIDTH = 76.0f;\n',
    '    private static final float NUMBER_FIELD_WIDTH = 23.0f;\n    private static final float KEYBIND_ROW_HEIGHT = 14.0f;\n    private static final float KEYBIND_FIELD_WIDTH = 42.0f;\n    private static final float TOGGLE_LABEL_SINGLE_LINE_WIDTH = 76.0f;\n',
    'keybind layout constants')

replace_once(
    '    private ModuleSetting editing;\n    private String editingText = "";\n',
    '    private ModuleSetting editing;\n    private String editingText = "";\n    private ClientModule binding;\n',
    'binding field')

replace_once(
    '    private static float settingContainerHeight(ClientModule module) {\n        float height = 4.0f;\n        for (var row : settings(module)) height += settingRowHeight(row);\n        return height;\n    }\n',
    '    private static float settingContainerHeight(ClientModule module) {\n        float height = 4.0f + KEYBIND_ROW_HEIGHT;\n        for (var row : settings(module)) height += settingRowHeight(row);\n        return height;\n    }\n',
    'container height')

marker = '    private static void row(GuiGraphics graphics, String name, float x, float y, boolean enabled, boolean hover,\n'
if marker not in text:
    raise SystemExit('Missing row marker')
helpers = '''    private static String keyDisplay(int key) {
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

    private static void keybindRow(GuiGraphics graphics, ClientModule module, float left, float right, float y,
                                   boolean hover, float alpha, ClientScreen screen) {
        boolean listening = screen != null && screen.binding == module;
        int labelColor = hover || listening ? 0xFFFFFFFF : 0xD8FFFFFF;
        XdolfFont.draw(graphics, "Keybind", left, y + 1, fade(labelColor, alpha));

        float fieldRight = right - 1;
        float fieldLeft = fieldRight - KEYBIND_FIELD_WIDTH;
        float fieldTop = y;
        float fieldBottom = y + 11.0f;
        int fieldFill = listening ? 0xE01B2029 : hover ? 0xD0191D24 : 0xC0101318;
        int fieldBorder = listening ? 0xFF44AAFF : hover ? 0xFF7B828F : 0xFF4D535D;
        rect(graphics, fieldLeft, fieldTop, fieldRight, fieldBottom, fade(fieldFill, alpha));
        outline(graphics, fieldLeft, fieldTop, fieldRight, fieldBottom, fade(fieldBorder, alpha));

        String value = listening ? "Press..." : keyDisplay(module.key);
        value = XdolfFont.trim(value, Math.max(1, (int) (KEYBIND_FIELD_WIDTH - 4)));
        float valueX = fieldLeft + (KEYBIND_FIELD_WIDTH - XdolfFont.width(value)) / 2.0f;
        XdolfFont.draw(graphics, value, valueX, y + 1,
            fade(listening ? 0xFF44AAFF : 0xFFFFFFFF, alpha));
    }

'''
text = text.replace(marker, helpers + marker, 1)

# Replace the complete settings container so Keybind is always the first child row.
start = text.index('    private static void settingContainer(')
end = text.index('    private static void draw(', start)
new_container = '''    private static void settingContainer(GuiGraphics graphics, Panel panel, ClientModule module, float top,
                                         float progress, int mouseX, int mouseY, ClientScreen screen) {
        if (progress <= 0.001f) return;
        var rows = settings(module);
        float left = panel.x + 4;
        float right = panel.x + 96;
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
        float rowLeft = left + 4;
        float rowRight = right - 2;
        float keyFieldRight = rowRight - 1;
        float keyFieldLeft = keyFieldRight - KEYBIND_FIELD_WIDTH;
        boolean keyHover = interactive && hit(mouseX, mouseY, keyFieldLeft, y, KEYBIND_FIELD_WIDTH, 11);
        keybindRow(graphics, module, rowLeft, rowRight, y, keyHover, progress, screen);
        y += KEYBIND_ROW_HEIGHT;
        if (!rows.isEmpty()) rect(graphics, left + 4, y - 0.5f, right - 4, y, fade(0x28000000, progress));

        for (int i = 0; i < rows.size(); i++) {
            var settingRow = rows.get(i);
            boolean hover = interactive && hit(mouseX, mouseY, left + 5, y, right - left - 10, settingRowHeight(settingRow) - 1);
            if (settingRow.toggle) {
                toggleRow(graphics, settingRow, left + 4, right - 2, y, hover, progress);
            } else {
                numberRow(graphics, settingRow, left + 4, right - 2, y, hover, progress, screen);
            }
            y += settingRowHeight(settingRow);
            if (i + 1 < rows.size()) {
                rect(graphics, left + 4, y - 0.5f, right - 4, y, fade(0x28000000, progress));
            }
        }
        graphics.disableScissor();
    }

'''
text = text[:start] + new_container + text[end:]

replace_once(
    '                hit(mouseX, mouseY, panel.x + 2, moduleY, 96, 11), !moduleSettings.isEmpty(), expanded);\n',
    '                hit(mouseX, mouseY, panel.x + 2, moduleY, 96, 11), true, expanded);\n',
    'all modules configurable indicator')
replace_once(
    '            if (!moduleSettings.isEmpty() && progress > 0.001f) {\n',
    '            if (progress > 0.001f) {\n',
    'render empty-setting module config')

replace_once(
    '                        } else if (button == 1 && !rows.isEmpty()) {\n                            panel.toggleExpansion(module);\n',
    '                        } else if (button == 1) {\n                            panel.toggleExpansion(module);\n',
    'right click every module')

# Replace the module settings click block, adding the keybind button before ordinary settings.
old_block = '''                    if (!rows.isEmpty()) {
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
                                    float rowLeft = left + 4;
                                    float rowRight = right - 2;
                                    float fieldRight = rowRight - 1;
                                    float fieldLeft = fieldRight - NUMBER_FIELD_WIDTH;
                                    float fieldTop = settingY;
                                    if (button == 0 && hit(mouseX, mouseY, fieldLeft, fieldTop, NUMBER_FIELD_WIDTH, 11)) {
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
'''
new_block = '''                    {
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
                                    float fieldTop = settingY;
                                    if (button == 0 && hit(mouseX, mouseY, fieldLeft, fieldTop, NUMBER_FIELD_WIDTH, 11)) {
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
'''
replace_once(old_block, new_block, 'settings click block')

# Clicking elsewhere cancels an active key-capture operation before handling the new click.
replace_once(
    '    private boolean click(double mouseX, double mouseY, int button) {\n        for (int index = PANELS.size() - 1; index >= 0; index--) {\n',
    '    private boolean click(double mouseX, double mouseY, int button) {\n        if (binding != null) binding = null;\n        for (int index = PANELS.size() - 1; index >= 0; index--) {\n',
    'click cancels capture')

replace_once(
    '    private void beginEditing(ModuleSetting setting) {\n',
    '''    private void beginBinding(ClientModule module) {
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

    private void beginEditing(ModuleSetting setting) {
''',
    'binding helpers')

old_key = '''    @Override
    public boolean keyPressed(KeyEvent event) {
        if (editing == null) return super.keyPressed(event);
        int key = event.key();
'''
new_key = '''    @Override
    public boolean keyPressed(KeyEvent event) {
        int key = event.key();
        if (binding != null) {
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
        if (editing == null) return super.keyPressed(event);
'''
replace_once(old_key, new_key, 'key capture')

replace_once(
    '    public void removed() {\n        commitEditing();\n        dragging = null;\n',
    '    public void removed() {\n        commitEditing();\n        cancelBinding();\n        dragging = null;\n',
    'cancel binding on close')

# Smoke test: verify a module with no ordinary settings still expands and persists a key assignment.
needle = '''        var logout = render.modules.stream().filter(module -> module.name.equals("LogoutSpot")).findFirst().orElseThrow();
        if (settings(logout).size() != 1 || !settings(logout).get(0).label.equals("Tracers")) {
            throw new IllegalStateException("LogoutSpot tracer setting missing from click GUI");
        }

'''
insert = needle + '''        var storageEsp = render.modules.stream().filter(module -> module.name.equals("StorageESP")).findFirst().orElseThrow();
        if (!settings(storageEsp).isEmpty()) throw new IllegalStateException("StorageESP smoke fixture unexpectedly has ordinary settings");
        int originalStorageKey = storageEsp.key;
        screen.beginBinding(storageEsp);
        if (screen.binding != storageEsp) throw new IllegalStateException("Keybind capture did not start");
        screen.assignBinding(storageEsp, GLFW.GLFW_KEY_F8);
        if (storageEsp.key != GLFW.GLFW_KEY_F8 || screen.binding != null) {
            throw new IllegalStateException("Keybind capture did not assign F8");
        }
        storageEsp.key = originalStorageKey;
        ClientConfig.save(ClientRuntime.MODULES);

'''
replace_once(needle, insert, 'keybind smoke test')

replace_once(
    '        LogUtils.getLogger().info("XDOLF_GUI_OK: six windows, compact inline settings, typed values, animation, pin/open/drag controls");\n',
    '        LogUtils.getLogger().info("XDOLF_GUI_OK: six windows, inline keybind capture, compact settings, typed values, animation, pin/open/drag controls");\n',
    'GUI smoke log')

path.write_text(text)
print('Patched ClientScreen with inline per-module keybind capture UI')
