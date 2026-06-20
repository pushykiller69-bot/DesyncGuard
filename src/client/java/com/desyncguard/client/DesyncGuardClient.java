package com.desyncguard.client;

import com.desyncguard.DesyncTracker;
import net.fabricmc.api.ClientModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DesyncGuardClient implements ClientModInitializer {
    public static final String MOD_ID = "desyncguard";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitializeClient() {
        LOGGER.info("[DesyncGuard] Client initialized. Watching for desyncs...");

        DesyncHud.register();

        LOGGER.info("[DesyncGuard] Features loaded:");
        LOGGER.info("[DesyncGuard]  - Inventory Ghost Item Watchdog");
        LOGGER.info("[DesyncGuard]  - Item Drop Desync Corrector");
        LOGGER.info("[DesyncGuard]  - Rubber-Band Detection & Auto Ghost Block Fix");
        LOGGER.info("[DesyncGuard]  - Desync Warning HUD");
    }
}
