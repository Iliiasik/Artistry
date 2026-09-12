package iliiasik.artistry.client.image;

import iliiasik.artistry.data.CanvasImage;
import iliiasik.artistry.data.CanvasImageLayer;
import net.minecraft.util.Mth;

import java.util.List;
import java.util.UUID;

public class ImageLayerController {

    public enum ResizeHandle { NONE, TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT }

    private static final int HANDLE_SCREEN_PIXELS = 6;

    private final CanvasImageLayer layer;

    private UUID selectedUuid = null;
    private boolean moving = false;
    private ResizeHandle resizing = ResizeHandle.NONE;

    private double grabOffsetCellsX;
    private double grabOffsetCellsY;
    private int anchorEdgeX;
    private int anchorEdgeY;

    public ImageLayerController(CanvasImageLayer layer) {
        this.layer = layer;
    }

    public UUID getSelectedUuid() { return selectedUuid; }

    public void clearSelection() {
        selectedUuid = null;
        moving = false;
        resizing = ResizeHandle.NONE;
    }

    public boolean trySelect(double mouseX, double mouseY,
                             int drawX, int drawY, int drawSize, int canvasSize) {
        double cellScreenSize = (double) drawSize / canvasSize;
        List<CanvasImage> images = layer.getImages();

        for (int index = images.size() - 1; index >= 0; index--) {
            CanvasImage image = images.get(index);
            if (image.isLocked() && !image.uuid.equals(selectedUuid)) continue;

            ScreenRect rect = screenRect(image, drawX, drawY, cellScreenSize);
            if (!rect.contains(mouseX, mouseY)) continue;

            selectedUuid = image.uuid;
            layer.moveToTop(image.uuid);

            ResizeHandle handle = detectHandle(mouseX, mouseY, rect);
            if (handle == ResizeHandle.NONE) {
                moving = true;
                resizing = ResizeHandle.NONE;
                grabOffsetCellsX = screenToCanvas(mouseX, drawX, cellScreenSize) - image.gridX;
                grabOffsetCellsY = screenToCanvas(mouseY, drawY, cellScreenSize) - image.gridY;
            } else {
                moving = false;
                resizing = handle;
                anchorEdgeX = grabsLeftEdge(handle) ? image.gridX + image.gridW : image.gridX;
                anchorEdgeY = grabsTopEdge(handle) ? image.gridY + image.gridH : image.gridY;
            }
            return true;
        }
        return false;
    }

    public void onDrag(double mouseX, double mouseY,
                       int drawX, int drawY, int drawSize, int canvasSize,
                       Runnable onChanged) {
        if (selectedUuid == null) return;
        CanvasImage image = layer.findByUuid(selectedUuid);
        if (image == null) return;

        double cellScreenSize = (double) drawSize / canvasSize;
        double pointerCellsX = screenToCanvas(mouseX, drawX, cellScreenSize);
        double pointerCellsY = screenToCanvas(mouseY, drawY, cellScreenSize);

        if (moving) {
            image.gridX = clampOrigin(Math.round(pointerCellsX - grabOffsetCellsX), image.gridW, canvasSize);
            image.gridY = clampOrigin(Math.round(pointerCellsY - grabOffsetCellsY), image.gridH, canvasSize);
            onChanged.run();
            return;
        }

        if (resizing == ResizeHandle.NONE) return;

        Span horizontal = resizeSpan(pointerCellsX, anchorEdgeX, grabsLeftEdge(resizing), canvasSize);
        Span vertical = resizeSpan(pointerCellsY, anchorEdgeY, grabsTopEdge(resizing), canvasSize);

        image.gridX = horizontal.origin();
        image.gridW = horizontal.length();
        image.gridY = vertical.origin();
        image.gridH = vertical.length();
        onChanged.run();
    }

    public boolean nudge(int stepX, int stepY, int canvasSize) {
        if (selectedUuid == null) return false;
        CanvasImage image = layer.findByUuid(selectedUuid);
        if (image == null) return false;

        int movedX = clampOrigin(image.gridX + stepX, image.gridW, canvasSize);
        int movedY = clampOrigin(image.gridY + stepY, image.gridH, canvasSize);
        if (movedX == image.gridX && movedY == image.gridY) return false;

        image.gridX = movedX;
        image.gridY = movedY;
        return true;
    }

