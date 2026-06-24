package com.desyncguard.client;

import com.desyncguard.DesyncTracker;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DesyncGuardClient implements ClientModInitializer {
    public static final String MOD_ID = "desyncguard";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static KeyMapping historyScreenKey;

    @Override
    public void onInitializeClient() {
        LOGGER.info("[DesyncGuard] Client initialized. Watching for desyncs...");

        // Load config (Improvement 1)
        DesyncGuardConfig.load();

        // Register HUD
        DesyncHud.register();

        // Fix 5: Clear all state on disconnect
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            DesyncTracker.clearAll();
            DesyncTracker.clearAllBreaks();
            DesyncTracker.resetAll();
            DesyncHud.clearInventoryDesync();
            DesyncHud.resetSession();
            DesyncStats.resetSession();
            LOGGER.info("[DesyncGuard] Session state cleared on disconnect.");
        });

        // Idea 4: Register keybind for desync history screen (default: H)
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
        });

        LOGGER.info("[DesyncGuard] Features loaded:");
        LOGGER.info("[DesyncGuard]  - Inventory Ghost Item Watchdog");
        LOGGER.info("[DesyncGuard]  - Item Drop Desync Corrector");
        LOGGER.info("[DesyncGuard]  - Rubber-Band Detection & Auto Ghost Block Fix");
        LOGGER.info("[DesyncGuard]  - Desync Warning HUD");
        LOGGER.info("[DesyncGuard]  - Client-Side TPS Estimator");
        LOGGER.info("[DesyncGuard]  - Ghost Block Detector");
        LOGGER.info("[DesyncGuard]  - Ping Spike Detector");
        LOGGER.info("[DesyncGuard]  - Desync History Screen (press H)");
        LOGGER.info("[DesyncGuard]  - Configurable Thresholds (config/desyncguard.json)");
    }
}
