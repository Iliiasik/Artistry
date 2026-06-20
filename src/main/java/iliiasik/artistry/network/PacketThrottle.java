package iliiasik.artistry.network;

import iliiasik.artistry.config.ArtistryConfig;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PacketThrottle {

    public enum Channel { SAVE, CURSOR }

    private static final double BURST_SECONDS = 0.5;

    private static final Map<UUID, Bucket> saveBuckets = new HashMap<>();
    private static final Map<UUID, Bucket> cursorBuckets = new HashMap<>();

    private static final class Bucket {
        double tokens;
        long lastMs;

        Bucket(double tokens) {
            this.tokens = tokens;
            this.lastMs = System.currentTimeMillis();
        }
    }

    public static boolean allow(UUID player, Channel channel) {
        long interval = channel == Channel.SAVE
                ? ArtistryConfig.get().network.batchIntervalMs
                : ArtistryConfig.get().network.cursorIntervalMs;
        if (interval <= 0) interval = 1;

        double refillPerMs = 1.0 / interval;
        double capacity = Math.max(3.0, refillPerMs * 1000.0 * BURST_SECONDS);

        Map<UUID, Bucket> map = channel == Channel.SAVE ? saveBuckets : cursorBuckets;
        long now = System.currentTimeMillis();
        Bucket b = map.get(player);
        if (b == null) {
            b = new Bucket(capacity);
            map.put(player, b);
        }

        b.tokens = Math.min(capacity, b.tokens + (now - b.lastMs) * refillPerMs);
        b.lastMs = now;

        if (b.tokens >= 1.0) {
            b.tokens -= 1.0;
            return true;
        }
        return false;
    }

    public static void remove(UUID player) {
        saveBuckets.remove(player);
        cursorBuckets.remove(player);
    }
}