    public void endDrag() {
        moving = false;
        resizing = ResizeHandle.NONE;
    }

    public boolean isOnImage(double mouseX, double mouseY,
                             int drawX, int drawY, int drawSize, int canvasSize) {
        double cellScreenSize = (double) drawSize / canvasSize;
        for (CanvasImage image : layer.getImages()) {
            if (screenRect(image, drawX, drawY, cellScreenSize).contains(mouseX, mouseY)) return true;
        }
        return false;
    }

    private record Span(int origin, int length) {}

    private record ScreenRect(int x, int y, int width, int height) {
        boolean contains(double mouseX, double mouseY) {
            return mouseX >= x && mouseX < x + width
                    && mouseY >= y && mouseY < y + height;
        }
    }

    private static ScreenRect screenRect(CanvasImage image, int drawX, int drawY, double cellScreenSize) {
        return new ScreenRect(
                drawX + (int) (image.gridX * cellScreenSize),
                drawY + (int) (image.gridY * cellScreenSize),
                (int) (image.gridW * cellScreenSize),
                (int) (image.gridH * cellScreenSize));
    }

    private static Span resizeSpan(double pointerCells, int anchorEdge,
                                   boolean movingEdgeIsLeading, int canvasSize) {
        int minimumLength = Math.min(CanvasImage.MIN_GRID, canvasSize);
        int pointerEdge = clampEdge(Math.round(pointerCells), canvasSize);
        int fixedEdge = clampEdge(anchorEdge, canvasSize);

        int startEdge;
        int endEdge;
        if (movingEdgeIsLeading) {
            endEdge = Math.max(fixedEdge, minimumLength);
            startEdge = Math.min(pointerEdge, endEdge - minimumLength);
        } else {
            startEdge = Math.min(fixedEdge, canvasSize - minimumLength);
            endEdge = Math.max(pointerEdge, startEdge + minimumLength);
        }

        int length = endEdge - startEdge;
        return new Span(clampOrigin(startEdge, length, canvasSize), length);
    }

    private static ResizeHandle detectHandle(double mouseX, double mouseY, ScreenRect rect) {
        boolean nearLeft = mouseX < rect.x() + HANDLE_SCREEN_PIXELS;
        boolean nearRight = mouseX >= rect.x() + rect.width() - HANDLE_SCREEN_PIXELS;
        boolean nearTop = mouseY < rect.y() + HANDLE_SCREEN_PIXELS;
        boolean nearBottom = mouseY >= rect.y() + rect.height() - HANDLE_SCREEN_PIXELS;

        if (nearTop && nearLeft) return ResizeHandle.TOP_LEFT;
        if (nearTop && nearRight) return ResizeHandle.TOP_RIGHT;
        if (nearBottom && nearLeft) return ResizeHandle.BOTTOM_LEFT;
        if (nearBottom && nearRight) return ResizeHandle.BOTTOM_RIGHT;
        return ResizeHandle.NONE;
    }

    private static boolean grabsLeftEdge(ResizeHandle handle) {
        return handle == ResizeHandle.TOP_LEFT || handle == ResizeHandle.BOTTOM_LEFT;
    }

    private static boolean grabsTopEdge(ResizeHandle handle) {
        return handle == ResizeHandle.TOP_LEFT || handle == ResizeHandle.TOP_RIGHT;
    }

    private static double screenToCanvas(double screen, int drawOrigin, double cellScreenSize) {
        return (screen - drawOrigin) / cellScreenSize;
    }

    private static int clampEdge(long edge, int canvasSize) {
        return (int) Mth.clamp(edge, 0L, (long) canvasSize);
    }

    private static int clampOrigin(long origin, int length, int canvasSize) {
        return (int) Mth.clamp(origin, 0L, (long) Math.max(0, canvasSize - length));
    }
}
