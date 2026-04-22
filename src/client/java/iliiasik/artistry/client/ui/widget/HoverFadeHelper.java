package iliiasik.artistry.client.ui.widget;

public class HoverFadeHelper {
    private float hoverProgress = 0f;
    private long lastFrameTime = System.currentTimeMillis();

    public void update(boolean hovered) {
        long now = System.currentTimeMillis();
        float dt = (now - lastFrameTime) / 1000f;
        lastFrameTime = now;

        float targetHover = hovered ? 1f : 0f;
        hoverProgress += (targetHover - hoverProgress) * Math.min(1f, dt * 8f);
    }

    public int computeColor() {
        int brightness = (int) (200 + 55 * hoverProgress);
        return (0xFF << 24) | (brightness << 16) | (brightness << 8) | brightness;
    }
}