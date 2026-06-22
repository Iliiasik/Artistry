package iliiasik.artistry.client.image;

import com.mojang.blaze3d.platform.NativeImage;
import iliiasik.artistry.Artistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class ClientImageCache {

    private static final int PIXELIZE_BLOCK_SIZE = 4;

    private static final Map<UUID, byte[]> rawBytes = new HashMap<>();
    private static final Map<UUID, ResourceLocation> textures = new HashMap<>();
    private static final Map<UUID, int[]> sourceDimensions = new HashMap<>();
    private static final Map<String, ResourceLocation> pixelizedTextures = new HashMap<>();

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
            DynamicTexture tex = new DynamicTexture(img);
            ResourceLocation id = ResourceLocation.fromNamespaceAndPath("artistry", "canvas_image/" + uuid);
            Minecraft.getInstance().getTextureManager().register(id, tex);
            textures.put(uuid, id);
        } catch (IOException e) {
            Artistry.LOGGER.error("Failed to register canvas image texture {}", uuid, e);
        }
    }

    public static ResourceLocation getTexture(UUID uuid) {
        return textures.get(uuid);
    }

    public static int getWidth(UUID uuid) {
        int[] dims = sourceDimensions.get(uuid);
        return dims != null ? dims[0] : 1;
    }

    public static int getHeight(UUID uuid) {
        int[] dims = sourceDimensions.get(uuid);
        return dims != null ? dims[1] : 1;
    }

    private static String pxKey(UUID uuid, int gridW, int gridH) {
        return uuid + "_" + gridW + "_" + gridH;
    }

    public static ResourceLocation getOrBuildPixelizedTexture(UUID uuid, int gridW, int gridH) {
        ResourceLocation cached = pixelizedTextures.get(pxKey(uuid, gridW, gridH));
        if (cached != null) return cached;
        return rebuildPixelizedTexture(uuid, gridW, gridH);
    }

    public static ResourceLocation rebuildPixelizedTexture(UUID uuid, int gridW, int gridH) {
        byte[] bytes = rawBytes.get(uuid);
        if (bytes == null) return null;
        try (NativeImage src = NativeImage.read(new ByteArrayInputStream(bytes))) {
            int srcW = src.getWidth();
            int srcH = src.getHeight();
            int blockW = gridW * PIXELIZE_BLOCK_SIZE;
            int blockH = gridH * PIXELIZE_BLOCK_SIZE;

            NativeImage dst = new NativeImage(blockW, blockH, false);
            for (int bx = 0; bx < blockW; bx++) {
                for (int by = 0; by < blockH; by++) {
                    int px = Math.min((int) ((float) bx / blockW * srcW), srcW - 1);
                    int py = Math.min((int) ((float) by / blockH * srcH), srcH - 1);
                    dst.setPixelRGBA(bx, by, src.getPixelRGBA(px, py));
                }
            }

            String key = pxKey(uuid, gridW, gridH);
            ResourceLocation old = pixelizedTextures.get(key);
            if (old != null) {
                Minecraft.getInstance().getTextureManager().release(old);
            }
            ResourceLocation id = ResourceLocation.fromNamespaceAndPath("artistry", "canvas_image_px/" + key);
            Minecraft.getInstance().getTextureManager().register(id, new DynamicTexture(dst));
            pixelizedTextures.put(key, id);
            return id;
        } catch (IOException e) {
            Artistry.LOGGER.error("Failed to build pixelized texture {} ({}x{})", uuid, gridW, gridH, e);
            return null;
        }
    }

    public static void evict(UUID uuid) {
        rawBytes.remove(uuid);
        sourceDimensions.remove(uuid);
        ResourceLocation id = textures.remove(uuid);
        if (id != null) Minecraft.getInstance().getTextureManager().release(id);
        String prefix = uuid + "_";
        pixelizedTextures.entrySet().removeIf(e -> {
            if (e.getKey().startsWith(prefix)) {
                Minecraft.getInstance().getTextureManager().release(e.getValue());
                return true;
            }
            return false;
        });
    }

    public static void clear() {
        for (ResourceLocation id : textures.values())
            Minecraft.getInstance().getTextureManager().release(id);
        for (ResourceLocation id : pixelizedTextures.values())
            Minecraft.getInstance().getTextureManager().release(id);
        rawBytes.clear();
        textures.clear();
        sourceDimensions.clear();
        pixelizedTextures.clear();
    }
}