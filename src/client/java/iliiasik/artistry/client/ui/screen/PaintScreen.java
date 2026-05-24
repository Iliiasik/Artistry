package iliiasik.artistry.client.ui.screen;

import iliiasik.artistry.client.tools.DrawingTool;
import iliiasik.artistry.client.tools.PixelPainter;
import iliiasik.artistry.client.ui.layout.PaintDimensions;
import iliiasik.artistry.client.renderer.CanvasRenderer;
import iliiasik.artistry.client.ui.widget.PaletteSwitcherWidget;
import iliiasik.artistry.client.util.ModTextures;
import iliiasik.artistry.client.ui.widget.ColorPaletteWidget;
import iliiasik.artistry.client.ui.widget.SizeSwitcherWidget;
import iliiasik.artistry.client.ui.widget.ToolSwitchWidget;
import iliiasik.artistry.block.entity.PosterBlockEntity;
import iliiasik.artistry.data.CanvasData;
import iliiasik.artistry.network.SaveCanvasC2SPacket;
import iliiasik.artistry.network.SaveItemCanvasC2SPacket;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;

import java.util.List;

public class PaintScreen extends Screen {

    private static final long BATCH_INTERVAL_MS =
            iliiasik.artistry.config.ArtistryConfig.get().network.batchIntervalMs;

    private final PaintDimensions dims = new PaintDimensions();
    private final CanvasData canvasData = new CanvasData();
    private final CanvasData lastSentSnapshot = new CanvasData();
    private final PixelPainter pixelPainter = new PixelPainter();
    private final CanvasRenderer canvasRenderer = new CanvasRenderer();

    private final PosterBlockEntity targetEntity;
    private final ItemStack targetStack;
    private final Hand targetHand;

    private ToolSwitchWidget toolSwitchWidget;
    private SizeSwitcherWidget sizeSwitcherWidget;
    private ColorPaletteWidget colorPaletteWidget;
    private PaletteSwitcherWidget paletteSwitcherWidget;
    private boolean isDrawing = false;

    private long lastFlushTime = 0;
    private boolean pendingClose = false;

    private int hoverMouseX = -1;
    private int hoverMouseY = -1;

    public PaintScreen(PosterBlockEntity entity) {
        super(Text.empty());
        this.targetEntity = entity;
        this.targetStack = null;
        this.targetHand = null;
        this.canvasData.copyFrom(entity.canvasData);
        this.lastSentSnapshot.copyFrom(entity.canvasData);
    }

    public PaintScreen(ItemStack stack, Hand hand) {
        this(stack, hand, 0);
    }

    public PaintScreen(ItemStack stack, Hand hand, int chosenSize) {
        super(Text.empty());
        this.targetEntity = null;
        this.targetStack = stack;
        this.targetHand = hand;
        NbtComponent comp = stack.get(DataComponentTypes.CUSTOM_DATA);
        if (comp != null) {
            NbtCompound nbt = comp.copyNbt();
            if (nbt.contains("canvas")) {
                canvasData.fromNbt(nbt.getCompound("canvas"));
            }
        }
        if (chosenSize > 0) {
            canvasData.canvasSize = chosenSize;
        }
        this.lastSentSnapshot.copyFrom(this.canvasData);
    }

    public BlockPos getTargetPos() {
        return targetEntity != null ? targetEntity.getPos() : null;
    }

    public void applyRemoteChanges(List<CanvasData.PixelChange> changes) {
        for (CanvasData.PixelChange c : changes) {
            int x = c.x() & 0xFF;
            int y = c.y() & 0xFF;
            canvasData.pixels[y][x] = c.blockIndex();
            canvasData.colors[y][x] = c.color();
            lastSentSnapshot.pixels[y][x] = c.blockIndex();
            lastSentSnapshot.colors[y][x] = c.color();
        }
    }

    public void scheduledClose() {
        pendingClose = true;
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
                new ColorPaletteWidget.SelectionListener() {
                    @Override
                    public void onBlockSelected(int blockIndex) {
                        pixelPainter.setBlock(blockIndex);
                        if (pixelPainter.getTool() == DrawingTool.ERASER) {
                            pixelPainter.setTool(DrawingTool.BRUSH);
                            toolSwitchWidget.setActiveTool(DrawingTool.BRUSH);
                        }
                    }
                    @Override
                    public void onColorSelected(int argbColor) {
                        pixelPainter.setColor(argbColor);
                        if (pixelPainter.getTool() == DrawingTool.ERASER) {
                            pixelPainter.setTool(DrawingTool.BRUSH);
                            toolSwitchWidget.setActiveTool(DrawingTool.BRUSH);
                        }
                    }
                }
        );
        pixelPainter.setBlock(colorPaletteWidget.getSelectedIndex());
        addDrawableChild(colorPaletteWidget);

