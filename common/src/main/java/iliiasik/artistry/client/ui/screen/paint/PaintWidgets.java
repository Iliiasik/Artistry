package iliiasik.artistry.client.ui.screen.paint;

import iliiasik.artistry.client.palette.ColorPalette;
import iliiasik.artistry.client.palette.PaintSwatch;
import iliiasik.artistry.client.palette.PaintSwatches;
import iliiasik.artistry.client.tools.DrawingTool;
import iliiasik.artistry.client.tools.PixelPainter;
import iliiasik.artistry.client.ui.layout.PaintDimensions;
import iliiasik.artistry.client.ui.widget.ColorPaletteWidget;
import iliiasik.artistry.client.ui.widget.HexInputWidget;
import iliiasik.artistry.client.ui.widget.ImageToolWidget;
import iliiasik.artistry.client.ui.widget.PaletteSwitcherWidget;
import iliiasik.artistry.client.ui.widget.SignButtonWidget;
import iliiasik.artistry.client.ui.widget.SizeSwitcherWidget;
import iliiasik.artistry.client.ui.widget.SwatchStripWidget;
import iliiasik.artistry.client.ui.widget.ToolSwitchWidget;
import net.minecraft.client.gui.components.AbstractWidget;

import java.util.function.Consumer;

public class PaintWidgets {

    private final PaintDimensions dims;
    private final PaintSwatches swatches;
    private final PixelPainter pixelPainter;
    private final Runnable onPickImage;
    private final Consumer<ImageToolWidget.Action> onImageAction;
    private final Runnable onSign;

    private ToolSwitchWidget toolSwitchWidget;
    private SizeSwitcherWidget sizeSwitcherWidget;
    private ColorPaletteWidget colorPaletteWidget;
    private PaletteSwitcherWidget paletteSwitcherWidget;
    private HexInputWidget hexInput;
    private ImageToolWidget imageToolWidget;
    private SignButtonWidget signButton;
    private SwatchStripWidget swatchStripWidget;

    private boolean updatingHexFromPalette = false;
    private boolean imageMode = false;

    public PaintWidgets(PaintDimensions dims, PaintSwatches swatches, PixelPainter pixelPainter,
                        Runnable onPickImage, Consumer<ImageToolWidget.Action> onImageAction,
                        Runnable onSign) {
        this.dims = dims;
        this.swatches = swatches;
        this.pixelPainter = pixelPainter;
        this.onPickImage = onPickImage;
        this.onImageAction = onImageAction;
        this.onSign = onSign;
    }

    public void build(Consumer<AbstractWidget> adder, boolean signed, boolean sizeChosen) {
        if (signed) return;

        if (sizeChosen) {
            signButton = new SignButtonWidget(
                    dims.signX, dims.signY,
                    dims.signW, dims.signH,
                    onSign
            );
            adder.accept(signButton);
        }

        toolSwitchWidget = new ToolSwitchWidget(
                dims.toolSwitchX, dims.toolSwitchY,
                dims.toolSwitchW, dims.toolSwitchH,
                tool -> {
                    if (tool == DrawingTool.IMAGE) {
                        onPickImage.run();
                    } else {
                        pixelPainter.setTool(tool);
                    }
                }
        );
        adder.accept(toolSwitchWidget);

        sizeSwitcherWidget = new SizeSwitcherWidget(
                dims.sizeSwitchX, dims.sizeSwitchY,
                dims.sizeSwitchW, dims.sizeSwitchH,
                pixelPainter::setSize
        );
        pixelPainter.setSize(sizeSwitcherWidget.getCurrentSize());
        adder.accept(sizeSwitcherWidget);

        colorPaletteWidget = new ColorPaletteWidget(
                dims.paletteX, dims.paletteY,
                dims.paletteW, dims.paletteH,
                new ColorPaletteWidget.SelectionListener() {
                    @Override
                    public void onBlockSelected(int blockIndex, boolean secondary) {
                        swatches.select(PaintSwatch.ofBlock(blockIndex), secondary);
                        if (!secondary) leaveNonPaintingTool();
                    }
                    @Override
                    public void onColorSelected(int argbColor) {
                        swatches.select(PaintSwatch.ofColor(argbColor), false);
                        leaveNonPaintingTool();
                    }
                },
                secondary -> {
                    PaintSwatch swatch = swatches.slot(secondary);
                    return swatch.isColor() ? 0 : swatch.blockIndex();
                }
        );
        swatches.select(PaintSwatch.ofBlock(colorPaletteWidget.getSelectedIndex()), false);
        adder.accept(colorPaletteWidget);

        swatchStripWidget = new SwatchStripWidget(dims, swatches, this::syncPaletteToPrimary);
        adder.accept(swatchStripWidget);

        hexInput = new HexInputWidget(0, 0, 1, 1);
        hexInput.setText("#FF0000");
        hexInput.setChangedListener(text -> {
            if (updatingHexFromPalette) return;
            if (text.length() == 7) {
                colorPaletteWidget.setColorFromHex(text);
            }
        });
        hexInput.setVisible(false);
        adder.accept(hexInput);

        colorPaletteWidget.setOnColorChanged(color -> {
            updatingHexFromPalette = true;
            hexInput.setText(ColorPalette.argbToHex(color));
            updatingHexFromPalette = false;
        });

        paletteSwitcherWidget = new PaletteSwitcherWidget(
                dims.paletteSwitcherX, dims.paletteSwitcherY,
                dims.paletteSwitcherW, dims.paletteSwitcherH,
                mode -> {
                    colorPaletteWidget.setMode(mode);
                    hexInput.setVisible(mode == PaletteSwitcherWidget.PaletteMode.COLORS);
                }
        );
        adder.accept(paletteSwitcherWidget);

        imageToolWidget = new ImageToolWidget(
                dims.toolSwitchX, dims.toolSwitchY,
                dims.toolSwitchW, dims.toolSwitchH,
                onImageAction::accept
        );
        imageToolWidget.setVisible(false);
        adder.accept(imageToolWidget);
    }

