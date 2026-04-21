package iliiasik.artistry.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class ArtistryConfig {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = FabricLoader.getInstance()
            .getConfigDir()
            .resolve("artistry")
            .resolve("artistry.json");

    private static ArtistryConfig instance;

    public final NetworkConfig network = new NetworkConfig();

    public static class NetworkConfig {
        public long batchIntervalMs = 50;

        private static final long MIN_BATCH_INTERVAL_MS = 10;
        private static final long MAX_BATCH_INTERVAL_MS = 5000;

        public void validate() {
            if (batchIntervalMs < MIN_BATCH_INTERVAL_MS) {
                System.err.println("[Artistry] batchIntervalMs too low (" + batchIntervalMs + "), clamping to " + MIN_BATCH_INTERVAL_MS);
                batchIntervalMs = MIN_BATCH_INTERVAL_MS;
            } else if (batchIntervalMs > MAX_BATCH_INTERVAL_MS) {
                System.err.println("[Artistry] batchIntervalMs too high (" + batchIntervalMs + "), clamping to " + MAX_BATCH_INTERVAL_MS);
                batchIntervalMs = MAX_BATCH_INTERVAL_MS;
            }
        }
    }

    public static ArtistryConfig get() {
        if (instance == null) instance = load();
        return instance;
    }

    private static ArtistryConfig load() {
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            if (Files.exists(CONFIG_PATH)) {
                String json = Files.readString(CONFIG_PATH);
                ArtistryConfig cfg = GSON.fromJson(json, ArtistryConfig.class);
                cfg.validate();
                save(cfg);
                return cfg;
            }
        } catch (IOException e) {
            System.err.println("[Artistry] Failed to load config: " + e.getMessage());
        }
        ArtistryConfig defaults = new ArtistryConfig();
        save(defaults);
        return defaults;
    }

    private void validate() {
        network.validate();
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