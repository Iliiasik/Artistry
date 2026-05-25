package iliiasik.artistry.client.ui.widget;

import iliiasik.artistry.client.util.ModTextures;
import com.mojang.blaze3d.systems.RenderSystem;
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

    public void setMode(PaletteMode mode) {
        this.mode = mode;
    }

    public PaletteMode getMode() {
        return mode;
    }

    @Override
    protected void renderWidget(DrawContext ctx, int mouseX, int mouseY, float delta) {
        hoverFade.update(isHovered());
        hoverFade.applyShaderColor();
        ctx.drawTexture(
                ModTextures.PALETTE_SWITCHER,
                getX(), getY(),
                getWidth(), getHeight(),
                0f, 0f,
                32, 32,
                32, 32
        );
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && isMouseOver(mouseX, mouseY)) {
            mode = (mode == PaletteMode.BLOCKS) ? PaletteMode.COLORS : PaletteMode.BLOCKS;
            onModeChanged.accept(mode);
            return true;
        }
        return false;
    }

    @Override
    protected void appendClickableNarrations(NarrationMessageBuilder builder) {}
}