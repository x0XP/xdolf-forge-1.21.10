package com.x0xp.xdolf;

import net.minecraft.client.gui.GuiGraphics;

/** Shared pixel-aligned primitives for Xdolf HUD and click-GUI rendering. */
final class UiDraw {
    private static float globalAlpha = 1.0f;

    private UiDraw() {}

    static void setGlobalAlpha(float alpha) { globalAlpha = clamp01(alpha); }
    static float globalAlpha() { return globalAlpha; }

    static void rect(GuiGraphics graphics, float x, float y, float right, float bottom, int color) {
        graphics.pose().pushMatrix();
        graphics.pose().scale(0.5f, 0.5f);
        graphics.fill(Math.round(x * 2), Math.round(y * 2), Math.round(right * 2), Math.round(bottom * 2), fade(color, globalAlpha));
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
