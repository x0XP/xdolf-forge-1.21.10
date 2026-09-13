package com.x0xp.xdolf.update;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import java.util.List;
import java.util.Locale;

/** Startup prompt and download progress UI for Xdolf updates. */
public final class UpdateScreen extends Screen {
    private static final int MAX_PANEL_WIDTH = 390;
    private static final int PANEL_HEIGHT = 196;
    private static final int HORIZONTAL_MARGIN = 20;
    private static final int ACCENT = 0xFFE21C2A;
    private static final int PANEL = 0xF0151518;
    private static final int PANEL_INNER = 0xFF1D1D21;
    private static final int TEXT = 0xFFF3F3F3;
    private static final int MUTED = 0xFFA5A5AA;

    private final Screen parent;
    private final AutoUpdater.ReleaseInfo release;
    private Button updateButton;
    private Button laterButton;
    private AutoUpdater.Phase lastPhase;

    public UpdateScreen(Screen parent, AutoUpdater.ReleaseInfo release) {
        super(Component.literal("Xdolf Update"));
        this.parent = parent;
        this.release = release;
    }

    private int panelWidth() {
        return Math.min(MAX_PANEL_WIDTH, Math.max(280, width - HORIZONTAL_MARGIN * 2));
    }

    @Override
    protected void init() {
        int panelWidth = panelWidth();
        int left = (width - panelWidth) / 2;
        int top = (height - PANEL_HEIGHT) / 2;
        int buttonY = top + PANEL_HEIGHT - 34;
        int gap = 14;
        int buttonWidth = (panelWidth - 40 - gap) / 2;

        updateButton = addRenderableWidget(Button.builder(Component.literal("Update now"), button -> {
            button.active = false;
            if (laterButton != null) laterButton.active = false;
            AutoUpdater.downloadAndRestart();
        }).bounds(left + 20, buttonY, buttonWidth, 20).build());

        laterButton = addRenderableWidget(Button.builder(Component.literal("Later"), button -> {
            AutoUpdater.dismiss();
            if (minecraft != null) minecraft.setScreen(parent);
        }).bounds(left + 20 + buttonWidth + gap, buttonY, buttonWidth, 20).build());

        syncButtons();
    }

    @Override
    public void tick() {
        super.tick();
        if (lastPhase != AutoUpdater.phase()) syncButtons();
    }

    private void syncButtons() {
        lastPhase = AutoUpdater.phase();
        if (updateButton == null || laterButton == null) return;
        boolean canStart = lastPhase == AutoUpdater.Phase.AVAILABLE || lastPhase == AutoUpdater.Phase.ERROR;
        updateButton.active = canStart;
        laterButton.active = canStart;
        updateButton.setMessage(Component.literal(lastPhase == AutoUpdater.Phase.ERROR ? "Retry update" : "Update now"));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, width, height, 0xE509090B);

        int panelWidth = panelWidth();
        int left = (width - panelWidth) / 2;
        int top = (height - PANEL_HEIGHT) / 2;
        int right = left + panelWidth;
        int bottom = top + PANEL_HEIGHT;
        int centerX = width / 2;
        int innerTextWidth = panelWidth - 56;

        graphics.fill(left - 1, top - 1, right + 1, bottom + 1, 0xFF000000);
        graphics.fill(left, top, right, bottom, PANEL);
        graphics.fill(left, top, right, top + 3, ACCENT);
        graphics.fill(left + 12, top + 42, right - 12, bottom - 48, PANEL_INNER);

        graphics.drawCenteredString(font, Component.literal("XDOLF UPDATE"), centerX, top + 14, TEXT);
        graphics.drawCenteredString(font,
            Component.literal(AutoUpdater.currentVersion() + "  →  " + release.version()),
            centerX, top + 28, MUTED);

