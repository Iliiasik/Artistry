package iliiasik.artistry.client.ui.widget;

import com.mojang.blaze3d.platform.NativeImage;
import iliiasik.artistry.client.palette.BlockPalette;
import iliiasik.artistry.client.palette.ColorPalette;
import iliiasik.artistry.client.palette.PaintSwatch;
import iliiasik.artistry.client.util.ModTextures;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.function.Consumer;

public class ColorPaletteWidget extends AbstractWidget {

    public interface SelectionListener {
        void onBlockSelected(int blockIndex, boolean secondary);
        void onColorSelected(int argbColor);
    }

    public interface SlotSwatches {
        PaintSwatch slot(boolean secondary);
    }

    private static final int COLS       = 7;
    private static final int TEX_W      = 32;
    private static final int TEX_H      = 162;
    private static final int BORDER     = 2;
    private static final int CELL       = 4;
    private static final int PREVIEW    = 28;
    private static final int HEX_H      = 8;
    private static final int SPECTRUM_H = 8;

    private static final int PRIMARY_OUTLINE   = 0xFFFFFFFF;
    private static final int SECONDARY_OUTLINE = 0xFFFFAA00;

    private static final ResourceLocation SPECTRUM_ID = ResourceLocation.fromNamespaceAndPath("artistry", "palette_spectrum_cache");
    private static final ResourceLocation HSV_ID      = ResourceLocation.fromNamespaceAndPath("artistry", "palette_hsv_cache");
    private static DynamicTexture spectrumTex;
    private static DynamicTexture hsvTex;
    private static float hsvBuiltHue = Float.NaN;

    private final SelectionListener listener;
    private final SlotSwatches slots;
    private int selectedColor = 0xFFFFFFFF;
    private float selectedHue = 0f;
    private float selectedSat = 0f;
    private float selectedVal = 1f;
    private PaletteSwitcherWidget.PaletteMode mode = PaletteSwitcherWidget.PaletteMode.BLOCKS;
    private Consumer<Integer> onColorChanged;

