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

    private static final int PIXELIZE_BLOCK_SIZE = 4;

    private static final Map<UUID, byte[]> rawBytes     = new HashMap<>();
    private static final Map<UUID, Identifier> textures = new HashMap<>();
    private static final Map<UUID, Identifier> pixelizedTextures = new HashMap<>();
    private static final Map<UUID, long[]> pixelizedDimensions  = new HashMap<>();
    private static final Map<UUID, int[]> sourceDimensions = new HashMap<>();

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
            sourceDimensions.put(uuid, new int[]{img.getWidth(), img.getHeight()});
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

    public static byte[] getRawBytes(UUID uuid) {
        return rawBytes.get(uuid);
    }

    public static int getWidth(UUID uuid) {
        int[] dims = sourceDimensions.get(uuid);
        return dims != null ? dims[0] : 1;
    }

    public static int getHeight(UUID uuid) {
        int[] dims = sourceDimensions.get(uuid);
        return dims != null ? dims[1] : 1;
    }

    public static Identifier getOrBuildPixelizedTexture(UUID uuid, int gridW, int gridH) {
        long[] dims = pixelizedDimensions.get(uuid);
        if (dims != null && dims[0] == gridW && dims[1] == gridH) {
            Identifier cached = pixelizedTextures.get(uuid);
            if (cached != null) return cached;
        }
        return rebuildPixelizedTexture(uuid, gridW, gridH);
    }

    public static Identifier rebuildPixelizedTexture(UUID uuid, int gridW, int gridH) {
        byte[] bytes = rawBytes.get(uuid);
        if (bytes == null) return null;
        try {
            NativeImage src = NativeImage.read(new ByteArrayInputStream(bytes));
            int srcW = src.getWidth();
            int srcH = src.getHeight();

            int blockW = gridW * PIXELIZE_BLOCK_SIZE;
            int blockH = gridH * PIXELIZE_BLOCK_SIZE;

            NativeImage dst = new NativeImage(blockW, blockH, false);

            for (int bx = 0; bx < blockW; bx++) {
                for (int by = 0; by < blockH; by++) {
                    int px = Math.min((int)((float) bx / blockW * srcW), srcW - 1);
                    int py = Math.min((int)((float) by / blockH * srcH), srcH - 1);
                    dst.setColor(bx, by, src.getColor(px, py));
                }
            }
            src.close();

            Identifier oldId = pixelizedTextures.get(uuid);
            if (oldId != null) {
                MinecraftClient.getInstance().getTextureManager().destroyTexture(oldId);
            }

            Identifier id = Identifier.of("artistry", "canvas_image_px/" + uuid + "_" + gridW + "_" + gridH);
            NativeImageBackedTexture tex = new NativeImageBackedTexture(dst);
            MinecraftClient.getInstance().getTextureManager().registerTexture(id, tex);
            pixelizedTextures.put(uuid, id);
            pixelizedDimensions.put(uuid, new long[]{gridW, gridH});
            return id;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static void evict(UUID uuid) {
        rawBytes.remove(uuid);
        sourceDimensions.remove(uuid);
        Identifier id = textures.remove(uuid);
        if (id != null) MinecraftClient.getInstance().getTextureManager().destroyTexture(id);
        Identifier pxId = pixelizedTextures.remove(uuid);
        if (pxId != null) MinecraftClient.getInstance().getTextureManager().destroyTexture(pxId);
        pixelizedDimensions.remove(uuid);
    }

    public static void clear() {
        for (Identifier id : textures.values())
            MinecraftClient.getInstance().getTextureManager().destroyTexture(id);
        for (Identifier id : pixelizedTextures.values())
            MinecraftClient.getInstance().getTextureManager().destroyTexture(id);
        rawBytes.clear();
        textures.clear();
        pixelizedTextures.clear();
        pixelizedDimensions.clear();
        sourceDimensions.clear();
    }
}