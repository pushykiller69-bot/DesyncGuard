package com.desyncguard.client;

import com.desyncguard.DesyncTracker;
import com.desyncguard.client.mixin.TimeSyncMixin;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * Idea 4: Desync History Screen showing session summary.
 * Opened via configurable keybind (default: H).
 */
public class DesyncHistoryScreen extends Screen {

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss");

    public DesyncHistoryScreen() {
        super(Component.literal("DesyncGuard — Session Summary"));
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(guiGraphics, mouseX, mouseY, partialTick);

        int centerX = this.width / 2;
        int y = 30;
        int lineHeight = 14;

        // Title
        guiGraphics.drawCenteredString(this.font, "§e§lDesyncGuard — Session Summary", centerX, y, 0xFFFFAA00);
        y += lineHeight + 4;

        // Separator
        String separator = "──────────────────────────────";
        guiGraphics.drawCenteredString(this.font, separator, centerX, y, 0xFF666666);
        y += lineHeight + 6;

        // Session start time
        String sessionStart = LocalTime.ofInstant(
                Instant.ofEpochMilli(DesyncStats.getSessionStartTime()),
                ZoneId.systemDefault()
        ).format(TIME_FORMAT);
        drawStatLine(guiGraphics, centerX, y, "Session started", sessionStart, 0xFFAAAAAA);
        y += lineHeight;

        y += 6; // spacer

        // Rubber-band stats
        int rbCount = DesyncStats.getRubberbandCount();
        double rbMax = DesyncStats.getMaxRubberbandDistance();
        String rbText = rbCount == 0
                ? "0"
                : String.format("%d  (max: %.1f blocks)", rbCount, rbMax);
        int rbColor = rbCount == 0 ? 0xFF55FF55 : (rbCount > 5 ? 0xFFFF5555 : 0xFFFFAA00);
        drawStatLine(guiGraphics, centerX, y, "Rubber-bands", rbText, rbColor);
        y += lineHeight;

        // Inventory desync stats
        int invCount = DesyncStats.getInventoryDesyncCount();
        int invColor = invCount == 0 ? 0xFF55FF55 : (invCount > 10 ? 0xFFFF5555 : 0xFFFFAA00);
        drawStatLine(guiGraphics, centerX, y, "Inventory desyncs", String.valueOf(invCount), invColor);
        y += lineHeight;

        // Ghost block stats
        int ghostCount = DesyncStats.getGhostBlockCount();
        int ghostColor = ghostCount == 0 ? 0xFF55FF55 : 0xFFFF5555;
        drawStatLine(guiGraphics, centerX, y, "Ghost blocks", String.valueOf(ghostCount), ghostColor);
        y += lineHeight;

        y += 6; // spacer

        // Estimated TPS
        double tps = TimeSyncMixin.getEstimatedTPS();
        String tpsText = String.format("%.1f", tps);
        int tpsColor;
        if (tps >= 19.0) {
            tpsColor = 0xFF55FF55; // green
        } else if (tps >= 15.0) {
            tpsColor = 0xFFFFAA00; // yellow
        } else {
            tpsColor = 0xFFFF5555; // red
        }
        drawStatLine(guiGraphics, centerX, y, "Est. server TPS", tpsText, tpsColor);
        y += lineHeight;

        // Average ping
        long avgPing = DesyncStats.getAvgPingMs();
        String pingText = avgPing > 0 ? avgPing + "ms" : "N/A";
        int pingColor = avgPing < 100 ? 0xFF55FF55 : (avgPing < 200 ? 0xFFFFAA00 : 0xFFFF5555);
        drawStatLine(guiGraphics, centerX, y, "Avg ping", pingText, pingColor);
        y += lineHeight;

        // Adaptive threshold
        long threshold = DesyncTracker.getAdaptiveThreshold();
        if (threshold > 0) {
            String threshText = threshold + "ms";
            drawStatLine(guiGraphics, centerX, y, "Adaptive threshold", threshText, 0xFFAAAAAA);
            y += lineHeight;
        }

        y += lineHeight;

        // Footer hint
        guiGraphics.drawCenteredString(this.font, "§7Press ESC to close", centerX, y, 0xFF888888);

        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    private void drawStatLine(GuiGraphics guiGraphics, int centerX, int y, String label, String value, int valueColor) {
        int labelWidth = this.font.width(label + ":  ");
        int totalWidth = labelWidth + this.font.width(value);
        int startX = centerX - totalWidth / 2;

        guiGraphics.drawString(this.font, label + ":", startX, y, 0xFFCCCCCC, true);
        guiGraphics.drawString(this.font, value, startX + labelWidth, y, valueColor, true);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
