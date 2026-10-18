package com.desyncguard.client;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class DesyncStats {

    private static long sessionStartTime = System.currentTimeMillis();

    private static final AtomicInteger rubberbandCount = new AtomicInteger(0);
    private static volatile double maxRubberbandDistance = 0.0;
    private static volatile double lastRubberbandDistance = 0.0;

    private static final AtomicInteger inventoryDesyncCount = new AtomicInteger(0);

    private static final AtomicInteger ghostBlockCount = new AtomicInteger(0);

    private static final Map<Integer, AtomicInteger> slotDesyncFrequency = new ConcurrentHashMap<>();

    private static volatile long avgPingMs = 0;
    private static volatile long pingJitterMs = 0;
    private static final long[] pingHistory = new long[20];
    private static int pingIndex = 0;
    private static int pingSamples = 0;

    public static void recordRubberband(double distance) {
        rubberbandCount.incrementAndGet();
        lastRubberbandDistance = distance;
        if (distance > maxRubberbandDistance) {
            maxRubberbandDistance = distance;
        }
    }

    public static void recordInventoryDesync(int slotKey) {
        inventoryDesyncCount.incrementAndGet();
        slotDesyncFrequency.computeIfAbsent(slotKey, k -> new AtomicInteger(0)).incrementAndGet();
    }

    public static void recordGhostBlock() {
        ghostBlockCount.incrementAndGet();
    }

    public static void recordPing(long pingMs) {
        synchronized (pingHistory) {
            pingHistory[pingIndex % pingHistory.length] = pingMs;
            pingIndex++;
            if (pingSamples < pingHistory.length) {
                pingSamples++;
            }
            long sum = 0;
            for (int i = 0; i < pingSamples; i++) {
                sum += pingHistory[i];
            }
            avgPingMs = sum / pingSamples;

            long jitterSum = 0;
            for (int i = 1; i < pingSamples; i++) {
                long previous = pingHistory[(pingIndex - pingSamples + i - 1 + pingHistory.length) % pingHistory.length];
                long current = pingHistory[(pingIndex - pingSamples + i + pingHistory.length) % pingHistory.length];
                jitterSum += Math.abs(current - previous);
            }
            pingJitterMs = pingSamples > 1 ? jitterSum / (pingSamples - 1) : 0;
        }
    }

    public static int getRubberbandCount() {
        return rubberbandCount.get();
    }

    public static double getMaxRubberbandDistance() {
        return maxRubberbandDistance;
    }

    public static double getLastRubberbandDistance() {
        return lastRubberbandDistance;
    }

    public static int getInventoryDesyncCount() {
        return inventoryDesyncCount.get();
    }

    public static int getGhostBlockCount() {
        return ghostBlockCount.get();
    }

    public static long getAvgPingMs() {
        return avgPingMs;
    }

    public static long getPingJitterMs() {
        return pingJitterMs;
    }

    public static int getPingSamples() {
        synchronized (pingHistory) {
            return pingSamples;
        }
    }

    public static long getSessionStartTime() {
        return sessionStartTime;
    }

    public static Map<Integer, AtomicInteger> getSlotDesyncFrequency() {
        return slotDesyncFrequency;
    }

    public static int getMaxSlotDesyncCount() {
        int max = 0;
        for (AtomicInteger count : slotDesyncFrequency.values()) {
            max = Math.max(max, count.get());
        }
        return max;
    }

    public static void resetSession() {
        sessionStartTime = System.currentTimeMillis();
        rubberbandCount.set(0);
        maxRubberbandDistance = 0.0;
        lastRubberbandDistance = 0.0;
        inventoryDesyncCount.set(0);
        ghostBlockCount.set(0);
        slotDesyncFrequency.clear();
        synchronized (pingHistory) {
            pingIndex = 0;
            pingSamples = 0;
            avgPingMs = 0;
            pingJitterMs = 0;
            for (int i = 0; i < pingHistory.length; i++) {
                pingHistory[i] = 0;
            }
        }
    }
}
