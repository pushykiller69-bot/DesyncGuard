package com.desyncguard.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class DesyncGuardConfig {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static DesyncGuardConfig INSTANCE;

    public int invCorrelationMs = 1500;
    public int warnDurationMs = 4500;
    public boolean showRubberbandCoords = true;
    public double rbThreshold = 1.35;
    public double rbMaxBlocks = 12.0;

    public boolean hudEnabled = true;
    public int hudX = 6;
    public int hudY = 6;

    public boolean displayTps = true;
    public boolean logSessionToFile = true;

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
                    save();
                }
                INSTANCE.validate();
                DesyncGuardClient.LOGGER.info("[DesyncGuard] Config loaded from {}", configPath);
            } catch (Exception e) {
                DesyncGuardClient.LOGGER.warn("[DesyncGuard] Config unreadable, backing it up and writing defaults", e);
                try {
                    Files.copy(configPath, configPath.resolveSibling("desyncguard.json.bak"),
                            java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException backupError) {
                    DesyncGuardClient.LOGGER.error("[DesyncGuard] Failed to back up unreadable config", backupError);
                }
                INSTANCE = new DesyncGuardConfig();
                save();
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
        invCorrelationMs = Math.max(300, Math.min(5000, invCorrelationMs));
        rbMaxBlocks = Math.max(2.0, Math.min(64.0, rbMaxBlocks));
        warnDurationMs = Math.max(1000, Math.min(30000, warnDurationMs));
        rbThreshold = Math.max(0.5, Math.min(20.0, rbThreshold));
        hudX = Math.max(0, hudX);
        hudY = Math.max(0, hudY);
    }

    private static Path getConfigPath() {
        return FabricLoader.getInstance().getConfigDir().resolve("desyncguard.json");
    }
}
