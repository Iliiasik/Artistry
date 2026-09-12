package iliiasik.artistry.client.renderer;

import com.mojang.blaze3d.platform.NativeImage;
import iliiasik.artistry.client.image.ClientImageCache;
import iliiasik.artistry.client.renderer.ImageOcclusionClipper.VisibleFragment;
import iliiasik.artistry.data.CanvasData;
import iliiasik.artistry.data.CanvasImage;
import net.minecraft.util.Mth;

import java.util.List;

public final class CanvasComposer {

    public static final int BG_COLOR = 0xFFFDF7E8;

    private static final int CUTOUT_ALPHA = 26;

    private CanvasComposer() {}

    public static void fillBackground(NativeImage target, int originX, int originY, int spanPixels) {
        CanvasCellPainter.fillRect(target, originX, originY,
                originX + spanPixels, originY + spanPixels, BG_COLOR);
    }

    public static void composeCells(NativeImage target, int originX, int originY,
                                    int spanPixels, CanvasData data) {
        if (data == null || !data.isSizeChosen()) {
            fillBackground(target, originX, originY, spanPixels);
            return;
        }
        int cellCount = data.canvasSize;
        for (int cellY = 0; cellY < cellCount; cellY++) {
            int top = originY + CanvasCellPainter.edge(cellY, cellCount, spanPixels);
            int bottom = originY + CanvasCellPainter.edge(cellY + 1, cellCount, spanPixels);
            for (int cellX = 0; cellX < cellCount; cellX++) {
                CanvasCellPainter.paintCell(target,
                        originX + CanvasCellPainter.edge(cellX, cellCount, spanPixels), top,
                        originX + CanvasCellPainter.edge(cellX + 1, cellCount, spanPixels), bottom,
                        data.pixels[cellY][cellX], data.colors[cellY][cellX], BG_COLOR);
            }
        }
    }

    public static boolean composeFragments(NativeImage target, int originX, int originY, int spanPixels,
                                           int cellCount, List<VisibleFragment> fragments) {
        boolean complete = true;
        for (VisibleFragment fragment : fragments) {
            CanvasImage image = fragment.image();
            NativeImage source = ClientImageCache.getBakeSource(
                    image.uuid, image.pixelized, image.gridW, image.gridH);
            if (source == null) {
                complete = false;
                continue;
            }
            blitFragment(target, originX, originY, spanPixels, cellCount, fragment, source);
        }
        return complete;
    }

    private static void blitFragment(NativeImage target, int originX, int originY, int spanPixels,
                                     int cellCount, VisibleFragment fragment, NativeImage source) {
        int destinationLeft = originX + cellEdge(fragment.destRect().x0(), cellCount, spanPixels);
        int destinationRight = originX + cellEdge(fragment.destRect().x1(), cellCount, spanPixels);
        int destinationTop = originY + cellEdge(fragment.destRect().y0(), cellCount, spanPixels);
        int destinationBottom = originY + cellEdge(fragment.destRect().y1(), cellCount, spanPixels);

        int width = destinationRight - destinationLeft;
        int height = destinationBottom - destinationTop;
        if (width <= 0 || height <= 0) return;

        int sourceWidth = source.getWidth();
        int sourceHeight = source.getHeight();
        CanvasCellPainter.PixelSource pixels = CanvasCellPainter.sourceOf(source);
        float sourceLeft = fragment.u0() * sourceWidth;
        float sourceRight = fragment.u1() * sourceWidth;
        float sourceTop = fragment.v0() * sourceHeight;
        float sourceBottom = fragment.v1() * sourceHeight;

        for (int offsetY = 0; offsetY < height; offsetY++) {
            int sampleTop = bound(sourceTop + (sourceBottom - sourceTop) * offsetY / height,
                    0, sourceHeight - 1);
            int sampleBottom = bound(sourceTop + (sourceBottom - sourceTop) * (offsetY + 1) / height,
                    sampleTop + 1, sourceHeight);
            for (int offsetX = 0; offsetX < width; offsetX++) {
                int sampleLeft = bound(sourceLeft + (sourceRight - sourceLeft) * offsetX / width,
                        0, sourceWidth - 1);
                int sampleRight = bound(sourceLeft + (sourceRight - sourceLeft) * (offsetX + 1) / width,
                        sampleLeft + 1, sourceWidth);
                int pixel = CanvasCellPainter.average(pixels,
                        sampleLeft, sampleTop, sampleRight, sampleBottom);
                if (((pixel >>> 24) & 0xFF) < CUTOUT_ALPHA) continue;
                target.setPixelRGBA(destinationLeft + offsetX, destinationTop + offsetY,
                        pixel | 0xFF000000);
            }
        }
    }

    private static int cellEdge(int cellIndex, int cellCount, int spanPixels) {
        return CanvasCellPainter.edge(Mth.clamp(cellIndex, 0, cellCount), cellCount, spanPixels);
    }

    private static int bound(float value, int min, int max) {
        return Mth.clamp((int) value, min, max);
    }
}