    public void layout() {
        if (toolSwitchWidget != null) {
            toolSwitchWidget.setPosition(dims.toolSwitchX, dims.toolSwitchY);
            toolSwitchWidget.setSize(dims.toolSwitchW, dims.toolSwitchH);
        }
        if (sizeSwitcherWidget != null) {
            sizeSwitcherWidget.setPosition(dims.sizeSwitchX, dims.sizeSwitchY);
            sizeSwitcherWidget.setSize(dims.sizeSwitchW, dims.sizeSwitchH);
        }
        if (colorPaletteWidget != null) {
            colorPaletteWidget.setPosition(dims.paletteX, dims.paletteY);
            colorPaletteWidget.setSize(dims.paletteW, dims.paletteH);
        }
        if (paletteSwitcherWidget != null) {
            paletteSwitcherWidget.setPosition(dims.paletteSwitcherX, dims.paletteSwitcherY);
            paletteSwitcherWidget.setSize(dims.paletteSwitcherW, dims.paletteSwitcherH);
        }
        if (imageToolWidget != null) {
            imageToolWidget.setPosition(dims.toolSwitchX, dims.toolSwitchY);
            imageToolWidget.setSize(dims.toolSwitchW, dims.toolSwitchH);
        }
        if (hexInput != null) {
            hexInput.setX(dims.hexInputX);
            hexInput.setY(dims.hexInputY);
            hexInput.setWidth(dims.hexInputW);
            hexInput.setHeight(dims.hexInputH);
        }
        if (signButton != null) {
            signButton.setPosition(dims.signX, dims.signY);
            signButton.setSize(dims.signW, dims.signH);
        }
        if (swatchStripWidget != null) {
            swatchStripWidget.setPosition(dims.swatchStripX, dims.swatchStripY);
            swatchStripWidget.setSize(dims.swatchStripW, dims.swatchStripH);
        }
    }

    private void leaveNonPaintingTool() {
        DrawingTool current = pixelPainter.getTool();
        if (current != DrawingTool.ERASER && current != DrawingTool.PIPETTE) return;
        pixelPainter.setTool(DrawingTool.BRUSH);
        if (toolSwitchWidget != null) toolSwitchWidget.setActiveTool(DrawingTool.BRUSH);
    }

    private void syncPaletteToPrimary() {
        PaintSwatch primary = swatches.primary();
        if (primary.isColor()) {
            applyPickedColor(primary.color(), false);
        } else {
            applyPickedBlock(primary.blockIndex(), false);
        }
    }

    public void setImageMode(boolean on) {
        imageMode = on;
        if (toolSwitchWidget != null) toolSwitchWidget.setVisible(!on);
        if (sizeSwitcherWidget != null) sizeSwitcherWidget.setVisible(!on);
        if (imageToolWidget != null) imageToolWidget.setVisible(on);
        if (swatchStripWidget != null) swatchStripWidget.setVisible(!on);
        if (colorPaletteWidget != null) colorPaletteWidget.setVisible(!on);
        if (paletteSwitcherWidget != null) paletteSwitcherWidget.setVisible(!on);
        if (hexInput != null) {
            hexInput.setVisible(!on && paletteSwitcherWidget != null
                    && paletteSwitcherWidget.getMode() == PaletteSwitcherWidget.PaletteMode.COLORS);
        }
        updateSignVisibility();
    }

    public void swapSwatches() {
        swatches.swapSlots();
        syncPaletteToPrimary();
    }

    public boolean stepBrushSize(int delta) {
        return sizeSwitcherWidget != null && sizeSwitcherWidget.stepSize(delta);
    }

    public boolean isTextFieldFocused() {
        return hexInput != null && hexInput.isVisible() && hexInput.isFocused();
    }

    private void updateSignVisibility() {
        if (signButton != null) signButton.setVisible(!imageMode);
    }

    public void setActiveTool(DrawingTool tool) {
        if (toolSwitchWidget != null) toolSwitchWidget.setActiveTool(tool);
    }

    public void applyPickedColor(int color, boolean secondary) {
        swatches.select(PaintSwatch.ofColor(color), secondary);
        if (secondary || colorPaletteWidget == null) return;
        colorPaletteWidget.selectColor(color);
        paletteSwitcherWidget.setMode(PaletteSwitcherWidget.PaletteMode.COLORS);
        updatingHexFromPalette = true;
        hexInput.setText(ColorPalette.argbToHex(color));
        updatingHexFromPalette = false;
        hexInput.setVisible(true);
    }

    public void applyPickedBlock(int blockIndex, boolean secondary) {
        swatches.select(PaintSwatch.ofBlock(blockIndex), secondary);
        if (secondary || colorPaletteWidget == null) return;
        colorPaletteWidget.selectBlock(blockIndex);
        paletteSwitcherWidget.setMode(PaletteSwitcherWidget.PaletteMode.BLOCKS);
        hexInput.setVisible(false);
    }

    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (button == 0 && hexInput != null && hexInput.isVisible() && hexInput.isFocused()) {
            if (hexInput.mouseDragged(mouseX, mouseY, button, deltaX, deltaY)) return true;
        }
        if (button == 0 && !imageMode && colorPaletteWidget != null) {
            return colorPaletteWidget.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
        }
        return false;
    }
}