package iliiasik.artistry.client.ui.widget;

import com.mojang.blaze3d.systems.RenderSystem;

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

    public void applyShaderColor() {
        int color = computeColor();
        float a = ((color >> 24) & 0xFF) / 255f;
        float r = ((color >> 16) & 0xFF) / 255f;
        float g = ((color >> 8)  & 0xFF) / 255f;
        float b = ( color        & 0xFF) / 255f;
        RenderSystem.setShaderColor(r, g, b, a);
    }
}