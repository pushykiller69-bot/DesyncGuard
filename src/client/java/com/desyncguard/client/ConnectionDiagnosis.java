package com.desyncguard.client;

public final class ConnectionDiagnosis {
    private static final double LOW_TPS = 18.0;
    private static final long HIGH_JITTER_MS = 50;
    private static final long HIGH_PING_MS = 200;

    private ConnectionDiagnosis() {}

    public static String describe(int fps) {
        boolean clientSlow = fps > 0 && fps < 20;
        boolean enoughServerSamples = TpsEstimator.getSamples() >= 3;
        boolean enoughPingSamples = DesyncStats.getPingSamples() >= 5;
        boolean networkUnstable = DesyncStats.getPingJitterMs() >= HIGH_JITTER_MS;

        if (TpsEstimator.isStale(System.currentTimeMillis())) {
            if (networkUnstable) return "Server updates stalled; connection is unstable";
            return "No recent server updates; possible server or network stall";
        }
        if (!enoughServerSamples || !enoughPingSamples) {
            return clientSlow ? "Client FPS is low; collecting server/network data" : "Collecting server/network data";
        }

        boolean serverSlow = TpsEstimator.get() < LOW_TPS;
        boolean highStablePing = DesyncStats.getAvgPingMs() >= HIGH_PING_MS && !networkUnstable;

        if (clientSlow && serverSlow && networkUnstable) return "Client, server TPS and connection are struggling";
        if (clientSlow && serverSlow) return "Client FPS is low; server TPS estimate is also low";
        if (clientSlow && networkUnstable) return "Client FPS is low; connection latency is unstable";
        if (clientSlow) return "Client FPS is low";
        if (serverSlow && networkUnstable) return "Low server TPS estimate and unstable connection";
        if (serverSlow) return "Server TPS estimate is low";
        if (networkUnstable) return "Connection latency is unstable";
        if (highStablePing) return "High but stable ping";
        return "No clear server or connection slowdown detected";
    }
}
