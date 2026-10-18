package com.desyncguard.client.mixin;

import com.desyncguard.client.TpsEstimator;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundSetTimePacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public class TimeSyncMixin {

    @Inject(method = "handleSetTime", at = @At("HEAD"))
    private void desyncguard$trackTps(ClientboundSetTimePacket packet, CallbackInfo ci) {
        if (!Minecraft.getInstance().isSameThread()) return;
        TpsEstimator.onTimePacket(System.currentTimeMillis());
    }
}
