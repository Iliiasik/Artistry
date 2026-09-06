package iliiasik.artistry.client.tools;

import iliiasik.artistry.data.CanvasData;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Deque;
import java.util.List;

public class CanvasHistory {

    private static final int MAX_ENTRIES = 64;
    private static final int CELLS = CanvasData.MAX_SIZE * CanvasData.MAX_SIZE;

    private record Entry(List<CanvasData.PixelChange> undo,
                         List<CanvasData.PixelChange> redo,
                         int[] writtenAt) {}

    private final Deque<Entry> undoStack = new ArrayDeque<>();
    private final Deque<Entry> redoStack = new ArrayDeque<>();

    private final int[] stamps = new int[CELLS];
    private int clock = 0;

    public void push(CanvasData before, CanvasData after, Collection<Integer> cells) {
        List<CanvasData.PixelChange> undo = new ArrayList<>();
        List<CanvasData.PixelChange> redo = new ArrayList<>();

        for (int packed : cells) {
            int x = packed % CanvasData.MAX_SIZE;
            int y = packed / CanvasData.MAX_SIZE;
            if (!before.inBounds(x, y)) continue;
            if (before.pixels[y][x] == after.pixels[y][x] && before.colors[y][x] == after.colors[y][x]) continue;
            undo.add(new CanvasData.PixelChange((byte) x, (byte) y, before.pixels[y][x], before.colors[y][x]));
            redo.add(new CanvasData.PixelChange((byte) x, (byte) y, after.pixels[y][x], after.colors[y][x]));
        }
        if (undo.isEmpty()) return;

        int[] writtenAt = new int[undo.size()];
        for (int i = 0; i < undo.size(); i++) {
            writtenAt[i] = touch(cellOf(undo.get(i)));
        }

        undoStack.push(new Entry(undo, redo, writtenAt));
        while (undoStack.size() > MAX_ENTRIES) {
            undoStack.removeLast();
        }
        redoStack.clear();
    }

    public void noteExternal(List<CanvasData.PixelChange> changes) {
        for (CanvasData.PixelChange change : changes) {
            int cell = cellOf(change);
            if (cell >= 0) touch(cell);
        }
    }

    public boolean undo(CanvasData canvas) {
        Entry entry = undoStack.poll();
        if (entry == null) return false;
        apply(canvas, entry, entry.undo());
        redoStack.push(entry);
        return true;
    }

    public boolean redo(CanvasData canvas) {
        Entry entry = redoStack.poll();
        if (entry == null) return false;
        apply(canvas, entry, entry.redo());
        undoStack.push(entry);
        return true;
    }

    public void clear() {
        undoStack.clear();
        redoStack.clear();
        Arrays.fill(stamps, 0);
        clock = 0;
    }

    private void apply(CanvasData canvas, Entry entry, List<CanvasData.PixelChange> target) {
        List<CanvasData.PixelChange> owned = new ArrayList<>(target.size());
        for (int i = 0; i < target.size(); i++) {
            int cell = cellOf(target.get(i));
            if (cell < 0 || stamps[cell] != entry.writtenAt()[i]) continue;
            owned.add(target.get(i));
            entry.writtenAt()[i] = touch(cell);
        }
        if (owned.isEmpty()) return;
        canvas.applyChanges(owned);
        canvas.markChanged();
    }

    private int touch(int cell) {
        stamps[cell] = ++clock;
        return stamps[cell];
    }

    private static int cellOf(CanvasData.PixelChange change) {
        int x = change.x() & 0xFF;
        int y = change.y() & 0xFF;
        if (x >= CanvasData.MAX_SIZE || y >= CanvasData.MAX_SIZE) return -1;
        return y * CanvasData.MAX_SIZE + x;
    }
}
