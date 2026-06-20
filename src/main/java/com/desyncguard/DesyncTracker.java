package com.desyncguard;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class DesyncTracker {
    public static final Map<Integer, Long> PENDING_SLOTS = new ConcurrentHashMap<>();

    public static final long INVENTORY_DESYNC_THRESHOLD = 500;

    public static void markSlotPending(int containerId, int slotId) {
        int key = (containerId << 16) | (slotId & 0xFFFF);
        PENDING_SLOTS.put(key, System.currentTimeMillis());
    }

    public static void clearSlotPending(int containerId, int slotId) {
        int key = (containerId << 16) | (slotId & 0xFFFF);
        PENDING_SLOTS.remove(key);
    }

    public static void clearAll() {
        PENDING_SLOTS.clear();
    }

    public static boolean isSlotDesynced(int containerId, int slotId) {
        int key = (containerId << 16) | (slotId & 0xFFFF);
        Long timestamp = PENDING_SLOTS.get(key);
        if (timestamp != null) {
            return System.currentTimeMillis() - timestamp > INVENTORY_DESYNC_THRESHOLD;
        }
        return false;
    }
}
