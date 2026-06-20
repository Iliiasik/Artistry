package iliiasik.artistry.client;

public class ClientServerSettings {

    private static boolean disableImages = false;

    public static void setDisableImages(boolean value) {
        disableImages = value;
    }

    public static boolean imagesDisabled() {
        return disableImages;
    }

    public static void reset() {
        disableImages = false;
    }
}