package com.desyncguard.client.mixin;

import com.desyncguard.client.DesyncTracker;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ServerboundContainerClickPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Connection.class)
public class ConnectionMixin {

    @Inject(method = "send(Lnet/minecraft/network/protocol/Packet;)V", at = @At("HEAD"))
    private void desyncguard$onSendPacket(Packet<?> packet, CallbackInfo ci) {
        if (packet instanceof ServerboundContainerClickPacket click) {
            DesyncTracker.markClick(click.getContainerId());
        } else if (packet instanceof ServerboundPlayerActionPacket action) {
            ServerboundPlayerActionPacket.Action a = action.getAction();
            if (a == ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK
                    || a == ServerboundPlayerActionPacket.Action.STOP_DESTROY_BLOCK) {
                DesyncTracker.markBreakPending(action.getPos());
            }
        }
    }
}
