package com.desyncguard.client;

import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;

public class DesyncHud {

    private static long lastRubberbandTime = 0;
    private static long lastInventoryDesyncTime = 0;
    private static long lastGhostBlockTime = 0;
    private static BlockPos lastGhostBlockPos = null;

    private static int rubberbandCount = 0;
    private static double lastRubberbandDistance = 0.0;

    public static void register() {
        HudRenderCallback.EVENT.register((guiGraphics, tickDelta) -> render(guiGraphics));
    }

    public static void notifyRubberband(double distance) {
        lastRubberbandTime = System.currentTimeMillis();
        rubberbandCount++;
        lastRubberbandDistance = distance;
    }

    public static void notifyInventoryDesync() {
        lastInventoryDesyncTime = System.currentTimeMillis();
    }

    public static void notifyGhostBlock(BlockPos pos) {
        lastGhostBlockTime = System.currentTimeMillis();
        lastGhostBlockPos = pos;
    }

    public static void resetSession() {
        lastRubberbandTime = 0;
        lastInventoryDesyncTime = 0;
        lastGhostBlockTime = 0;
        lastGhostBlockPos = null;
        rubberbandCount = 0;
        lastRubberbandDistance = 0.0;
    }

    private static float fade(long elapsed, long duration) {
        if (elapsed >= duration) return 0f;
        long remaining = duration - elapsed;
        return remaining >= 1000 ? 1.0f : remaining / 1000f;
    }

    private static boolean draw(GuiGraphics g, Minecraft mc, String text, int x, int y, float alpha, int rgb) {
        int a = (int) (alpha * 255);
        if (a < 8) return false;
        g.drawString(mc.font, text, x, y, (a << 24) | rgb, true);
        return true;
    }

    private static void render(GuiGraphics guiGraphics) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        long now = System.currentTimeMillis();
        DesyncGuardConfig config = DesyncGuardConfig.get();
        if (!config.hudEnabled) return;
        int x = config.hudX;
        int y = config.hudY;
        long duration = config.warnDurationMs;

        if (config.displayTps) {
            double tps = TpsEstimator.get();
            int color = tps >= 19.0 ? 0xFF55FF55 : (tps >= 15.0 ? 0xFFFFAA00 : 0xFFFF5555);
            guiGraphics.drawString(mc.font, String.format("TPS: ~%.1f", tps), x, y, color, true);
            y += 12;
        }

        if (config.displayDiagnostics) {
            int pingSamples = DesyncStats.getPingSamples();
            String ping = pingSamples == 0
                    ? "Ping: collecting"
                    : String.format("Ping: %d ms (jitter %d ms)",
                            DesyncStats.getAvgPingMs(), DesyncStats.getPingJitterMs());
            guiGraphics.drawString(mc.font, ping, x, y, 0xFFFFFFFF, true);
            y += 12;
            guiGraphics.drawString(mc.font, ConnectionDiagnosis.describe(mc.getFps()), x, y, 0xFFFFFFFF, true);
            y += 12;
        }

        float a = fade(now - lastRubberbandTime, duration);
        if (a > 0) {
            String text;
            if (rubberbandCount > 1 && lastRubberbandDistance > 0) {
                text = String.format("\u26A0 Rubber-band \u00D7%d (last: %.1f blocks)", rubberbandCount, lastRubberbandDistance);
            } else if (lastRubberbandDistance > 0) {
                text = String.format("\u26A0 Rubber-band detected! (%.1f blocks)", lastRubberbandDistance);
            } else {
                text = "\u26A0 Rubber-band detected!";
            }
            if (draw(guiGraphics, mc, text, x, y, a, 0xFF4444)) y += 12;
        }

        a = fade(now - lastInventoryDesyncTime, duration);
        if (a > 0 && draw(guiGraphics, mc, "\u26A0 Inventory desync!", x, y, a, 0xFFAA00)) y += 12;

        a = fade(now - lastGhostBlockTime, duration);
        if (a > 0) {
            String text = lastGhostBlockPos != null
                    ? String.format("\u26A0 Ghost block at (%d, %d, %d)",
                        lastGhostBlockPos.getX(), lastGhostBlockPos.getY(), lastGhostBlockPos.getZ())
                    : "\u26A0 Ghost block detected!";
            if (draw(guiGraphics, mc, text, x, y, a, 0xFF66FF)) y += 12;
        }

        long spikeMs = DesyncTracker.getLastPingSpikeMs();
        if (spikeMs > 0) {
            a = fade(now - DesyncTracker.getLastPingSpikeTime(), duration);
            if (a > 0) {
                draw(guiGraphics, mc, String.format("\u26A0 Connection spike detected (+%dms)", spikeMs), x, y, a, 0xFF8800);
            }
        }
    }
}
