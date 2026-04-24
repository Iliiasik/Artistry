package iliiasik.artistry.client.renderer;

import iliiasik.artistry.client.palette.BlockPalette;
import iliiasik.artistry.data.CanvasData;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.Sprite;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

public final class CanvasCellPainter {

    private static Field mipmapField = null;
    private static boolean fieldResolved = false;
    private static final Map<Sprite, NativeImage> IMAGE_CACHE = new HashMap<>();

    private CanvasCellPainter() {}

    private static NativeImage getSpriteImage(Sprite sp) {
        NativeImage cached = IMAGE_CACHE.get(sp);
        if (cached != null) return cached;

        if (!fieldResolved) {
            fieldResolved = true;
            try {
                Field f = sp.getContents().getClass().getDeclaredField("mipmapLevelsImages");
                f.setAccessible(true);
                mipmapField = f;
            } catch (NoSuchFieldException e) {
                for (Field f : sp.getContents().getClass().getDeclaredFields()) {
                    f.setAccessible(true);
                    try {
                        if (f.get(sp.getContents()) instanceof NativeImage[]) {
                            mipmapField = f;
                            break;
                        }
                    } catch (Exception ignored) {}
                }
            }
        }
        if (mipmapField == null) return null;
        try {
            NativeImage[] imgs = (NativeImage[]) mipmapField.get(sp.getContents());
            NativeImage img = (imgs != null && imgs.length > 0) ? imgs[0] : null;
            if (img != null) IMAGE_CACHE.put(sp, img);
            return img;
        } catch (Exception e) {
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
        Sprite sp = BlockPalette.getSprite(idx);
        if (sp == null) {
            fillCell(image, cx, cy, cell, bgColor);
            return;
        }
        NativeImage src = getSpriteImage(sp);
        if (src == null) {
            fillCell(image, cx, cy, cell, bgColor);
            return;
        }
        blitSprite(image, src, sp.getContents().getWidth(), sp.getContents().getHeight(), cx, cy, cell);
    }

    public static void fillCell(NativeImage image, int cx, int cy, int cell, int argb) {
        for (int py = 0; py < cell; py++)
            for (int px = 0; px < cell; px++)
                image.setColorArgb(cx + px, cy + py, argb);
    }

    private static void blitSprite(NativeImage dst, NativeImage src, int sprW, int sprH,
                                   int cellX, int cellY, int cell) {
        for (int py = 0; py < cell; py++) {
            for (int px = 0; px < cell; px++) {
                int sx = Math.min(px * sprW / cell, sprW - 1);
                int sy = Math.min(py * sprH / cell, sprH - 1);
                dst.setColorArgb(cellX + px, cellY + py, src.getColorArgb(sx, sy));
            }
        }
    }
}