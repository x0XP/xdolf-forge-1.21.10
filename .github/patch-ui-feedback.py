from pathlib import Path


def replace(path, old, new):
    p = Path(path)
    text = p.read_text()
    if old not in text:
        raise SystemExit(f"pattern not found in {path}: {old[:100]!r}")
    p.write_text(text.replace(old, new, 1))

client = "src/main/java/com/x0xp/xdolf/ClientScreen.java"
replace(client,
    "private static final float NUMBER_FIELD_WIDTH = 23.0f;",
    "private static final float NUMBER_FIELD_WIDTH = 19.0f;")
replace(client,
    "private static final float KEYBIND_FIELD_WIDTH = 48.0f;",
    "private static final float KEYBIND_FIELD_WIDTH = 34.0f;")
replace(client,
    "        for (var panel : PANELS) draw(graphics, panel, mouseX, mouseY, true, this);\n        ClientSmoke.frame();",
    "        for (var panel : PANELS) draw(graphics, panel, mouseX, mouseY, true, this);\n        // HUD notifications are normally below Screen rendering; redraw them here so the click GUI never dims them.\n        NotificationCards.render(graphics);\n        ClientSmoke.frame();")
replace(client,
    "        float fieldTop = y;\n        float fieldBottom = y + 11.0f;",
    "        float fieldTop = y + 0.5f;\n        float fieldBottom = y + 10.5f;")
replace(client,
    "        XdolfFont.draw(graphics, label, left, y, fade(hover ? 0xFFFFFFFF : 0xD8FFFFFF, alpha));\n\n        int fieldFill",
    "        XdolfFont.draw(graphics, label, left, y + 1, fade(hover ? 0xFFFFFFFF : 0xD8FFFFFF, alpha));\n\n        int fieldFill")
replace(client,
    "        float fieldTop = y;\n        float fieldBottom = y + 10.5f;",
    "        float fieldTop = y + 0.5f;\n        float fieldBottom = y + 10.5f;")
replace(client,
    "        XdolfFont.draw(graphics, value, Math.max(fieldLeft + 1.5f, valueX), y, fade(0xFFFFFFFF, alpha));",
    "        XdolfFont.draw(graphics, value, Math.max(fieldLeft + 1.5f, valueX), y + 1, fade(0xFFFFFFFF, alpha));")

notifications = "src/main/java/com/x0xp/xdolf/NotificationCards.java"
replace(notifications,
    "        rect(graphics, 0, 0, CARD_WIDTH, CARD_HEIGHT, fade(0xE014171D, alpha));",
    "        // Match the click GUI panels exactly: translucent black rather than a separate opaque HUD theme.\n        rect(graphics, 0, 0, CARD_WIDTH, CARD_HEIGHT, fade(0x80000000, alpha));")

hud = "src/main/java/com/x0xp/xdolf/Hud.java"
replace(hud,
    "        NotificationCards.render(graphics);",
    "        // ClientScreen redraws notifications after its dim layer/panels so cards stay in the foreground.\n        if(!(mc.screen instanceof ClientScreen))NotificationCards.render(graphics);")

print("Applied notification foreground/theme and compact aligned field fixes")
