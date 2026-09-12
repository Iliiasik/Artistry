package iliiasik.artistry.client.tools;

import iliiasik.artistry.client.palette.PaintSwatch;
import iliiasik.artistry.client.palette.PaintSwatches;
import iliiasik.artistry.data.CanvasData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PixelPainterTest {

    private static final int SIZE = 16;
    private static final double SCALE = 1.0;

    private CanvasData canvas;
    private PaintSwatches swatches;
    private PixelPainter painter;

    @BeforeEach
    void setUp() {
        canvas = new CanvasData();
        canvas.canvasSize = SIZE;
        swatches = new PaintSwatches();
        painter = new PixelPainter(swatches);
    }

    @Test
    @DisplayName("The brush is the default tool")
    void brushIsDefault() {
        assertEquals(DrawingTool.BRUSH, painter.getTool());
    }

    @Test
    @DisplayName("Every tool can be selected")
    void everyToolCanBeSelected() {
        for (DrawingTool tool : DrawingTool.values()) {
            painter.setTool(tool);
            assertEquals(tool, painter.getTool());
        }
    }

    @Test
    @DisplayName("The brush paints the selected block index")
    void brushPaintsBlock() {
        swatches.select(PaintSwatch.ofBlock(42), false);
        assertTrue(painter.beginStroke(canvas, 5, 6, 0, 0, SCALE, false));

        assertEquals((short) 42, canvas.pixels[6][5]);
        assertEquals(0, canvas.colors[6][5]);
    }

    @Test
    @DisplayName("The brush in colour mode paints a raw colour")
    void brushPaintsColor() {
        swatches.select(PaintSwatch.ofColor(0xFF123456), false);
        painter.beginStroke(canvas, 2, 3, 0, 0, SCALE, false);

        assertEquals(CanvasData.COLOR_PIXEL, canvas.pixels[3][2]);
        assertEquals(0xFF123456, canvas.colors[3][2]);
    }

    @Test
    @DisplayName("Selecting a block leaves colour mode")
    void blockSelectionLeavesColorMode() {
        swatches.select(PaintSwatch.ofColor(0xFFFF0000), false);
        swatches.select(PaintSwatch.ofBlock(7), false);
        painter.beginStroke(canvas, 1, 1, 0, 0, SCALE, false);

        assertEquals((short) 7, canvas.pixels[1][1]);
        assertEquals(0, canvas.colors[1][1]);
    }

    @Test
    @DisplayName("The eraser clears both the block and the colour")
    void eraserClearsPixel() {
        swatches.select(PaintSwatch.ofColor(0xFFABCDEF), false);
        painter.beginStroke(canvas, 4, 4, 0, 0, SCALE, false);
        painter.endStroke();

        painter.setTool(DrawingTool.ERASER);
        painter.beginStroke(canvas, 4, 4, 0, 0, SCALE, false);

        assertEquals(0, canvas.pixels[4][4]);
        assertEquals(0, canvas.colors[4][4]);
    }

    @Test
    @DisplayName("The pipette never paints and never starts a stroke")
    void pipetteDoesNotPaint() {
        swatches.select(PaintSwatch.ofBlock(9), false);
        painter.setTool(DrawingTool.PIPETTE);

        assertFalse(painter.beginStroke(canvas, 3, 3, 0, 0, SCALE, false));
        assertEquals(0, canvas.pixels[3][3]);

        painter.continueStroke(canvas, 8, 8, 0, 0, SCALE);
        assertEquals(0, canvas.pixels[8][8]);
    }

    @Test
    @DisplayName("The pipette reads the block and the colour under the cursor")
    void pipetteReadsPixel() {
        canvas.pixels[7][6] = 13;
        canvas.colors[7][6] = 0xFF00FF00;

        painter.setTool(DrawingTool.PIPETTE);
        int[] picked = painter.pickPixel(canvas, 6, 7, 0, 0, SCALE);

        assertArrayEquals(new int[]{13, 0xFF00FF00}, picked);
    }

    @Test
    @DisplayName("The pipette on a null canvas returns nothing")
    void pipetteOnNullCanvas() {
        assertNull(painter.pickPixel(null, 0, 0, 0, 0, SCALE));
    }

    @Test
    @DisplayName("Brush size is clamped to the supported range")
    void brushSizeIsClamped() {
        swatches.select(PaintSwatch.ofBlock(1), false);

        painter.setSize(3);
        assertEquals(3, painter.getSize(), "the brush must report the size it was given");
        painter.setSize(0);
        painter.beginStroke(canvas, 8, 8, 0, 0, SCALE, false);
        assertEquals(0, canvas.pixels[7][7], "a size of zero must behave like a single pixel");

        painter.setSize(99);
        int[] bounds = painter.getBrushGridBounds(8, 8, 0, 0, SCALE, SIZE);
        assertEquals(5, bounds[2] - bounds[0] + 1, "the brush must not grow past five cells");
        assertEquals(5, painter.getSize(), "an oversized brush reports the clamped size");
        assertEquals(1, new PixelPainter(swatches).getSize(), "a fresh brush starts at one cell");
    }

    @Test
    @DisplayName("A three cell brush paints a three by three square")
    void brushSizePaintsSquare() {
        swatches.select(PaintSwatch.ofBlock(5), false);
        painter.setSize(3);
        painter.beginStroke(canvas, 8, 8, 0, 0, SCALE, false);

        for (int y = 7; y <= 9; y++) {
            for (int x = 7; x <= 9; x++) {
                assertEquals((short) 5, canvas.pixels[y][x], "cell " + x + "," + y);
            }
        }
        assertEquals(0, canvas.pixels[6][8]);
        assertEquals(0, canvas.pixels[8][6]);
    }

    @Test
    @DisplayName("A stroke interpolates between the sampled points")
    void strokeInterpolates() {
        swatches.select(PaintSwatch.ofBlock(3), false);
        painter.beginStroke(canvas, 0, 0, 0, 0, SCALE, false);
        painter.continueStroke(canvas, 5, 0, 0, 0, SCALE);

        for (int x = 0; x <= 5; x++) {
            assertEquals((short) 3, canvas.pixels[0][x], "cell " + x);
        }
    }

    @Test
    @DisplayName("A diagonal stroke leaves no gaps")
    void diagonalStrokeHasNoGaps() {
        swatches.select(PaintSwatch.ofBlock(4), false);
        painter.beginStroke(canvas, 0, 0, 0, 0, SCALE, false);
        painter.continueStroke(canvas, 6, 6, 0, 0, SCALE);

        for (int i = 0; i <= 6; i++) {
            assertEquals((short) 4, canvas.pixels[i][i], "cell " + i);
        }
    }

    @Test
    @DisplayName("Repeating the same cell does not restart the stroke")
    void repeatedCellIsIgnored() {
        swatches.select(PaintSwatch.ofBlock(2), false);
        painter.beginStroke(canvas, 4, 4, 0, 0, SCALE, false);
        painter.continueStroke(canvas, 4, 4, 0, 0, SCALE);

        assertEquals((short) 2, canvas.pixels[4][4]);
    }

    @Test
    @DisplayName("Painting outside the canvas is clamped instead of throwing")
    void paintingOutsideIsClamped() {
        swatches.select(PaintSwatch.ofBlock(6), false);
        painter.setSize(5);

        painter.beginStroke(canvas, -50, -50, 0, 0, SCALE, false);
        assertEquals((short) 6, canvas.pixels[0][0]);

        painter.endStroke();
        painter.beginStroke(canvas, 1000, 1000, 0, 0, SCALE, false);
        assertEquals((short) 6, canvas.pixels[SIZE - 1][SIZE - 1]);
    }

    @Test
    @DisplayName("Brush bounds stay inside the canvas")
    void brushBoundsStayInside() {
        painter.setSize(5);

        int[] topLeft = painter.getBrushGridBounds(0, 0, 0, 0, SCALE, SIZE);
        assertEquals(0, topLeft[0]);
        assertEquals(0, topLeft[1]);

        int[] bottomRight = painter.getBrushGridBounds(SIZE - 1, SIZE - 1, 0, 0, SCALE, SIZE);
        assertEquals(SIZE - 1, bottomRight[2]);
        assertEquals(SIZE - 1, bottomRight[3]);
    }

    @Test
    @DisplayName("The area offset and scale are taken into account")
    void areaOffsetAndScaleAreApplied() {
        swatches.select(PaintSwatch.ofBlock(8), false);
        painter.beginStroke(canvas, 100 + 24, 200 + 36, 100, 200, 12.0, false);

        assertEquals((short) 8, canvas.pixels[3][2]);
    }

    @Test
    @DisplayName("A canvas without a chosen size falls back to the maximum grid")
    void canvasWithoutSizeUsesMaximum() {
        CanvasData unsized = new CanvasData();
        swatches.select(PaintSwatch.ofBlock(1), false);
        painter.beginStroke(unsized, CanvasData.MAX_SIZE - 1, CanvasData.MAX_SIZE - 1, 0, 0, SCALE, false);

        assertEquals((short) 1, unsized.pixels[CanvasData.MAX_SIZE - 1][CanvasData.MAX_SIZE - 1]);
    }

    @Test
    @DisplayName("A stroke on a null canvas is refused")
    void strokeOnNullCanvasIsRefused() {
        assertFalse(painter.beginStroke(null, 0, 0, 0, 0, SCALE, false));
    }
}