        paletteSwitcherWidget = new PaletteSwitcherWidget(
                dims.paletteSwitcherX, dims.paletteSwitcherY,
                dims.paletteSwitcherW, dims.paletteSwitcherH,
                mode -> colorPaletteWidget.setMode(mode)
        );
        addDrawableChild(paletteSwitcherWidget);
    }

    @Override
    public void removed() {
        canvasRenderer.close();
        flushToServer();
        if (targetStack != null) {
            saveToItem();
        }
        super.removed();
    }

    private void flushToServer() {
        if (targetEntity == null) return;
        List<CanvasData.PixelChange> changes = canvasData.diff(lastSentSnapshot);
        if (changes.isEmpty()) return;
        lastSentSnapshot.copyFrom(canvasData);
        lastFlushTime = System.currentTimeMillis();
        if (MinecraftClient.getInstance().getNetworkHandler() != null) {
            ClientPlayNetworking.send(new SaveCanvasC2SPacket(targetEntity.getPos(), changes));
        }
    }

    private void saveToItem() {
        List<CanvasData.PixelChange> changes = canvasData.diff(lastSentSnapshot);
        if (changes.isEmpty()) return;
        NbtComponent comp = targetStack.get(DataComponentTypes.CUSTOM_DATA);
        NbtCompound tag = comp != null ? comp.copyNbt() : new NbtCompound();
        tag.put("canvas", canvasData.toNbt());
        targetStack.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(tag));
        if (MinecraftClient.getInstance().getNetworkHandler() != null) {
            ClientPlayNetworking.send(new SaveItemCanvasC2SPacket(targetHand, changes));
        }
    }

    private void tickBatch() {
        if (targetEntity == null) return;
        long now = System.currentTimeMillis();
        if (now - lastFlushTime >= BATCH_INTERVAL_MS) {
            flushToServer();
        }
    }

    public boolean isInsideDrawingArea(double mouseX, double mouseY) {
        return mouseX >= dims.drawingAreaX && mouseX < dims.drawingAreaX + dims.drawingAreaSize
                && mouseY >= dims.drawingAreaY && mouseY < dims.drawingAreaY + dims.drawingAreaSize;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && isInsideDrawingArea(mouseX, mouseY)) {
            double scale = (double) dims.drawingAreaSize / canvasData.canvasSize;
            if (pixelPainter.getTool() == DrawingTool.PIPETTE) {
                int[] picked = pixelPainter.pickPixel(canvasData, (int) mouseX, (int) mouseY,
                        dims.drawingAreaX, dims.drawingAreaY, scale);
                if (picked != null) {
                    int blockIndex = picked[0];
                    int color = picked[1];
                    if (color != 0) {
                        colorPaletteWidget.selectColor(color);
                        pixelPainter.setColor(color);
                    } else if (blockIndex > 0) {
                        colorPaletteWidget.selectBlock(blockIndex);
                        pixelPainter.setBlock(blockIndex);
                    }
                    pixelPainter.setTool(DrawingTool.BRUSH);
                    toolSwitchWidget.setActiveTool(DrawingTool.BRUSH);
                }
                return true;
            }
            if (pixelPainter.beginStroke(canvasData, (int) mouseX, (int) mouseY,
                    dims.drawingAreaX, dims.drawingAreaY, scale)) {
                isDrawing = true;
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (isDrawing && button == 0) {
            double scale = (double) dims.drawingAreaSize / canvasData.canvasSize;
            pixelPainter.continueStroke(canvasData, (int) mouseX, (int) mouseY,
                    dims.drawingAreaX, dims.drawingAreaY, scale);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (isDrawing && button == 0) {
            pixelPainter.endStroke();
            isDrawing = false;
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        hoverMouseX = (int) mouseX;
        hoverMouseY = (int) mouseY;
        super.mouseMoved(mouseX, mouseY);
    }

    private void renderHoverHighlight(DrawContext context) {
        if (!canvasData.isSizeChosen()) return;
        if (!isInsideDrawingArea(hoverMouseX, hoverMouseY)) return;
        if (pixelPainter.getTool() == DrawingTool.PIPETTE) return;

        int canvasSize = canvasData.canvasSize;
        double scale = (double) dims.drawingAreaSize / canvasSize;

        int[] bounds = pixelPainter.getBrushGridBounds(
                hoverMouseX, hoverMouseY,
                dims.drawingAreaX, dims.drawingAreaY,
                scale, canvasSize
        );

        int px0 = dims.drawingAreaX + (int)(bounds[0] * scale);
        int py0 = dims.drawingAreaY + (int)(bounds[1] * scale);
        int px1 = dims.drawingAreaX + (int)((bounds[2] + 1) * scale);
        int py1 = dims.drawingAreaY + (int)((bounds[3] + 1) * scale);

        context.fill(px0, py0, px1, py1, 0x40000000);
        context.fill(px0, py0, px1, py1, 0x18FFFFFF);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        if (pendingClose) {
            MinecraftClient.getInstance().setScreen(null);
            return;
        }

        tickBatch();
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
        if (paletteSwitcherWidget != null) {
            paletteSwitcherWidget.setPosition(dims.paletteSwitcherX, dims.paletteSwitcherY);
            paletteSwitcherWidget.setDimensions(dims.paletteSwitcherW, dims.paletteSwitcherH);
        }

        context.drawTexture(
                ModTextures.FRAME,
                dims.canvasX, dims.canvasY,
                dims.canvasSize, dims.canvasSize,
                0.0F, 0.0F,
                128, 128,
                128, 128
        );

        canvasRenderer.update(canvasData);
        canvasRenderer.render(context, dims.drawingAreaX, dims.drawingAreaY, dims.drawingAreaSize);

        renderHoverHighlight(context);

        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
    }
}