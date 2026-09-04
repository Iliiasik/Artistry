package iliiasik.artistry.network;

import iliiasik.artistry.config.ArtistryConfig;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PacketThrottle {

    public enum Channel { SAVE, CURSOR }

    private static final double BURST_SECONDS = 0.5;
    private static final double IMAGE_BURST_SECONDS = 2.0;

    private static final Map<UUID, Bucket> saveBuckets = new HashMap<>();
    private static final Map<UUID, Bucket> cursorBuckets = new HashMap<>();
    private static final Map<UUID, Bucket> imageBuckets = new HashMap<>();

    private static final class Bucket {
        double tokens;
        long lastMs;

        Bucket(double tokens) {
            this.tokens = tokens;
            this.lastMs = System.currentTimeMillis();
        }

        boolean denies(double cost, double refillPerMs, double capacity) {
            long now = System.currentTimeMillis();
            tokens = Math.min(capacity, tokens + (now - lastMs) * refillPerMs);
            lastMs = now;
            if (tokens < cost) return true;
            tokens -= cost;
            return false;
        }
    }

    private PacketThrottle() {}

    public static boolean throttled(UUID player, Channel channel) {
        long interval = channel == Channel.SAVE
                ? ArtistryConfig.get().network.batchIntervalMs
                : ArtistryConfig.get().network.cursorIntervalMs;
        if (interval <= 0) interval = 1;

        double refillPerMs = 1.0 / interval;
        double capacity = Math.max(3.0, refillPerMs * 1000.0 * BURST_SECONDS);
        Map<UUID, Bucket> map = channel == Channel.SAVE ? saveBuckets : cursorBuckets;

        return bucket(map, player, capacity).denies(1.0, refillPerMs, capacity);
    }

    public static boolean imageThrottled(UUID player, int bytes) {
        double perSecond = ArtistryConfig.get().network.imageBytesPerSecond;
        double refillPerMs = perSecond / 1000.0;
        double capacity = Math.max(bytes, perSecond * IMAGE_BURST_SECONDS);

        return bucket(imageBuckets, player, capacity).denies(bytes, refillPerMs, capacity);
    }

    private static Bucket bucket(Map<UUID, Bucket> map, UUID player, double capacity) {
        Bucket existing = map.get(player);
        if (existing != null) return existing;
        Bucket created = new Bucket(capacity);
        map.put(player, created);
        return created;
    }

    public static void remove(UUID player) {
        saveBuckets.remove(player);
        cursorBuckets.remove(player);
        imageBuckets.remove(player);
    }
}
