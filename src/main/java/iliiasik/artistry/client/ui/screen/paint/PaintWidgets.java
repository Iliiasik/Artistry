package iliiasik.artistry.client.ui.screen.paint;

import iliiasik.artistry.client.palette.ColorPalette;
import iliiasik.artistry.client.tools.DrawingTool;
import iliiasik.artistry.client.tools.PixelPainter;
import iliiasik.artistry.client.ui.layout.PaintDimensions;
import iliiasik.artistry.client.ui.widget.*;
import net.minecraft.client.gui.components.AbstractWidget;

import java.util.function.Consumer;

public class PaintWidgets {

    private final PaintDimensions dims;
    private final PixelPainter pixelPainter;
    private final Runnable onPickImage;
    private final Consumer<ImageToolWidget.Action> onImageAction;

    private ToolSwitchWidget toolSwitchWidget;
    private SizeSwitcherWidget sizeSwitcherWidget;
    private ColorPaletteWidget colorPaletteWidget;
    private PaletteSwitcherWidget paletteSwitcherWidget;
    private HexInputWidget hexInput;
    private ImageToolWidget imageToolWidget;

    private boolean updatingHexFromPalette = false;

    public PaintWidgets(PaintDimensions dims, PixelPainter pixelPainter,
                        Runnable onPickImage, Consumer<ImageToolWidget.Action> onImageAction) {
        this.dims = dims;
        this.pixelPainter = pixelPainter;
        this.onPickImage = onPickImage;
        this.onImageAction = onImageAction;
    }

    public void build(Consumer<AbstractWidget> adder) {
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
                    public void onBlockSelected(int blockIndex) {
                        pixelPainter.setBlock(blockIndex);
                        if (pixelPainter.getTool() == DrawingTool.ERASER || pixelPainter.getTool() == DrawingTool.PIPETTE) {
                            pixelPainter.setTool(DrawingTool.BRUSH);
                            toolSwitchWidget.setActiveTool(DrawingTool.BRUSH);
                        }
                    }
                    @Override
                    public void onColorSelected(int argbColor) {
                        pixelPainter.setColor(argbColor);
                        if (pixelPainter.getTool() == DrawingTool.ERASER || pixelPainter.getTool() == DrawingTool.PIPETTE) {
                            pixelPainter.setTool(DrawingTool.BRUSH);
                            toolSwitchWidget.setActiveTool(DrawingTool.BRUSH);
                        }
                    }
                }
        );
        pixelPainter.setBlock(colorPaletteWidget.getSelectedIndex());
        adder.accept(colorPaletteWidget);

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
            hexInput.setSize(dims.hexInputW, dims.hexInputH);
        }
    }

    public void setImageMode(boolean on) {
        if (toolSwitchWidget != null) toolSwitchWidget.setVisible(!on);
        if (sizeSwitcherWidget != null) sizeSwitcherWidget.setVisible(!on);
        if (imageToolWidget != null) imageToolWidget.setVisible(on);
    }

    public void setActiveTool(DrawingTool tool) {
        if (toolSwitchWidget != null) toolSwitchWidget.setActiveTool(tool);
    }

    public void applyPickedColor(int color) {
        colorPaletteWidget.selectColor(color);
        paletteSwitcherWidget.setMode(PaletteSwitcherWidget.PaletteMode.COLORS);
        pixelPainter.setColor(color);
        updatingHexFromPalette = true;
        hexInput.setText(ColorPalette.argbToHex(color));
        updatingHexFromPalette = false;
        hexInput.setVisible(true);
    }

    public void applyPickedBlock(int blockIndex) {
        colorPaletteWidget.selectBlock(blockIndex);
        paletteSwitcherWidget.setMode(PaletteSwitcherWidget.PaletteMode.BLOCKS);
        pixelPainter.setBlock(blockIndex);
        hexInput.setVisible(false);
    }

    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (button == 0 && hexInput != null && hexInput.isVisible() && hexInput.isFocused()) {
            if (hexInput.mouseDragged(mouseX, mouseY, button, deltaX, deltaY)) return true;
        }
        if (button == 0 && colorPaletteWidget != null) {
            return colorPaletteWidget.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
        }
        return false;
    }
}