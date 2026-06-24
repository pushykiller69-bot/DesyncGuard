package com.desyncguard.client;

import com.desyncguard.DesyncTracker;
import com.desyncguard.client.mixin.TimeSyncMixin;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;

public class DesyncHud {

    private static long lastRubberbandTime = 0;
    private static long lastInventoryDesyncTime = 0;
    private static long lastGhostBlockTime = 0;
    private static BlockPos lastGhostBlockPos = null;

    // Fix 2: State guard so repeated calls don't reset the fade timer
    private static boolean inventoryDesyncActive = false;

    // Improvement 5: Rubber-band counter
    private static int rubberbandCount = 0;
    private static double lastRubberbandDistance = 0.0;

    public static void register() {
        HudRenderCallback.EVENT.register((guiGraphics, tickDelta) -> {
            render(guiGraphics);
        });
    }

    public static void notifyRubberband(double distance) {
        lastRubberbandTime = System.currentTimeMillis();
        rubberbandCount++;
        lastRubberbandDistance = distance;
    }

    // Keep the no-arg version as a fallback
    public static void notifyRubberband() {
        notifyRubberband(0.0);
    }

    /**
     * Fix 2: Only set the timestamp once when desync begins.
     * Prevents the fade timer from resetting every render frame.
     */
    public static void notifyInventoryDesync() {
        if (!inventoryDesyncActive) {
            lastInventoryDesyncTime = System.currentTimeMillis();
            inventoryDesyncActive = true;
        }
    }

    /**
     * Fix 2: Called when no slots are desynced in a render pass,
     * allowing the fade timer to start counting down.
     */
    public static void clearInventoryDesync() {
        inventoryDesyncActive = false;
    }

    /**
     * Idea 2: Ghost block notification.
     */
    public static void notifyGhostBlock(BlockPos pos) {
        lastGhostBlockTime = System.currentTimeMillis();
        lastGhostBlockPos = pos;
    }

    /**
     * Reset session state (called on disconnect).
     */
    public static void resetSession() {
        lastRubberbandTime = 0;
        lastInventoryDesyncTime = 0;
        lastGhostBlockTime = 0;
        lastGhostBlockPos = null;
        inventoryDesyncActive = false;
        rubberbandCount = 0;
        lastRubberbandDistance = 0.0;
    }

    private static void render(GuiGraphics guiGraphics) {
        long now = System.currentTimeMillis();
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        DesyncGuardConfig config = DesyncGuardConfig.get();
        int x = config.hudX;
        int y = config.hudY;
        long warningDuration = config.warnDurationMs; // Updated field name

        // --- Estimated TPS display (Idea 3) ---
        if (config.displayTps) { // Updated field name
            double tps = TimeSyncMixin.getEstimatedTPS();
            String tpsText = String.format("TPS: ~%.1f", tps);
            int tpsColor;
            if (tps >= 19.0) {
                tpsColor = 0xFF55FF55; // green
            } else if (tps >= 15.0) {
                tpsColor = 0xFFFFAA00; // yellow
            } else {
                tpsColor = 0xFFFF5555; // red
            }
            guiGraphics.drawString(mc.font, tpsText, x, y, tpsColor, true);
            y += 12;
        }

        // --- Rubber-band warning (Improvement 5: with counter) ---
        if (now - lastRubberbandTime < warningDuration) {
            long elapsed = now - lastRubberbandTime;
            float alpha = elapsed > (warningDuration - 1000)
                    ? (float)(warningDuration - elapsed) / 1000f
                    : 1.0f;
            int color = ((int)(alpha * 255) << 24) | 0xFF4444;

            String text;
            if (rubberbandCount > 1 && lastRubberbandDistance > 0) {
                text = String.format("\u26A0 Rubber-band \u00D7%d (last: %.1f blocks)",
                        rubberbandCount, lastRubberbandDistance);
            } else if (lastRubberbandDistance > 0) {
                text = String.format("\u26A0 Rubber-band detected! (%.1f blocks)",
                        lastRubberbandDistance);
            } else {
                text = "\u26A0 Rubber-band detected!";
            }

            guiGraphics.drawString(mc.font, text, x, y, color, true);
            y += 12;
        }

        // --- Inventory desync warning ---
        if (now - lastInventoryDesyncTime < warningDuration) {
            long elapsed = now - lastInventoryDesyncTime;
            float alpha = elapsed > (warningDuration - 1000)
                    ? (float)(warningDuration - elapsed) / 1000f
                    : 1.0f;
            int color = ((int)(alpha * 255) << 24) | 0xFFAA00;

            guiGraphics.drawString(mc.font, "\u26A0 Inventory desync!", x, y, color, true);
            y += 12;
        }

        // --- Ghost block warning (Idea 2) ---
        if (now - lastGhostBlockTime < warningDuration) {
            long elapsed = now - lastGhostBlockTime;
            float alpha = elapsed > (warningDuration - 1000)
                    ? (float)(warningDuration - elapsed) / 1000f
                    : 1.0f;
            int color = ((int)(alpha * 255) << 24) | 0xFF66FF;

            String text = "\u26A0 Ghost block detected!";
            if (lastGhostBlockPos != null) {
                text = String.format("\u26A0 Ghost block at (%d, %d, %d)",
                        lastGhostBlockPos.getX(), lastGhostBlockPos.getY(), lastGhostBlockPos.getZ());
            }
            guiGraphics.drawString(mc.font, text, x, y, color, true);
            y += 12;
        }

        // --- Ping spike warning (Idea 1) ---
        long spikeMs = DesyncTracker.getLastPingSpikeMs();
        if (spikeMs > 0) {
            long spikeTime = DesyncTracker.getLastPingSpikeTime();
            long spikeElapsed = now - spikeTime;
            if (spikeElapsed < warningDuration) {
                float alpha = spikeElapsed > (warningDuration - 1000)
                        ? (float)(warningDuration - spikeElapsed) / 1000f
                        : 1.0f;
                int color = ((int)(alpha * 255) << 24) | 0xFF8800;

                String text = String.format("\u26A0 Connection spike detected (+%dms)", spikeMs);
                guiGraphics.drawString(mc.font, text, x, y, color, true);
            }
        }
    }
}
