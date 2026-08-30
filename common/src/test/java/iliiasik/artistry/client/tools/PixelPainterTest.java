package iliiasik.artistry.client.tools;

import iliiasik.artistry.data.CanvasData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PixelPainterTest {

    private static final int SIZE = 16;
    private static final double SCALE = 1.0;

    private CanvasData canvas;
    private PixelPainter painter;

    @BeforeEach
    void setUp() {
        canvas = new CanvasData();
        canvas.canvasSize = SIZE;
        painter = new PixelPainter();
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
        painter.setBlock(42);
        assertTrue(painter.beginStroke(canvas, 5, 6, 0, 0, SCALE));

        assertEquals((short) 42, canvas.pixels[6][5]);
        assertEquals(0, canvas.colors[6][5]);
    }

    @Test
    @DisplayName("The brush in colour mode paints a raw colour")
    void brushPaintsColor() {
        painter.setColor(0xFF123456);
        painter.beginStroke(canvas, 2, 3, 0, 0, SCALE);

        assertEquals(CanvasData.COLOR_PIXEL, canvas.pixels[3][2]);
        assertEquals(0xFF123456, canvas.colors[3][2]);
    }

    @Test
    @DisplayName("Selecting a block leaves colour mode")
    void blockSelectionLeavesColorMode() {
        painter.setColor(0xFFFF0000);
        painter.setBlock(7);
        painter.beginStroke(canvas, 1, 1, 0, 0, SCALE);

        assertEquals((short) 7, canvas.pixels[1][1]);
        assertEquals(0, canvas.colors[1][1]);
    }

    @Test
    @DisplayName("The eraser clears both the block and the colour")
    void eraserClearsPixel() {
        painter.setColor(0xFFABCDEF);
        painter.beginStroke(canvas, 4, 4, 0, 0, SCALE);
        painter.endStroke();

        painter.setTool(DrawingTool.ERASER);
        painter.beginStroke(canvas, 4, 4, 0, 0, SCALE);

        assertEquals(0, canvas.pixels[4][4]);
        assertEquals(0, canvas.colors[4][4]);
    }

    @Test
    @DisplayName("The pipette never paints and never starts a stroke")
    void pipetteDoesNotPaint() {
        painter.setBlock(9);
        painter.setTool(DrawingTool.PIPETTE);

        assertFalse(painter.beginStroke(canvas, 3, 3, 0, 0, SCALE));
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
        assertEquals(null, painter.pickPixel(null, 0, 0, 0, 0, SCALE));
    }

    @Test
    @DisplayName("Brush size is clamped to the supported range")
    void brushSizeIsClamped() {
        painter.setBlock(1);

        painter.setSize(0);
        painter.beginStroke(canvas, 8, 8, 0, 0, SCALE);
        assertEquals(0, canvas.pixels[7][7], "a size of zero must behave like a single pixel");

        painter.setSize(99);
        int[] bounds = painter.getBrushGridBounds(8, 8, 0, 0, SCALE, SIZE);
        assertEquals(5, bounds[2] - bounds[0] + 1, "the brush must not grow past five cells");
    }

    @Test
    @DisplayName("A three cell brush paints a three by three square")
    void brushSizePaintsSquare() {
        painter.setBlock(5);
        painter.setSize(3);
        painter.beginStroke(canvas, 8, 8, 0, 0, SCALE);

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
        painter.setBlock(3);
        painter.beginStroke(canvas, 0, 0, 0, 0, SCALE);
        painter.continueStroke(canvas, 5, 0, 0, 0, SCALE);

        for (int x = 0; x <= 5; x++) {
            assertEquals((short) 3, canvas.pixels[0][x], "cell " + x);
        }
    }

    @Test
    @DisplayName("A diagonal stroke leaves no gaps")
    void diagonalStrokeHasNoGaps() {
        painter.setBlock(4);
        painter.beginStroke(canvas, 0, 0, 0, 0, SCALE);
        painter.continueStroke(canvas, 6, 6, 0, 0, SCALE);

        for (int i = 0; i <= 6; i++) {
            assertEquals((short) 4, canvas.pixels[i][i], "cell " + i);
        }
    }

    @Test
    @DisplayName("Repeating the same cell does not restart the stroke")
    void repeatedCellIsIgnored() {
        painter.setBlock(2);
        painter.beginStroke(canvas, 4, 4, 0, 0, SCALE);
        painter.continueStroke(canvas, 4, 4, 0, 0, SCALE);

        assertEquals((short) 2, canvas.pixels[4][4]);
    }

    @Test
    @DisplayName("Painting outside the canvas is clamped instead of throwing")
    void paintingOutsideIsClamped() {
        painter.setBlock(6);
        painter.setSize(5);

        painter.beginStroke(canvas, -50, -50, 0, 0, SCALE);
        assertEquals((short) 6, canvas.pixels[0][0]);

        painter.endStroke();
        painter.beginStroke(canvas, 1000, 1000, 0, 0, SCALE);
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
        painter.setBlock(8);
        painter.beginStroke(canvas, 100 + 24, 200 + 36, 100, 200, 12.0);

        assertEquals((short) 8, canvas.pixels[3][2]);
    }

    @Test
    @DisplayName("A canvas without a chosen size falls back to the maximum grid")
    void canvasWithoutSizeUsesMaximum() {
        CanvasData unsized = new CanvasData();
        painter.setBlock(1);
        painter.beginStroke(unsized, CanvasData.MAX_SIZE - 1, CanvasData.MAX_SIZE - 1, 0, 0, SCALE);

        assertEquals((short) 1, unsized.pixels[CanvasData.MAX_SIZE - 1][CanvasData.MAX_SIZE - 1]);
    }

    @Test
    @DisplayName("A stroke on a null canvas is refused")
    void strokeOnNullCanvasIsRefused() {
        assertFalse(painter.beginStroke(null, 0, 0, 0, 0, SCALE));
    }
}
