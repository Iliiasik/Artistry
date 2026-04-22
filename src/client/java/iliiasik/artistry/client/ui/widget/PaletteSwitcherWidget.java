package iliiasik.artistry.client.ui.widget;

import iliiasik.artistry.client.util.ModTextures;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.text.Text;

import java.util.function.Consumer;

public class PaletteSwitcherWidget extends ClickableWidget {

    public enum PaletteMode { BLOCKS, COLORS }

    private PaletteMode mode = PaletteMode.BLOCKS;
    private final Consumer<PaletteMode> onModeChanged;
    private final HoverFadeHelper hoverFade = new HoverFadeHelper();

    public PaletteSwitcherWidget(int x, int y, int w, int h, Consumer<PaletteMode> onModeChanged) {
        super(x, y, w, h, Text.empty());
        this.onModeChanged = onModeChanged;
    }

    @Override
    protected void renderWidget(DrawContext ctx, int mouseX, int mouseY, float delta) {
        hoverFade.update(isHovered());
        int color = hoverFade.computeColor();

        ctx.drawTexture(
                RenderPipelines.GUI_TEXTURED,
                ModTextures.PALETTE_SWITCHER,
                getX(), getY(),
                0f, 0f,
                getWidth(), getHeight(),
                32, 32,
                32, 32,
                color
        );
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        if (click.button() == 0 && isMouseOver(click.x(), click.y())) {
            mode = (mode == PaletteMode.BLOCKS) ? PaletteMode.COLORS : PaletteMode.BLOCKS;
            onModeChanged.accept(mode);
            return true;
        }
        return false;
    }

    @Override
    protected void appendClickableNarrations(NarrationMessageBuilder builder) {}
}