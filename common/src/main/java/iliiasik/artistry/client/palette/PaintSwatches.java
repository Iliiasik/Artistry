package iliiasik.artistry.client.palette;

public class PaintSwatches {

    public static final int HISTORY_SIZE = 3;

    private static final int DEFAULT_SECONDARY = 0xFF000000;
    private static final int[] DEFAULT_HISTORY = {0xFFFF0000, 0xFF00FF00, 0xFF00BFFF};

    private PaintSwatch primary = PaintSwatch.ofBlock(1);
    private PaintSwatch secondary = PaintSwatch.ofColor(DEFAULT_SECONDARY);
    private final PaintSwatch[] history = new PaintSwatch[HISTORY_SIZE];

    public PaintSwatches() {
        for (int i = 0; i < HISTORY_SIZE; i++) {
            history[i] = PaintSwatch.ofColor(DEFAULT_HISTORY[i]);
        }
    }

    public PaintSwatch primary() {
        return primary;
    }

    public PaintSwatch secondary() {
        return secondary;
    }

    public PaintSwatch history(int index) {
        return history[index];
    }

    public PaintSwatch slot(boolean secondarySlot) {
        return secondarySlot ? secondary : primary;
    }

    public void swapSlots() {
        PaintSwatch previous = primary;
        primary = secondary;
        secondary = previous;
    }

    public void select(PaintSwatch swatch, boolean secondarySlot) {
        PaintSwatch previous = slot(secondarySlot);
        if (swatch.equals(previous)) return;
        if (swatch.equals(slot(!secondarySlot))) {
            swapSlots();
            return;
        }

        if (secondarySlot) {
            secondary = swatch;
        } else {
            primary = swatch;
        }

        int found = indexOf(swatch);
        if (found >= 0) {
            history[found] = previous;
        } else {
            push(previous);
        }
    }

    private int indexOf(PaintSwatch swatch) {
        for (int i = 0; i < HISTORY_SIZE; i++) {
            if (swatch.equals(history[i])) return i;
        }
        return -1;
    }

    private void push(PaintSwatch swatch) {
        if (swatch.equals(primary) || swatch.equals(secondary)) return;
        for (int i = HISTORY_SIZE - 1; i > 0; i--) {
            history[i] = history[i - 1];
        }
        history[0] = swatch;
    }
}
