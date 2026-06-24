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
import net.minecraft.world.phys.Vec3;
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
import java.util.concurrent.CompletableFuture;

@Mixin(ClientPacketListener.class)
public class PlayerPositionDesyncMixin {

    @Unique
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");

    // Commented-out experiment (Item 3)
    // tried comparing with packet.getRelativeArguments() but it was extremely buggy
    // and teleported the player back to 0,0,0 when they joined server.
    // private boolean isRelative(ClientboundPlayerPositionPacket.RelativeArgument arg) { ... }

    // Inconsistent mixin prefix naming (Item 2: early method name is full prefix, late one got lazy)
    @Inject(method = "handleMovePlayer", at = @At("HEAD"))
    private void desyncguard$onServerPositionCorrection(ClientboundPlayerPositionPacket packet, CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;

        if (player == null) {
            return;
        }

        DesyncGuardConfig config = DesyncGuardConfig.get();
        // Updated field names to match new human config fields
        double threshold = config.rbThreshold;

        Vec3 clientPos = player.position();
        double serverX = packet.getX();
        double serverY = packet.getY();
        double serverZ = packet.getZ();

        double dx = clientPos.x - serverX;
        double dy = clientPos.y - serverY;
        double dz = clientPos.z - serverZ;
        double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);

        if (distance > threshold) {
            if (config.showRubberbandCoords) {
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
            } else {
                DesyncGuardClient.LOGGER.warn(
                        "[DesyncGuard] Rubber-band detected! Distance: {} blocks.",
                        String.format("%.2f", distance)
                );
            }

            DesyncHud.notifyRubberband(distance);
            DesyncStats.recordRubberband(distance);

            // HACK: calling writeSessionLog asynchronously is fine but if the client crashes right after
            // the rubberband we might lose the log. Good enough for now without blocking the main tick.
            if (config.logSessionToFile) {
                dg$quickLog(distance, clientPos.x, clientPos.y, clientPos.z);
            }
        }
    }

    // Lazy prefix naming (Item 2: dg$quickLog instead of desyncguard$writeSessionLog)
    @Unique
    private void dg$quickLog(double distance, double x, double y, double z) {
        CompletableFuture.runAsync(() -> {
            try {
                Path logFile = FabricLoader.getInstance()
                        .getGameDir()
                        .resolve("desyncguard_session.log");
                String entry = String.format("[%s] Rubber-band %.2f blocks at (%.1f, %.1f, %.1f)%n",
                        LocalTime.now().format(TIME_FORMAT), distance, x, y, z);
                Files.writeString(logFile, entry,
                        StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            } catch (IOException e) {
                DesyncGuardClient.LOGGER.warn("[DesyncGuard] Failed to write session log", e);
            }
        });
    }
}
