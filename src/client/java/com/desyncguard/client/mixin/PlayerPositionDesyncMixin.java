package com.desyncguard.client.mixin;

import com.desyncguard.client.DesyncGuardClient;
import com.desyncguard.client.DesyncHud;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public class PlayerPositionDesyncMixin {

    @Unique
    private static final double RUBBERBAND_THRESHOLD = 1.5;

    @Inject(method = "handleMovePlayer", at = @At("HEAD"))
    private void desyncguard$onServerPositionCorrection(ClientboundPlayerPositionPacket packet, CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;

        if (player == null) {
            return;
        }

        Vec3 clientPos = player.position();
        double serverX = packet.getX();
        double serverY = packet.getY();
        double serverZ = packet.getZ();

        double dx = clientPos.x - serverX;
        double dy = clientPos.y - serverY;
        double dz = clientPos.z - serverZ;
        double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);

        if (distance > RUBBERBAND_THRESHOLD) {
            DesyncGuardClient.LOGGER.warn(
                "[DesyncGuard] Rubber-band detected! Distance: {} blocks. " +
                "Client pos: ({}, {}, {}) -> Server correction: ({}, {}, {})",
                String.format("%.2f", distance),
                String.format("%.1f", clientPos.x),
                String.format("%.1f", clientPos.y),
                String.format("%.1f", clientPos.z),
                String.format("%.1f", serverX),
                String.format("%.1f", serverY),
                String.format("%.1f", serverZ)
            );

            DesyncHud.notifyRubberband();
        }
    }
}
