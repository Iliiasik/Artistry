package iliiasik.artistry.client.ui.widget;

import iliiasik.artistry.client.util.ModTextures;
import iliiasik.artistry.client.palette.BlockPalette;
import iliiasik.artistry.client.palette.ColorPalette;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.texture.Sprite;
import net.minecraft.text.Text;

public class ColorPaletteWidget extends ClickableWidget {

    public interface SelectionListener {
        void onBlockSelected(int blockIndex);
        void onColorSelected(int argbColor);
    }

    private static final int COLS    = 7;
    private static final int TEX_W   = 32;
    private static final int TEX_H   = 162;
    private static final int BORDER  = 2;
    private static final int CELL    = 4;
    private static final int PREVIEW = 28;

    private final SelectionListener listener;
    private int selectedBlockIndex = 1;
    private int selectedColorIndex = 0;
    private PaletteSwitcherWidget.PaletteMode mode = PaletteSwitcherWidget.PaletteMode.BLOCKS;

    public ColorPaletteWidget(int x, int y, int w, int h, SelectionListener listener) {
        super(x, y, w, h, Text.empty());
        this.listener = listener;
    }

    public void setMode(PaletteSwitcherWidget.PaletteMode mode) {
        this.mode = mode;
    }

    public int getSelectedIndex() {
        return selectedBlockIndex;
    }

    private int cellsStartY() {
        return BORDER + PREVIEW + BORDER;
    }

    private float[] getScale() {
        return new float[]{ (float) getWidth() / TEX_W, (float) getHeight() / TEX_H };
    }

    private int[] toVirtual(double screenX, double screenY) {
        float[] scale = getScale();
        return new int[]{ (int) ((screenX - getX()) / scale[0]), (int) ((screenY - getY()) / scale[1]) };
    }

    @Override
    protected void renderWidget(DrawContext ctx, int mouseX, int mouseY, float delta) {
        BlockPalette.ensureLoaded();

        float[] scale = getScale();
        int[] vMouse = toVirtual(mouseX, mouseY);

        ctx.getMatrices().push();
        ctx.getMatrices().translate((float) getX(), (float) getY(), 0);
        ctx.getMatrices().scale(scale[0], scale[1], 1f);

        ctx.drawTexture(ModTextures.PALETTE,
                0, 0, TEX_W, TEX_H, 0f, 0f, TEX_W, TEX_H, TEX_W, TEX_H);

        int sx = BORDER;
        int previewY = BORDER;

        if (mode == PaletteSwitcherWidget.PaletteMode.BLOCKS) {
            Sprite previewSprite = BlockPalette.getSprite(selectedBlockIndex);
            if (previewSprite != null) {
                ctx.drawSprite(sx, previewY, 0, PREVIEW, PREVIEW, previewSprite);
            } else {
                ctx.fill(sx, previewY, sx + PREVIEW, previewY + PREVIEW, 0xFFFDF7E8);
            }
        } else {
            int previewColor = (selectedColorIndex >= 0 && selectedColorIndex < ColorPalette.COUNT)
                    ? ColorPalette.COLORS[selectedColorIndex] : 0xFFFFFFFF;
            ctx.fill(sx, previewY, sx + PREVIEW, previewY + PREVIEW, previewColor);
        }

        int startY = cellsStartY();
        int maxY = TEX_H - BORDER;
        int count = (mode == PaletteSwitcherWidget.PaletteMode.BLOCKS)
                ? BlockPalette.COUNT : ColorPalette.COUNT;

        for (int i = 0; i < count; i++) {
            int col = i % COLS;
            int row = i / COLS;
            int cx = sx + col * CELL;
            int cy = startY + row * CELL;
            if (cy + CELL > maxY) break;

            if (mode == PaletteSwitcherWidget.PaletteMode.BLOCKS) {
                Sprite sp = BlockPalette.getSprite(i + 1);
                if (sp != null) {
                    ctx.drawSprite(cx, cy, 0, CELL, CELL, sp);
                } else {
                    ctx.fill(cx, cy, cx + CELL, cy + CELL, 0xFF888888);
                }
                if (i + 1 == selectedBlockIndex) {
                    ctx.drawBorder(cx, cy, CELL, CELL, 0xFFFFFFFF);
                }
            } else {
                int argb = ColorPalette.COLORS[i];
                ctx.fill(cx, cy, cx + CELL, cy + CELL, argb);
                if (i == selectedColorIndex) {
                    ctx.drawBorder(cx, cy, CELL, CELL, 0xFFFFFFFF);
                }
            }

            if (vMouse[0] >= cx && vMouse[0] < cx + CELL && vMouse[1] >= cy && vMouse[1] < cy + CELL) {
                ctx.fill(cx, cy, cx + CELL, cy + CELL, 0x55FFFFFF);
            }
        }

        ctx.getMatrices().pop();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) return false;
        BlockPalette.ensureLoaded();

        int[] vClick = toVirtual(mouseX, mouseY);
        int startY = cellsStartY();
        int maxY = TEX_H - BORDER;
        int count = (mode == PaletteSwitcherWidget.PaletteMode.BLOCKS)
                ? BlockPalette.COUNT : ColorPalette.COUNT;

        for (int i = 0; i < count; i++) {
            int col = i % COLS;
            int row = i / COLS;
            int cx = BORDER + col * CELL;
            int cy = startY + row * CELL;
            if (cy + CELL > maxY) break;

            if (vClick[0] >= cx && vClick[0] < cx + CELL && vClick[1] >= cy && vClick[1] < cy + CELL) {
                if (mode == PaletteSwitcherWidget.PaletteMode.BLOCKS) {
                    selectedBlockIndex = i + 1;
                    listener.onBlockSelected(selectedBlockIndex);
                } else {
                    selectedColorIndex = i;
                    listener.onColorSelected(ColorPalette.COLORS[i]);
                }
                return true;
            }
        }
        return false;
    }

    @Override
    protected void appendClickableNarrations(NarrationMessageBuilder builder) {}
}