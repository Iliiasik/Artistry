package iliiasik.artistry.client.renderer;

public final class RenderBudget {

    private static long windowStart = 0;
    private static long spentNanos = 0;

    private RenderBudget() {}

    public static boolean claim() {
        long now = System.nanoTime();
        if (now - windowStart > RenderTuning.BUDGET_WINDOW_NANOS) {
            windowStart = now;
            spentNanos = 0;
        }
        return spentNanos < RenderTuning.BUDGET_NANOS_PER_WINDOW;
    }

    public static void charge(long nanos) {
        spentNanos += nanos;
    }

    public static void reset() {
        windowStart = 0;
        spentNanos = 0;
    }
}
