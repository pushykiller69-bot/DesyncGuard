package com.desyncguard.client;

import net.minecraft.core.BlockPos;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class DesyncTracker {
    private static final Map<Integer, Long> DESYNCED_SLOTS = new ConcurrentHashMap<>();
    private static final Map<BlockPos, Long> PENDING_BREAKS = new ConcurrentHashMap<>();
    private static final long MAX_BREAK_PENDING_AGE = 5000L;

    private static volatile long lastClickTime = 0L;
    private static volatile int lastClickContainer = Integer.MIN_VALUE;

    private static volatile long lastPingSpikeMs = 0L;
    private static volatile long lastPingSpikeTime = 0L;
    private static final long PING_SPIKE_DISPLAY_DURATION = 5000L;

    private DesyncTracker() {}

    public static int slotKey(int containerId, int slotId) {
        return (containerId << 16) | (slotId & 0xFFFF);
    }

    public static void markClick(int containerId) {
        lastClickContainer = containerId;
        lastClickTime = System.currentTimeMillis();
    }

    public static boolean recentClickIn(int containerId) {
        if (containerId != lastClickContainer) return false;
        return System.currentTimeMillis() - lastClickTime <= getCorrelationWindowMs();
    }

    public static void flagSlotDesync(int key) {
        DESYNCED_SLOTS.put(key, System.currentTimeMillis());
    }

    public static boolean isSlotDesynced(int containerId, int slotId) {
        int key = slotKey(containerId, slotId);
        Long t = DESYNCED_SLOTS.get(key);
        if (t == null) return false;
        if (System.currentTimeMillis() - t > DesyncGuardConfig.get().warnDurationMs) {
            DESYNCED_SLOTS.remove(key);
            return false;
        }
        return true;
    }

    public static void clearAll() {
        DESYNCED_SLOTS.clear();
        lastClickContainer = Integer.MIN_VALUE;
    }

    public static long getCorrelationWindowMs() {
        return Math.max(DesyncGuardConfig.get().invCorrelationMs, DesyncStats.getAvgPingMs() * 3);
    }

    public static void recordPing(long pingMs) {
        long baseline = DesyncStats.getAvgPingMs();
        int samples = DesyncStats.getPingSamples();
        if (samples >= 5 && pingMs > Math.max(150L, baseline * 3)) {
            lastPingSpikeMs = pingMs - baseline;
            lastPingSpikeTime = System.currentTimeMillis();
        }
        DesyncStats.recordPing(pingMs);
    }

    public static long getLastPingSpikeMs() {
        return System.currentTimeMillis() - lastPingSpikeTime > PING_SPIKE_DISPLAY_DURATION ? 0 : lastPingSpikeMs;
    }

    public static long getLastPingSpikeTime() {
        return lastPingSpikeTime;
    }

    public static void markBreakPending(BlockPos pos) {
        PENDING_BREAKS.put(pos.immutable(), System.currentTimeMillis());
    }

    public static boolean isPendingBreak(BlockPos pos) {
        Long t = PENDING_BREAKS.get(pos);
        if (t == null) return false;
        if (System.currentTimeMillis() - t > MAX_BREAK_PENDING_AGE) {
            PENDING_BREAKS.remove(pos);
            return false;
        }
        return true;
    }

    public static void clearBreakPending(BlockPos pos) {
        PENDING_BREAKS.remove(pos);
    }

    public static void clearAllBreaks() {
        PENDING_BREAKS.clear();
    }

    public static void resetAll() {
        DESYNCED_SLOTS.clear();
        PENDING_BREAKS.clear();
        lastClickContainer = Integer.MIN_VALUE;
        lastPingSpikeMs = 0;
        lastPingSpikeTime = 0;
    }
}
