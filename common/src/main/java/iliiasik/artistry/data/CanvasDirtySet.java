package iliiasik.artistry.data;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

public final class CanvasDirtySet {

    private static final int CELLS = CanvasData.MAX_SIZE * CanvasData.MAX_SIZE;

    private final BitSet cells = new BitSet(CELLS);

    public void add(List<CanvasData.PixelChange> changes) {
        for (CanvasData.PixelChange change : changes) {
            int x = change.x() & 0xFF;
            int y = change.y() & 0xFF;
            if (x >= CanvasData.MAX_SIZE || y >= CanvasData.MAX_SIZE) continue;
            cells.set(y * CanvasData.MAX_SIZE + x);
        }
    }

    public boolean isEmpty() {
        return cells.isEmpty();
    }

    public List<CanvasData.PixelChange> collect(CanvasData canvas) {
        List<CanvasData.PixelChange> merged = new ArrayList<>(cells.cardinality());
        for (int cell = cells.nextSetBit(0); cell >= 0; cell = cells.nextSetBit(cell + 1)) {
            merged.add(canvas.changeAt(cell % CanvasData.MAX_SIZE, cell / CanvasData.MAX_SIZE));
        }
        return merged;
    }
}
