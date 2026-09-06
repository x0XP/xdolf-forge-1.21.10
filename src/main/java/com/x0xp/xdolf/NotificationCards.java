package com.x0xp.xdolf;

import net.minecraft.client.gui.GuiGraphics;

import java.util.ArrayList;
import java.util.List;

/** Animated centre-left HUD notifications used for module enable/disable feedback. */
final class NotificationCards {
    private static final int MAX_VISIBLE = 3;
    private static final float LEFT = 7.0f;
    private static final float CARD_HEIGHT = 20.0f;
    private static final float CARD_GAP = 3.0f;
    private static final int MIN_WIDTH = 78;
    private static final int MAX_WIDTH = 132;

    private static final long ENTER_NS = 180_000_000L;
    private static final long MOVE_NS = 160_000_000L;
    private static final long HOLD_NS = 2_650_000_000L;
    private static final long FADE_NS = 340_000_000L;

    private static final List<Card> ACTIVE = new ArrayList<>();
    private static final List<Card> EXITING = new ArrayList<>();

    private NotificationCards() {}

    static void module(ClientModule module, boolean enabled) {
        long now = System.nanoTime();
        expire(now);

        for (Card card : ACTIVE) card.moveTo(card.targetSlot + 1.0f, now);

        if (ACTIVE.size() >= MAX_VISIBLE) {
            Card oldest = ACTIVE.remove(ACTIVE.size() - 1);
            oldest.startExit(now, true);
            EXITING.add(oldest);
        }

        String moduleName = ClientScreen.label(module);
        ACTIVE.add(0, new Card(moduleName, enabled, now));
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

    static int visibleCount() {
        return ACTIVE.size();
    }

    static void clear() {
        ACTIVE.clear();
        EXITING.clear();
    }

    private static void expire(long now) {
        for (int i = ACTIVE.size() - 1; i >= 0; i--) {
            Card card = ACTIVE.get(i);
            if (now - card.created < ENTER_NS + HOLD_NS) continue;
            ACTIVE.remove(i);
            card.startExit(now, false);
            EXITING.add(card);
        }
    }

    private static void draw(GuiGraphics graphics, Card card, int screenHeight, long now) {
        float enter = clamp01((now - card.created) / (float) ENTER_NS);
        float enterEase = easeOutCubic(enter);
        float alpha = enter;

        if (card.exiting) {
            float exit = clamp01((now - card.exitStarted) / (float) FADE_NS);
            alpha *= 1.0f - easeInCubic(exit);
        }
        if (alpha <= 0.01f) return;

        float slot = card.slot(now);
        float y = screenHeight / 2.0f - CARD_HEIGHT / 2.0f + slot * (CARD_HEIGHT + CARD_GAP);

        int textWidth = XdolfFont.width(card.moduleName) + 4 + XdolfFont.width(card.enabled ? "enabled" : "disabled");
        int width = Math.max(MIN_WIDTH, Math.min(MAX_WIDTH, textWidth + 16));
        float hiddenX = -width - 5.0f;
        float x = hiddenX + (LEFT - hiddenX) * enterEase;
        if (card.exiting) {
            float exit = clamp01((now - card.exitStarted) / (float) FADE_NS);
            x -= 7.0f * easeInCubic(exit);
        }

        graphics.pose().pushMatrix();
        graphics.pose().translate(x, y);

        fill(graphics, 0, 0, width, CARD_HEIGHT, fade(0xD914171D, alpha));
        outline(graphics, 0, 0, width, CARD_HEIGHT, fade(0xF0000000, alpha));
        fill(graphics, 1, 1, 3, CARD_HEIGHT - 1,
            fade(card.enabled ? 0xFF35D07F : 0xFFFF4D5E, alpha));

        String state = card.enabled ? "enabled" : "disabled";
        int stateWidth = XdolfFont.width(state);
        int availableName = width - 12 - stateWidth;
        String name = XdolfFont.trim(card.moduleName, Math.max(8, availableName));

        XdolfFont.draw(graphics, name, 7, 5, fade(0xFFFFFFFF, alpha));
        XdolfFont.draw(graphics, state, width - stateWidth - 5, 5,
            fade(card.enabled ? 0xFF72E8A6 : 0xFFFF7B88, alpha));

        graphics.pose().popMatrix();
    }

    private static void fill(GuiGraphics graphics, float left, float top, float right, float bottom, int color) {
        graphics.fill(Math.round(left), Math.round(top), Math.round(right), Math.round(bottom), color);
    }

    private static void outline(GuiGraphics graphics, float left, float top, float right, float bottom, int color) {
        fill(graphics, left, top, right, top + 1, color);
        fill(graphics, left, bottom - 1, right, bottom, color);
        fill(graphics, left, top, left + 1, bottom, color);
        fill(graphics, right - 1, top, right, bottom, color);
    }

    private static int fade(int color, float alpha) {
        int originalAlpha = color >>> 24;
        int fadedAlpha = Math.max(0, Math.min(255, Math.round(originalAlpha * alpha)));
        return (color & 0x00FFFFFF) | fadedAlpha << 24;
    }

    private static float clamp01(float value) {
        return Math.max(0.0f, Math.min(1.0f, value));
    }

    private static float easeOutCubic(float t) {
        float inverse = 1.0f - t;
        return 1.0f - inverse * inverse * inverse;
    }

    private static float easeInCubic(float t) {
        return t * t * t;
    }

    private static final class Card {
        final String moduleName;
        final boolean enabled;
        final long created;
        float fromSlot;
        float targetSlot;
        long moveStarted;
        boolean exiting;
        long exitStarted;

        Card(String moduleName, boolean enabled, long created) {
            this.moduleName = moduleName;
            this.enabled = enabled;
            this.created = created;
            this.fromSlot = 0.0f;
            this.targetSlot = 0.0f;
            this.moveStarted = created;
        }

        float slot(long now) {
            float t = clamp01((now - moveStarted) / (float) MOVE_NS);
            return fromSlot + (targetSlot - fromSlot) * easeOutCubic(t);
        }

        void moveTo(float target, long now) {
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

        boolean finished(long now) {
            return exiting && now - exitStarted >= FADE_NS;
        }
    }
}
