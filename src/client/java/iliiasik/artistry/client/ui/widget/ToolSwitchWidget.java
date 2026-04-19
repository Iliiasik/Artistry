package iliiasik.artistry.client.ui.widget;

import iliiasik.artistry.client.tools.DrawingTool;
import iliiasik.artistry.client.ui.util.ModTextures;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.function.Consumer;

public class ToolSwitchWidget extends ClickableWidget {

    private static final Identifier BRUSH_TEXTURE  = ModTextures.BRUSH;
    private static final Identifier ERASER_TEXTURE = ModTextures.ERASER;

    private DrawingTool activeTool = DrawingTool.BRUSH;
    private final Consumer<DrawingTool> onToolChanged;

    private float hoverProgress = 0f;
    private long lastFrameTime = System.currentTimeMillis();

    public ToolSwitchWidget(int x, int y, int w, int h, Consumer<DrawingTool> onToolChanged) {
        super(x, y, w, h, Text.empty());
        this.onToolChanged = onToolChanged;
    }

    @Override
    protected void renderWidget(DrawContext ctx, int mouseX, int mouseY, float delta) {
        long now = System.currentTimeMillis();
        float dt = (now - lastFrameTime) / 1000f;
        lastFrameTime = now;

        float targetHover = isHovered() ? 1f : 0f;
        hoverProgress += (targetHover - hoverProgress) * Math.min(1f, dt * 8f);

        Identifier texture = activeTool == DrawingTool.BRUSH ? BRUSH_TEXTURE : ERASER_TEXTURE;

        int brightness = (int) (200 + 55 * hoverProgress);
        int color = (0xFF << 24) | (brightness << 16) | (brightness << 8) | brightness;

        ctx.drawTexture(
                RenderPipelines.GUI_TEXTURED,
                texture,
                getX(), getY(),
                0f, 0f,
                getWidth(), getHeight(),
                32, 64,
                32, 64,
                color
        );
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        if (click.button() == 0 && isMouseOver(click.x(), click.y())) {
            activeTool = (activeTool == DrawingTool.BRUSH) ? DrawingTool.ERASER : DrawingTool.BRUSH;
            onToolChanged.accept(activeTool);
            return true;
        }
        return false;
    }

    @Override
    protected void appendClickableNarrations(NarrationMessageBuilder builder) {}
}