package iliiasik.artistry.client;

public class ClientServerSettings {

    private static boolean disableImages = false;
    private static long batchIntervalMs = 50;
    private static long cursorIntervalMs = 100;

    public static void apply(boolean disableImages, long batchIntervalMs, long cursorIntervalMs) {
        ClientServerSettings.disableImages = disableImages;
        ClientServerSettings.batchIntervalMs = batchIntervalMs;
        ClientServerSettings.cursorIntervalMs = cursorIntervalMs;
    }

    public static boolean imagesDisabled() {
        return disableImages;
    }

    public static long batchIntervalMs() {
        return batchIntervalMs;
    }

    public static long cursorIntervalMs() {
        return cursorIntervalMs;
    }

    public static void reset() {
        disableImages = false;
        batchIntervalMs = 50;
        cursorIntervalMs = 100;
    }
}