    public ColorPaletteWidget(int x, int y, int w, int h, SelectionListener listener, SlotSwatches slots) {
        super(x, y, w, h, Component.empty());
        this.listener = listener;
        this.slots = slots;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    public void setOnColorChanged(Consumer<Integer> callback) {
        this.onColorChanged = callback;
    }

    public void setMode(PaletteSwitcherWidget.PaletteMode mode) {
        this.mode = mode;
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

    private static void ensureSpectrumTexture() {
        if (spectrumTex != null) return;
        NativeImage img = new NativeImage(PREVIEW, SPECTRUM_H, false);
        for (int px = 0; px < PREVIEW; px++) {
            int c = ColorPalette.argbToAbgr(ColorPalette.hsvToArgb((float) px / PREVIEW, 1f, 1f));
            for (int py = 0; py < SPECTRUM_H; py++) img.setPixelRGBA(px, py, c);
        }
        spectrumTex = new DynamicTexture(img);
        Minecraft.getInstance().getTextureManager().register(SPECTRUM_ID, spectrumTex);
    }

    private void ensureHsvTexture() {
        int h = hsvH();
        if (hsvTex == null) {
            NativeImage img = new NativeImage(PREVIEW, h, false);
            hsvTex = new DynamicTexture(img);
            Minecraft.getInstance().getTextureManager().register(HSV_ID, hsvTex);
            hsvBuiltHue = Float.NaN;
        }
        if (hsvBuiltHue != selectedHue) {
            NativeImage img = hsvTex.getPixels();
            if (img != null) {
                for (int px = 0; px < PREVIEW; px++) {
                    float sat = (float) px / PREVIEW;
                    for (int py = 0; py < h; py++) {
                        float val = 1f - (float) py / h;
                        img.setPixelRGBA(px, py, ColorPalette.argbToAbgr(ColorPalette.hsvToArgb(selectedHue, sat, val)));
                    }
                }
                hsvTex.upload();
            }
            hsvBuiltHue = selectedHue;
        }
    }

    private void drawPreview(GuiGraphics ctx) {
        PaintSwatch primary = slots.slot(false);
        if (primary.isColor()) {
            ctx.fill(BORDER, BORDER, BORDER + PREVIEW, BORDER + PREVIEW, primary.color());
            return;
        }
        TextureAtlasSprite previewSprite = BlockPalette.getSprite(primary.blockIndex());
        if (previewSprite != null) {
            ctx.blit(BORDER, BORDER, 0, PREVIEW, PREVIEW, previewSprite);
        } else {
            ctx.fill(BORDER, BORDER, BORDER + PREVIEW, BORDER + PREVIEW, 0xFFFDF7E8);
        }
    }

    private int blockInSlot(boolean secondary) {
        PaintSwatch swatch = slots.slot(secondary);
        return swatch.isColor() ? 0 : swatch.blockIndex();
    }

    @Override
    protected void renderWidget(GuiGraphics ctx, int mouseX, int mouseY, float delta) {
        BlockPalette.ensureLoaded();

        float[] scale = getScale();
        int[] vMouse = toVirtual(mouseX, mouseY);

        ctx.pose().pushPose();
        ctx.pose().translate((float) getX(), (float) getY(), 0);
        ctx.pose().scale(scale[0], scale[1], 1f);

        if (mode == PaletteSwitcherWidget.PaletteMode.BLOCKS) {
            ctx.blit(ModTextures.PALETTE_BLOCKS,
                    0, 0, TEX_W, TEX_H, 0f, 0f, TEX_W, TEX_H, TEX_W, TEX_H);

            drawPreview(ctx);

            int startY = cellsStartY();
            int maxY = TEX_H - BORDER;
            for (int i = 0; i < BlockPalette.COUNT; i++) {
                int col = i % COLS;
                int row = i / COLS;
                int cx = BORDER + col * CELL;
                int cy = startY + row * CELL;
                if (cy + CELL > maxY) break;
                TextureAtlasSprite sp = BlockPalette.getSprite(i + 1);
                if (sp != null) {
                    ctx.blit(cx, cy, 0, CELL, CELL, sp);
                } else {
                    ctx.fill(cx, cy, cx + CELL, cy + CELL, 0xFF888888);
                }
                int index = i + 1;
                if (blockInSlot(false) == index) {
                    ctx.renderOutline(cx, cy, CELL, CELL, PRIMARY_OUTLINE);
                } else if (blockInSlot(true) == index) {
                    ctx.renderOutline(cx, cy, CELL, CELL, SECONDARY_OUTLINE);
                }
                if (vMouse[0] >= cx && vMouse[0] < cx + CELL && vMouse[1] >= cy && vMouse[1] < cy + CELL) {
                    ctx.fill(cx, cy, cx + CELL, cy + CELL, 0x55FFFFFF);
                }
            }
        } else {
            ctx.blit(ModTextures.PALETTE_HEX,
                    0, 0, TEX_W, TEX_H, 0f, 0f, TEX_W, TEX_H, TEX_W, TEX_H);

            drawPreview(ctx);

            ensureSpectrumTexture();
            ensureHsvTexture();

            int hsvY = hsvY();
            int hsvH = hsvH();
            ctx.blit(SPECTRUM_ID, BORDER, spectrumY(), 0f, 0f,
                    PREVIEW, SPECTRUM_H, PREVIEW, SPECTRUM_H);
            ctx.blit(HSV_ID, BORDER, hsvY, 0f, 0f,
                    PREVIEW, hsvH, PREVIEW, hsvH);

            int cursorSpecX = BORDER + 1 + Math.round(selectedHue * (PREVIEW - 2));
            ctx.fill(cursorSpecX - 1, spectrumY(), cursorSpecX + 1, spectrumY() + SPECTRUM_H, 0xFFFFFFFF);

            int cursorHsvX = BORDER + 1 + Math.round(selectedSat * (PREVIEW - 2));
            int cursorHsvY = hsvY + 1 + Math.round((1f - selectedVal) * (hsvH - 2));
            ctx.fill(cursorHsvX - 1, cursorHsvY - 1, cursorHsvX + 1, cursorHsvY + 1, 0xFFFFFFFF);
        }

        ctx.pose().popPose();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!visible || (button != 0 && button != 1)) return false;
        BlockPalette.ensureLoaded();

        boolean secondary = button == 1;
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
                    listener.onBlockSelected(i + 1, secondary);
                    return true;
                }
            }
        } else if (!secondary) {
            pickFromClick(mouseX, mouseY);
        }
        return false;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (!visible || button != 0 || mode != PaletteSwitcherWidget.PaletteMode.COLORS) return false;
        pickFromClick(mouseX, mouseY);
        return true;
    }

    private float clamp01(float v) {
        return Math.clamp(v, 0f, 1f);
    }

    private void pickFromClick(double mouseX, double mouseY) {
        float[] vf = toVirtualF(mouseX, mouseY);
        float vx = vf[0];
        float vy = vf[1];

        int specY = spectrumY();
        if (vx >= BORDER && vx < BORDER + PREVIEW && vy >= specY && vy < specY + SPECTRUM_H) {
            selectedHue = clamp01((vx - BORDER) / PREVIEW);
            applyHsv();
            return;
        }

        int hsvY = hsvY();
        int hsvH = hsvH();
        if (vx >= BORDER && vx < BORDER + PREVIEW && vy >= hsvY && vy < hsvY + hsvH) {
            selectedSat = clamp01((vx - BORDER) / PREVIEW);
            selectedVal = clamp01(1f - (vy - hsvY) / hsvH);
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
        float[] hsv = ColorPalette.argbToHsv(argb);
        selectedHue = hsv[0];
        selectedSat = hsv[1];
        selectedVal = hsv[2];
        this.mode = PaletteSwitcherWidget.PaletteMode.COLORS;
    }

    public void showBlocks() {
        this.mode = PaletteSwitcherWidget.PaletteMode.BLOCKS;
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput builder) {}
}