package com.desyncguard.client.mixin;

import com.desyncguard.client.DesyncGuardClient;
import com.desyncguard.client.DesyncHud;
import com.desyncguard.client.DesyncStats;
import com.desyncguard.client.DesyncTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public class BlockUpdateMixin {

    @Inject(method = "handleBlockUpdate", at = @At("HEAD"))
    private void desyncguard$onBlockUpdate(ClientboundBlockUpdatePacket packet, CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();
        if (!mc.isSameThread() || mc.level == null) return;

        BlockPos pos = packet.getPos();
        if (!DesyncTracker.isPendingBreak(pos)) return;

        boolean clientShowsAir = mc.level.getBlockState(pos).isAir();
        boolean serverHasBlock = !packet.getBlockState().isAir();
        if (clientShowsAir && serverHasBlock) {
            DesyncTracker.clearBreakPending(pos);
            DesyncHud.notifyGhostBlock(pos);
            DesyncStats.recordGhostBlock();
            DesyncGuardClient.LOGGER.warn("[DesyncGuard] Ghost block at ({}, {}, {}): server restored it",
                    pos.getX(), pos.getY(), pos.getZ());
        } else if (!serverHasBlock) {
            DesyncTracker.clearBreakPending(pos);
        }
    }
}
