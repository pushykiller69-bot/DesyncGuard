package com.desyncguard.client.mixin;

import com.desyncguard.DesyncTracker;
import net.minecraft.core.BlockPos;
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
        // Fix 4: Filter invalid slot IDs (slot -1 = drop outside window)
        if (packet instanceof ServerboundContainerClickPacket clickPacket) {
            int slot = clickPacket.getSlotNum();
            if (slot >= 0) {
                DesyncTracker.markSlotPending(clickPacket.getContainerId(), slot);
            }
        }

        // Idea 2: Track block break attempts for ghost block detection
        if (packet instanceof ServerboundPlayerActionPacket actionPacket) {
            if (actionPacket.getAction() == ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK) {
                DesyncTracker.markBreakPending(actionPacket.getPos());
            }
        }
    }
}
