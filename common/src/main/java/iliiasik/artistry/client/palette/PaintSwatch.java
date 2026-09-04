package iliiasik.artistry.client.palette;

import iliiasik.artistry.data.CanvasData;

public record PaintSwatch(short blockIndex, int color) {

    public static PaintSwatch ofColor(int argb) {
        return new PaintSwatch(CanvasData.COLOR_PIXEL, argb);
    }

    public static PaintSwatch ofBlock(int index) {
        return new PaintSwatch((short) index, 0);
    }

    public boolean isColor() {
        return blockIndex == CanvasData.COLOR_PIXEL;
    }
}
