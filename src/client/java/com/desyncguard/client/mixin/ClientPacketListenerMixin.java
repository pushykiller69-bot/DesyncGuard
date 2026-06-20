package com.desyncguard.client.mixin;

import com.desyncguard.DesyncTracker;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundContainerSetContentPacket;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public class ClientPacketListenerMixin {

    @Inject(method = "handleContainerContent", at = @At("TAIL"))
    private void desyncguard$onContainerContent(ClientboundContainerSetContentPacket packet, CallbackInfo ci) {
        DesyncTracker.clearAll();
    }

    @Inject(method = "handleContainerSetSlot", at = @At("TAIL"))
    private void desyncguard$onContainerSetSlot(ClientboundContainerSetSlotPacket packet, CallbackInfo ci) {
        DesyncTracker.clearSlotPending(packet.getContainerId(), packet.getSlot());
    }
}
