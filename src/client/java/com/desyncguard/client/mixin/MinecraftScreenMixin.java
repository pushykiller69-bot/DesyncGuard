package com.desyncguard.client.mixin;

import com.desyncguard.client.DesyncTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class MinecraftScreenMixin {

    @Inject(method = "setScreen", at = @At("HEAD"))
    private void desyncguard$onScreenChange(Screen screen, CallbackInfo ci) {
        Minecraft mc = (Minecraft)(Object)this;
        if (mc.screen instanceof AbstractContainerScreen
                && !(screen instanceof AbstractContainerScreen)) {
            DesyncTracker.clearAll();
        }
    }
}
