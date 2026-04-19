package iliiasik.artistry.client.ui.screen;

import iliiasik.artistry.client.tools.PixelPainter;
import iliiasik.artistry.client.ui.layout.PaintDimensions;
import iliiasik.artistry.client.ui.renderer.CanvasRenderer;
import iliiasik.artistry.client.ui.util.ModTextures;
import iliiasik.artistry.client.ui.widget.ColorPaletteWidget;
import iliiasik.artistry.client.ui.widget.SizeSwitcherWidget;
import iliiasik.artistry.client.ui.widget.ToolSwitchWidget;
import iliiasik.artistry.block.entity.PosterBlockEntity;
import iliiasik.artistry.client.renderer.PosterBlockEntityRenderer;
import iliiasik.artistry.data.CanvasData;
import iliiasik.artistry.network.SaveCanvasC2SPacket;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.List;

public class PaintScreen extends Screen {
    private static final Identifier FRAME_TEXTURE = ModTextures.FRAME;

    private final PaintDimensions dims = new PaintDimensions();
    private final CanvasData canvasData = new CanvasData();
    private final CanvasData snapshotBeforeEdit = new CanvasData();
    private final PixelPainter pixelPainter = new PixelPainter();
    private final CanvasRenderer canvasRenderer = new CanvasRenderer();
    private final PosterBlockEntity targetEntity;

    private ToolSwitchWidget toolSwitchWidget;
    private SizeSwitcherWidget sizeSwitcherWidget;
    private ColorPaletteWidget colorPaletteWidget;
    private boolean isDrawing = false;

    public PaintScreen(PosterBlockEntity entity) {
        super(Text.empty());
        this.targetEntity = entity;
        this.canvasData.copyFrom(entity.canvasData);
        this.snapshotBeforeEdit.copyFrom(entity.canvasData);
    }

    @Override
    protected void init() {
        super.init();
        dims.calculate(width, height);

        toolSwitchWidget = new ToolSwitchWidget(
                dims.toolSwitchX, dims.toolSwitchY,
                dims.toolSwitchW, dims.toolSwitchH,
                pixelPainter::setTool
        );
        addDrawableChild(toolSwitchWidget);

        sizeSwitcherWidget = new SizeSwitcherWidget(
                dims.sizeSwitchX, dims.sizeSwitchY,
                dims.sizeSwitchW, dims.sizeSwitchH,
                pixelPainter::setSize
        );
        pixelPainter.setSize(sizeSwitcherWidget.getCurrentSize());
        addDrawableChild(sizeSwitcherWidget);

        colorPaletteWidget = new ColorPaletteWidget(
                dims.paletteX, dims.paletteY,
                dims.paletteW, dims.paletteH,
                index -> {
                    pixelPainter.setBlock(index);
                    colorPaletteWidget.setSelectedIndex(index);
                }
        );
        pixelPainter.setBlock(colorPaletteWidget.getSelectedIndex());
        addDrawableChild(colorPaletteWidget);
    }

    @Override
    public void removed() {
        canvasRenderer.close();
        saveToEntity();
        super.removed();
    }

    private void saveToEntity() {
        if (targetEntity == null) return;

        List<CanvasData.PixelChange> changes = canvasData.diff(snapshotBeforeEdit);
        if (changes.isEmpty()) return;

        targetEntity.canvasData.copyFrom(canvasData);
        PosterBlockEntityRenderer.invalidate(targetEntity.getPos());

        if (MinecraftClient.getInstance().getNetworkHandler() != null) {
            ClientPlayNetworking.send(new SaveCanvasC2SPacket(targetEntity.getPos(), changes));
        }
    }

    public boolean isInsideDrawingArea(double mouseX, double mouseY) {
        return mouseX >= dims.drawingAreaX && mouseX < dims.drawingAreaX + dims.drawingAreaSize
                && mouseY >= dims.drawingAreaY && mouseY < dims.drawingAreaY + dims.drawingAreaSize;
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        if (click.button() == 0 && isInsideDrawingArea(click.x(), click.y())) {
            double scale = (double) dims.drawingAreaSize / CanvasData.SIZE;
            if (pixelPainter.beginStroke(canvasData, (int) click.x(), (int) click.y(),
                    dims.drawingAreaX, dims.drawingAreaY, scale)) {
                isDrawing = true;
                return true;
            }
        }
        return super.mouseClicked(click, doubled);
    }

    @Override
    public boolean mouseDragged(Click click, double deltaX, double deltaY) {
        if (isDrawing && click.button() == 0) {
            double scale = (double) dims.drawingAreaSize / CanvasData.SIZE;
            pixelPainter.continueStroke(canvasData, (int) click.x(), (int) click.y(),
                    dims.drawingAreaX, dims.drawingAreaY, scale);
            return true;
        }
        return super.mouseDragged(click, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(Click click) {
        if (isDrawing && click.button() == 0) {
            pixelPainter.endStroke();
            isDrawing = false;
            return true;
        }
        return super.mouseReleased(click);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        dims.calculate(width, height);

        if (toolSwitchWidget != null) {
            toolSwitchWidget.setPosition(dims.toolSwitchX, dims.toolSwitchY);
            toolSwitchWidget.setDimensions(dims.toolSwitchW, dims.toolSwitchH);
        }
        if (sizeSwitcherWidget != null) {
            sizeSwitcherWidget.setPosition(dims.sizeSwitchX, dims.sizeSwitchY);
            sizeSwitcherWidget.setDimensions(dims.sizeSwitchW, dims.sizeSwitchH);
        }
        if (colorPaletteWidget != null) {
            colorPaletteWidget.setPosition(dims.paletteX, dims.paletteY);
            colorPaletteWidget.setDimensions(dims.paletteW, dims.paletteH);
        }

        context.drawTexture(
                RenderPipelines.GUI_TEXTURED,
                FRAME_TEXTURE,
                dims.canvasX, dims.canvasY,
                0.0F, 0.0F,
                dims.canvasSize, dims.canvasSize,
                128, 128,
                128, 128,
                0xFFFFFFFF
        );

        canvasRenderer.update(canvasData);
        canvasRenderer.render(context, dims.drawingAreaX, dims.drawingAreaY, dims.drawingAreaSize);

        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}