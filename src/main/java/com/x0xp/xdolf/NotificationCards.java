package com.x0xp.xdolf;

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

        // Match the click GUI panels exactly: translucent black rather than a separate opaque HUD theme.
        rect(graphics, 0, 0, CARD_WIDTH, CARD_HEIGHT, fade(0x80000000, alpha));
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
