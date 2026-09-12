package iliiasik.artistry.client.renderer;

import com.mojang.blaze3d.platform.NativeImage;
import iliiasik.artistry.client.palette.BlockPalette;
import iliiasik.artistry.client.palette.ColorPalette;
import iliiasik.artistry.data.CanvasData;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

@SuppressWarnings("resource")
public final class CanvasCellPainter {

    private static Field mipmapField = null;
    private static boolean fieldResolved = false;
    private static final Map<TextureAtlasSprite, BakedSprite> BAKED_CACHE = new HashMap<>();
    private static int generation = 0;

    private CanvasCellPainter() {}

    private record BakedSprite(int width, int height, int[] pixels) {}

    public static int generation() {
        return generation;
    }

    public static void reset() {
        BAKED_CACHE.clear();
        generation++;
    }

    public static int edge(int cellIndex, int cellCount, int spanPixels) {
        return cellIndex * spanPixels / cellCount;
    }

    private static NativeImage resolveSpriteImage(TextureAtlasSprite sprite) {
        if (!fieldResolved) {
            fieldResolved = true;
            try {
                Field field = sprite.contents().getClass().getDeclaredField("byMipLevel");
                field.setAccessible(true);
                mipmapField = field;
            } catch (NoSuchFieldException e) {
                for (Field field : sprite.contents().getClass().getDeclaredFields()) {
                    field.setAccessible(true);
                    try {
                        if (field.get(sprite.contents()) instanceof NativeImage[]) {
                            mipmapField = field;
                            break;
                        }
                    } catch (Exception ignored) {}
                }
            }
        }
        if (mipmapField == null) return null;
        try {
            NativeImage[] mipLevels = (NativeImage[]) mipmapField.get(sprite.contents());
            return (mipLevels != null && mipLevels.length > 0) ? mipLevels[0] : null;
        } catch (Exception e) {
            return null;
        }
    }

    private static BakedSprite bake(TextureAtlasSprite sprite) {
        BakedSprite cached = BAKED_CACHE.get(sprite);
        if (cached != null) return cached;

        NativeImage source = resolveSpriteImage(sprite);
        if (source == null) return null;

        try {
            int width = Math.min(sprite.contents().width(), source.getWidth());
            int height = Math.min(sprite.contents().height(), source.getHeight());
            if (width <= 0 || height <= 0) return null;
            int[] pixels = new int[width * height];
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    pixels[y * width + x] = source.getPixelRGBA(x, y);
                }
            }
            BakedSprite baked = new BakedSprite(width, height, pixels);
            BAKED_CACHE.put(sprite, baked);
            return baked;
        } catch (RuntimeException e) {
            return null;
        }
    }

    public static void paintCell(NativeImage target, int left, int top, int right, int bottom,
                                 short blockIndex, int color, int backgroundColor) {
        if (right <= left || bottom <= top) return;
        if (blockIndex == CanvasData.COLOR_PIXEL) {
            fillRect(target, left, top, right, bottom, color);
            return;
        }
        if (blockIndex <= 0) {
            fillRect(target, left, top, right, bottom, backgroundColor);
            return;
        }
        TextureAtlasSprite sprite = BlockPalette.getSprite(blockIndex);
        if (sprite == null) {
            fillRect(target, left, top, right, bottom, backgroundColor);
            return;
        }
        BakedSprite baked = bake(sprite);
        if (baked == null) {
            fillRect(target, left, top, right, bottom, backgroundColor);
            return;
        }
        blitSprite(target, baked, left, top, right, bottom);
    }

    public static void fillRect(NativeImage target, int left, int top, int right, int bottom, int argb) {
        int abgr = ColorPalette.argbToAbgr(argb);
        for (int y = top; y < bottom; y++) {
            for (int x = left; x < right; x++) {
                target.setPixelRGBA(x, y, abgr);
            }
        }
    }

    public interface PixelSource {
        int at(int x, int y);
    }

    public static PixelSource sourceOf(NativeImage image) {
        return image::getPixelRGBA;
    }

    private static void blitSprite(NativeImage target, BakedSprite sprite,
                                   int left, int top, int right, int bottom) {
        int width = right - left;
        int height = bottom - top;
        int spriteWidth = sprite.width();
        int spriteHeight = sprite.height();
        int[] pixels = sprite.pixels();
        PixelSource source = (x, y) -> pixels[y * spriteWidth + x];

        if (width >= spriteWidth && height >= spriteHeight) {
            for (int offsetY = 0; offsetY < height; offsetY++) {
                int row = Math.min(offsetY * spriteHeight / height, spriteHeight - 1) * spriteWidth;
                for (int offsetX = 0; offsetX < width; offsetX++) {
                    int column = Math.min(offsetX * spriteWidth / width, spriteWidth - 1);
                    target.setPixelRGBA(left + offsetX, top + offsetY, pixels[row + column]);
                }
            }
            return;
        }

        for (int offsetY = 0; offsetY < height; offsetY++) {
            int sourceTop = offsetY * spriteHeight / height;
            int sourceBottom = Math.max(sourceTop + 1, (offsetY + 1) * spriteHeight / height);
            for (int offsetX = 0; offsetX < width; offsetX++) {
                int sourceLeft = offsetX * spriteWidth / width;
                int sourceRight = Math.max(sourceLeft + 1, (offsetX + 1) * spriteWidth / width);
                target.setPixelRGBA(left + offsetX, top + offsetY,
                        average(source, sourceLeft, sourceTop, sourceRight, sourceBottom));
            }
        }
    }

    private static int sampleStep(int from, int to) {
        int maxSamples = RenderTuning.CELL_MAX_SAMPLES_PER_AXIS;
        return Math.max(1, (to - from + maxSamples - 1) / maxSamples);
    }

    public static int average(PixelSource source, int left, int top, int right, int bottom) {
        int stepX = sampleStep(left, right);
        int stepY = sampleStep(top, bottom);
        long alphaSum = 0;
        long blueSum = 0;
        long greenSum = 0;
        long redSum = 0;
        int samples = 0;

        for (int y = top; y < bottom; y += stepY) {
            for (int x = left; x < right; x += stepX) {
                int pixel = source.at(x, y);
                int alpha = (pixel >>> 24) & 0xFF;
                alphaSum += alpha;
                blueSum += ((pixel >>> 16) & 0xFF) * alpha;
                greenSum += ((pixel >>> 8) & 0xFF) * alpha;
                redSum += (pixel & 0xFF) * alpha;
                samples++;
            }
        }

        if (samples == 0 || alphaSum == 0) return 0;
        return (int) (((alphaSum / samples) << 24)
                | ((blueSum / alphaSum) << 16)
                | ((greenSum / alphaSum) << 8)
                | (redSum / alphaSum));
    }
}
