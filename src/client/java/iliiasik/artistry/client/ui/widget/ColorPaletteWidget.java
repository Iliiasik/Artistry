package iliiasik.artistry.client.ui.widget;

import iliiasik.artistry.client.ui.util.ModTextures;
import iliiasik.artistry.client.ui.palette.BlockPalette;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.texture.Sprite;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.function.IntConsumer;

public class ColorPaletteWidget extends ClickableWidget {

    private static final Identifier TEXTURE = ModTextures.PALETTE;

    private static final int COLS = 7;
    private static final int TEX_W = 32;
    private static final int TEX_H = 162;
    private static final int BORDER = 2;
    private static final int CELL = 4;
    private static final int PREVIEW = 28;

    private final IntConsumer onBlockChanged;
    private int selectedIndex = 1;

    public ColorPaletteWidget(int x, int y, int w, int h, IntConsumer onBlockChanged) {
        super(x, y, w, h, Text.empty());
        this.onBlockChanged = onBlockChanged;
    }

    public void setSelectedIndex(int index) {
        this.selectedIndex = index;
    }

    public int getSelectedIndex() {
        return selectedIndex;
    }

    @Override
    protected void renderWidget(DrawContext ctx, int mouseX, int mouseY, float delta) {
        BlockPalette.ensureLoaded();

        float scaleX = (float) getWidth() / TEX_W;
        float scaleY = (float) getHeight() / TEX_H;

        int vMouseX = (int) ((mouseX - getX()) / scaleX);
        int vMouseY = (int) ((mouseY - getY()) / scaleY);

        ctx.getMatrices().pushMatrix();
        ctx.getMatrices().translate((float) getX(), (float) getY());
        ctx.getMatrices().scale(scaleX, scaleY);

        ctx.drawTexture(
                RenderPipelines.GUI_TEXTURED,
                TEXTURE,
                0, 0,
                0f, 0f,
                TEX_W, TEX_H,
                TEX_W, TEX_H,
                0xFFFFFFFF
        );

        int sx = BORDER;
        int previewY = BORDER;

        Sprite previewSprite = BlockPalette.getSprite(selectedIndex);
        if (previewSprite != null) {
            ctx.drawSpriteStretched(RenderPipelines.GUI_TEXTURED, previewSprite, sx, previewY, PREVIEW, PREVIEW, 0xFFFFFFFF);
        } else {
            ctx.fill(sx, previewY, sx + PREVIEW, previewY + PREVIEW, 0xFFFDF7E8);
        }

        int startY = BORDER + PREVIEW + BORDER;
        int maxY = TEX_H - BORDER;

        for (int i = 0; i < BlockPalette.COUNT; i++) {
            int col = i % COLS;
            int row = i / COLS;
            int cx = sx + col * CELL;
            int cy = startY + row * CELL;
            if (cy + CELL > maxY) break;

            Sprite sp = BlockPalette.getSprite(i + 1);
            if (sp != null) {
                ctx.drawSpriteStretched(RenderPipelines.GUI_TEXTURED, sp, cx, cy, CELL, CELL, 0xFFFFFFFF);
            } else {
                ctx.fill(cx, cy, cx + CELL, cy + CELL, 0xFF888888);
            }

            if (i + 1 == selectedIndex) {
                ctx.drawStrokedRectangle(cx, cy, CELL, CELL, 0xFFFFFFFF);
            }

            if (vMouseX >= cx && vMouseX < cx + CELL && vMouseY >= cy && vMouseY < cy + CELL) {
                ctx.fill(cx, cy, cx + CELL, cy + CELL, 0x55FFFFFF);
            }
        }

        ctx.getMatrices().popMatrix();
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        if (click.button() != 0) return false;
        BlockPalette.ensureLoaded();

        float scaleX = (float) getWidth() / TEX_W;
        float scaleY = (float) getHeight() / TEX_H;

        int vClickX = (int) ((click.x() - getX()) / scaleX);
        int vClickY = (int) ((click.y() - getY()) / scaleY);

        int sx = BORDER;
        int startY = BORDER + PREVIEW + BORDER;
        int maxY = TEX_H - BORDER;

        for (int i = 0; i < BlockPalette.COUNT; i++) {
            int col = i % COLS;
            int row = i / COLS;
            int cx = sx + col * CELL;
            int cy = startY + row * CELL;
            if (cy + CELL > maxY) break;

            if (vClickX >= cx && vClickX < cx + CELL && vClickY >= cy && vClickY < cy + CELL) {
                selectedIndex = i + 1;
                onBlockChanged.accept(selectedIndex);
                return true;
            }
        }
        return false;
    }

    @Override
    protected void appendClickableNarrations(NarrationMessageBuilder builder) {}
}