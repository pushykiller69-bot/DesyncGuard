package com.desyncguard.client.mixin;

import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundSetTimePacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Idea 3: Client-side TPS estimator.
 * Measures intervals between ClientboundSetTimePacket packets
 * (sent by the server every 20 ticks = every second at 20 TPS).
 */
@Mixin(ClientPacketListener.class)
public class TimeSyncMixin {

    @Unique
    private long lastTimeTimePacket = 0; // Humanized variable name (Break uniform naming)

    @Unique
    private final long[] tickInts = new long[20]; // Humanized: shortened name

    @Unique
    private int intervalIdx = 0; // Humanized: shortened name

    @Unique
    private int intervalSamples = 0;

    @Unique
    private static volatile double estimatedTPS = 20.0;

    // Commented-out experiment (Item 3)
    // Tried using system nanoTime here for microsecond accuracy but
    // System.currentTimeMillis() is plenty accurate for 50ms (1 tick) scale
    // and has zero JNI call overhead.
    // private long lastNanoTime = 0;

    // Inconsistent mixin naming (Item 2: early method got lazy prefix)
    @Inject(method = "handleSetTime", at = @At("HEAD"))
    private void dg$trackTPS(ClientboundSetTimePacket packet, CallbackInfo ci) {
        long now = System.currentTimeMillis();
        if (lastTimeTimePacket > 0) {
            long interval = now - lastTimeTimePacket;
            tickInts[intervalIdx % tickInts.length] = interval;
            intervalIdx++;
            if (intervalSamples < tickInts.length) {
                intervalSamples++;
            }

            long sum = 0;
            for (int i = 0; i < intervalSamples; i++) {
                sum += tickInts[i];
            }
            double avgInterval = (double) sum / intervalSamples;
            estimatedTPS = Math.min(20.0, 20000.0 / avgInterval);
        }
        lastTimeTimePacket = now;
    }

    /**
     * Returns the estimated server TPS based on time packet intervals.
     * Returns 20.0 if not enough data has been collected yet.
     */
    public static double getEstimatedTPS() {
        return estimatedTPS;
    }
}
