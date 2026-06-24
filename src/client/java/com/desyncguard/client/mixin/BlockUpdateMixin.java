package com.desyncguard.client.mixin;

import com.desyncguard.DesyncTracker;
import com.desyncguard.client.DesyncGuardClient;
import com.desyncguard.client.DesyncHud;
import com.desyncguard.client.DesyncStats;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Idea 2: Detects ghost blocks by watching for server block updates
 * that restore a block the player just tried to break.
 */
@Mixin(ClientPacketListener.class)
public class BlockUpdateMixin {

    @Inject(method = "handleBlockUpdate", at = @At("TAIL"))
    private void desyncguard$onBlockUpdate(ClientboundBlockUpdatePacket packet, CallbackInfo ci) {
        BlockPos pos = packet.getPos();
        if (DesyncTracker.isPendingBreak(pos)) {
            // Server sent the block back — this is a ghost block desync
            DesyncTracker.clearBreakPending(pos);
            DesyncHud.notifyGhostBlock(pos);
            DesyncStats.recordGhostBlock();
            DesyncGuardClient.LOGGER.warn(
                    "[DesyncGuard] Ghost block detected at ({}, {}, {})!",
                    pos.getX(), pos.getY(), pos.getZ()
            );
        }
    }
}
