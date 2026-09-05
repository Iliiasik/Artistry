package iliiasik.artistry.client.ui.widget;

import com.mojang.blaze3d.systems.RenderSystem;
import iliiasik.artistry.client.util.ModTextures;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

import java.util.function.Consumer;

public class PaletteSwitcherWidget extends AbstractWidget {

    public enum PaletteMode { BLOCKS, COLORS }

    private PaletteMode mode = PaletteMode.BLOCKS;
    private final Consumer<PaletteMode> onModeChanged;
    private final HoverFadeHelper hoverFade = new HoverFadeHelper();

    public PaletteSwitcherWidget(int x, int y, int w, int h, Consumer<PaletteMode> onModeChanged) {
        super(x, y, w, h, Component.empty());
        this.onModeChanged = onModeChanged;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    public void setMode(PaletteMode mode) {
        this.mode = mode;
    }

    public PaletteMode getMode() {
        return mode;
    }

    @Override
    protected void renderWidget(GuiGraphics ctx, int mouseX, int mouseY, float delta) {
        hoverFade.update(isHovered());
        hoverFade.applyShaderColor();
        ctx.blit(
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
        if (!visible) return false;
        if (button == 0 && isMouseOver(mouseX, mouseY)) {
            mode = (mode == PaletteMode.BLOCKS) ? PaletteMode.COLORS : PaletteMode.BLOCKS;
            onModeChanged.accept(mode);
            return true;
        }
        return false;
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput builder) {}
}