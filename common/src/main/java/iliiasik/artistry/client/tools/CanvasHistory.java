package iliiasik.artistry.client.tools;

import iliiasik.artistry.data.CanvasData;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.List;

public class CanvasHistory {

    private static final int MAX_ENTRIES = 64;

    private record Entry(List<CanvasData.PixelChange> undo, List<CanvasData.PixelChange> redo) {}

    private final Deque<Entry> undoStack = new ArrayDeque<>();
    private final Deque<Entry> redoStack = new ArrayDeque<>();

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

        undoStack.push(new Entry(undo, redo));
        while (undoStack.size() > MAX_ENTRIES) {
            undoStack.removeLast();
        }
        redoStack.clear();
    }

    public boolean undo(CanvasData canvas) {
        Entry entry = undoStack.poll();
        if (entry == null) return false;
        apply(canvas, entry.undo());
        redoStack.push(entry);
        return true;
    }

    public boolean redo(CanvasData canvas) {
        Entry entry = redoStack.poll();
        if (entry == null) return false;
        apply(canvas, entry.redo());
        undoStack.push(entry);
        return true;
    }

    public void clear() {
        undoStack.clear();
        redoStack.clear();
    }

    private static void apply(CanvasData canvas, List<CanvasData.PixelChange> changes) {
        canvas.applyChanges(changes);
        canvas.markChanged();
    }
}
