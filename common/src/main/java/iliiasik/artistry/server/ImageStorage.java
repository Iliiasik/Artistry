package iliiasik.artistry.server;

import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;
import java.util.stream.Stream;

public final class ImageStorage {

    private static final String EXTENSION = ".img";

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
        UUID uuid = idOf(bytes);
        Path path = pathOf(uuid);
        if (Files.exists(path)) return uuid;

        Path temp = Files.createTempFile(storageDir, uuid.toString(), ".tmp");
        try {
            Files.write(temp, bytes);
            Files.move(temp, path, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            Files.deleteIfExists(temp);
            if (!Files.exists(path)) throw e;
        }
        return uuid;
    }

    public static byte[] load(UUID uuid) throws IOException {
        Path path = pathOf(uuid);
        if (!Files.exists(path)) throw new IOException("Image not found: " + uuid);
        return Files.readAllBytes(path);
    }

    public static boolean exists(UUID uuid) {
        return storageDir != null && Files.exists(pathOf(uuid));
    }

    public static Usage usage() throws IOException {
        if (storageDir == null || !Files.isDirectory(storageDir)) return new Usage(0, 0L);
        try (Stream<Path> files = Files.list(storageDir)) {
            long[] totals = files
                    .filter(path -> path.getFileName().toString().endsWith(EXTENSION))
                    .mapToLong(ImageStorage::sizeOf)
                    .collect(() -> new long[2],
                            (acc, size) -> {
                                acc[0]++;
                                acc[1] += size;
                            },
                            (a, b) -> {
                                a[0] += b[0];
                                a[1] += b[1];
                            });
            return new Usage((int) totals[0], totals[1]);
        }
    }

    public static UUID idOf(byte[] bytes) {
        byte[] digest = sha256(bytes);
        ByteBuffer buffer = ByteBuffer.wrap(digest, 0, 16);
        return new UUID(buffer.getLong(), buffer.getLong());
    }

    private static byte[] sha256(byte[] bytes) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(bytes);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is required but unavailable", e);
        }
    }

    private static long sizeOf(Path path) {
        try {
            return Files.size(path);
        } catch (IOException e) {
            return 0L;
        }
    }

    private static Path pathOf(UUID uuid) {
        return storageDir.resolve(uuid + EXTENSION);
    }

    public record Usage(int files, long bytes) {}
}
