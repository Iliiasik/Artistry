package iliiasik.artistry.config;

import iliiasik.artistry.support.TestPlatformHelper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ArtistryConfigTest {

    private static final Path CONFIG_FILE =
            TestPlatformHelper.CONFIG_DIRECTORY.resolve("artistry").resolve("artistry.json");

    @BeforeEach
    void clean() throws IOException {
        Files.deleteIfExists(CONFIG_FILE);
    }

    @AfterEach
    void restoreDefaults() throws IOException {
        Files.deleteIfExists(CONFIG_FILE);
        ArtistryConfig.reload();
    }

    private static void write(String json) throws IOException {
        Files.createDirectories(CONFIG_FILE.getParent());
        Files.writeString(CONFIG_FILE, json);
    }

    @Test
    @DisplayName("A missing config is created with the documented defaults")
    void missingConfigIsCreated() {
        ArtistryConfig config = ArtistryConfig.reload();

        assertTrue(Files.exists(CONFIG_FILE), "the config file should be written on first load");
        assertEquals(50, config.network.batchIntervalMs);
        assertEquals(100, config.network.cursorIntervalMs);
        assertEquals(3, config.poster.maxEditors);
        assertFalse(config.poster.disableImages);
    }

    @Test
    @DisplayName("get returns the same cached instance")
    void getIsCached() {
        ArtistryConfig first = ArtistryConfig.get();
        assertSame(first, ArtistryConfig.get());
    }

    @Test
    @DisplayName("Values on disk are read back")
    void valuesAreRead() throws IOException {
        write("""
                {
                  "network": { "batchIntervalMs": 120, "cursorIntervalMs": 300 },
                  "poster": { "maxEditors": 7, "disableImages": true }
                }
                """);

        ArtistryConfig config = ArtistryConfig.reload();

        assertEquals(120, config.network.batchIntervalMs);
        assertEquals(300, config.network.cursorIntervalMs);
        assertEquals(7, config.poster.maxEditors);
        assertTrue(config.poster.disableImages);
    }

    @Test
    @DisplayName("Out of range values are clamped and written back")
    void outOfRangeValuesAreClamped() throws IOException {
        write("""
                {
                  "network": { "batchIntervalMs": 1, "cursorIntervalMs": 99999 },
                  "poster": { "maxEditors": -5, "disableImages": false }
                }
                """);

        ArtistryConfig config = ArtistryConfig.reload();

        assertEquals(10, config.network.batchIntervalMs);
        assertEquals(1000, config.network.cursorIntervalMs);
        assertEquals(0, config.poster.maxEditors);

        String written = Files.readString(CONFIG_FILE);
        assertTrue(written.contains("10"), "the clamped value should be persisted");
        assertFalse(written.contains("99999"), "the rejected value should not survive");
    }

    @Test
    @DisplayName("Too small intervals are raised to the minimum")
    void tooSmallIntervalsAreRaised() throws IOException {
        write("{\"network\": {\"batchIntervalMs\": 0, \"cursorIntervalMs\": 0}}");
        ArtistryConfig config = ArtistryConfig.reload();
        assertEquals(10, config.network.batchIntervalMs);
        assertEquals(50, config.network.cursorIntervalMs);
    }

    @Test
    @DisplayName("Malformed JSON falls back to the defaults")
    void malformedJsonFallsBack() throws IOException {
        write("{ this is not json at all ]]]");

        ArtistryConfig config = ArtistryConfig.reload();

        assertNotNull(config);
        assertEquals(50, config.network.batchIntervalMs);
        assertEquals(3, config.poster.maxEditors);
    }

    @Test
    @DisplayName("An empty file falls back to the defaults")
    void emptyFileFallsBack() throws IOException {
        write("");
        ArtistryConfig config = ArtistryConfig.reload();
        assertEquals(50, config.network.batchIntervalMs);
    }

    @Test
    @DisplayName("A partial config keeps the defaults for the missing sections")
    void partialConfigKeepsDefaults() throws IOException {
        write("{\"poster\": {\"disableImages\": true}}");

        ArtistryConfig config = ArtistryConfig.reload();

        assertTrue(config.poster.disableImages);
        assertEquals(50, config.network.batchIntervalMs);
        assertEquals(100, config.network.cursorIntervalMs);
    }

    @Test
    @DisplayName("A config of the wrong shape falls back to the defaults")
    void wrongShapeFallsBack() throws IOException {
        write("[1, 2, 3]");
        ArtistryConfig config = ArtistryConfig.reload();
        assertEquals(50, config.network.batchIntervalMs);
    }

    @Test
    @DisplayName("A deleted config directory is recreated on reload")
    void deletedDirectoryIsRecreated() throws IOException {
        ArtistryConfig.reload();
        Files.deleteIfExists(CONFIG_FILE);
        Files.deleteIfExists(CONFIG_FILE.getParent());

        ArtistryConfig config = ArtistryConfig.reload();

        assertTrue(Files.exists(CONFIG_FILE));
        assertEquals(50, config.network.batchIntervalMs);
    }

    @Test
    @DisplayName("The written config can be read back unchanged")
    void writtenConfigIsStable() throws IOException {
        write("""
                {
                  "network": { "batchIntervalMs": 80, "cursorIntervalMs": 200 },
                  "poster": { "maxEditors": 2, "disableImages": true }
                }
                """);
        ArtistryConfig first = ArtistryConfig.reload();
        ArtistryConfig second = ArtistryConfig.reload();

        assertEquals(first.network.batchIntervalMs, second.network.batchIntervalMs);
        assertEquals(first.network.cursorIntervalMs, second.network.cursorIntervalMs);
        assertEquals(first.poster.maxEditors, second.poster.maxEditors);
        assertEquals(first.poster.disableImages, second.poster.disableImages);
    }
}
