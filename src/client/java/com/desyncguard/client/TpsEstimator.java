package com.desyncguard.client;

public final class TpsEstimator {
    private static final long[] intervals = new long[10];
    private static int idx = 0;
    private static int samples = 0;
    private static long lastPacketMs = 0;
    private static volatile double tps = 20.0;

    private TpsEstimator() {}

    public static synchronized void onTimePacket(long nowMs) {
        if (lastPacketMs > 0) {
            long interval = nowMs - lastPacketMs;
            if (interval >= 200 && interval <= 30_000) {
                intervals[idx % intervals.length] = interval;
                idx++;
                if (samples < intervals.length) samples++;
                long sum = 0;
                for (int i = 0; i < samples; i++) sum += intervals[i];
                double avg = (double) sum / samples;
                tps = Math.max(0.0, Math.min(20.0, 20000.0 / avg));
            }
        }
        lastPacketMs = nowMs;
    }

    public static double get() {
        return tps;
    }

    public static synchronized int getSamples() {
        return samples;
    }

    public static synchronized boolean isStale(long nowMs) {
        return lastPacketMs > 0 && nowMs - lastPacketMs >= 3000;
    }

    public static synchronized void reset() {
        idx = 0;
        samples = 0;
        lastPacketMs = 0;
        tps = 20.0;
        for (int i = 0; i < intervals.length; i++) intervals[i] = 0;
    }
}
