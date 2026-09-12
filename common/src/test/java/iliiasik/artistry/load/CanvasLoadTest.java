package iliiasik.artistry.load;

import iliiasik.artistry.data.CanvasData;
import iliiasik.artistry.data.CanvasImage;
import iliiasik.artistry.data.CanvasImageLayer;
import iliiasik.artistry.network.SyncCanvasS2CPacket;
import iliiasik.artistry.network.SyncImageLayerS2CPacket;
import iliiasik.artistry.support.McFixture;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.FriendlyByteBuf;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("load")
class CanvasLoadTest {

    private static final int POSTER_COUNT = 2_000;

    private static CanvasData randomCanvas(Random random) {
        CanvasData data = new CanvasData();
        data.canvasSize = CanvasData.MAX_SIZE;
        for (int y = 0; y < CanvasData.MAX_SIZE; y++) {
            for (int x = 0; x < CanvasData.MAX_SIZE; x++) {
                if (random.nextBoolean()) {
                    data.pixels[y][x] = (short) (1 + random.nextInt(249));
                } else {
                    data.setColor(x, y, random.nextInt());
                }
            }
        }
        return data;
    }

    @Test
    @DisplayName("Thousands of fully painted posters survive a save and load cycle")
    void thousandsOfPostersRoundTrip() {
        Random random = new Random(1234);
        List<CompoundTag> saved = new ArrayList<>(POSTER_COUNT);
        List<CanvasData> originals = new ArrayList<>(POSTER_COUNT);

        long startSave = System.nanoTime();
        for (int i = 0; i < POSTER_COUNT; i++) {
            CanvasData canvas = randomCanvas(random);
            originals.add(canvas);
            saved.add(canvas.toNbt());
        }
        long saveMs = (System.nanoTime() - startSave) / 1_000_000;

        long startLoad = System.nanoTime();
        for (int i = 0; i < POSTER_COUNT; i++) {
            CanvasData restored = new CanvasData();
            restored.fromNbt(saved.get(i));
            assertTrue(restored.diff(originals.get(i)).isEmpty(), "poster " + i + " changed on reload");
        }
        long loadMs = (System.nanoTime() - startLoad) / 1_000_000;

        assertTrue(saveMs < 30_000, "saving " + POSTER_COUNT + " posters took " + saveMs + " ms");
        assertTrue(loadMs < 30_000, "loading " + POSTER_COUNT + " posters took " + loadMs + " ms");
    }

    @Test
    @DisplayName("A fully painted poster produces a bounded diff")
    void fullRepaintDiffIsBounded() {
        Random random = new Random(99);
        CanvasData before = randomCanvas(random);
        CanvasData after = randomCanvas(random);

        List<CanvasData.PixelChange> changes = after.diff(before);

        assertTrue(changes.size() <= CanvasData.MAX_SIZE * CanvasData.MAX_SIZE,
                "a diff can never exceed the pixel count");
    }

    @Test
    @DisplayName("A full canvas sync packet round trips at the maximum payload size")
    void fullCanvasSyncPacketRoundTrips() {
        Random random = new Random(7);
        CanvasData canvas = randomCanvas(random);
        List<CanvasData.PixelChange> changes = canvas.diff(new CanvasData());
        assertEquals(CanvasData.MAX_SIZE * CanvasData.MAX_SIZE, changes.size());

        SyncCanvasS2CPacket packet = new SyncCanvasS2CPacket(new BlockPos(0, 64, 0), changes);
        FriendlyByteBuf buf = McFixture.buffer();
        packet.write(buf);
        int size = buf.readableBytes();
        SyncCanvasS2CPacket decoded = SyncCanvasS2CPacket.read(buf);

        assertEquals(packet, decoded);
        assertTrue(size < 32_767, "a full canvas sync must fit into a single custom payload, was " + size);
    }

    @Test
    @DisplayName("Image layers stay bounded under constant churn")
    void imageLayerChurnStaysBounded() {
        CanvasImageLayer layer = new CanvasImageLayer();
        Random random = new Random(4321);
        int evictedTotal = 0;

        for (int i = 0; i < 5_000; i++) {
            CanvasImage image = new CanvasImage(UUID.randomUUID(),
                    random.nextInt(24), random.nextInt(24),
                    3 + random.nextInt(8), 3 + random.nextInt(8));
            evictedTotal += layer.addImage(image).size();
            assertTrue(layer.getImages().size() <= CanvasImageLayer.MAX_IMAGES,
                    "layer grew past the limit at iteration " + i);
        }

        assertEquals(5_000 - CanvasImageLayer.MAX_IMAGES, evictedTotal);
    }

    @Test
    @DisplayName("Image layer sync packets round trip for a full layer")
    void fullImageLayerPacketRoundTrips() {
        CanvasImageLayer layer = new CanvasImageLayer();
        for (int i = 0; i < CanvasImageLayer.MAX_IMAGES; i++) {
            layer.addImage(new CanvasImage(UUID.randomUUID(), i, i, 8, 8));
        }

        SyncImageLayerS2CPacket packet =
                new SyncImageLayerS2CPacket(new BlockPos(1, 2, 3), layer.getImages());
        FriendlyByteBuf buf = McFixture.buffer();
        packet.write(buf);
        SyncImageLayerS2CPacket decoded = SyncImageLayerS2CPacket.read(buf);

        assertEquals(CanvasImageLayer.MAX_IMAGES, decoded.images().size());
    }

    @Test
    @DisplayName("Thousands of image layers survive a save and load cycle")
    void thousandsOfImageLayersRoundTrip() {
        Random random = new Random(2024);
        List<ListTag> saved = new ArrayList<>(POSTER_COUNT);

        for (int i = 0; i < POSTER_COUNT; i++) {
            CanvasImageLayer layer = new CanvasImageLayer();
            for (int j = 0; j < CanvasImageLayer.MAX_IMAGES; j++) {
                CanvasImage image = new CanvasImage(UUID.randomUUID(),
                        random.nextInt(20), random.nextInt(20), 4, 4);
                image.pixelized = random.nextBoolean();
                layer.addImage(image);
            }
            saved.add(layer.toNbt());
        }

        for (ListTag tag : saved) {
            CanvasImageLayer restored = new CanvasImageLayer();
            restored.fromNbt(tag);
            assertEquals(CanvasImageLayer.MAX_IMAGES, restored.getImages().size());
        }
    }
}
