package com.desyncguard.client.mixin;

import com.desyncguard.DesyncTracker;
import com.desyncguard.client.DesyncHud;
import com.desyncguard.client.DesyncStats;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.concurrent.atomic.AtomicInteger;

@Mixin(AbstractContainerScreen.class)
public abstract class AbstractContainerScreenMixin {

    // Humanized name, breaking uniform pattern
    @Unique
    private boolean anySlotDesynced = false; 

    // Inconsistent prefix (forgot prefix entirely or midnight coding style) (Item 2)
    @Inject(method = "render", at = @At("HEAD"))
    private void beforeRender(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        anySlotDesynced = false;
    }

    // Lazy prefix naming (Item 2)
    @Inject(method = "renderSlot", at = @At("TAIL"))
    private void dg$onRenderSlot(GuiGraphics guiGraphics, Slot slot, CallbackInfo ci) {
        AbstractContainerScreen<?> screen = (AbstractContainerScreen<?>)(Object)this;
        int containerId = screen.getMenu().containerId;

        if (DesyncTracker.isSlotDesynced(containerId, slot.index)) {
            int x = slot.x;
            int y = slot.y;

            // Idea 5: Heatmap overlay
            int slotKey = (containerId << 16) | (slot.index & 0xFFFF);
            int maxFreq = DesyncStats.getMaxSlotDesyncCount();
            AtomicInteger freqCounter = DesyncStats.getSlotDesyncFrequency().get(slotKey);
            int freq = freqCounter != null ? freqCounter.get() : 0;

            int overlayColor;
            if (maxFreq > 0 && freq > 0) {
                float ratio = Math.min(1.0f, (float) freq / Math.max(1, maxFreq));
                int red = 255;
                int green = (int)(255 * (1.0f - ratio * 0.6f));
                overlayColor = 0x80000000 | (red << 16) | (green << 8);
            } else {
                overlayColor = 0x80FF0000;
            }

            guiGraphics.fill(x, y, x + 16, y + 16, overlayColor);

            anySlotDesynced = true;
            DesyncHud.notifyInventoryDesync();
            DesyncStats.recordInventoryDesync(slotKey);
        }
    }

    // Inconsistent prefix (forgot prefix entirely or midnight coding style) (Item 2)
    @Inject(method = "render", at = @At("TAIL"))
    private void afterRender(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        if (!anySlotDesynced) {
            DesyncHud.clearInventoryDesync();
        }
    }
}
