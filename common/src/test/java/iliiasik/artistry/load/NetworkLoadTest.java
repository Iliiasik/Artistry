package iliiasik.artistry.load;

import iliiasik.artistry.config.ArtistryConfig;
import iliiasik.artistry.data.CanvasData;
import iliiasik.artistry.network.ArtistryNetwork;
import iliiasik.artistry.network.CanvasCursorS2CPacket;
import iliiasik.artistry.network.DeliverImageS2CPacket;
import iliiasik.artistry.network.PacketThrottle;
import iliiasik.artistry.network.PosterTarget;
import iliiasik.artistry.network.SaveCanvasC2SPacket;
import iliiasik.artistry.network.UploadImageC2SPacket;
import iliiasik.artistry.support.McFixture;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("load")
class NetworkLoadTest {

    @Test
    @DisplayName("A storm of cursor packets encodes and decodes without drift")
    void cursorPacketStorm() {
        Random random = new Random(11);
        FriendlyByteBuf buf = McFixture.buffer();
        int count = 100_000;

        long start = System.nanoTime();
        for (int i = 0; i < count; i++) {
            CanvasCursorS2CPacket packet = new CanvasCursorS2CPacket(
                    new BlockPos(random.nextInt(1000), 64, random.nextInt(1000)),
                    UUID.randomUUID(),
                    (short) random.nextInt(512),
                    (short) random.nextInt(512));

            packet.write(buf);
            assertEquals(packet, CanvasCursorS2CPacket.read(buf));
        }
        long ms = (System.nanoTime() - start) / 1_000_000;

        assertEquals(0, buf.readableBytes(), "the buffer must be fully drained");
        assertTrue(ms < 30_000, "encoding " + count + " cursor packets took " + ms + " ms");
    }

    @Test
    @DisplayName("A storm of save packets keeps every pixel change")
    void savePacketStorm() {
        Random random = new Random(22);
        int batches = 2_000;

        for (int i = 0; i < batches; i++) {
            List<CanvasData.PixelChange> changes = new ArrayList<>();
            int size = 1 + random.nextInt(64);
            for (int j = 0; j < size; j++) {
                changes.add(new CanvasData.PixelChange(
                        (byte) random.nextInt(CanvasData.MAX_SIZE),
                        (byte) random.nextInt(CanvasData.MAX_SIZE),
                        (short) random.nextInt(250),
                        random.nextInt()));
            }

            SaveCanvasC2SPacket packet =
                    new SaveCanvasC2SPacket(new PosterTarget.World(new BlockPos(i, 64, i)), changes);
            FriendlyByteBuf buf = McFixture.buffer();
            packet.write(buf);

            assertEquals(packet, SaveCanvasC2SPacket.read(buf));
        }
    }

    @Test
    @DisplayName("Large image payloads survive the codec chunk by chunk")
    void largeImagePayloads() {
        int[] sizes = {64 * 1024, 256 * 1024, 1024 * 1024};

        for (int size : sizes) {
            byte[] bytes = McFixture.pattern(size);

            assertArrayEquals(bytes, reassembleUpload(bytes), "upload of " + size + " bytes");
            assertArrayEquals(bytes, reassembleDeliver(bytes), "delivery of " + size + " bytes");
        }
    }

    private static byte[] reassembleUpload(byte[] bytes) {
        byte[] received = new byte[bytes.length];
        for (int offset = 0; offset < bytes.length; offset += ArtistryNetwork.UPLOAD_CHUNK_SIZE) {
            int length = Math.min(ArtistryNetwork.UPLOAD_CHUNK_SIZE, bytes.length - offset);
            byte[] chunk = new byte[length];
            System.arraycopy(bytes, offset, chunk, 0, length);

            FriendlyByteBuf buf = McFixture.buffer();
            new UploadImageC2SPacket(new PosterTarget.World(BlockPos.ZERO), bytes.length, offset, chunk).write(buf);
            assertTrue(buf.readableBytes() <= 32767, "an upload chunk outgrew the payload limit");

            UploadImageC2SPacket decoded = UploadImageC2SPacket.read(buf);
            assertEquals(bytes.length, decoded.total());
            System.arraycopy(decoded.chunk(), 0, received, decoded.offset(), decoded.chunk().length);
        }
        return received;
    }

    private static byte[] reassembleDeliver(byte[] bytes) {
        UUID uuid = UUID.randomUUID();
        byte[] received = new byte[bytes.length];
        for (int offset = 0; offset < bytes.length; offset += ArtistryNetwork.DELIVER_CHUNK_SIZE) {
            int length = Math.min(ArtistryNetwork.DELIVER_CHUNK_SIZE, bytes.length - offset);
            byte[] chunk = new byte[length];
            System.arraycopy(bytes, offset, chunk, 0, length);

            FriendlyByteBuf buf = McFixture.buffer();
            new DeliverImageS2CPacket(uuid, bytes.length, offset, chunk).write(buf);

            DeliverImageS2CPacket decoded = DeliverImageS2CPacket.read(buf);
            assertEquals(uuid, decoded.uuid());
            assertEquals(bytes.length, decoded.total());
            System.arraycopy(decoded.chunk(), 0, received, decoded.offset(), decoded.chunk().length);
        }
        return received;
    }

    @Test
    @DisplayName("The throttle keeps a flood of players within the configured budget")
    void throttleHandlesManyPlayers() {
        ArtistryConfig.get();
        int players = 500;
        List<UUID> ids = new ArrayList<>(players);
        for (int i = 0; i < players; i++) {
            UUID id = UUID.randomUUID();
            ids.add(id);
            PacketThrottle.remove(id);
        }

        int accepted = 0;
        for (int round = 0; round < 50; round++) {
            for (UUID id : ids) {
                if (!PacketThrottle.throttled(id, PacketThrottle.Channel.SAVE)) accepted++;
            }
        }

        assertTrue(accepted > 0, "the throttle must let some packets through");
        assertTrue(accepted < players * 50, "the throttle must reject part of the flood");

        for (UUID id : ids) {
            PacketThrottle.remove(id);
        }
    }
}
