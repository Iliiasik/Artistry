package iliiasik.artistry.client.ui.widget;

import iliiasik.artistry.client.palette.BlockPalette;
import iliiasik.artistry.client.palette.ColorPalette;
import iliiasik.artistry.client.util.ModTextures;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.texture.Sprite;
import net.minecraft.text.Text;

import java.util.function.Consumer;

public class ColorPaletteWidget extends ClickableWidget {

    public interface SelectionListener {
        void onBlockSelected(int blockIndex);
        void onColorSelected(int argbColor);
    }

    private static final int COLS       = 7;
    private static final int TEX_W      = 32;
    private static final int TEX_H      = 162;
    private static final int BORDER     = 2;
    private static final int CELL       = 4;
    private static final int PREVIEW    = 28;
    private static final int HEX_H      = 8;
    private static final int SPECTRUM_H = 8;

    private final SelectionListener listener;
    private int selectedBlockIndex = 1;
    private int selectedColor = 0xFFFF0000;
    private float selectedHue = 0f;
    private float selectedSat = 1f;
    private float selectedVal = 1f;
    private PaletteSwitcherWidget.PaletteMode mode = PaletteSwitcherWidget.PaletteMode.BLOCKS;
    private Consumer<Integer> onColorChanged;

    public ColorPaletteWidget(int x, int y, int w, int h, SelectionListener listener) {
        super(x, y, w, h, Text.empty());
        this.listener = listener;
    }

    public void setOnColorChanged(Consumer<Integer> callback) {
        this.onColorChanged = callback;
    }

    public void setMode(PaletteSwitcherWidget.PaletteMode mode) {
        this.mode = mode;
    }

    public int getSelectedIndex() {
        return selectedBlockIndex;
    }

    private int spectrumY() { return BORDER + PREVIEW + BORDER + HEX_H + BORDER; }
    private int hsvY()      { return spectrumY() + SPECTRUM_H + BORDER; }
    private int hsvH()      { return TEX_H - hsvY() - BORDER; }
    private int cellsStartY() { return BORDER + PREVIEW + BORDER; }

    private float[] getScale() {
        return new float[]{ (float) getWidth() / TEX_W, (float) getHeight() / TEX_H };
    }

    private float[] toVirtualF(double screenX, double screenY) {
        float[] scale = getScale();
        return new float[]{
                (float)((screenX - getX()) / scale[0]),
                (float)((screenY - getY()) / scale[1])
        };
    }

    private int[] toVirtual(double screenX, double screenY) {
        float[] vf = toVirtualF(screenX, screenY);
        return new int[]{ (int) vf[0], (int) vf[1] };
    }

    private void drawSpectrumRow(DrawContext ctx, int y) {
        for (int px = 0; px < PREVIEW; px++) {
            float hue = (float) px / PREVIEW;
            ctx.fill(BORDER + px, y, BORDER + px + 1, y + SPECTRUM_H,
                    ColorPalette.hsvToArgb(hue, 1f, 1f));
        }
    }

    private void drawHsvSquare(DrawContext ctx, int y, int h) {
        for (int px = 0; px < PREVIEW; px++) {
            float sat = (float) px / PREVIEW;
            for (int py = 0; py < h; py++) {
                float val = 1f - (float) py / h;
                ctx.fill(BORDER + px, y + py, BORDER + px + 1, y + py + 1,
                        ColorPalette.hsvToArgb(selectedHue, sat, val));
            }
        }
    }

