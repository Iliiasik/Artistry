package iliiasik.artistry.network;

import iliiasik.artistry.config.ArtistryConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PacketThrottleTest {

    private static final int MAX_DRAIN_ATTEMPTS = 10_000;

    private UUID player;

    @BeforeEach
    void setUp() {
        player = UUID.randomUUID();
        PacketThrottle.remove(player);
        ArtistryConfig.get();
    }

    private static void drain(UUID player, PacketThrottle.Channel channel) {
        for (int i = 0; i < MAX_DRAIN_ATTEMPTS; i++) {
            if (PacketThrottle.throttled(player, channel)) return;
        }
        throw new AssertionError("bucket for " + channel + " never ran out of tokens");
    }

    @Test
    @DisplayName("The first packet of a player is never throttled")
    void firstPacketPasses() {
        assertFalse(PacketThrottle.throttled(player, PacketThrottle.Channel.SAVE));
    }

    @Test
    @DisplayName("A burst beyond the bucket capacity gets throttled")
    void burstIsThrottled() {
        drain(player, PacketThrottle.Channel.SAVE);
        assertTrue(PacketThrottle.throttled(player, PacketThrottle.Channel.SAVE));
    }

    @Test
    @DisplayName("The save and cursor channels have independent buckets")
    void channelsAreIndependent() {
        drain(player, PacketThrottle.Channel.SAVE);
        assertFalse(PacketThrottle.throttled(player, PacketThrottle.Channel.CURSOR));
    }

    @Test
    @DisplayName("Players have independent buckets")
    void playersAreIndependent() {
        UUID other = UUID.randomUUID();
        PacketThrottle.remove(other);

        drain(player, PacketThrottle.Channel.SAVE);
        assertFalse(PacketThrottle.throttled(other, PacketThrottle.Channel.SAVE));
        PacketThrottle.remove(other);
    }

    @Test
    @DisplayName("Removing a player resets the bucket")
    void removeResetsBucket() {
        drain(player, PacketThrottle.Channel.SAVE);
        PacketThrottle.remove(player);
        assertFalse(PacketThrottle.throttled(player, PacketThrottle.Channel.SAVE));
    }

    @Test
    @DisplayName("Tokens refill over time")
    void bucketRefills() throws InterruptedException {
        drain(player, PacketThrottle.Channel.CURSOR);
        Thread.sleep(ArtistryConfig.get().network.cursorIntervalMs * 3);
        assertFalse(PacketThrottle.throttled(player, PacketThrottle.Channel.CURSOR));
    }
}
