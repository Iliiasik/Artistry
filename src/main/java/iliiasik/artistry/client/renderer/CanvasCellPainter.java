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

    private static NativeImage resolveSpriteImage(TextureAtlasSprite sp) {
        if (!fieldResolved) {
            fieldResolved = true;
            try {
                Field f = sp.contents().getClass().getDeclaredField("byMipLevel");
                f.setAccessible(true);
                mipmapField = f;
            } catch (NoSuchFieldException e) {
                for (Field f : sp.contents().getClass().getDeclaredFields()) {
                    f.setAccessible(true);
                    try {
                        if (f.get(sp.contents()) instanceof NativeImage[]) {
                            mipmapField = f;
                            break;
                        }
                    } catch (Exception ignored) {}
                }
            }
        }
        if (mipmapField == null) return null;
        try {
            NativeImage[] imgs = (NativeImage[]) mipmapField.get(sp.contents());
            return (imgs != null && imgs.length > 0) ? imgs[0] : null;
        } catch (Exception e) {
            return null;
        }
    }

    private static BakedSprite bake(TextureAtlasSprite sp) {
        BakedSprite cached = BAKED_CACHE.get(sp);
        if (cached != null) return cached;

        NativeImage src = resolveSpriteImage(sp);
        if (src == null) return null;

        try {
            int w = Math.min(sp.contents().width(), src.getWidth());
            int h = Math.min(sp.contents().height(), src.getHeight());
            if (w <= 0 || h <= 0) return null;
            int[] pixels = new int[w * h];
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    pixels[y * w + x] = src.getPixelRGBA(x, y);
                }
            }
            BakedSprite baked = new BakedSprite(w, h, pixels);
            BAKED_CACHE.put(sp, baked);
            return baked;
        } catch (RuntimeException e) {
            return null;
        }
    }

    public static void paintCell(NativeImage image, int cx, int cy, int cell,
                                 short idx, int col, int bgColor) {
        if (idx == CanvasData.COLOR_PIXEL) {
            fillCell(image, cx, cy, cell, col);
            return;
        }
        if (idx <= 0) {
            fillCell(image, cx, cy, cell, bgColor);
            return;
        }
        TextureAtlasSprite sp = BlockPalette.getSprite(idx);
        if (sp == null) {
            fillCell(image, cx, cy, cell, bgColor);
            return;
        }
        BakedSprite baked = bake(sp);
        if (baked == null) {
            fillCell(image, cx, cy, cell, bgColor);
            return;
        }
        blitSprite(image, baked, cx, cy, cell);
    }

    public static void fillCell(NativeImage image, int cx, int cy, int cell, int argb) {
        int abgr = ColorPalette.argbToAbgr(argb);
        for (int py = 0; py < cell; py++)
            for (int px = 0; px < cell; px++)
                image.setPixelRGBA(cx + px, cy + py, abgr);
    }

    private static void blitSprite(NativeImage dst, BakedSprite src,
                                   int cellX, int cellY, int cell) {
        int sprW = src.width();
        int sprH = src.height();
        int[] pixels = src.pixels();
        for (int py = 0; py < cell; py++) {
            int sy = Math.min(py * sprH / cell, sprH - 1);
            for (int px = 0; px < cell; px++) {
                int sx = Math.min(px * sprW / cell, sprW - 1);
                dst.setPixelRGBA(cellX + px, cellY + py, pixels[sy * sprW + sx]);
            }
        }
    }
}
