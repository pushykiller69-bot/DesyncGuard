package com.desyncguard.client.mixin;

import com.desyncguard.DesyncTracker;
import com.desyncguard.client.DesyncHud;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractContainerScreen.class)
public abstract class AbstractContainerScreenMixin {

    @Inject(method = "renderSlot", at = @At("TAIL"))
    private void desyncguard$onRenderSlot(GuiGraphics guiGraphics, Slot slot, CallbackInfo ci) {
        AbstractContainerScreen<?> screen = (AbstractContainerScreen<?>)(Object)this;
        int containerId = screen.getMenu().containerId;

        if (DesyncTracker.isSlotDesynced(containerId, slot.index)) {
            int x = slot.x;
            int y = slot.y;
            guiGraphics.fill(x, y, x + 16, y + 16, 0x80FF0000);
            DesyncHud.notifyInventoryDesync();
        }
    }
}
