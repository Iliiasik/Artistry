package iliiasik.artistry.client.ui.widget;

import iliiasik.artistry.client.util.ModTextures;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
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
    private final HoverFadeHelper hoverFade = new HoverFadeHelper();
    private boolean visible = true;

    public SizeSwitcherWidget(int x, int y, int w, int h, IntConsumer onSizeChanged) {
        super(x, y, w, h, Text.empty());
        this.onSizeChanged = onSizeChanged;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    public int getCurrentSize() {
        return SIZES[sizeIndex];
    }

    @Override
    protected void renderWidget(DrawContext ctx, int mouseX, int mouseY, float delta) {
        if (!visible) return;
        hoverFade.update(isHovered());
        hoverFade.applyShaderColor();
        ctx.drawTexture(
                TEXTURE,
                getX(), getY(),
                getWidth(), getHeight(),
                0f, 0f,
                32, 32,
                32, 32
        );
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);

        String label = String.valueOf(SIZES[sizeIndex]);
        MinecraftClient client = MinecraftClient.getInstance();
        int textX = getX() + getWidth() / 2 - client.textRenderer.getWidth(label) / 2;
        int textY = getY() + getHeight() / 2 - 4;
        ctx.drawText(client.textRenderer, label, textX, textY, TEXT_COLOR, false);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!visible) return false;
        if (button == 0 && isMouseOver(mouseX, mouseY)) {
            sizeIndex = (sizeIndex + 1) % SIZES.length;
            onSizeChanged.accept(SIZES[sizeIndex]);
            return true;
        }
        return false;
    }

    @Override
    protected void appendClickableNarrations(NarrationMessageBuilder builder) {}
}