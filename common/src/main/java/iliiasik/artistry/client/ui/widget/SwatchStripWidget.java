package iliiasik.artistry.client.ui.widget;

import iliiasik.artistry.client.palette.BlockPalette;
import iliiasik.artistry.client.palette.PaintSwatch;
import iliiasik.artistry.client.palette.PaintSwatches;
import iliiasik.artistry.client.ui.layout.PaintDimensions;
import iliiasik.artistry.client.util.ModTextures;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public class SwatchStripWidget extends AbstractWidget {

    private static final int EMPTY_COLOR = 0xFFFDF7E8;

    private final PaintDimensions dims;
    private final PaintSwatches swatches;
    private final Runnable onPrimaryChanged;

    public SwatchStripWidget(PaintDimensions dims, PaintSwatches swatches, Runnable onPrimaryChanged) {
        super(dims.swatchStripX, dims.swatchStripY, dims.swatchStripW, dims.swatchStripH, Component.empty());
        this.dims = dims;
        this.swatches = swatches;
        this.onPrimaryChanged = onPrimaryChanged;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    public void setSize(int w, int h) {
        this.width = w;
        this.height = h;
    }

    @Override
    protected void renderWidget(GuiGraphics ctx, int mouseX, int mouseY, float delta) {
        if (!visible) return;
        BlockPalette.ensureLoaded();

        boolean overSecondary = isOverSecondary(mouseX, mouseY);
        drawSwatch(ctx, swatches.secondary(),
                overSecondary ? ModTextures.SECONDARY_COLOR_HOVER : ModTextures.SECONDARY_COLOR,
                PaintDimensions.SECONDARY_TEXTURE_SIZE,
                dims.secondaryX, dims.secondaryY, dims.secondarySize);

        for (int i = 0; i < PaintDimensions.HISTORY_CELLS; i++) {
            boolean overCell = isOverHistory(mouseX, mouseY, i);
            drawSwatch(ctx, swatches.history(i),
                    overCell ? ModTextures.PREVIOUS_COLOR_HOVER : ModTextures.PREVIOUS_COLOR,
                    PaintDimensions.HISTORY_TEXTURE_SIZE,
                    dims.historyX, historyCellY(i), dims.historySize);
        }
    }

    private int historyCellY(int index) {
        return dims.historyY + index * (dims.historySize + dims.historyGap);
    }

    private boolean isOverSecondary(double mouseX, double mouseY) {
        return inside(mouseX, mouseY, dims.secondaryX, dims.secondaryY, dims.secondarySize);
    }

    private boolean isOverHistory(double mouseX, double mouseY, int index) {
        return inside(mouseX, mouseY, dims.historyX, historyCellY(index), dims.historySize);
    }

    private static boolean inside(double mouseX, double mouseY, int x, int y, int size) {
        return mouseX >= x && mouseX < x + size && mouseY >= y && mouseY < y + size;
    }

    private static void drawSwatch(GuiGraphics ctx, PaintSwatch swatch, ResourceLocation frame,
                                   int textureSize, int x, int y, int size) {
        ctx.blit(frame, x, y, size, size, 0f, 0f, textureSize, textureSize, textureSize, textureSize);

        int inset = Math.round((float) PaintDimensions.SWATCH_FRAME_BORDER * size / textureSize);
        int innerX = x + inset;
        int innerY = y + inset;
        int innerSize = size - inset * 2;
        if (innerSize <= 0) return;

        if (swatch.isColor()) {
            ctx.fill(innerX, innerY, innerX + innerSize, innerY + innerSize, swatch.color());
            return;
        }
        TextureAtlasSprite sprite = BlockPalette.getSprite(swatch.blockIndex());
        if (sprite != null) {
            ctx.blit(innerX, innerY, 0, innerSize, innerSize, sprite);
        } else {
            ctx.fill(innerX, innerY, innerX + innerSize, innerY + innerSize, EMPTY_COLOR);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!visible || (button != 0 && button != 1)) return false;

        if (isOverSecondary(mouseX, mouseY)) {
            swatches.swapSlots();
            onPrimaryChanged.run();
            return true;
        }

        for (int i = 0; i < PaintDimensions.HISTORY_CELLS; i++) {
            if (!isOverHistory(mouseX, mouseY, i)) continue;
            boolean secondary = button == 1;
            swatches.select(swatches.history(i), secondary);
            if (!secondary) onPrimaryChanged.run();
            return true;
        }
        return false;
    }

    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        return visible && super.isMouseOver(mouseX, mouseY);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput builder) {}
}
