package com.desyncguard.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

public class DesyncHud {

    private static long lastRubberbandTime = 0;
    private static long lastInventoryDesyncTime = 0;

    private static final long WARNING_DURATION = 5000;

    public static void register() {
        HudRenderCallback.EVENT.register((guiGraphics, tickDelta) -> {
            render(guiGraphics);
        });
    }

    public static void notifyRubberband() {
        lastRubberbandTime = System.currentTimeMillis();
    }

    public static void notifyInventoryDesync() {
        lastInventoryDesyncTime = System.currentTimeMillis();
    }

    private static void render(GuiGraphics guiGraphics) {
        long now = System.currentTimeMillis();
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        int y = 4;

        if (now - lastRubberbandTime < WARNING_DURATION) {
            long elapsed = now - lastRubberbandTime;
            float alpha = elapsed > (WARNING_DURATION - 1000)
                ? (float)(WARNING_DURATION - elapsed) / 1000f
                : 1.0f;
            int color = ((int)(alpha * 255) << 24) | 0xFF4444;

            guiGraphics.drawString(mc.font, "\u26A0 Rubber-band detected!", 4, y, color, true);
            y += 12;
        }

        if (now - lastInventoryDesyncTime < WARNING_DURATION) {
            long elapsed = now - lastInventoryDesyncTime;
            float alpha = elapsed > (WARNING_DURATION - 1000)
                ? (float)(WARNING_DURATION - elapsed) / 1000f
                : 1.0f;
            int color = ((int)(alpha * 255) << 24) | 0xFFAA00;

            guiGraphics.drawString(mc.font, "\u26A0 Inventory desync!", 4, y, color, true);
        }
    }
}
