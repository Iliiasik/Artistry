package iliiasik.artistry.data;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CanvasDirtySetTest {

    private static final int SIZE = 32;

    private static CanvasData canvas() {
        CanvasData canvas = new CanvasData();
        canvas.canvasSize = SIZE;
        return canvas;
    }

    private static CanvasData.PixelChange change(int x, int y, int blockIndex) {
        return new CanvasData.PixelChange((byte) x, (byte) y, (short) blockIndex, 0);
    }

    @Test
    @DisplayName("A fresh set holds nothing")
    void startsEmpty() {
        CanvasDirtySet dirty = new CanvasDirtySet();

        assertTrue(dirty.isEmpty());
        assertEquals(0, dirty.size());
        assertTrue(dirty.collect(canvas()).isEmpty());
    }

    @Test
    @DisplayName("Repeated strokes over the same cell collapse into one change")
    void repeatedCellsCollapse() {
        CanvasDirtySet dirty = new CanvasDirtySet();

        for (int i = 1; i <= 20; i++) {
            dirty.add(List.of(change(4, 7, i)));
        }

        assertEquals(1, dirty.size());
        assertFalse(dirty.isEmpty());
    }

    @Test
    @DisplayName("The merged list carries the canvas state, not the queued values")
    void collectReadsTheCanvas() {
        CanvasData canvas = canvas();
        CanvasDirtySet dirty = new CanvasDirtySet();

        dirty.add(List.of(change(2, 3, 11)));
        canvas.pixels[3][2] = 42;
        canvas.colors[3][2] = 0xFF00FF00;

        List<CanvasData.PixelChange> merged = dirty.collect(canvas);

        assertEquals(1, merged.size());
        assertEquals(2, merged.get(0).x());
        assertEquals(3, merged.get(0).y());
        assertEquals(42, merged.get(0).blockIndex());
        assertEquals(0xFF00FF00, merged.get(0).color());
    }

    @Test
    @DisplayName("Coordinates outside the canvas arrays are dropped instead of throwing")
    void outOfRangeCellsAreDropped() {
        CanvasDirtySet dirty = new CanvasDirtySet();

        dirty.add(List.of(
                change(SIZE, 0, 1),
                change(0, SIZE, 1),
                change(-1, -1, 1)
        ));

        assertTrue(dirty.isEmpty());
    }

    @Test
    @DisplayName("Coalescing a batch of strokes lands on the same canvas as applying them one by one")
    void coalescedResultMatchesSequentialApply() {
        Random random = new Random(20260904L);

        for (int round = 0; round < 200; round++) {
            CanvasData authority = canvas();
            CanvasData bystander = canvas();
            CanvasDirtySet dirty = new CanvasDirtySet();

            for (int batch = 0; batch < 10; batch++) {
                List<CanvasData.PixelChange> changes = new ArrayList<>();
                for (int i = 0; i < 1 + random.nextInt(12); i++) {
                    changes.add(change(random.nextInt(SIZE), random.nextInt(SIZE), 1 + random.nextInt(30)));
                }
                authority.applyChanges(changes);
                dirty.add(changes);
            }

            bystander.applyChanges(dirty.collect(authority));

            assertTrue(authority.diff(bystander).isEmpty(),
                    "round " + round + " did not converge");
        }
    }

    @Test
    @DisplayName("Coalescing never sends more changes than there are cells touched")
    void coalescingShrinksThePayload() {
        Random random = new Random(7L);
        CanvasData canvas = canvas();
        CanvasDirtySet dirty = new CanvasDirtySet();

        int rawChanges = 0;
        for (int batch = 0; batch < 20; batch++) {
            List<CanvasData.PixelChange> changes = new ArrayList<>();
            for (int i = 0; i < 9; i++) {
                changes.add(change(random.nextInt(4), random.nextInt(4), 1 + random.nextInt(5)));
            }
            canvas.applyChanges(changes);
            dirty.add(changes);
            rawChanges += changes.size();
        }

        assertEquals(180, rawChanges);
        assertTrue(dirty.size() <= 16, "only a 4x4 corner was touched, got " + dirty.size());
        assertTrue(dirty.size() < rawChanges);
    }

    @Test
    @DisplayName("Clearing drops everything that was queued")
    void clearEmptiesTheSet() {
        CanvasDirtySet dirty = new CanvasDirtySet();
        dirty.add(List.of(change(1, 1, 5), change(2, 2, 6)));

        dirty.clear();

        assertTrue(dirty.isEmpty());
        assertEquals(0, dirty.size());
    }
}
