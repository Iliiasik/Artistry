package iliiasik.artistry.client.tools;

import iliiasik.artistry.data.CanvasData;

public class PixelPainter {

    private int selectedBlockIndex = 1;
    private int brushSize = 1;
    private boolean erasing = false;

    private int lastX = -1;
    private int lastY = -1;

    public void setTool(DrawingTool tool) {
        this.erasing = (tool == DrawingTool.ERASER);
    }

    public void setBlock(int paletteIndex) {
        this.selectedBlockIndex = paletteIndex;
    }

    public void setSize(int size) {
        this.brushSize = Math.max(1, Math.min(5, size));
    }

    public boolean beginStroke(CanvasData canvas, int mx, int my, int areaX, int areaY, double scale) {
        if (canvas == null) return false;
        int gx = worldToGrid(mx, areaX, scale);
        int gy = worldToGrid(my, areaY, scale);
        lastX = gx;
        lastY = gy;
        paintBlock(canvas, gx, gy);
        return true;
    }

    public void continueStroke(CanvasData canvas, int mx, int my, int areaX, int areaY, double scale) {
        int gx = worldToGrid(mx, areaX, scale);
        int gy = worldToGrid(my, areaY, scale);
        if (gx == lastX && gy == lastY) return;
        interpolate(canvas, lastX, lastY, gx, gy);
        lastX = gx;
        lastY = gy;
    }

    public void endStroke() {
        lastX = -1;
        lastY = -1;
    }

    private int worldToGrid(int mouseCoord, int areaStart, double scale) {
        int g = (int) Math.floor((mouseCoord - areaStart) / scale);
        return Math.max(0, Math.min(CanvasData.SIZE - 1, g));
    }

    private void interpolate(CanvasData canvas, int x0, int y0, int x1, int y1) {
        int dx = Math.abs(x1 - x0), dy = Math.abs(y1 - y0);
        int steps = Math.max(dx, dy);
        for (int i = 0; i <= steps; i++) {
            float t = steps == 0 ? 0 : (float) i / steps;
            int ix = Math.round(x0 + t * (x1 - x0));
            int iy = Math.round(y0 + t * (y1 - y0));
            paintBlock(canvas, ix, iy);
        }
    }

    private void paintBlock(CanvasData canvas, int cx, int cy) {
        int half = brushSize / 2;
        for (int dy = -half; dy < brushSize - half; dy++) {
            for (int dx = -half; dx < brushSize - half; dx++) {
                int px = cx + dx;
                int py = cy + dy;
                if (px < 0 || px >= CanvasData.SIZE || py < 0 || py >= CanvasData.SIZE) continue;
                canvas.pixels[py][px] = erasing ? 0 : (short) selectedBlockIndex;
            }
        }
    }
}