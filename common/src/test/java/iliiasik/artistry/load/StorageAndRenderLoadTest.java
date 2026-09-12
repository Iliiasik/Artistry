package iliiasik.artistry.load;

import iliiasik.artistry.client.renderer.ImageOcclusionClipper;
import iliiasik.artistry.client.renderer.ImageOcclusionClipper.VisibleFragment;
import iliiasik.artistry.data.CanvasImage;
import iliiasik.artistry.data.CanvasImageLayer;
import iliiasik.artistry.server.ImageStorage;
import iliiasik.artistry.support.McFixture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("load")
class StorageAndRenderLoadTest {

    @TempDir
    Path storage;

    @BeforeEach
    void redirectStorage() throws Exception {
        Field field = ImageStorage.class.getDeclaredField("storageDir");
        field.setAccessible(true);
        field.set(null, storage);
        Files.createDirectories(storage);
    }

    @Test
    @DisplayName("Hundreds of large images can be stored and read back")
    void hundredsOfLargeImages() throws IOException {
        int count = 200;
        int size = 512 * 1024;
        Map<UUID, byte[]> saved = new HashMap<>();

        long start = System.nanoTime();
        for (int i = 0; i < count; i++) {
            byte[] payload = McFixture.pattern(size + i);
            saved.put(ImageStorage.save(payload), payload);
        }
        long writeMs = (System.nanoTime() - start) / 1_000_000;

        assertEquals(count, saved.size(), "distinct images must get distinct ids");

        for (Map.Entry<UUID, byte[]> entry : saved.entrySet()) {
            assertTrue(ImageStorage.exists(entry.getKey()));
            assertArrayEquals(entry.getValue(), ImageStorage.load(entry.getKey()));
        }

        assertTrue(writeMs < 120_000, "writing " + count + " images took " + writeMs + " ms");
    }

    @Test
    @DisplayName("Storage keeps working when many posters upload at once")
    void manyPostersUploadInParallel() throws Exception {
        int threads = 8;
        int perThread = 40;
        List<Thread> workers = new ArrayList<>();
        Set<UUID> ids = java.util.Collections.synchronizedSet(new HashSet<>());
        List<Throwable> failures = java.util.Collections.synchronizedList(new ArrayList<>());

        for (int t = 0; t < threads; t++) {
            int offset = t * perThread;
            Thread worker = new Thread(() -> {
                try {
                    for (int i = 0; i < perThread; i++) {
                        ids.add(ImageStorage.save(McFixture.pattern(64 * 1024 + offset + i)));
                    }
                } catch (Throwable throwable) {
                    failures.add(throwable);
                }
            });
            workers.add(worker);
            worker.start();
        }
        for (Thread worker : workers) {
            worker.join();
        }

        assertTrue(failures.isEmpty(), "concurrent uploads failed: " + failures);
        assertEquals(threads * perThread, ids.size());
    }

    @Test
    @DisplayName("A crowd uploading the same picture costs one file")
    void duplicateUploadsCollapse() throws IOException {
        byte[] shared = McFixture.pattern(256 * 1024);
        Set<UUID> ids = new HashSet<>();

        for (int i = 0; i < 500; i++) {
            ids.add(ImageStorage.save(shared));
        }

        assertEquals(1, ids.size());
        assertEquals(1, ImageStorage.usage().files());
        assertEquals(shared.length, ImageStorage.usage().bytes());
    }

    @Test
    @DisplayName("Occlusion stays correct and fast for a wall of posters")
    void occlusionForAWallOfPosters() {
        Random random = new Random(555);
        int posters = 1_000;

        long start = System.nanoTime();
        for (int p = 0; p < posters; p++) {
            CanvasImageLayer layer = new CanvasImageLayer();
            for (int i = 0; i < CanvasImageLayer.MAX_IMAGES; i++) {
                layer.addImage(new CanvasImage(UUID.randomUUID(),
                        random.nextInt(24), random.nextInt(24),
                        3 + random.nextInt(10), 3 + random.nextInt(10)));
            }

            List<VisibleFragment> fragments =
                    ImageOcclusionClipper.computeVisibleFragments(layer.getImages());

            for (VisibleFragment fragment : fragments) {
                assertTrue(fragment.destRect().x1() > fragment.destRect().x0());
                assertTrue(fragment.destRect().y1() > fragment.destRect().y0());
                assertTrue(fragment.u0() >= 0f && fragment.u1() <= 1f);
                assertTrue(fragment.v0() >= 0f && fragment.v1() <= 1f);
            }
        }
        long ms = (System.nanoTime() - start) / 1_000_000;

        assertTrue(ms < 30_000, "clipping " + posters + " posters took " + ms + " ms");
    }

    @Test
    @DisplayName("Deeply stacked images never produce overlapping fragments")
    void deeplyStackedImagesDoNotOverlap() {
        List<CanvasImage> images = new ArrayList<>();
        for (int i = 0; i < 12; i++) {
            images.add(new CanvasImage(UUID.randomUUID(), i, i, 20, 20));
        }

        List<VisibleFragment> fragments = ImageOcclusionClipper.computeVisibleFragments(images);

        boolean[][] covered = new boolean[64][64];
        for (VisibleFragment fragment : fragments) {
            for (int y = fragment.destRect().y0(); y < fragment.destRect().y1(); y++) {
                for (int x = fragment.destRect().x0(); x < fragment.destRect().x1(); x++) {
                    assertTrue(!covered[y][x], "cell " + x + "," + y + " is drawn twice");
                    covered[y][x] = true;
                }
            }
        }
    }
}
