package iliiasik.artistry.client.image;

import iliiasik.artistry.data.CanvasImage;
import iliiasik.artistry.data.CanvasImageLayer;

import java.util.List;
import java.util.UUID;

public class ImageLayerController {

    public enum ResizeHandle { NONE, TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT }

    private final CanvasImageLayer layer;
    private UUID selectedUuid = null;
    private boolean dragging = false;
    private ResizeHandle resizing = ResizeHandle.NONE;

    private int dragOffsetGridX;
    private int dragOffsetGridY;
    private int resizeAnchorGridX;
    private int resizeAnchorGridY;

    private static final int HANDLE_PX = 6;

    public ImageLayerController(CanvasImageLayer layer) {
        this.layer = layer;
    }

    public UUID getSelectedUuid() { return selectedUuid; }

    public boolean hasSelection() { return selectedUuid != null; }

    public void clearSelection() {
        selectedUuid = null;
        dragging = false;
        resizing = ResizeHandle.NONE;
    }

    public boolean trySelect(double mouseX, double mouseY,
                             int drawX, int drawY, int drawSize, int canvasSize) {
        double pixelSize = (double) drawSize / canvasSize;
        List<CanvasImage> images = layer.getImages();

        for (int i = images.size() - 1; i >= 0; i--) {
            CanvasImage img = images.get(i);
            int sx = drawX + (int)(img.gridX * pixelSize);
            int sy = drawY + (int)(img.gridY * pixelSize);
            int sw = (int)(img.gridW * pixelSize);
            int sh = (int)(img.gridH * pixelSize);

            if (mouseX >= sx && mouseX < sx + sw && mouseY >= sy && mouseY < sy + sh) {
                selectedUuid = img.uuid;
                layer.moveToTop(img.uuid);

                ResizeHandle handle = detectHandle(mouseX, mouseY, sx, sy, sw, sh);
                if (handle != ResizeHandle.NONE) {
                    resizing = handle;
                    dragging = false;
                    CanvasImage sel = layer.findByUuid(selectedUuid);
                    if (sel != null) {
                        resizeAnchorGridX = (handle == ResizeHandle.TOP_LEFT || handle == ResizeHandle.BOTTOM_LEFT)
                                ? sel.gridX + sel.gridW : sel.gridX;
                        resizeAnchorGridY = (handle == ResizeHandle.TOP_LEFT || handle == ResizeHandle.TOP_RIGHT)
                                ? sel.gridY + sel.gridH : sel.gridY;
                    }
                } else {
                    dragging = true;
                    resizing = ResizeHandle.NONE;
                    CanvasImage sel = layer.findByUuid(selectedUuid);
                    if (sel != null) {
                        int gx = screenToGrid(mouseX, drawX, pixelSize);
                        int gy = screenToGrid(mouseY, drawY, pixelSize);
                        dragOffsetGridX = gx - sel.gridX;
                        dragOffsetGridY = gy - sel.gridY;
                    }
                }
                return true;
            }
        }
        return false;
    }

    public boolean onDrag(double mouseX, double mouseY,
                          int drawX, int drawY, int drawSize, int canvasSize,
                          Runnable onChanged) {
        if (selectedUuid == null) return false;
        CanvasImage img = layer.findByUuid(selectedUuid);
        if (img == null) return false;

        double pixelSize = (double) drawSize / canvasSize;
        int gx = screenToGrid(mouseX, drawX, pixelSize);
        int gy = screenToGrid(mouseY, drawY, pixelSize);

        if (dragging) {
            img.gridX = Math.max(0, Math.min(canvasSize - img.gridW, gx - dragOffsetGridX));
            img.gridY = Math.max(0, Math.min(canvasSize - img.gridH, gy - dragOffsetGridY));
            onChanged.run();
            return true;
        }

        if (resizing != ResizeHandle.NONE) {
            int newX, newY, newW, newH;
            int ancX = resizeAnchorGridX;
            int ancY = resizeAnchorGridY;

            int lo = Math.min(gx, ancX);
            int hi = Math.max(gx, ancX);
            newX = Math.max(0, lo);
            newW = Math.max(CanvasImage.MIN_GRID, Math.min(canvasSize - newX, hi - lo + 1));

            int loY = Math.min(gy, ancY);
            int hiY = Math.max(gy, ancY);
            newY = Math.max(0, loY);
            newH = Math.max(CanvasImage.MIN_GRID, Math.min(canvasSize - newY, hiY - loY + 1));

            img.gridX = newX;
            img.gridY = newY;
            img.gridW = newW;
            img.gridH = newH;
            onChanged.run();
            return true;
        }

        return false;
    }

    public void endDrag() {
        dragging = false;
        resizing = ResizeHandle.NONE;
    }

    public boolean isOnImage(double mouseX, double mouseY,
                             int drawX, int drawY, int drawSize, int canvasSize) {
        double pixelSize = (double) drawSize / canvasSize;
        for (CanvasImage img : layer.getImages()) {
            int sx = drawX + (int)(img.gridX * pixelSize);
            int sy = drawY + (int)(img.gridY * pixelSize);
            int sw = (int)(img.gridW * pixelSize);
            int sh = (int)(img.gridH * pixelSize);
            if (mouseX >= sx && mouseX < sx + sw && mouseY >= sy && mouseY < sy + sh) return true;
        }
        return false;
    }

    private ResizeHandle detectHandle(double mx, double my, int sx, int sy, int sw, int sh) {
        boolean left  = mx < sx + HANDLE_PX;
        boolean right = mx >= sx + sw - HANDLE_PX;
        boolean top   = my < sy + HANDLE_PX;
        boolean bot   = my >= sy + sh - HANDLE_PX;
        if (top && left)  return ResizeHandle.TOP_LEFT;
        if (top && right) return ResizeHandle.TOP_RIGHT;
        if (bot && left)  return ResizeHandle.BOTTOM_LEFT;
        if (bot && right) return ResizeHandle.BOTTOM_RIGHT;
        return ResizeHandle.NONE;
    }

    private int screenToGrid(double screen, int drawOrigin, double pixelSize) {
        return (int)((screen - drawOrigin) / pixelSize);
    }
}