package iliiasik.artistry.server;

import iliiasik.artistry.support.McFixture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ImageStorageTest {

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
    @DisplayName("A saved image can be read back byte for byte")
    void saveAndLoad() throws IOException {
        byte[] bytes = McFixture.pattern(2048);
        UUID uuid = ImageStorage.save(bytes);

        assertTrue(ImageStorage.exists(uuid));
        assertArrayEquals(bytes, ImageStorage.load(uuid));
    }

    @Test
    @DisplayName("Identical payloads collapse onto one identifier and one file")
    void identicalPayloadsAreDeduplicated() throws IOException {
        byte[] bytes = McFixture.pattern(512);
        Set<UUID> ids = new HashSet<>();
        for (int i = 0; i < 16; i++) {
            ids.add(ImageStorage.save(bytes));
        }

        assertEquals(1, ids.size(), "the same bytes must always map to the same id");
        assertArrayEquals(bytes, ImageStorage.load(ids.iterator().next()));
        assertEquals(1, ImageStorage.usage().files(), "only one file should have been written");
    }

    @Test
    @DisplayName("Different payloads keep different identifiers")
    void differentPayloadsStayApart() throws IOException {
        UUID first = ImageStorage.save(McFixture.pattern(512));
        UUID second = ImageStorage.save(McFixture.pattern(513));

        assertNotEquals(first, second);
        assertEquals(2, ImageStorage.usage().files());
    }

    @Test
    @DisplayName("A single flipped byte produces a different identifier")
    void oneBitApartIsADifferentImage() throws IOException {
        byte[] bytes = McFixture.pattern(256);
        byte[] altered = bytes.clone();
        altered[128] = (byte) (altered[128] ^ 0x01);

        assertNotEquals(ImageStorage.save(bytes), ImageStorage.save(altered));
    }

    @Test
    @DisplayName("The identifier depends only on the bytes, not on the storage state")
    void identifierIsPureFunctionOfContent() {
        byte[] bytes = McFixture.pattern(1024);

        assertEquals(ImageStorage.idOf(bytes), ImageStorage.idOf(bytes.clone()));
        assertNotEquals(ImageStorage.idOf(bytes), ImageStorage.idOf(McFixture.pattern(1025)));
    }

    @Test
    @DisplayName("Re-saving does not disturb a file that was edited on disk")
    void resavingKeepsTheExistingFile() throws IOException {
        byte[] bytes = McFixture.pattern(64);
        UUID uuid = ImageStorage.save(bytes);
        Files.write(storage.resolve(uuid + ".img"), new byte[]{9, 9, 9});

        assertEquals(uuid, ImageStorage.save(bytes));
        assertArrayEquals(new byte[]{9, 9, 9}, ImageStorage.load(uuid),
                "an existing file must not be rewritten");
    }

    @Test
    @DisplayName("A file only becomes visible once it is written whole")
    void savePublishesAtomically() throws Exception {
        int threads = 8;
        byte[] bytes = McFixture.pattern(2 * 1024 * 1024);
        List<Throwable> failures = Collections.synchronizedList(new ArrayList<>());
        List<Thread> workers = new ArrayList<>();

        for (int i = 0; i < threads; i++) {
            Thread worker = new Thread(() -> {
                try {
                    UUID uuid = ImageStorage.save(bytes);
                    assertArrayEquals(bytes, ImageStorage.load(uuid), "a half written file was published");
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

        assertTrue(failures.isEmpty(), "concurrent saves failed: " + failures);
        assertEquals(1, ImageStorage.usage().files(), "no temporary files may be left behind");
    }

    @Test
    @DisplayName("Usage counts the files and their total size")
    void usageReportsFilesAndBytes() throws IOException {
        assertEquals(0, ImageStorage.usage().files());
        assertEquals(0L, ImageStorage.usage().bytes());

        ImageStorage.save(McFixture.pattern(100));
        ImageStorage.save(McFixture.pattern(200));
        Files.writeString(storage.resolve("not-an-image.txt"), "ignore me");

        ImageStorage.Usage usage = ImageStorage.usage();
        assertEquals(2, usage.files(), "only .img files count");
        assertEquals(300L, usage.bytes());
    }

    @Test
    @DisplayName("An unknown image does not exist and cannot be loaded")
    void unknownImage() {
        UUID unknown = UUID.randomUUID();
        assertFalse(ImageStorage.exists(unknown));
        assertThrows(IOException.class, () -> ImageStorage.load(unknown));
    }

    @Test
    @DisplayName("An empty payload round trips")
    void emptyPayload() throws IOException {
        UUID uuid = ImageStorage.save(new byte[0]);
        assertArrayEquals(new byte[0], ImageStorage.load(uuid));
    }

    @Test
    @DisplayName("A multi megabyte payload round trips")
    void largePayload() throws IOException {
        byte[] bytes = McFixture.pattern(4 * 1024 * 1024);
        UUID uuid = ImageStorage.save(bytes);
        assertArrayEquals(bytes, ImageStorage.load(uuid));
    }

    @Test
    @DisplayName("A deleted file is reported as missing")
    void deletedFileIsMissing() throws IOException {
        UUID uuid = ImageStorage.save(McFixture.pattern(64));
        Files.delete(storage.resolve(uuid + ".img"));
        assertFalse(ImageStorage.exists(uuid));
        assertThrows(IOException.class, () -> ImageStorage.load(uuid));
    }

    @Test
    @DisplayName("A truncated file loads as the truncated content instead of failing")
    void truncatedFileIsReadable() throws IOException {
        UUID uuid = ImageStorage.save(McFixture.pattern(256));
        Files.write(storage.resolve(uuid + ".img"), new byte[]{1, 2, 3});
        assertArrayEquals(new byte[]{1, 2, 3}, ImageStorage.load(uuid));
    }
}
