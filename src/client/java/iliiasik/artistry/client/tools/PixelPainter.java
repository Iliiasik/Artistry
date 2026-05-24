package iliiasik.artistry.client.tools;

import iliiasik.artistry.data.CanvasData;

public class PixelPainter {

    private int selectedBlockIndex = 1;
    private int selectedColor = 0xFFFFFFFF;
    private boolean colorMode = false;
    private int brushSize = 1;
    private DrawingTool tool = DrawingTool.BRUSH;

    private int lastX = -1;
    private int lastY = -1;

    public void setTool(DrawingTool tool) {
        this.tool = tool;
    }

    public DrawingTool getTool() {
        return tool;
    }

    public void setBlock(int paletteIndex) {
        this.selectedBlockIndex = paletteIndex;
        this.colorMode = false;
    }

    public void setColor(int argb) {
        this.selectedColor = argb;
        this.colorMode = true;
    }

    public void setSize(int size) {
        this.brushSize = Math.max(1, Math.min(5, size));
    }

    public int[] getBrushGridBounds(int mx, int my, int areaX, int areaY, double scale, int canvasSize) {
        int cx = worldToGrid(mx, areaX, scale, canvasSize);
        int cy = worldToGrid(my, areaY, scale, canvasSize);
        int half = brushSize / 2;
        int startX = Math.max(0, cx - half);
        int startY = Math.max(0, cy - half);
        int endX   = Math.min(canvasSize - 1, cx - half + brushSize - 1);
        int endY   = Math.min(canvasSize - 1, cy - half + brushSize - 1);
        return new int[]{startX, startY, endX, endY};
    }

    public int[] pickPixel(CanvasData canvas, int mx, int my, int areaX, int areaY, double scale) {
        if (canvas == null) return null;
        int size = canvas.canvasSize > 0 ? canvas.canvasSize : CanvasData.MAX_SIZE;
        int gx = worldToGrid(mx, areaX, scale, size);
        int gy = worldToGrid(my, areaY, scale, size);
        int blockIndex = canvas.pixels[gy][gx];
        int color = canvas.colors[gy][gx];
        return new int[]{blockIndex, color};
    }

    public boolean beginStroke(CanvasData canvas, int mx, int my, int areaX, int areaY, double scale) {
        if (canvas == null) return false;
        if (tool == DrawingTool.PIPETTE) return false;
        int size = canvas.canvasSize > 0 ? canvas.canvasSize : CanvasData.MAX_SIZE;
        lastX = worldToGrid(mx, areaX, scale, size);
        lastY = worldToGrid(my, areaY, scale, size);
        paintBlock(canvas, lastX, lastY, size);
        return true;
    }

    public void continueStroke(CanvasData canvas, int mx, int my, int areaX, int areaY, double scale) {
        if (tool == DrawingTool.PIPETTE) return;
        int size = canvas.canvasSize > 0 ? canvas.canvasSize : CanvasData.MAX_SIZE;
        int gx = worldToGrid(mx, areaX, scale, size);
        int gy = worldToGrid(my, areaY, scale, size);
        if (gx == lastX && gy == lastY) return;
        interpolate(canvas, lastX, lastY, gx, gy, size);
        lastX = gx;
        lastY = gy;
    }

    public void endStroke() {
        lastX = -1;
        lastY = -1;
    }

    private int worldToGrid(int coord, int areaStart, double scale, int size) {
        return Math.max(0, Math.min(size - 1, (int) Math.floor((coord - areaStart) / scale)));
    }

    private void interpolate(CanvasData canvas, int x0, int y0, int x1, int y1, int size) {
        int steps = Math.max(Math.abs(x1 - x0), Math.abs(y1 - y0));
        for (int i = 0; i <= steps; i++) {
            float t = steps == 0 ? 0 : (float) i / steps;
            paintBlock(canvas, Math.round(x0 + t * (x1 - x0)), Math.round(y0 + t * (y1 - y0)), size);
        }
    }

    private void paintBlock(CanvasData canvas, int cx, int cy, int size) {
        int half = brushSize / 2;
        for (int dy = -half; dy < brushSize - half; dy++) {
            for (int dx = -half; dx < brushSize - half; dx++) {
                int px = cx + dx, py = cy + dy;
                if (px < 0 || px >= size || py < 0 || py >= size) continue;
                if (tool == DrawingTool.ERASER) {
                    canvas.pixels[py][px] = 0;
                    canvas.colors[py][px] = 0;
                } else if (colorMode) {
                    canvas.setColor(px, py, selectedColor);
                } else {
                    canvas.pixels[py][px] = (short) selectedBlockIndex;
                    canvas.colors[py][px] = 0;
                }
            }
        }
    }
}