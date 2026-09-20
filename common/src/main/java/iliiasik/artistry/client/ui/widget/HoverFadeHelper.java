package iliiasik.artistry.client.ui.widget;

import com.mojang.blaze3d.systems.RenderSystem;

public class HoverFadeHelper {

    private static final int RESTING_BRIGHTNESS = 200;
    private static final int HOVER_BRIGHTNESS = 255;
    private static final int DISABLED_BRIGHTNESS = 70;
    private static final float FADE_SPEED = 8f;

    private float hoverProgress = 0f;
    private float disabledProgress = 0f;
    private long lastFrameTime = System.currentTimeMillis();

    public void update(boolean hovered) {
        update(hovered, false);
    }

    public void update(boolean hovered, boolean disabled) {
        long now = System.currentTimeMillis();
        float dt = (now - lastFrameTime) / 1000f;
        lastFrameTime = now;

        float step = Math.min(1f, dt * FADE_SPEED);
        hoverProgress += ((hovered && !disabled ? 1f : 0f) - hoverProgress) * step;
        disabledProgress += ((disabled ? 1f : 0f) - disabledProgress) * step;
    }

    public int computeColor() {
        float brightness = RESTING_BRIGHTNESS + (HOVER_BRIGHTNESS - RESTING_BRIGHTNESS) * hoverProgress;
        brightness += (DISABLED_BRIGHTNESS - brightness) * disabledProgress;
        int value = (int) brightness;
        return (0xFF << 24) | (value << 16) | (value << 8) | value;
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
