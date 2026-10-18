package com.desyncguard.client.mixin;

import com.desyncguard.client.DesyncStats;
import com.desyncguard.client.DesyncTracker;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.concurrent.atomic.AtomicInteger;

@Mixin(AbstractContainerScreen.class)
public abstract class AbstractContainerScreenMixin {

    @Inject(method = "renderSlot", at = @At("TAIL"))
    private void desyncguard$overlaySlot(GuiGraphics guiGraphics, Slot slot, CallbackInfo ci) {
        AbstractContainerScreen<?> screen = (AbstractContainerScreen<?>) (Object) this;
        int containerId = screen.getMenu().containerId;
        if (!DesyncTracker.isSlotDesynced(containerId, slot.index)) return;

        int slotKey = DesyncTracker.slotKey(containerId, slot.index);
        int maxFreq = DesyncStats.getMaxSlotDesyncCount();
        AtomicInteger counter = DesyncStats.getSlotDesyncFrequency().get(slotKey);
        int freq = counter != null ? counter.get() : 0;

        int overlayColor;
        if (maxFreq > 0 && freq > 0) {
            float ratio = Math.min(1.0f, (float) freq / maxFreq);
            int green = (int) (255 * (1.0f - ratio * 0.6f));
            overlayColor = 0x80000000 | (255 << 16) | (green << 8);
        } else {
            overlayColor = 0x80FF0000;
        }
        guiGraphics.fill(slot.x, slot.y, slot.x + 16, slot.y + 16, overlayColor);
    }
}
