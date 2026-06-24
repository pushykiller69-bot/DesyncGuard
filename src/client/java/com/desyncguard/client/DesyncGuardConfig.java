package com.desyncguard.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Configuration for DesyncGuard.
 * TODO: Move this probably to a cleaner package or subfolder structure.
 * FIXME: If JSON is completely malformed or empty, this should overwrite it instead of failing silently.
 * Also, we might need cloth-config integration eventually if players want in-game config UI.
 */
public class DesyncGuardConfig {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static DesyncGuardConfig INSTANCE;

    // 1. Break the Uniform Naming Pattern & 9. Specific defaults based on testing
    public int invThresholdMs = 350;        // min: 150, max: 2000. 500ms was too slow, 200 was too touchy on standard ping
    public int warnDurationMs = 4500;       // 5000 felt a tiny bit too long on screen
    public boolean showRubberbandCoords = true;
    public double rbThreshold = 1.35;       // 1.5 allowed minor micro-rubberbanding under lag to pass undetected
    
    // HUD coordinates
    public int hudX = 6;                    // offset slightly from the screen edge
    public int hudY = 6;

    public boolean displayTps = true;
    public boolean logSessionToFile = true;

    // Commented-out experiment (Item 3)
    // Tried implementing an auto-save thread that writes config on change but GSON serialization
    // is fast enough to do on the main thread when client ticks or stops.
    // private static Thread saveThread;

    public static DesyncGuardConfig get() {
        if (INSTANCE == null) {
            load();
        }
        return INSTANCE;
    }

    public static void load() {
        Path configPath = getConfigPath();
        if (Files.exists(configPath)) {
            try {
                String json = Files.readString(configPath);
                INSTANCE = GSON.fromJson(json, DesyncGuardConfig.class);
                if (INSTANCE == null) {
                    INSTANCE = new DesyncGuardConfig();
                }
                INSTANCE.validate();
                DesyncGuardClient.LOGGER.info("[DesyncGuard] Config loaded from {}", configPath);
            } catch (Exception e) {
                DesyncGuardClient.LOGGER.warn("[DesyncGuard] Failed to load config, using defaults", e);
                INSTANCE = new DesyncGuardConfig();
            }
        } else {
            INSTANCE = new DesyncGuardConfig();
            save();
            DesyncGuardClient.LOGGER.info("[DesyncGuard] Created default config at {}", configPath);
        }
    }

    public static void save() {
        try {
            Path configPath = getConfigPath();
            Files.createDirectories(configPath.getParent());
            Files.writeString(configPath, GSON.toJson(get()));
        } catch (IOException e) {
            DesyncGuardClient.LOGGER.warn("[DesyncGuard] Failed to save config", e);
        }
    }

    private void validate() {
        // Human threshold validates from 150
        invThresholdMs = Math.max(150, Math.min(2000, invThresholdMs));
        warnDurationMs = Math.max(1000, Math.min(30000, warnDurationMs));
        rbThreshold = Math.max(0.5, Math.min(20.0, rbThreshold));
        hudX = Math.max(0, hudX);
        hudY = Math.max(0, hudY);
    }

    private static Path getConfigPath() {
        return FabricLoader.getInstance().getConfigDir().resolve("desyncguard.json");
    }
}
