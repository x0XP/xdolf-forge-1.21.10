package com.darkcart.xdolf;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

final class SettingsScreen extends Screen {
    private final Screen parent;
    private final ClientModule module;
    private boolean capture;

    SettingsScreen(Screen parent, ClientModule module) {
        super(Component.literal(module.name)); this.parent = parent; this.module = module;
    }

    @Override
    protected void init() {
        int panel = Math.min(360, width - 24), x = (width - panel) / 2;
        int y = 48;
        for (var setting : module.settings) {
            addRenderableWidget(Button.builder(Component.literal("-"), button -> change(setting, -1)).bounds(x, y, 30, 20).build());
            addRenderableWidget(Button.builder(Component.literal(setting.name + ": " + setting.display()), button -> change(setting, 1))
                .bounds(x + 34, y, panel - 68, 20).build());
            addRenderableWidget(Button.builder(Component.literal("+"), button -> change(setting, 1)).bounds(x + panel - 30, y, 30, 20).build());
            y += 24;
        }
        String key = module.key == -1 ? "NONE" : Integer.toString(module.key);
        addRenderableWidget(Button.builder(Component.literal(capture ? "Press a key; Escape clears" : "Key binding: " + key), button -> {
            capture = true; rebuildWidgets();
        }).bounds(x, y + 4, panel, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Back"), button -> onClose()).bounds(x, y + 32, panel, 20).build());
    }

    private void change(ModuleSetting setting, int direction) {
        setting.increment(direction); ClientConfig.save(ClientRuntime.MODULES); rebuildWidgets();
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (!capture) return super.keyPressed(event);
        int key = event.key();
        if (key == GLFW.GLFW_KEY_RIGHT_SHIFT) return true;
        module.key = key == GLFW.GLFW_KEY_ESCAPE ? -1 : key;
        capture = false; ClientConfig.save(ClientRuntime.MODULES); rebuildWidgets(); return true;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, width, height, 0xE6101620);
        graphics.drawCenteredString(font, module.name, width / 2, 16, 0xFF70D7FF);
        super.render(graphics, mouseX, mouseY, partialTick);
    }
    @Override public void onClose() { minecraft.setScreen(parent); }
    @Override public boolean isPauseScreen() { return false; }
}