    @Override
    protected void renderWidget(DrawContext ctx, int mouseX, int mouseY, float delta) {
        BlockPalette.ensureLoaded();

        float[] scale = getScale();
        int[] vMouse = toVirtual(mouseX, mouseY);

        ctx.getMatrices().push();
        ctx.getMatrices().translate((float) getX(), (float) getY(), 0);
        ctx.getMatrices().scale(scale[0], scale[1], 1f);

        if (mode == PaletteSwitcherWidget.PaletteMode.BLOCKS) {
            ctx.drawTexture(ModTextures.PALETTE_BLOCKS,
                    0, 0, TEX_W, TEX_H, 0f, 0f, TEX_W, TEX_H, TEX_W, TEX_H);

            Sprite previewSprite = BlockPalette.getSprite(selectedBlockIndex);
            if (previewSprite != null) {
                ctx.drawSprite(BORDER, BORDER, 0, PREVIEW, PREVIEW, previewSprite);
            } else {
                ctx.fill(BORDER, BORDER, BORDER + PREVIEW, BORDER + PREVIEW, 0xFFFDF7E8);
            }

            int startY = cellsStartY();
            int maxY = TEX_H - BORDER;
            for (int i = 0; i < BlockPalette.COUNT; i++) {
                int col = i % COLS;
                int row = i / COLS;
                int cx = BORDER + col * CELL;
                int cy = startY + row * CELL;
                if (cy + CELL > maxY) break;
                Sprite sp = BlockPalette.getSprite(i + 1);
                if (sp != null) {
                    ctx.drawSprite(cx, cy, 0, CELL, CELL, sp);
                } else {
                    ctx.fill(cx, cy, cx + CELL, cy + CELL, 0xFF888888);
                }
                if (i + 1 == selectedBlockIndex) {
                    ctx.drawBorder(cx, cy, CELL, CELL, 0xFFFFFFFF);
                }
                if (vMouse[0] >= cx && vMouse[0] < cx + CELL && vMouse[1] >= cy && vMouse[1] < cy + CELL) {
                    ctx.fill(cx, cy, cx + CELL, cy + CELL, 0x55FFFFFF);
                }
            }
        } else {
            ctx.drawTexture(ModTextures.PALETTE_HEX,
                    0, 0, TEX_W, TEX_H, 0f, 0f, TEX_W, TEX_H, TEX_W, TEX_H);

            ctx.fill(BORDER, BORDER, BORDER + PREVIEW, BORDER + PREVIEW, selectedColor);

            drawSpectrumRow(ctx, spectrumY());

            int hsvY = hsvY();
            int hsvH = hsvH();
            drawHsvSquare(ctx, hsvY, hsvH);

            int cursorSpecX = BORDER + 1 + Math.round(selectedHue * (PREVIEW - 2));
            ctx.fill(cursorSpecX - 1, spectrumY(), cursorSpecX + 1, spectrumY() + SPECTRUM_H, 0xFFFFFFFF);

            int cursorHsvX = BORDER + 1 + Math.round(selectedSat * (PREVIEW - 2));
            int cursorHsvY = hsvY + 1 + Math.round((1f - selectedVal) * (hsvH - 2));
            ctx.fill(cursorHsvX - 1, cursorHsvY - 1, cursorHsvX + 1, cursorHsvY + 1, 0xFFFFFFFF);
        }

        ctx.getMatrices().pop();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) return false;
        BlockPalette.ensureLoaded();

        int[] vClick = toVirtual(mouseX, mouseY);

        if (mode == PaletteSwitcherWidget.PaletteMode.BLOCKS) {
            int startY = cellsStartY();
            int maxY = TEX_H - BORDER;
            for (int i = 0; i < BlockPalette.COUNT; i++) {
                int col = i % COLS;
                int row = i / COLS;
                int cx = BORDER + col * CELL;
                int cy = startY + row * CELL;
                if (cy + CELL > maxY) break;
                if (vClick[0] >= cx && vClick[0] < cx + CELL && vClick[1] >= cy && vClick[1] < cy + CELL) {
                    selectedBlockIndex = i + 1;
                    listener.onBlockSelected(selectedBlockIndex);
                    return true;
                }
            }
        } else {
            pickFromClick(mouseX, mouseY);
        }
        return false;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (button != 0 || mode != PaletteSwitcherWidget.PaletteMode.COLORS) return false;
        pickFromClick(mouseX, mouseY);
        return true;
    }

    private void pickFromClick(double mouseX, double mouseY) {
        float[] vf = toVirtualF(mouseX, mouseY);
        float vx = vf[0];
        float vy = vf[1];

        int specY = spectrumY();
        if (vx >= BORDER && vx < BORDER + PREVIEW && vy >= specY && vy < specY + SPECTRUM_H) {
            selectedHue = Math.max(0f, Math.min(1f, (vx - BORDER) / PREVIEW));
            applyHsv();
            return;
        }

        int hsvY = hsvY();
        int hsvH = hsvH();
        if (vx >= BORDER && vx < BORDER + PREVIEW && vy >= hsvY && vy < hsvY + hsvH) {
            selectedSat = Math.max(0f, Math.min(1f, (vx - BORDER) / PREVIEW));
            selectedVal = Math.max(0f, Math.min(1f, 1f - (vy - hsvY) / hsvH));
            applyHsv();
        }
    }

    private void applyHsv() {
        selectedColor = ColorPalette.hsvToArgb(selectedHue, selectedSat, selectedVal);
        listener.onColorSelected(selectedColor);
        if (onColorChanged != null) onColorChanged.accept(selectedColor);
    }

    public void setColorFromHex(String hex) {
        try {
            int argb = ColorPalette.hexToArgb(hex);
            selectedColor = argb;
            float[] hsv = ColorPalette.argbToHsv(argb);
            selectedHue = hsv[0];
            selectedSat = hsv[1];
            selectedVal = hsv[2];
            listener.onColorSelected(selectedColor);
        } catch (NumberFormatException ignored) {}
    }

    public void selectColor(int argb) {
        selectedColor = argb;
        float[] hsv = ColorPalette.argbToHsv(argb);
        selectedHue = hsv[0];
        selectedSat = hsv[1];
        selectedVal = hsv[2];
        this.mode = PaletteSwitcherWidget.PaletteMode.COLORS;
    }

    public void selectBlock(int blockIndex) {
        this.selectedBlockIndex = blockIndex;
        this.mode = PaletteSwitcherWidget.PaletteMode.BLOCKS;
    }

    @Override
    protected void appendClickableNarrations(NarrationMessageBuilder builder) {}
}