package com.desyncguard;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.core.BlockPos;

public class DesyncTracker {
    public static final Map<Integer, Long> PENDING_SLOTS = new ConcurrentHashMap<>();

    // Fix 1: Orphaned slot eviction
    private static volatile long lastResetTime = 0L;
    private static final long MAX_PENDING_AGE = 2000L;

    // Improvement 2: Ping-aware adaptive threshold
    private static final long[] recentResponseTimes = new long[10];
    private static int responseIndex = 0;
    private static int responseSamples = 0;
    private static volatile long adaptiveThreshold = 0L;

    // Idea 1: Ping spike detection
    private static volatile long lastPingSpikeMs = 0L;
    private static volatile long lastPingSpikeTime = 0L;
    private static final long PING_SPIKE_DISPLAY_DURATION = 5000L;

    // Idea 2: Ghost block tracking
    private static final Map<BlockPos, Long> PENDING_BREAKS = new ConcurrentHashMap<>();
    private static final long MAX_BREAK_PENDING_AGE = 5000L;

    // Commented-out experiment (Item 3)
    // tried tracking block placements here too but since client handles placement prediction
    // relatively well compared to block breaks, it was causing too many false alerts when building fast.
    // private static final Map<BlockPos, Long> PENDING_PLACES = new ConcurrentHashMap<>();

    // --- Inventory Slot Tracking ---

    public static void markSlotPending(int containerId, int slotId) {
        int key = (containerId << 16) | (slotId & 0xFFFF);
        PENDING_SLOTS.put(key, System.currentTimeMillis());
    }

    public static void clearSlotPending(int containerId, int slotId) {
        int key = (containerId << 16) | (slotId & 0xFFFF);
        Long sendTime = PENDING_SLOTS.remove(key);

        // Improvement 2: Record response time for adaptive calibration
        if (sendTime != null) {
            long responseTime = System.currentTimeMillis() - sendTime;
            recordResponseTime(responseTime);
        }
    }

    public static void clearAll() {
        PENDING_SLOTS.clear();
        lastResetTime = System.currentTimeMillis();
    }

    public static boolean isSlotDesynced(int containerId, int slotId) {
        int key = (containerId << 16) | (slotId & 0xFFFF);
        Long timestamp = PENDING_SLOTS.get(key);
        if (timestamp != null) {
            // Fix 1: Evict entries from before the last clearAll()
            if (timestamp < lastResetTime) {
                PENDING_SLOTS.remove(key);
                return false;
            }
            long age = System.currentTimeMillis() - timestamp;
            // Fix 1: Evict entries older than MAX_PENDING_AGE
            if (age > MAX_PENDING_AGE) {
                PENDING_SLOTS.remove(key);
                return false;
            }
            return age > getEffectiveThreshold();
        }
        return false;
    }

    // --- Adaptive Threshold (Improvement 2) ---

    private static void recordResponseTime(long responseTimeMs) {
        synchronized (recentResponseTimes) {
            long baseline = getBaselineResponseTime();

            recentResponseTimes[responseIndex % recentResponseTimes.length] = responseTimeMs;
            responseIndex++;
            if (responseSamples < recentResponseTimes.length) {
                responseSamples++;
            }

            // Recalculate adaptive threshold = average + 150ms margin
            long sum = 0;
            for (int i = 0; i < responseSamples; i++) {
                sum += recentResponseTimes[i];
            }
            adaptiveThreshold = (sum / responseSamples) + 150L;

            // Idea 1: Spike detection — if response > 3x baseline
            if (baseline > 0 && responseTimeMs > baseline * 3) {
                lastPingSpikeMs = responseTimeMs - baseline;
                lastPingSpikeTime = System.currentTimeMillis();
            }

            // Feed into DesyncStats for avg ping display
            try {
                Class<?> statsClass = Class.forName("com.desyncguard.client.DesyncStats");
                statsClass.getMethod("recordPing", long.class).invoke(null, responseTimeMs);
            } catch (Exception ignored) {
                // DesyncStats is client-side only, may not be available
            }
        }
    }

    private static long getBaselineResponseTime() {
        if (responseSamples == 0) return 0;
        long sum = 0;
        for (int i = 0; i < responseSamples; i++) {
            sum += recentResponseTimes[i];
        }
        return sum / responseSamples;
    }

    public static long getEffectiveThreshold() {
        long configThreshold;
        try {
            Class<?> configClass = Class.forName("com.desyncguard.client.DesyncGuardConfig");
            Object config = configClass.getMethod("get").invoke(null);
            // Updated field name to match human config field
            configThreshold = ((Number) configClass.getField("invThresholdMs").get(config)).longValue();
        } catch (Exception e) {
            configThreshold = 500L;
        }
        return Math.max(configThreshold, adaptiveThreshold);
    }

    public static long getAdaptiveThreshold() {
        return adaptiveThreshold;
    }

    // --- Ping Spike Detection (Idea 1) ---

    public static long getLastPingSpikeMs() {
        if (System.currentTimeMillis() - lastPingSpikeTime > PING_SPIKE_DISPLAY_DURATION) {
            return 0;
        }
        return lastPingSpikeMs;
    }

    public static long getLastPingSpikeTime() {
        return lastPingSpikeTime;
    }

    // --- Ghost Block Tracking (Idea 2) ---

    public static void markBreakPending(BlockPos pos) {
        PENDING_BREAKS.put(pos.immutable(), System.currentTimeMillis());
    }

    public static boolean isPendingBreak(BlockPos pos) {
        Long timestamp = PENDING_BREAKS.get(pos);
        if (timestamp != null) {
            long age = System.currentTimeMillis() - timestamp;
            if (age > MAX_BREAK_PENDING_AGE) {
                PENDING_BREAKS.remove(pos);
                return false;
            }
            return true;
        }
        return false;
    }

    public static void clearBreakPending(BlockPos pos) {
        PENDING_BREAKS.remove(pos);
    }

    public static void clearAllBreaks() {
        PENDING_BREAKS.clear();
    }

    // --- Session Reset ---

    public static void resetAll() {
        PENDING_SLOTS.clear();
        PENDING_BREAKS.clear();
        lastResetTime = System.currentTimeMillis();
        synchronized (recentResponseTimes) {
            responseIndex = 0;
            responseSamples = 0;
            adaptiveThreshold = 0L;
            for (int i = 0; i < recentResponseTimes.length; i++) {
                recentResponseTimes[i] = 0;
            }
        }
        lastPingSpikeMs = 0;
        lastPingSpikeTime = 0;
    }
}
