package iliiasik.artistry.client.image;

import com.mojang.blaze3d.platform.NativeImage;
import iliiasik.artistry.Artistry;
import iliiasik.artistry.client.renderer.CanvasCellPainter;
import iliiasik.artistry.client.renderer.RenderTuning;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class ClientImageCache {

    private static final int PIXELIZE_BLOCK_SIZE = 4;

    private static final Map<UUID, byte[]> sources = new HashMap<>();
    private static final Map<UUID, Long> sourceLastUsed = new HashMap<>();
    private static final LinkedHashMap<String, NativeImage> decoded = new LinkedHashMap<>(16, 0.75f, true);
    private static final Map<UUID, ResourceLocation> textures = new HashMap<>();
    private static final Map<UUID, int[]> sourceDimensions = new HashMap<>();
    private static final Map<String, ResourceLocation> pixelizedTextures = new HashMap<>();
    private static final Map<ResourceLocation, Long> textureSizes = new HashMap<>();
    private static final Map<ResourceLocation, Long> lastUsed = new HashMap<>();

    private static long textureBytes = 0;
    private static long sourceBytes = 0;
    private static long decodedBytes = 0;
    private static long lastSweep = 0;
    private static long clockMillis = System.currentTimeMillis();

    private ClientImageCache() {}

    public static int textureCount() {
        return textureSizes.size();
    }

    public static long textureBytes() {
        return textureBytes;
    }

    public static long sourceBytes() {
        return sourceBytes;
    }

    public static long decodedBytes() {
        return decodedBytes;
    }

    private static void trackTexture(ResourceLocation id, int width, int height) {
        long bytes = (long) width * height * 4L;
        Long previous = textureSizes.put(id, bytes);
        if (previous != null) textureBytes -= previous;
        textureBytes += bytes;
    }

    private static void untrackTexture(ResourceLocation id) {
        Long bytes = textureSizes.remove(id);
        if (bytes != null) textureBytes -= bytes;
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public static boolean has(UUID uuid) {
        if (!sources.containsKey(uuid)) return false;
        touchSource(uuid);
        return true;
    }

    public static void store(UUID uuid, byte[] bytes) {
        byte[] previous = sources.put(uuid, bytes);
        if (previous != null) sourceBytes -= previous.length;
        sourceBytes += bytes.length;
        touchSource(uuid);
        dropDecoded(uuid);
        registerTexture(uuid, bytes);
    }

    private static void touchSource(UUID uuid) {
        Long previous = sourceLastUsed.get(uuid);
        if (previous != null && clockMillis - previous < RenderTuning.IMAGE_TOUCH_RESOLUTION_MILLIS) return;
        sourceLastUsed.put(uuid, clockMillis);
    }

    private static byte[] takeSource(UUID uuid) {
        byte[] bytes = sources.get(uuid);
        if (bytes != null) touchSource(uuid);
        return bytes;
    }

    private static void sweepSources(long now) {
        List<UUID> expired = new ArrayList<>();
        for (Map.Entry<UUID, Long> entry : sourceLastUsed.entrySet()) {
            if (now - entry.getValue() > RenderTuning.IMAGE_SOURCE_IDLE_MILLIS) expired.add(entry.getKey());
        }
        for (UUID uuid : expired) {
            dropImage(uuid);
        }

        if (sourceBytes <= RenderTuning.IMAGE_SOURCE_BUDGET_BYTES) return;

        List<UUID> evictable = new ArrayList<>();
        for (Map.Entry<UUID, Long> entry : sourceLastUsed.entrySet()) {
            if (now - entry.getValue() >= RenderTuning.IMAGE_SOURCE_PROTECT_MILLIS) evictable.add(entry.getKey());
        }
        evictable.sort(Comparator.comparingLong(sourceLastUsed::get));
        for (UUID uuid : evictable) {
            if (sourceBytes <= RenderTuning.IMAGE_SOURCE_BUDGET_BYTES) break;
            dropImage(uuid);
        }
    }

    private static void registerTexture(UUID uuid, byte[] bytes) {
        try {
            NativeImage img = NativeImage.read(new ByteArrayInputStream(bytes));
            sourceDimensions.put(uuid, new int[]{img.getWidth(), img.getHeight()});
            DynamicTexture tex = new DynamicTexture(img);
            ResourceLocation id = ResourceLocation.fromNamespaceAndPath("artistry", "canvas_image/" + uuid);
            Minecraft.getInstance().getTextureManager().register(id, tex);
            textures.put(uuid, id);
            trackTexture(id, img.getWidth(), img.getHeight());
        } catch (IOException e) {
            Artistry.LOGGER.error("Failed to register canvas image texture {}", uuid, e);
        }
    }

    public static ResourceLocation getTexture(UUID uuid) {
        ResourceLocation id = textures.get(uuid);
        if (id == null) {
            byte[] bytes = takeSource(uuid);
            if (bytes == null) return null;
            registerTexture(uuid, bytes);
            id = textures.get(uuid);
            if (id == null) return null;
        }
        lastUsed.put(id, System.currentTimeMillis());
        return id;
    }

    public static NativeImage getBakeSource(UUID uuid, boolean pixelized, int gridW, int gridH) {
        String key = pixelized ? pxKey(uuid, gridW, gridH) : uuid.toString();
        NativeImage cached = decoded.get(key);
        if (cached != null) return cached;

        byte[] bytes = takeSource(uuid);
        if (bytes == null) return null;

        NativeImage image;
        try {
            NativeImage source = NativeImage.read(new ByteArrayInputStream(bytes));
            if (pixelized) {
                image = buildPixelized(source, gridW, gridH);
                source.close();
            } else {
                image = source;
            }
            image = fitForBaking(image);
        } catch (IOException e) {
            Artistry.LOGGER.error("Failed to decode canvas image {}", uuid, e);
            return null;
        }

        decoded.put(key, image);
        decodedBytes += imageBytes(image);
        trimDecoded();
        return image;
    }

    private static NativeImage fitForBaking(NativeImage source) {
        int limit = RenderTuning.IMAGE_BAKE_SOURCE_MAX;
        int sourceWidth = source.getWidth();
        int sourceHeight = source.getHeight();
        int width = Math.min(sourceWidth, limit);
        int height = Math.min(sourceHeight, limit);
        if (width == sourceWidth && height == sourceHeight) return source;

        NativeImage reduced = new NativeImage(width, height, false);
        CanvasCellPainter.PixelSource pixels = CanvasCellPainter.sourceOf(source);
        for (int y = 0; y < height; y++) {
            int top = y * sourceHeight / height;
            int bottom = Math.max(top + 1, (y + 1) * sourceHeight / height);
            for (int x = 0; x < width; x++) {
                int left = x * sourceWidth / width;
                int right = Math.max(left + 1, (x + 1) * sourceWidth / width);
                reduced.setPixelRGBA(x, y, CanvasCellPainter.average(pixels, left, top, right, bottom));
            }
        }
        source.close();
        return reduced;
    }

    private static NativeImage buildPixelized(NativeImage src, int gridW, int gridH) {
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
        return dst;
    }

    private static long imageBytes(NativeImage image) {
        return (long) image.getWidth() * image.getHeight() * 4L;
    }

    private static void trimDecoded() {
        Iterator<Map.Entry<String, NativeImage>> it = decoded.entrySet().iterator();
        while (decodedBytes > RenderTuning.IMAGE_DECODED_BUDGET_BYTES && decoded.size() > 1 && it.hasNext()) {
            NativeImage evicted = it.next().getValue();
            it.remove();
            decodedBytes -= imageBytes(evicted);
            evicted.close();
        }
    }

    private static void dropDecoded(UUID uuid) {
        String prefix = uuid.toString();
        decoded.entrySet().removeIf(entry -> {
            if (!entry.getKey().startsWith(prefix)) return false;
            decodedBytes -= imageBytes(entry.getValue());
            entry.getValue().close();
            return true;
        });
    }

    public static void sweep() {
        long now = System.currentTimeMillis();
        clockMillis = now;
        if (now - lastSweep < RenderTuning.IMAGE_SWEEP_INTERVAL_MILLIS) return;
        lastSweep = now;

        textures.entrySet().removeIf(entry -> releaseIfIdle(entry.getValue(), now));
        pixelizedTextures.entrySet().removeIf(entry -> releaseIfIdle(entry.getValue(), now));
        sweepSources(now);
    }

    private static boolean releaseIfIdle(ResourceLocation id, long now) {
        Long used = lastUsed.get(id);
        if (used != null && now - used < RenderTuning.IMAGE_IDLE_MILLIS) return false;
        Minecraft.getInstance().getTextureManager().release(id);
        untrackTexture(id);
        lastUsed.remove(id);
        return true;
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
        if (cached == null) cached = rebuildPixelizedTexture(uuid, gridW, gridH);
        if (cached != null) lastUsed.put(cached, System.currentTimeMillis());
        return cached;
    }

    public static ResourceLocation rebuildPixelizedTexture(UUID uuid, int gridW, int gridH) {
        byte[] bytes = takeSource(uuid);
        if (bytes == null) return null;
        try (NativeImage src = NativeImage.read(new ByteArrayInputStream(bytes))) {
            NativeImage dst = buildPixelized(src, gridW, gridH);

            String key = pxKey(uuid, gridW, gridH);
            ResourceLocation old = pixelizedTextures.get(key);
            if (old != null) {
                Minecraft.getInstance().getTextureManager().release(old);
                untrackTexture(old);
            }
            ResourceLocation id = ResourceLocation.fromNamespaceAndPath("artistry", "canvas_image_px/" + key);
            Minecraft.getInstance().getTextureManager().register(id, new DynamicTexture(dst));
            pixelizedTextures.put(key, id);
            trackTexture(id, dst.getWidth(), dst.getHeight());
            return id;
        } catch (IOException e) {
            Artistry.LOGGER.error("Failed to build pixelized texture {} ({}x{})", uuid, gridW, gridH, e);
            return null;
        }
    }

    public static void evict(UUID uuid) {
        dropImage(uuid);
    }

    private static void dropImage(UUID uuid) {
        byte[] bytes = sources.remove(uuid);
        if (bytes != null) sourceBytes -= bytes.length;
        sourceLastUsed.remove(uuid);
        sourceDimensions.remove(uuid);
        dropDecoded(uuid);

        ResourceLocation id = textures.remove(uuid);
        if (id != null) {
            Minecraft.getInstance().getTextureManager().release(id);
            untrackTexture(id);
            lastUsed.remove(id);
        }

        String prefix = uuid + "_";
        pixelizedTextures.entrySet().removeIf(entry -> {
            if (!entry.getKey().startsWith(prefix)) return false;
            Minecraft.getInstance().getTextureManager().release(entry.getValue());
            untrackTexture(entry.getValue());
            lastUsed.remove(entry.getValue());
            return true;
        });
    }

    public static void clear() {
        for (ResourceLocation id : textures.values())
            Minecraft.getInstance().getTextureManager().release(id);
        for (ResourceLocation id : pixelizedTextures.values())
            Minecraft.getInstance().getTextureManager().release(id);
        for (NativeImage image : decoded.values())
            image.close();
        sources.clear();
        sourceLastUsed.clear();
        textures.clear();
        sourceDimensions.clear();
        pixelizedTextures.clear();
        textureSizes.clear();
        lastUsed.clear();
        decoded.clear();
        textureBytes = 0;
        sourceBytes = 0;
        decodedBytes = 0;
        lastSweep = 0;
    }
}
