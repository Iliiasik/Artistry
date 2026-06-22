package iliiasik.artistry.server;

import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

public final class ImageStorage {

    private static Path storageDir;

    private ImageStorage() {}

    public static void init(MinecraftServer server) {
        storageDir = server.getWorldPath(LevelResource.ROOT)
                .resolve("artistry_images");
        try {
            Files.createDirectories(storageDir);
        } catch (IOException e) {
            throw new RuntimeException("Failed to create artistry_images directory", e);
        }
    }

    public static UUID save(byte[] bytes) throws IOException {
        UUID uuid = UUID.nameUUIDFromBytes(bytes);
        Path path = storageDir.resolve(uuid + ".img");
        if (!Files.exists(path)) {
            Files.write(path, bytes);
        }
        return uuid;
    }

    public static byte[] load(UUID uuid) throws IOException {
        Path path = storageDir.resolve(uuid + ".img");
        if (!Files.exists(path)) throw new IOException("Image not found: " + uuid);
        return Files.readAllBytes(path);
    }

    public static boolean exists(UUID uuid) {
        return storageDir != null && Files.exists(storageDir.resolve(uuid + ".img"));
    }
}