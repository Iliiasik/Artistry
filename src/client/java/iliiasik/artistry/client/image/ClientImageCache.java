package iliiasik.artistry.client.image;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class ClientImageCache {

    private static final Map<UUID, byte[]> rawBytes = new HashMap<>();
    private static final Map<UUID, Identifier> textures = new HashMap<>();

    private ClientImageCache() {}

    public static boolean has(UUID uuid) {
        return rawBytes.containsKey(uuid);
    }

    public static void store(UUID uuid, byte[] bytes) {
        rawBytes.put(uuid, bytes);
        registerTexture(uuid, bytes);
    }

    private static void registerTexture(UUID uuid, byte[] bytes) {
        try {
            NativeImage img = NativeImage.read(new ByteArrayInputStream(bytes));
            NativeImageBackedTexture tex = new NativeImageBackedTexture(img);
            Identifier id = Identifier.of("artistry", "canvas_image/" + uuid);
            MinecraftClient.getInstance().getTextureManager().registerTexture(id, tex);
            textures.put(uuid, id);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static Identifier getTexture(UUID uuid) {
        return textures.get(uuid);
    }

    public static int getWidth(UUID uuid) {
        byte[] bytes = rawBytes.get(uuid);
        if (bytes == null) return 1;
        try {
            NativeImage img = NativeImage.read(new ByteArrayInputStream(bytes));
            int w = img.getWidth();
            img.close();
            return w;
        } catch (IOException e) {
            return 1;
        }
    }

    public static int getHeight(UUID uuid) {
        byte[] bytes = rawBytes.get(uuid);
        if (bytes == null) return 1;
        try {
            NativeImage img = NativeImage.read(new ByteArrayInputStream(bytes));
            int h = img.getHeight();
            img.close();
            return h;
        } catch (IOException e) {
            return 1;
        }
    }

    public static void evict(UUID uuid) {
        rawBytes.remove(uuid);
        Identifier id = textures.remove(uuid);
        if (id != null) {
            MinecraftClient.getInstance().getTextureManager().destroyTexture(id);
        }
    }

    public static void clear() {
        for (Identifier id : textures.values()) {
            MinecraftClient.getInstance().getTextureManager().destroyTexture(id);
        }
        rawBytes.clear();
        textures.clear();
    }

    public static byte[] getRawBytes(UUID uuid) {
        return rawBytes.get(uuid);
    }
}