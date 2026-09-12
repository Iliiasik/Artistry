package iliiasik.artistry.debug.impl;

import iliiasik.artistry.Artistry;
import iliiasik.artistry.client.image.ClientImageCache;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class RenderProfiler {

    private static final int SAMPLES = 240;
    private static final long WINDOW_NANOS = 1_000_000_000L;
    private static final DateTimeFormatter FILE_STAMP = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    private static boolean enabled = false;

    private static final long[] frameNanos = new long[SAMPLES];
    private static int frameCursor = 0;
    private static int frameFilled = 0;
    private static long lastFrameNanos = 0;

    private static int postersThisFrame = 0;
    private static long posterNanosThisFrame = 0;
    private static int postersLastFrame = 0;
    private static long posterNanosLastFrame = 0;
    private static int postersPeak = 0;

    private static int canvasTextures = 0;
    private static long canvasTextureBytes = 0;

    private static long windowStart = 0;
    private static int uploadsInWindow = 0;
    private static long uploadBytesInWindow = 0;
    private static int uploadsPerSecond = 0;
    private static long uploadBytesPerSecond = 0;

    private static BufferedWriter csv = null;
    private static Path csvPath = null;
    private static long csvStart = 0;

    private RenderProfiler() {}

    public static boolean isEnabled() {
        return enabled;
    }

    public static void toggle() {
        enabled = !enabled;
        if (enabled) {
            reset();
            openCsv();
        } else {
            closeCsv();
        }
    }

    public static void reset() {
        Arrays.fill(frameNanos, 0L);
        frameCursor = 0;
        frameFilled = 0;
        lastFrameNanos = 0;
        postersPeak = 0;
        windowStart = System.nanoTime();
        uploadsInWindow = 0;
        uploadBytesInWindow = 0;
        uploadsPerSecond = 0;
        uploadBytesPerSecond = 0;
    }

    public static void recordPoster(long nanos) {
        if (!enabled) return;
        postersThisFrame++;
        posterNanosThisFrame += nanos;
    }

    public static void textureCreated(int texSize) {
        canvasTextures++;
        canvasTextureBytes += (long) texSize * texSize * 4L;
    }

    public static void textureClosed(int texSize) {
        canvasTextures--;
        canvasTextureBytes -= (long) texSize * texSize * 4L;
    }

    public static void textureUploaded(int texSize) {
        if (!enabled) return;
        uploadsInWindow++;
        uploadBytesInWindow += (long) texSize * texSize * 4L;
    }

    public static long nativeTextureBytes() {
        return canvasTextureBytes + ClientImageCache.textureBytes() + ClientImageCache.decodedBytes();
    }

    private static void openCsv() {
        try {
            Path directory = Minecraft.getInstance().gameDirectory.toPath().resolve("artistry-profiles");
            Files.createDirectories(directory);
            csvPath = directory.resolve("profile-" + LocalDateTime.now().format(FILE_STAMP) + ".csv");
            csv = Files.newBufferedWriter(csvPath, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            csv.write("seconds,fps,frame_avg_ms,frame_p99_ms,frame_worst_ms,posters_drawn,posters_peak,"
                    + "poster_ms,atlas_pages,atlas_mb,image_textures,image_mb,decoded_mb,source_mb,"
                    + "uploads_per_s,upload_mb_per_s,native_texture_mb,heap_used_mb");
            csv.newLine();
            csv.flush();
            csvStart = System.nanoTime();
            Artistry.LOGGER.info("Render profiler writing to {}", csvPath);
        } catch (IOException e) {
            Artistry.LOGGER.error("Failed to open the profiler csv", e);
            csv = null;
        }
    }

    private static void closeCsv() {
        if (csv == null) return;
        try {
            csv.flush();
            csv.close();
            Artistry.LOGGER.info("Render profiler wrote {}", csvPath);
        } catch (IOException e) {
            Artistry.LOGGER.error("Failed to close the profiler csv", e);
        }
        csv = null;
    }

    private static void writeCsvRow() {
        if (csv == null) return;
        double avg = averageFrameMs();
        Runtime runtime = Runtime.getRuntime();
        long heapUsed = runtime.totalMemory() - runtime.freeMemory();
        try {
            csv.write(String.format(java.util.Locale.ROOT,
                    "%.1f,%d,%.3f,%.3f,%.3f,%d,%d,%.3f,%d,%.1f,%d,%.1f,%.1f,%.1f,%d,%.1f,%.1f,%.1f",
                    (System.nanoTime() - csvStart) / 1_000_000_000.0,
                    avg > 0 ? Math.round(1000.0 / avg) : 0,
                    avg,
                    percentileFrameMs(0.99),
                    worstFrameMs(),
                    postersLastFrame,
                    postersPeak,
                    posterNanosLastFrame / 1_000_000.0,
                    canvasTextures,
                    canvasTextureBytes / 1024.0 / 1024.0,
                    ClientImageCache.textureCount(),
                    ClientImageCache.textureBytes() / 1024.0 / 1024.0,
                    ClientImageCache.decodedBytes() / 1024.0 / 1024.0,
                    ClientImageCache.sourceBytes() / 1024.0 / 1024.0,
                    uploadsPerSecond,
                    uploadBytesPerSecond / 1024.0 / 1024.0,
                    nativeTextureBytes() / 1024.0 / 1024.0,
                    heapUsed / 1024.0 / 1024.0));
            csv.newLine();
            csv.flush();
        } catch (IOException e) {
            Artistry.LOGGER.error("Failed to write a profiler row", e);
            csv = null;
        }
    }

    private static void endFrame() {
        long now = System.nanoTime();
        if (lastFrameNanos != 0) {
            frameNanos[frameCursor] = now - lastFrameNanos;
            frameCursor = (frameCursor + 1) % SAMPLES;
            if (frameFilled < SAMPLES) frameFilled++;
        }
        lastFrameNanos = now;

        postersLastFrame = postersThisFrame;
        posterNanosLastFrame = posterNanosThisFrame;
        if (postersThisFrame > postersPeak) postersPeak = postersThisFrame;
        postersThisFrame = 0;
        posterNanosThisFrame = 0;

        if (now - windowStart >= WINDOW_NANOS) {
            uploadsPerSecond = uploadsInWindow;
            uploadBytesPerSecond = uploadBytesInWindow;
            uploadsInWindow = 0;
            uploadBytesInWindow = 0;
            windowStart = now;
            writeCsvRow();
        }
    }

    private static double averageFrameMs() {
        if (frameFilled == 0) return 0;
        long sum = 0;
        for (int i = 0; i < frameFilled; i++) sum += frameNanos[i];
        return sum / (double) frameFilled / 1_000_000.0;
    }

    private static double worstFrameMs() {
        long worst = 0;
        for (int i = 0; i < frameFilled; i++) worst = Math.max(worst, frameNanos[i]);
        return worst / 1_000_000.0;
    }

    private static double percentileFrameMs(double percentile) {
        if (frameFilled == 0) return 0;
        long[] sorted = Arrays.copyOf(frameNanos, frameFilled);
        Arrays.sort(sorted);
        int index = Mth.clamp((int) Math.round(percentile * (sorted.length - 1)), 0, sorted.length - 1);
        return sorted[index] / 1_000_000.0;
    }

    private static String megabytes(long bytes) {
        return String.format(java.util.Locale.ROOT, "%.1f MB", bytes / 1024.0 / 1024.0);
    }

    public static List<String> lines() {
        double avg = averageFrameMs();
        List<String> lines = new ArrayList<>();
        lines.add("Artistry render profiler" + (csvPath != null ? " -> " + csvPath.getFileName() : ""));
        lines.add(String.format(java.util.Locale.ROOT, "fps %d   frame %.2f ms avg   %.2f ms p99   %.2f ms worst",
                avg > 0 ? Math.round(1000.0 / avg) : 0, avg, percentileFrameMs(0.99), worstFrameMs()));
        lines.add(String.format(java.util.Locale.ROOT, "posters %d drawn (peak %d)   %.3f ms in poster renderer",
                postersLastFrame, postersPeak, posterNanosLastFrame / 1_000_000.0));
        lines.add(String.format(java.util.Locale.ROOT, "atlas pages %d   %s", canvasTextures, megabytes(canvasTextureBytes)));
        lines.add(String.format(java.util.Locale.ROOT, "image textures %d   %s   decoded %s   source %s",
                ClientImageCache.textureCount(), megabytes(ClientImageCache.textureBytes()),
                megabytes(ClientImageCache.decodedBytes()), megabytes(ClientImageCache.sourceBytes())));
        lines.add(String.format(java.util.Locale.ROOT, "canvas uploads %d/s   %s/s", uploadsPerSecond, megabytes(uploadBytesPerSecond)));
        lines.add(String.format(java.util.Locale.ROOT, "native texture memory %s", megabytes(nativeTextureBytes())));
        return lines;
    }

    public static void renderOverlay(GuiGraphics graphics) {
        if (!enabled) return;
        endFrame();

        Minecraft minecraft = Minecraft.getInstance();
        List<String> lines = lines();

        int x = 4;
        int y = 4;
        int lineHeight = minecraft.font.lineHeight + 2;
        int width = 0;
        for (String line : lines) {
            width = Math.max(width, minecraft.font.width(line));
        }

        graphics.fill(x - 2, y - 2, x + width + 2, y + lines.size() * lineHeight, 0xA0000000);
        for (String line : lines) {
            graphics.drawString(minecraft.font, line, x, y, 0xFFE0E0E0, false);
            y += lineHeight;
        }
    }
}
