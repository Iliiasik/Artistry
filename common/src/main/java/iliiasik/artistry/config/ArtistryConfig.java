package iliiasik.artistry.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import iliiasik.artistry.Artistry;
import iliiasik.artistry.platform.Services;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class ArtistryConfig {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = Services.PLATFORM.getConfigDirectory()
            .resolve(Artistry.MOD_ID)
            .resolve(Artistry.MOD_ID + ".json");

    private static ArtistryConfig instance;

    public final NetworkConfig network = new NetworkConfig();
    public final PosterConfig poster = new PosterConfig();

    public static class NetworkConfig {
        public long batchIntervalMs = 50;
        public long cursorIntervalMs = 100;
        public long worldSyncIntervalMs = 500;
        public long imageBytesPerSecond = 512 * 1024;

        private static final long MIN_BATCH_INTERVAL_MS = 10;
        private static final long MAX_BATCH_INTERVAL_MS = 5000;
        private static final long MIN_CURSOR_INTERVAL_MS = 50;
        private static final long MAX_CURSOR_INTERVAL_MS = 1000;
        private static final long MIN_WORLD_SYNC_INTERVAL_MS = 50;
        private static final long MAX_WORLD_SYNC_INTERVAL_MS = 5000;
        private static final long MIN_IMAGE_BYTES_PER_SECOND = 32 * 1024;
        private static final long MAX_IMAGE_BYTES_PER_SECOND = 64L * 1024 * 1024;

        public void validate() {
            if (imageBytesPerSecond < MIN_IMAGE_BYTES_PER_SECOND) {
                System.err.println("[Artistry] imageBytesPerSecond too low (" + imageBytesPerSecond + "), clamping to " + MIN_IMAGE_BYTES_PER_SECOND);
                imageBytesPerSecond = MIN_IMAGE_BYTES_PER_SECOND;
            } else if (imageBytesPerSecond > MAX_IMAGE_BYTES_PER_SECOND) {
                System.err.println("[Artistry] imageBytesPerSecond too high (" + imageBytesPerSecond + "), clamping to " + MAX_IMAGE_BYTES_PER_SECOND);
                imageBytesPerSecond = MAX_IMAGE_BYTES_PER_SECOND;
            }
            if (batchIntervalMs < MIN_BATCH_INTERVAL_MS) {
                System.err.println("[Artistry] batchIntervalMs too low (" + batchIntervalMs + "), clamping to " + MIN_BATCH_INTERVAL_MS);
                batchIntervalMs = MIN_BATCH_INTERVAL_MS;
            } else if (batchIntervalMs > MAX_BATCH_INTERVAL_MS) {
                System.err.println("[Artistry] batchIntervalMs too high (" + batchIntervalMs + "), clamping to " + MAX_BATCH_INTERVAL_MS);
                batchIntervalMs = MAX_BATCH_INTERVAL_MS;
            }
            if (cursorIntervalMs < MIN_CURSOR_INTERVAL_MS) {
                System.err.println("[Artistry] cursorIntervalMs too low (" + cursorIntervalMs + "), clamping to " + MIN_CURSOR_INTERVAL_MS);
                cursorIntervalMs = MIN_CURSOR_INTERVAL_MS;
            } else if (cursorIntervalMs > MAX_CURSOR_INTERVAL_MS) {
                System.err.println("[Artistry] cursorIntervalMs too high (" + cursorIntervalMs + "), clamping to " + MAX_CURSOR_INTERVAL_MS);
                cursorIntervalMs = MAX_CURSOR_INTERVAL_MS;
            }
            if (worldSyncIntervalMs < MIN_WORLD_SYNC_INTERVAL_MS) {
                System.err.println("[Artistry] worldSyncIntervalMs too low (" + worldSyncIntervalMs + "), clamping to " + MIN_WORLD_SYNC_INTERVAL_MS);
                worldSyncIntervalMs = MIN_WORLD_SYNC_INTERVAL_MS;
            } else if (worldSyncIntervalMs > MAX_WORLD_SYNC_INTERVAL_MS) {
                System.err.println("[Artistry] worldSyncIntervalMs too high (" + worldSyncIntervalMs + "), clamping to " + MAX_WORLD_SYNC_INTERVAL_MS);
                worldSyncIntervalMs = MAX_WORLD_SYNC_INTERVAL_MS;
            }
            if (worldSyncIntervalMs < batchIntervalMs) {
                System.err.println("[Artistry] worldSyncIntervalMs below batchIntervalMs, raising to " + batchIntervalMs);
                worldSyncIntervalMs = batchIntervalMs;
            }
        }
    }

    public static class PosterConfig {
        public int maxEditors = 3;
        public boolean disableImages = false;

        public void validate() {
            if (maxEditors < 0) {
                System.err.println("[Artistry] maxEditors below 0 (" + maxEditors + "), clamping to 0");
                maxEditors = 0;
            }
        }
    }

    public static ArtistryConfig get() {
        if (instance == null) instance = load();
        return instance;
    }

    public static ArtistryConfig reload() {
        instance = load();
        return instance;
    }

    private static ArtistryConfig load() {
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            if (Files.exists(CONFIG_PATH)) {
                String json = Files.readString(CONFIG_PATH);
                ArtistryConfig cfg = GSON.fromJson(json, ArtistryConfig.class);
                if (cfg != null) {
                    cfg.validate();
                    save(cfg);
                    return cfg;
                }
                System.err.println("[Artistry] Config file is empty, falling back to defaults");
            }
        } catch (IOException | RuntimeException e) {
            System.err.println("[Artistry] Failed to load config: " + e.getMessage());
        }
        ArtistryConfig defaults = new ArtistryConfig();
        save(defaults);
        return defaults;
    }

    private void validate() {
        network.validate();
        poster.validate();
    }

    private static void save(ArtistryConfig cfg) {
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            Files.writeString(CONFIG_PATH, GSON.toJson(cfg));
        } catch (IOException e) {
            System.err.println("[Artistry] Failed to save config: " + e.getMessage());
        }
    }

    private ArtistryConfig() {}
}