package iliiasik.artistry.client.renderer;

import iliiasik.artistry.config.ArtistryConfig;

public final class RenderBudget {

    private static final long NANOS_PER_MILLI = 1_000_000L;

    private static long budgetNanos = (long) (ArtistryConfig.ClientConfig.DEFAULT_BUILD_BUDGET_MS * NANOS_PER_MILLI);
    private static long windowStart = 0;
    private static long spentNanos = 0;

    private RenderBudget() {}

    public static void refresh() {
        budgetNanos = (long) (ArtistryConfig.get().client.buildBudgetMs * NANOS_PER_MILLI);
    }

    public static boolean exhausted() {
        long now = System.nanoTime();
        if (now - windowStart > RenderTuning.BUDGET_WINDOW_NANOS) {
            windowStart = now;
            spentNanos = 0;
        }
        return spentNanos >= budgetNanos;
    }

    public static void charge(long nanos) {
        spentNanos += nanos;
    }

    public static void reset() {
        windowStart = 0;
        spentNanos = 0;
    }
}
