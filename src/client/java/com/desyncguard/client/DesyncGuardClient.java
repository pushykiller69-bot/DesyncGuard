package com.desyncguard.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DesyncGuardClient implements ClientModInitializer {
    public static final String MOD_ID = "desyncguard";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static KeyMapping historyScreenKey;
    private static int pingTicks = 0;

    @Override
    public void onInitializeClient() {
        LOGGER.info("[DesyncGuard] Client initialized. Watching for desyncs...");

        DesyncGuardConfig.load();

        DesyncHud.register();

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            DesyncTracker.resetAll();
            DesyncHud.resetSession();
            TpsEstimator.reset();
            DesyncStats.resetSession();
            LOGGER.info("[DesyncGuard] Session state cleared on disconnect.");
        });

        historyScreenKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.desyncguard.history",
                GLFW.GLFW_KEY_H,
                "category.desyncguard"
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (historyScreenKey.consumeClick()) {
                if (client.screen == null) {
                    client.setScreen(new DesyncHistoryScreen());
                }
            }
            if (client.player != null && client.getConnection() != null && ++pingTicks >= 20) {
                pingTicks = 0;
                PlayerInfo info = client.getConnection().getPlayerInfo(client.player.getUUID());
                if (info != null && info.getLatency() > 0) {
                    DesyncTracker.recordPing(info.getLatency());
                }
            }
        });

        LOGGER.info("[DesyncGuard] Features loaded:");
        LOGGER.info("[DesyncGuard]  - Inventory Desync Detector (server corrections after your clicks)");
        LOGGER.info("[DesyncGuard]  - Server correction tracking for blocks and player position");
        LOGGER.info("[DesyncGuard]  - Rubber-Band Detection");
        LOGGER.info("[DesyncGuard]  - Desync Warning HUD");
        LOGGER.info("[DesyncGuard]  - Client-Side TPS Estimator");
        LOGGER.info("[DesyncGuard]  - Ghost Block Detector");
        LOGGER.info("[DesyncGuard]  - Ping Spike Detector");
        LOGGER.info("[DesyncGuard]  - Desync History Screen (press H)");
        LOGGER.info("[DesyncGuard]  - Configurable Thresholds (config/desyncguard.json)");
    }
}
