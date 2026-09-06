from pathlib import Path


def replace(path, old, new):
    p = Path(path)
    text = p.read_text()
    if old not in text:
        raise SystemExit(f"pattern not found in {path}: {old[:120]!r}")
    p.write_text(text.replace(old, new, 1))


client = "src/main/java/com/x0xp/xdolf/ClientScreen.java"

replace(
    client,
    '''        String value = pending ? Keybinds.display(screen.pendingBindingKey) + " !"
            : listening ? "Press..." : Keybinds.display(module.key) + (existingConflict ? " !" : "");
        value = XdolfFont.trim(value, Math.max(1, (int) (KEYBIND_FIELD_WIDTH - 4)));
        float valueX = fieldLeft + (KEYBIND_FIELD_WIDTH - XdolfFont.width(value)) / 2.0f;
        XdolfFont.draw(graphics, value, valueX, y + 1,
            fade(pending ? 0xFFFFC66D : listening ? 0xFF44AAFF : 0xFFFFFFFF, alpha));''',
    '''        String value = pending ? Keybinds.display(screen.pendingBindingKey) + " !"
            : listening ? "Press..." : Keybinds.display(module.key) + (existingConflict ? " !" : "");
        value = XdolfFont.trim(value, Math.max(1, (int) (KEYBIND_FIELD_WIDTH - 4)));
        float valueX = fieldLeft + (KEYBIND_FIELD_WIDTH - XdolfFont.width(value)) / 2.0f;
        int valueY = Math.round(fieldTop) + XdolfFont.centeredYOffset(
            Math.round(fieldTop), Math.round(fieldBottom - fieldTop), Math.round(fieldTop));
        XdolfFont.draw(graphics, value, valueX, valueY,
            fade(pending ? 0xFFFFC66D : listening ? 0xFF44AAFF : 0xFFFFFFFF, alpha));'''
)

replace(
    client,
    '''        String value = editing ? screen.editingText : row.setting.display();
        if (editing && (System.currentTimeMillis() / 450L) % 2 == 0) value += "_";
        value = XdolfFont.trim(value, (int) (fieldRight - fieldLeft - 3));
        float valueX = fieldRight - 1.5f - XdolfFont.width(value);
        XdolfFont.draw(graphics, value, Math.max(fieldLeft + 1.5f, valueX), y + 1, fade(0xFFFFFFFF, alpha));''',
    '''        String value = editing ? screen.editingText : row.setting.display();
        if (editing && (System.currentTimeMillis() / 450L) % 2 == 0) value += "_";
        value = XdolfFont.trim(value, (int) (fieldRight - fieldLeft - 3));
        float valueX = fieldRight - 1.5f - XdolfFont.width(value);
        int valueY = Math.round(fieldTop) + XdolfFont.centeredYOffset(
            Math.round(fieldTop), Math.round(fieldBottom - fieldTop), Math.round(fieldTop));
        XdolfFont.draw(graphics, value, Math.max(fieldLeft + 1.5f, valueX), valueY, fade(0xFFFFFFFF, alpha));'''
)

print("Applied textbox font vertical-centering fix")
