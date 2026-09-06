package iliiasik.artistry.client.tools;

import iliiasik.artistry.data.CanvasData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CanvasHistoryTest {

    private CanvasData canvas;
    private CanvasData before;
    private CanvasHistory history;

    @BeforeEach
    void setUp() {
        canvas = new CanvasData();
        canvas.canvasSize = 16;
        before = new CanvasData();
        history = new CanvasHistory();
    }

    private void stroke(int x, int y, short blockIndex) {
        before.copyFrom(canvas);
        canvas.pixels[y][x] = blockIndex;
        canvas.colors[y][x] = 0;
        history.push(before, canvas, cells(x, y));
    }

    private static List<Integer> cells(int... coordinates) {
        List<Integer> packed = new ArrayList<>();
        for (int i = 0; i < coordinates.length; i += 2) {
            packed.add(coordinates[i + 1] * CanvasData.MAX_SIZE + coordinates[i]);
        }
        return packed;
    }

    @Test
    @DisplayName("Undo restores the pixels a stroke replaced")
    void undoRestoresPixels() {
        canvas.pixels[3][4] = 7;
        stroke(4, 3, (short) 12);
        assertEquals((short) 12, canvas.pixels[3][4]);

        assertTrue(history.undo(canvas));
        assertEquals((short) 7, canvas.pixels[3][4]);
    }

    @Test
    @DisplayName("Redo puts the stroke back")
    void redoReappliesStroke() {
        stroke(1, 1, (short) 5);
        history.undo(canvas);
        assertTrue(history.redo(canvas));
        assertEquals((short) 5, canvas.pixels[1][1]);
    }

    @Test
    @DisplayName("Strokes are undone in reverse order")
    void strokesUndoInReverseOrder() {
        stroke(0, 0, (short) 1);
        stroke(1, 0, (short) 2);
        stroke(2, 0, (short) 3);

        history.undo(canvas);
        assertEquals((short) 0, canvas.pixels[0][2]);
        assertEquals((short) 2, canvas.pixels[0][1]);

        history.undo(canvas);
        assertEquals((short) 0, canvas.pixels[0][1]);
        assertEquals((short) 1, canvas.pixels[0][0]);

        history.undo(canvas);
        assertEquals((short) 0, canvas.pixels[0][0]);
        assertFalse(history.undo(canvas));
    }

    @Test
    @DisplayName("A colour stroke is restored with its colour")
    void colourStrokeIsRestored() {
        canvas.setColor(2, 2, 0xFF00FF00);
        before.copyFrom(canvas);
        canvas.setColor(2, 2, 0xFFFF0000);
        history.push(before, canvas, cells(2, 2));

        history.undo(canvas);
        assertEquals(0xFF00FF00, canvas.colors[2][2]);
        assertEquals(CanvasData.COLOR_PIXEL, canvas.pixels[2][2]);
    }

    @Test
    @DisplayName("A neighbour's change during a stroke stays out of the history")
    void remoteChangeIsNotRecorded() {
        canvas.pixels[0][0] = 1;
        canvas.pixels[9][9] = 4;
        before.copyFrom(canvas);

        canvas.pixels[0][0] = 2;
        canvas.pixels[9][9] = 8;

        history.push(before, canvas, cells(0, 0));
        assertTrue(history.undo(canvas));

        assertEquals((short) 1, canvas.pixels[0][0], "my own cell was not restored");
        assertEquals((short) 8, canvas.pixels[9][9], "a neighbour's cell was reverted");
    }

    @Test
    @DisplayName("Undo leaves alone a cell somebody else painted over afterwards")
    void undoSkipsCellsTakenOverByOthers() {
        canvas.pixels[3][3] = 1;
        stroke(3, 3, (short) 2);
        canvas.pixels[3][3] = 9;

        assertTrue(history.undo(canvas));
        assertEquals((short) 9, canvas.pixels[3][3], "another player's newer work was destroyed");
    }

    @Test
    @DisplayName("Redo leaves alone a cell somebody else painted over afterwards")
    void redoSkipsCellsTakenOverByOthers() {
        stroke(4, 4, (short) 2);
        history.undo(canvas);
        canvas.pixels[4][4] = 9;

        assertTrue(history.redo(canvas));
        assertEquals((short) 9, canvas.pixels[4][4], "redo climbed on top of a newer stroke");
    }

    @Test
    @DisplayName("A partly overpainted stroke is undone only where it survived")
    void undoAppliesToTheSurvivingPartOfAStroke() {
        before.copyFrom(canvas);
        canvas.pixels[0][0] = 5;
        canvas.pixels[0][1] = 5;
        history.push(before, canvas, cells(0, 0, 1, 0));

        canvas.pixels[0][1] = 9;

        assertTrue(history.undo(canvas));
        assertEquals((short) 0, canvas.pixels[0][0], "my untouched cell should have rolled back");
        assertEquals((short) 9, canvas.pixels[0][1], "the contested cell should have been left alone");
    }

    @Test
    @DisplayName("A fully overpainted stroke undoes nothing at all")
    void fullyOverpaintedStrokeIsInert() {
        stroke(6, 6, (short) 2);
        canvas.pixels[6][6] = 9;

        assertTrue(history.undo(canvas));
        assertTrue(history.redo(canvas));
        assertEquals((short) 9, canvas.pixels[6][6]);
    }

    @Test
    @DisplayName("A new stroke drops everything that was redoable")
    void newStrokeClearsRedo() {
        stroke(0, 0, (short) 1);
        history.undo(canvas);
        stroke(5, 5, (short) 9);

        assertFalse(history.redo(canvas));
    }

    @Test
    @DisplayName("A stroke that changed nothing is not recorded")
    void emptyStrokeIsIgnored() {
        before.copyFrom(canvas);
        history.push(before, canvas, cells(0, 0));
        assertFalse(history.undo(canvas));
    }

    @Test
    @DisplayName("Undo and redo on an empty history do nothing")
    void emptyHistoryIsSafe() {
        assertFalse(history.undo(canvas));
        assertFalse(history.redo(canvas));
    }

    @Test
    @DisplayName("The history keeps a bounded number of strokes")
    void historyDepthIsBounded() {
        int strokes = 200;
        for (int i = 0; i < strokes; i++) {
            stroke(i % 16, i / 16, (short) (1 + i % 100));
        }

        int undone = 0;
        while (history.undo(canvas)) {
            undone++;
            assertTrue(undone <= strokes, "the history grew without a limit");
        }
        assertTrue(undone > 0 && undone < strokes, "expected the oldest strokes to be dropped, undone " + undone);
    }
}
