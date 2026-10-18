package com.desyncguard.client.mixin;

import com.desyncguard.client.DesyncGuardClient;
import com.desyncguard.client.DesyncGuardConfig;
import com.desyncguard.client.DesyncHud;
import com.desyncguard.client.DesyncStats;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.world.entity.RelativeMovement;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

@Mixin(ClientPacketListener.class)
public class PlayerPositionDesyncMixin {

    @Unique
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");
    @Unique
    private static final long MAX_LOG_BYTES = 1_000_000L;

    @Inject(method = "handleMovePlayer", at = @At("HEAD"))
    private void desyncguard$onServerPositionCorrection(ClientboundPlayerPositionPacket packet, CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();
        if (!mc.isSameThread()) return;
        LocalPlayer player = mc.player;
        if (player == null || player.tickCount < 40 || player.isPassenger()) return;

        Set<RelativeMovement> rel = packet.getRelativeArguments();
        double serverX = rel.contains(RelativeMovement.X) ? player.getX() + packet.getX() : packet.getX();
        double serverY = rel.contains(RelativeMovement.Y) ? player.getY() + packet.getY() : packet.getY();
        double serverZ = rel.contains(RelativeMovement.Z) ? player.getZ() + packet.getZ() : packet.getZ();

        double dx = player.getX() - serverX;
        double dy = player.getY() - serverY;
        double dz = player.getZ() - serverZ;
        double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);

        DesyncGuardConfig config = DesyncGuardConfig.get();
        if (distance <= config.rbThreshold) return;
        if (distance > config.rbMaxBlocks) return;

        if (config.showRubberbandCoords) {
            DesyncGuardClient.LOGGER.warn(
                    "[DesyncGuard] Rubber-band: {} blocks. Client ({}, {}, {}) -> server ({}, {}, {})",
                    String.format("%.2f", distance),
                    String.format("%.1f", player.getX()), String.format("%.1f", player.getY()), String.format("%.1f", player.getZ()),
                    String.format("%.1f", serverX), String.format("%.1f", serverY), String.format("%.1f", serverZ));
        } else {
            DesyncGuardClient.LOGGER.warn("[DesyncGuard] Rubber-band: {} blocks.", String.format("%.2f", distance));
        }

        DesyncHud.notifyRubberband(distance);
        DesyncStats.recordRubberband(distance);

        if (config.logSessionToFile) {
            desyncguard$quickLog(distance, player.getX(), player.getY(), player.getZ(),
                    config.showRubberbandCoords);
        }
    }

    @Unique
    private void desyncguard$quickLog(double distance, double x, double y, double z, boolean showCoordinates) {
        CompletableFuture.runAsync(() -> {
            try {
                Path logFile = FabricLoader.getInstance().getGameDir().resolve("desyncguard_session.log");
                if (Files.exists(logFile) && Files.size(logFile) > MAX_LOG_BYTES) {
                    Files.delete(logFile);
                }
                String entry = showCoordinates
                        ? String.format("[%s] Rubber-band %.2f blocks at (%.1f, %.1f, %.1f)%n",
                                LocalTime.now().format(TIME_FORMAT), distance, x, y, z)
                        : String.format("[%s] Rubber-band %.2f blocks%n",
                                LocalTime.now().format(TIME_FORMAT), distance);
                Files.writeString(logFile, entry, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            } catch (IOException e) {
                DesyncGuardClient.LOGGER.warn("[DesyncGuard] Failed to write session log", e);
            }
        });
    }
}
