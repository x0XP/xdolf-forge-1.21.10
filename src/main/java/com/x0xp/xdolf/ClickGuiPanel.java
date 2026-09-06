package com.x0xp.xdolf;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Persistent window state and animation, independent from rendering and input. */
final class ClickGuiPanel {
    private static final long OPTION_ANIMATION_NS = 135_000_000L;
    private static final long SCROLL_ANIMATION_NS = 120_000_000L;
    static final float MAX_BODY_HEIGHT = 205.0f;

    static final class Expansion {
        boolean open;
        float progress;
        private float from;
        private float target;
        private long started;

        float value() {
            if (progress == target) return progress;
            float elapsed = Math.min(1.0f, (System.nanoTime() - started) / (float) OPTION_ANIMATION_NS);
            progress = from + (target - from) * UiDraw.easeOutCubic(elapsed);
            if (elapsed >= 1.0f) progress = target;
            return progress;
        }

        void setOpen(boolean value) {
            float current = value();
            open = value;
            from = current;
            target = value ? 1.0f : 0.0f;
            started = System.nanoTime();
        }

        void finish() {
            progress = target;
            from = target;
        }
    }

    final String title;
    final List<ClientModule> modules = new ArrayList<>();
    final Map<ClientModule, Expansion> expansions = new HashMap<>();
    int x = 2;
    int y;
    boolean open;
    boolean pinned;
    float scroll;
    float scrollFrom;
    float scrollTarget;
    private long scrollStarted;

    ClickGuiPanel(String title, int y) {
        this.title = title;
        this.y = y;
    }

    boolean text() { return title.equals("Info") || title.equals("Radar"); }

    Expansion expansion(ClientModule module) {
        return expansions.computeIfAbsent(module, ignored -> new Expansion());
    }

    void toggleExpansion(ClientModule module) {
        Expansion selected = expansion(module);
        boolean opening = !selected.open;
        if (opening) {
            for (var entry : expansions.entrySet())
                if (entry.getKey() != module && entry.getValue().open) entry.getValue().setOpen(false);
        }
        selected.setOpen(opening);
    }

    float animatedSettingsHeight(ClientModule module) {
        var expansion = expansions.get(module);
        return expansion == null ? 0 : ConfigContainer.height(module) * expansion.value();
    }

    float height(int textLines) {
        if (text()) return open ? textLines * 10 + 16 : 14;
        if (!open) return 13;
        float height = 13 + modules.size() * 12 + 0.5f;
        for (ClientModule module : modules) height += animatedSettingsHeight(module);
        return height;
    }

    float displayHeight(int screenHeight, int textLines) {
        float height = height(textLines);
        if (text() || !open) return height;
        float availableBody = Math.max(48.0f, Math.min(MAX_BODY_HEIGHT, screenHeight - y - 21.0f));
        return 13.0f + Math.min(height - 13.0f, availableBody);
    }

    float maxScroll(int screenHeight) {
        float height = height(0);
        return Math.max(0, height - displayHeight(screenHeight, 0));
    }

    float scrollValue(int screenHeight) {
        float max = maxScroll(screenHeight);
        if (max <= 0.01f) {
            scroll = scrollFrom = scrollTarget = 0;
            return 0;
        }
        scrollTarget = Math.max(0, Math.min(max, scrollTarget));
        if (scroll == scrollTarget) return scroll;
        float elapsed = Math.min(1, (System.nanoTime() - scrollStarted) / (float) SCROLL_ANIMATION_NS);
        scroll = scrollFrom + (scrollTarget - scrollFrom) * UiDraw.easeOutCubic(elapsed);
        if (elapsed >= 1) scroll = scrollTarget;
        return Math.max(0, Math.min(max, scroll));
    }

    void scrollBy(float amount, int screenHeight) {
        scrollFrom = scrollValue(screenHeight);
        scrollTarget = Math.max(0, Math.min(maxScroll(screenHeight), scrollTarget + amount));
        scrollStarted = System.nanoTime();
    }

    void finishScroll(int screenHeight) {
        scroll = scrollTarget = Math.max(0, Math.min(maxScroll(screenHeight), scrollTarget));
        scrollFrom = scroll;
    }
}
