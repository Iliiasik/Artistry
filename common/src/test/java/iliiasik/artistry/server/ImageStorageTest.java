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
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
    @DisplayName("Identical payloads get distinct identifiers")
    void identicalPayloadsAreStoredSeparately() throws IOException {
        byte[] bytes = McFixture.pattern(512);
        Set<UUID> ids = new HashSet<>();
        for (int i = 0; i < 16; i++) {
            ids.add(ImageStorage.save(bytes));
        }
        assertTrue(ids.size() == 16, "every save must produce a unique id");
        for (UUID id : ids) {
            assertArrayEquals(bytes, ImageStorage.load(id));
        }
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