        AutoUpdater.Phase phase = AutoUpdater.phase();
        String headline = switch (phase) {
            case AVAILABLE -> "A newer Xdolf developer build is available.";
            case DOWNLOADING -> "Downloading update...";
            case VERIFYING -> "Verifying update...";
            case READY -> "Update verified.";
            case RESTARTING -> "Restarting Minecraft...";
            case ERROR -> "The update could not be completed.";
            default -> "Preparing update...";
        };
        drawWrappedCentered(graphics, headline, centerX, top + 54, innerTextWidth, TEXT, 10);

        if (phase == AutoUpdater.Phase.AVAILABLE) {
            int y = top + 72;
            y = drawWrappedCentered(graphics,
                "Install " + release.tag() + " now, or continue with this build.",
                centerX, y, innerTextWidth, MUTED, 10);
            y += 3;
            y = drawWrappedCentered(graphics,
                "The new JAR is downloaded from the official Xdolf GitHub release.",
                centerX, y, innerTextWidth, MUTED, 10);
            y += 3;
            drawWrappedCentered(graphics,
                "Minecraft will restart automatically after the download is verified.",
                centerX, y, innerTextWidth, MUTED, 10);
        } else {
            drawProgress(graphics, left + 28, top + 83, panelWidth - 56, phase);
            drawWrappedCentered(graphics, AutoUpdater.status(), centerX, top + 105, innerTextWidth,
                phase == AutoUpdater.Phase.ERROR ? 0xFFFF7777 : MUTED, 10);
            if (phase == AutoUpdater.Phase.ERROR && !AutoUpdater.errorMessage().isBlank()) {
                drawWrappedCentered(graphics, trim(AutoUpdater.errorMessage(), 110), centerX, top + 121,
                    innerTextWidth, 0xFFFF9999, 10);
            }
        }

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private int drawWrappedCentered(GuiGraphics graphics, String text, int centerX, int y,
                                    int maxWidth, int color, int lineHeight) {
        List<FormattedCharSequence> lines = font.split(Component.literal(text), maxWidth);
        for (FormattedCharSequence line : lines) {
            graphics.drawCenteredString(font, line, centerX, y, color);
            y += lineHeight;
        }
        return y;
    }

    private void drawProgress(GuiGraphics graphics, int x, int y, int width, AutoUpdater.Phase phase) {
        float progress = AutoUpdater.progress();
        graphics.fill(x, y, x + width, y + 8, 0xFF08080A);
        graphics.fill(x + 1, y + 1, x + width - 1, y + 7, 0xFF2A2A2F);
        int filled = Math.round((width - 2) * progress);
        if (filled > 0) graphics.fill(x + 1, y + 1, x + 1 + filled, y + 7, ACCENT);

        String value;
        if (phase == AutoUpdater.Phase.DOWNLOADING) {
            value = String.format(Locale.ROOT, "%d%%  •  %s / %s",
                Math.round(progress * 100.0f), formatBytes(AutoUpdater.downloadedBytes()),
                formatBytes(AutoUpdater.totalBytes()));
        } else if (phase == AutoUpdater.Phase.ERROR) {
            value = "Update interrupted";
        } else {
            value = Math.round(progress * 100.0f) + "%";
        }
        graphics.drawCenteredString(font, Component.literal(value), x + width / 2, y + 12, TEXT);
    }

    private static String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        double kb = bytes / 1024.0;
        if (kb < 1024) return String.format(Locale.ROOT, "%.1f KB", kb);
        return String.format(Locale.ROOT, "%.2f MB", kb / 1024.0);
    }

    private static String trim(String value, int max) {
        if (value.length() <= max) return value;
        return value.substring(0, Math.max(0, max - 3)) + "...";
    }

    @Override
    public boolean shouldCloseOnEsc() {
        AutoUpdater.Phase phase = AutoUpdater.phase();
        return phase == AutoUpdater.Phase.AVAILABLE || phase == AutoUpdater.Phase.ERROR;
    }

    @Override
    public void onClose() {
        AutoUpdater.Phase phase = AutoUpdater.phase();
        if (phase == AutoUpdater.Phase.AVAILABLE || phase == AutoUpdater.Phase.ERROR) {
            AutoUpdater.dismiss();
            if (minecraft != null) minecraft.setScreen(parent);
        }
    }
}
