package iliiasik.artistry.client.renderer;

import net.minecraft.client.Minecraft;

public final class PosterLod {

    public static final int LEVELS = RenderTuning.LOD_SLOT_SIZE.length;

    private static final CanvasAtlas[] ATLASES = new CanvasAtlas[LEVELS];
    private static final int COARSEST_UNBAKED;

    private static float pixelsPerBlock = RenderTuning.LOD_DEFAULT_PIXELS_PER_BLOCK;
    private static long lastRefresh = 0;

    static {
        int unbaked = 0;
        for (int level = 0; level < LEVELS; level++) {
            ATLASES[level] = new CanvasAtlas("lod" + level,
                    RenderTuning.LOD_PAGE_SIZE[level],
                    RenderTuning.LOD_SLOT_SIZE[level],
                    RenderTuning.LOD_MAX_PAGES[level]);
            if (!RenderTuning.LOD_BAKES_IMAGES[level]) unbaked = level;
        }
        COARSEST_UNBAKED = unbaked;
    }

    private PosterLod() {}

    public static int initialLevel(int desired) {
        return RenderTuning.LOD_BAKES_IMAGES[desired] ? desired : COARSEST_UNBAKED;
    }

    public static CanvasAtlas atlas(int level) {
        return ATLASES[level];
    }

    public static int slotSize(int level) {
        return RenderTuning.LOD_SLOT_SIZE[level];
    }

    public static boolean bakesImages(int level) {
        return RenderTuning.LOD_BAKES_IMAGES[level];
    }

    public static void refresh(long now) {
        if (now - lastRefresh < RenderTuning.LOD_REFRESH_INTERVAL_MILLIS) return;
        lastRefresh = now;
        Minecraft minecraft = Minecraft.getInstance();
        int height = minecraft.getWindow().getHeight();
        double fov = minecraft.options.fov().get();
        if (height <= 0 || fov <= 1.0 || fov >= 179.0) return;
        pixelsPerBlock = (float) (height / (2.0 * Math.tan(Math.toRadians(fov) / 2.0)));
    }

    public static int select(int current, double distance, float worldSize) {
        for (int level = 0; level < LEVELS - 1; level++) {
            float boundary = pixelsPerBlock * worldSize / RenderTuning.LOD_SLOT_SIZE[level + 1];
            float threshold = current > level
                    ? boundary * RenderTuning.LOD_HYSTERESIS_FINER
                    : boundary * RenderTuning.LOD_HYSTERESIS_COARSER;
            if (distance < threshold) return level;
        }
        return LEVELS - 1;
    }

    public static void sweep(long now) {
        for (CanvasAtlas atlas : ATLASES) {
            atlas.sweep(now);
        }
    }

    public static void clear() {
        for (CanvasAtlas atlas : ATLASES) {
            atlas.clear();
        }
        lastRefresh = 0;
    }
}
