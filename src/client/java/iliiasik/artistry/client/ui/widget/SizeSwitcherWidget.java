package iliiasik.artistry.client.ui.widget;

import iliiasik.artistry.client.ui.util.ModTextures;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.function.IntConsumer;

public class SizeSwitcherWidget extends ClickableWidget {
    private static final Identifier TEXTURE = ModTextures.SIZE_SWITCHER;

    private static final int[] SIZES = {1, 2, 3, 4, 5};
    private static final int TEXT_COLOR = 0xFF666155;

    private int sizeIndex = 0;
    private final IntConsumer onSizeChanged;

    private float hoverProgress = 0f;
    private long lastFrameTime = System.currentTimeMillis();

    public SizeSwitcherWidget(int x, int y, int w, int h, IntConsumer onSizeChanged) {
        super(x, y, w, h, Text.empty());
        this.onSizeChanged = onSizeChanged;
    }

    public int getCurrentSize() {
        return SIZES[sizeIndex];
    }

    @Override
    protected void renderWidget(DrawContext ctx, int mouseX, int mouseY, float delta) {
        long now = System.currentTimeMillis();
        float dt = (now - lastFrameTime) / 1000f;
        lastFrameTime = now;

        float targetHover = isHovered() ? 1f : 0f;
        hoverProgress += (targetHover - hoverProgress) * Math.min(1f, dt * 8f);

        int brightness = (int) (200 + 55 * hoverProgress);
        int color = (0xFF << 24) | (brightness << 16) | (brightness << 8) | brightness;

        ctx.drawTexture(
                RenderPipelines.GUI_TEXTURED,
                TEXTURE,
                getX(), getY(),
                0f, 0f,
                getWidth(), getHeight(),
                32, 32,
                32, 32,
                color
        );

        String label = String.valueOf(SIZES[sizeIndex]);
        MinecraftClient client = MinecraftClient.getInstance();
        int textX = getX() + getWidth() / 2 - client.textRenderer.getWidth(label) / 2;
        int textY = getY() + getHeight() / 2 - 4;
        ctx.drawText(client.textRenderer, label, textX, textY, TEXT_COLOR, false);
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        if (click.button() == 0 && isMouseOver(click.x(), click.y())) {
            sizeIndex = (sizeIndex + 1) % SIZES.length;
            onSizeChanged.accept(SIZES[sizeIndex]);
            return true;
        }
        return false;
    }

    @Override
    protected void appendClickableNarrations(NarrationMessageBuilder builder) {}
}