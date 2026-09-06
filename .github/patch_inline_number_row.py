from pathlib import Path
import re

path = Path('src/main/java/com/x0xp/xdolf/ClientScreen.java')
text = path.read_text()

text = text.replace('    private static final float NUMBER_STACKED_ROW_HEIGHT = 36.0f;\n', '')
text = text.replace('    private static final float NUMBER_FIELD_WIDTH = 31.0f;\n',
                    '    private static final float NUMBER_FIELD_WIDTH = 23.0f;\n')
text = text.replace('    private static final float NUMBER_LABEL_COMPACT_WIDTH = 45.0f;\n', '')

text, count = re.subn(
    r'\n    private static boolean stackedNumber\(SettingRow row\) \{.*?\n    \}\n',
    '\n', text, count=1, flags=re.S)
if count != 1:
    raise SystemExit(f'Expected stackedNumber helper once, found {count}')

old_height = '''    private static float settingRowHeight(SettingRow row) {
        if (row.toggle) return wrappedToggle(row) ? BOOLEAN_WRAPPED_ROW_HEIGHT : BOOLEAN_ROW_HEIGHT;
        return stackedNumber(row) ? NUMBER_STACKED_ROW_HEIGHT : NUMBER_ROW_HEIGHT;
    }'''
new_height = '''    private static float settingRowHeight(SettingRow row) {
        if (row.toggle) return wrappedToggle(row) ? BOOLEAN_WRAPPED_ROW_HEIGHT : BOOLEAN_ROW_HEIGHT;
        return NUMBER_ROW_HEIGHT;
    }'''
if old_height not in text:
    raise SystemExit('Expected adaptive numeric row-height block not found')
text = text.replace(old_height, new_height, 1)

text, count = re.subn(
    r'\n    private static float numberFieldTop\(SettingRow row, float y\) \{.*?\n    \}\n\n'
    r'    private static float numberTrackTop\(SettingRow row, float y\) \{.*?\n    \}\n',
    '\n', text, count=1, flags=re.S)
if count != 1:
    raise SystemExit(f'Expected number positioning helpers once, found {count}')

start = text.index('    private static void numberRow(')
end = text.index('    private static void settingContainer(', start)
new_number = '''    private static void numberRow(GuiGraphics graphics, SettingRow row, float left, float right, float y,
                                  boolean hover, float alpha, ClientScreen screen) {
        boolean editing = screen != null && screen.editing == row.setting;

        float fieldRight = right - 1;
        float fieldLeft = fieldRight - NUMBER_FIELD_WIDTH;
        float fieldTop = y;
        float fieldBottom = y + 10.5f;
        float labelRight = fieldLeft - 2;
        String label = XdolfFont.trim(row.label, Math.max(1, (int) (labelRight - left)));
        XdolfFont.draw(graphics, label, left, y, fade(hover ? 0xFFFFFFFF : 0xD8FFFFFF, alpha));

        int fieldFill = editing ? 0xE01B2029 : hover ? 0xD0191D24 : 0xC0101318;
        int fieldBorder = editing ? 0xFF44AAFF : hover ? 0xFF7B828F : 0xFF4D535D;
        rect(graphics, fieldLeft, fieldTop, fieldRight, fieldBottom, fade(fieldFill, alpha));
        outline(graphics, fieldLeft, fieldTop, fieldRight, fieldBottom, fade(fieldBorder, alpha));

        String value = editing ? screen.editingText : row.setting.display();
        if (editing && (System.currentTimeMillis() / 450L) % 2 == 0) value += "_";
        value = XdolfFont.trim(value, (int) (fieldRight - fieldLeft - 3));
        float valueX = fieldRight - 1.5f - XdolfFont.width(value);
        XdolfFont.draw(graphics, value, Math.max(fieldLeft + 1.5f, valueX), y, fade(0xFFFFFFFF, alpha));

        float trackLeft = left;
        float trackRight = right - 1;
        float trackTop = y + 14.0f;
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

'''
text = text[:start] + new_number + text[end:]

text = text.replace('        float left = panel.x + 5;\n        float right = panel.x + 95;\n',
                    '        float left = panel.x + 4;\n        float right = panel.x + 96;\n', 2)
text = text.replace('                toggleRow(graphics, settingRow, left + 5, right - 3, y, hover, progress);\n',
                    '                toggleRow(graphics, settingRow, left + 4, right - 2, y, hover, progress);\n')
text = text.replace('                numberRow(graphics, settingRow, left + 5, right - 3, y, hover, progress, screen);\n',
                    '                numberRow(graphics, settingRow, left + 4, right - 2, y, hover, progress, screen);\n')

text = text.replace('                                    float rowLeft = left + 5;\n                                    float rowRight = right - 3;\n                                    float fieldRight = rowRight - 3;\n                                    float fieldLeft = fieldRight - NUMBER_FIELD_WIDTH;\n                                    float fieldTop = numberFieldTop(settingRow, settingY);\n',
                    '                                    float rowLeft = left + 4;\n                                    float rowRight = right - 2;\n                                    float fieldRight = rowRight - 1;\n                                    float fieldLeft = fieldRight - NUMBER_FIELD_WIDTH;\n                                    float fieldTop = settingY;\n')
text = text.replace('                                    float trackTop = numberTrackTop(settingRow, settingY);\n                                    if (button == 0 && hit(mouseX, mouseY, rowLeft, trackTop, rowRight - rowLeft - 3, 7)) {\n',
                    '                                    float trackTop = settingY + 14.0f;\n                                    if (button == 0 && hit(mouseX, mouseY, rowLeft, trackTop, rowRight - rowLeft - 1, 7)) {\n')
text = text.replace('                                        sliderWidth = rowRight - rowLeft - 3;\n',
                    '                                        sliderWidth = rowRight - rowLeft - 1;\n')

old_smoke = '''        var autoLog = combat.modules.stream().filter(module -> module.name.equals("AutoLog")).findFirst().orElseThrow();
        var health = settings(autoLog).stream().filter(row -> row.setting.name.equals("health")).findFirst().orElseThrow();
        if (!stackedNumber(health) || settingRowHeight(health) <= NUMBER_ROW_HEIGHT) {
            throw new IllegalStateException("Long numeric labels are not using stacked layout");
        }
'''
new_smoke = '''        var autoLog = combat.modules.stream().filter(module -> module.name.equals("AutoLog")).findFirst().orElseThrow();
        var health = settings(autoLog).stream().filter(row -> row.setting.name.equals("health")).findFirst().orElseThrow();
        if (settingRowHeight(health) != NUMBER_ROW_HEIGHT || NUMBER_FIELD_WIDTH >= 28.0f) {
            throw new IllegalStateException("Numeric settings must stay inline with compact value fields");
        }
'''
if old_smoke not in text:
    raise SystemExit('Expected stacked-layout smoke assertion not found')
text = text.replace(old_smoke, new_smoke, 1)
text = text.replace('XDOLF_GUI_OK: six windows, adaptive inline settings, typed values, animation, pin/open/drag controls',
                    'XDOLF_GUI_OK: six windows, compact inline settings, typed values, animation, pin/open/drag controls')

path.write_text(text)
