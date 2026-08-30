package iliiasik.artistry.client.renderer;

public final class RenderTuning {

    private RenderTuning() {}

    public static final int[] LOD_SLOT_SIZE = {512, 256, 128};
    public static final int[] LOD_PAGE_SIZE = {1024, 1024, 2048};
    public static final int[] LOD_MAX_PAGES = {12, 16, 8};
    public static final boolean[] LOD_BAKES_IMAGES = {false, false, true};

    public static final float LOD_HYSTERESIS_COARSER = 1.15f;
    public static final float LOD_HYSTERESIS_FINER = 0.95f;
    public static final float LOD_DEFAULT_PIXELS_PER_BLOCK = 771f;
    public static final long LOD_REFRESH_INTERVAL_MILLIS = 500;

    public static final long BUDGET_WINDOW_NANOS = 16_000_000L;
    public static final long BUDGET_NANOS_PER_WINDOW = 3_000_000L;

    public static final long POSTER_IDLE_MILLIS = 10_000;
    public static final long POSTER_SWEEP_INTERVAL_MILLIS = 1_000;
    public static final long POSTER_MIN_SWITCH_MILLIS = 250;
    public static final long POSTER_INCOMPLETE_RETRY_MILLIS = 250;

    public static final long ATLAS_EMPTY_PAGE_GRACE_MILLIS = 10_000;

    public static final long IMAGE_IDLE_MILLIS = 60_000;
    public static final long IMAGE_SWEEP_INTERVAL_MILLIS = 5_000;
    public static final long IMAGE_TOUCH_RESOLUTION_MILLIS = 1_000;
    public static final long IMAGE_SOURCE_IDLE_MILLIS = 120_000;
    public static final long IMAGE_SOURCE_PROTECT_MILLIS = 5_000;
    public static final long IMAGE_SOURCE_BUDGET_BYTES = 128L * 1024L * 1024L;
    public static final long IMAGE_DECODED_BUDGET_BYTES = 48L * 1024L * 1024L;

    public static final int CELL_MAX_SAMPLES_PER_AXIS = 4;

    public static final int IMAGE_BAKE_SOURCE_MAX;

    static {
        int largestBakingSlot = 0;
        for (int level = 0; level < LOD_SLOT_SIZE.length; level++) {
            if (LOD_BAKES_IMAGES[level]) largestBakingSlot = Math.max(largestBakingSlot, LOD_SLOT_SIZE[level]);
        }
        IMAGE_BAKE_SOURCE_MAX = largestBakingSlot;
    }
}
