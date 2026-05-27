package iliiasik.artistry.client.ui.screen;

import iliiasik.artistry.client.image.CanvasImageRenderer;
import iliiasik.artistry.client.image.ClientImageCache;
import iliiasik.artistry.client.image.ImageLayerController;
import iliiasik.artistry.client.palette.ColorPalette;
import iliiasik.artistry.client.tools.DrawingTool;
import iliiasik.artistry.client.tools.PixelPainter;
import iliiasik.artistry.client.ui.layout.PaintDimensions;
import iliiasik.artistry.client.renderer.CanvasRenderer;
import iliiasik.artistry.client.ui.widget.HexInputWidget;
import iliiasik.artistry.client.ui.widget.ImageToolWidget;
import iliiasik.artistry.client.ui.widget.PaletteSwitcherWidget;
import iliiasik.artistry.client.util.ModTextures;
import iliiasik.artistry.client.ui.widget.ColorPaletteWidget;
import iliiasik.artistry.client.ui.widget.SizeSwitcherWidget;
import iliiasik.artistry.client.ui.widget.ToolSwitchWidget;
import iliiasik.artistry.block.entity.PosterBlockEntity;
import iliiasik.artistry.data.CanvasData;
import iliiasik.artistry.data.CanvasImage;
import iliiasik.artistry.data.CanvasImageLayer;
import iliiasik.artistry.network.*;
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

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.UUID;

public class PaintScreen extends Screen {

    private static final long BATCH_INTERVAL_MS =
            iliiasik.artistry.config.ArtistryConfig.get().network.batchIntervalMs;

    private final PaintDimensions dims = new PaintDimensions();
    private final CanvasData canvasData = new CanvasData();
    private final CanvasData lastSentSnapshot = new CanvasData();
    private final PixelPainter pixelPainter = new PixelPainter();
    private final CanvasRenderer canvasRenderer = new CanvasRenderer();
    private final CanvasImageLayer imageLayer = new CanvasImageLayer();
    private final ImageLayerController imageController = new ImageLayerController(imageLayer);

    private final PosterBlockEntity targetEntity;
    private final ItemStack targetStack;
    private final Hand targetHand;

    private UUID localPlayerUuid;

    private ToolSwitchWidget toolSwitchWidget;
    private SizeSwitcherWidget sizeSwitcherWidget;
    private ColorPaletteWidget colorPaletteWidget;
    private PaletteSwitcherWidget paletteSwitcherWidget;
    private HexInputWidget hexInput;
    private ImageToolWidget imageToolWidget;

    private boolean isDrawing = false;
    private boolean updatingHexFromPalette = false;
    private boolean imageMode = false;
    private boolean imageDragging = false;
    private boolean pendingImageSync = false;

    private long lastFlushTime = 0;
    private long lastImageSyncTime = 0;
    private static final long IMAGE_SYNC_INTERVAL_MS = 150;

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
        this.imageLayer.copyFrom(entity.imageLayer);
        requestMissingImages();
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
            if (nbt.contains("images")) {
                imageLayer.fromNbt(nbt.getList("images", net.minecraft.nbt.NbtList.COMPOUND_TYPE));
            }
        }
        if (chosenSize > 0) {
            canvasData.canvasSize = chosenSize;
        }
        this.lastSentSnapshot.copyFrom(this.canvasData);
        requestMissingImages();
    }

    private void requestMissingImages() {
        for (CanvasImage img : imageLayer.getImages()) {
            if (!ClientImageCache.has(img.uuid)) {
                ClientPlayNetworking.send(new RequestImageC2SPacket(img.uuid));
            }
        }
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

    public void applyImageLayerSync(List<CanvasImage> images) {
        imageLayer.getImages().clear();
        for (CanvasImage img : images) {
            imageLayer.addImage(img);
        }
        requestMissingImages();
    }

    public void applyImageLockSync(UUID imageUuid, UUID playerUuid) {
        CanvasImage img = imageLayer.findByUuid(imageUuid);
        if (img != null) img.lockedByPlayer = playerUuid;
    }

    public void receiveImageBytes(UUID uuid, byte[] bytes) {
        ClientImageCache.store(uuid, bytes);
    }

    public void onImageUploaded(UUID uuid, int gridX, int gridY, int gridW, int gridH) {
        CanvasImage img = new CanvasImage(uuid, gridX, gridY, gridW, gridH);
        imageLayer.addImage(img);
        imageController.clearSelection();
        enterImageMode(uuid);
    }

    public void scheduledClose() {
        pendingClose = true;
    }

    private void enterImageMode(UUID uuid) {
        imageMode = true;
        if (toolSwitchWidget != null) toolSwitchWidget.setVisible(false);
        if (sizeSwitcherWidget != null) sizeSwitcherWidget.setVisible(false);
        if (imageToolWidget != null) imageToolWidget.setVisible(true);
        if (targetEntity != null) {
            ClientPlayNetworking.send(new LockCanvasImageC2SPacket(targetEntity.getPos(), uuid, true));
        }
    }

    private void exitImageMode() {
        if (imageMode && targetEntity != null) {
            UUID sel = imageController.getSelectedUuid();
            if (sel != null) {
                ClientPlayNetworking.send(new LockCanvasImageC2SPacket(targetEntity.getPos(), sel, false));
            }
        }
        imageMode = false;
        imageController.clearSelection();
        if (toolSwitchWidget != null) toolSwitchWidget.setVisible(true);
        if (sizeSwitcherWidget != null) sizeSwitcherWidget.setVisible(true);
        if (imageToolWidget != null) imageToolWidget.setVisible(false);
    }

    @Override
    protected void init() {
        super.init();
        dims.calculate(width, height);

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player != null) localPlayerUuid = mc.player.getUuid();

        toolSwitchWidget = new ToolSwitchWidget(
                dims.toolSwitchX, dims.toolSwitchY,
                dims.toolSwitchW, dims.toolSwitchH,
                tool -> {
                    if (tool == DrawingTool.IMAGE) {
                        openFilePicker();
                    } else {
                        pixelPainter.setTool(tool);
                    }
                }
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
        addDrawableChild(colorPaletteWidget);

        hexInput = new HexInputWidget(0, 0, 1, 1);
        hexInput.setText("#FF0000");
        hexInput.setChangedListener(text -> {
            if (updatingHexFromPalette) return;
            if (text.length() == 7) {
                colorPaletteWidget.setColorFromHex(text);
            }
        });
        hexInput.setVisible(false);
        addDrawableChild(hexInput);

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
        addDrawableChild(paletteSwitcherWidget);

        imageToolWidget = new ImageToolWidget(
                dims.toolSwitchX, dims.toolSwitchY,
                dims.toolSwitchW, dims.toolSwitchH,
                action -> {
                    UUID sel = imageController.getSelectedUuid();
                    if (sel == null || targetEntity == null) return;
                    if (action == ImageToolWidget.Action.DELETE) {
                        imageLayer.removeImage(sel);
                        ClientPlayNetworking.send(new DeleteCanvasImageC2SPacket(targetEntity.getPos(), sel));
                        exitImageMode();
                    } else if (action == ImageToolWidget.Action.PIXELIZE) {
                        CanvasImage img = imageLayer.findByUuid(sel);
                        if (img != null) {
                            img.pixelized = !img.pixelized;
                            if (img.pixelized) {
                                ClientImageCache.rebuildPixelizedTexture(sel, img.gridW, img.gridH);
                            }
                        }
                        ClientPlayNetworking.send(new TogglePixelizeC2SPacket(targetEntity.getPos(), sel));
                    }
                }
        );
        imageToolWidget.setVisible(false);
        addDrawableChild(imageToolWidget);
    }

    private void openFilePicker() {
        new Thread(() -> {
            org.lwjgl.PointerBuffer filters = org.lwjgl.BufferUtils.createPointerBuffer(4);
            filters.put(org.lwjgl.system.MemoryUtil.memASCII("*.png"));
            filters.put(org.lwjgl.system.MemoryUtil.memASCII("*.jpg"));
            filters.put(org.lwjgl.system.MemoryUtil.memASCII("*.jpeg"));
            filters.put(org.lwjgl.system.MemoryUtil.memASCII("*.bmp"));
            filters.flip();

            String path = org.lwjgl.util.tinyfd.TinyFileDialogs.tinyfd_openFileDialog(
                    "Select Image", "", filters, "Image Files", false);

            if (path != null) {
                try {
                    byte[] bytes = convertToPng(new File(path));
                    if (targetEntity != null) {
                        ClientPlayNetworking.send(new UploadImageC2SPacket(targetEntity.getPos(), bytes));
                    } else if (targetStack != null && targetHand != null) {
                        ClientPlayNetworking.send(new UploadItemImageC2SPacket(targetHand, bytes));
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (toolSwitchWidget != null) {
                toolSwitchWidget.setActiveTool(DrawingTool.BRUSH);
            }
            pixelPainter.setTool(DrawingTool.BRUSH);
        }, "artistry-file-picker").start();
    }

    private byte[] convertToPng(File file) throws IOException {
        BufferedImage img = ImageIO.read(file);
        if (img == null) throw new IOException("Cannot read image: " + file.getName());
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(img, "png", out);
        return out.toByteArray();
    }

    @Override
    public void removed() {
        if (imageMode) exitImageMode();
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

    private void flushImageMove() {
        if (targetEntity == null) return;
        UUID sel = imageController.getSelectedUuid();
        if (sel == null) return;
        CanvasImage img = imageLayer.findByUuid(sel);
        if (img == null) return;
        if (img.pixelized) {
            ClientImageCache.rebuildPixelizedTexture(sel, img.gridW, img.gridH);
        }
        ClientPlayNetworking.send(new MoveCanvasImageC2SPacket(
                targetEntity.getPos(), sel, img.gridX, img.gridY, img.gridW, img.gridH));
        pendingImageSync = false;
        lastImageSyncTime = System.currentTimeMillis();
    }

    private void saveToItem() {
        NbtComponent comp = targetStack.get(DataComponentTypes.CUSTOM_DATA);
        NbtCompound tag = comp != null ? comp.copyNbt() : new NbtCompound();
        List<CanvasData.PixelChange> changes = canvasData.diff(lastSentSnapshot);
        if (!changes.isEmpty()) {
            tag.put("canvas", canvasData.toNbt());
        }
        tag.put("images", imageLayer.toNbt());
        targetStack.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(tag));
        if (!changes.isEmpty() && MinecraftClient.getInstance().getNetworkHandler() != null) {
            ClientPlayNetworking.send(new SaveItemCanvasC2SPacket(targetHand, changes));
        }
    }

    private void tickBatch() {
        if (targetEntity == null) return;
        long now = System.currentTimeMillis();
        if (now - lastFlushTime >= BATCH_INTERVAL_MS) {
            flushToServer();
        }
        if (pendingImageSync && now - lastImageSyncTime >= IMAGE_SYNC_INTERVAL_MS) {
            flushImageMove();
        }
    }

    public boolean isInsideDrawingArea(double mouseX, double mouseY) {
        return mouseX >= dims.drawingAreaX && mouseX < dims.drawingAreaX + dims.drawingAreaSize
                && mouseY >= dims.drawingAreaY && mouseY < dims.drawingAreaY + dims.drawingAreaSize;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && isInsideDrawingArea(mouseX, mouseY)) {
            if (imageMode) {
                boolean hit = imageController.trySelect(mouseX, mouseY,
                        dims.drawingAreaX, dims.drawingAreaY, dims.drawingAreaSize, canvasData.canvasSize);
                if (!hit) {
                    exitImageMode();
                }
                imageDragging = hit;
                return true;
            }

            if (!imageLayer.getImages().isEmpty() &&
                    imageController.isOnImage(mouseX, mouseY,
                            dims.drawingAreaX, dims.drawingAreaY, dims.drawingAreaSize, canvasData.canvasSize)) {
                CanvasImage hovered = getHoveredUnlockedImage(mouseX, mouseY);
                if (hovered != null) {
                    boolean hit = imageController.trySelect(mouseX, mouseY,
                            dims.drawingAreaX, dims.drawingAreaY, dims.drawingAreaSize, canvasData.canvasSize);
                    if (hit) {
                        enterImageMode(imageController.getSelectedUuid());
                        imageDragging = true;
                        return true;
                    }
                }
            }

            double scale = (double) dims.drawingAreaSize / canvasData.canvasSize;
            if (pixelPainter.getTool() == DrawingTool.PIPETTE) {
                int[] picked = pixelPainter.pickPixel(canvasData, (int) mouseX, (int) mouseY,
                        dims.drawingAreaX, dims.drawingAreaY, scale);
                if (picked != null) {
                    int blockIndex = picked[0];
                    int color = picked[1];
                    if (color != 0) {
                        colorPaletteWidget.selectColor(color);
                        paletteSwitcherWidget.setMode(PaletteSwitcherWidget.PaletteMode.COLORS);
                        pixelPainter.setColor(color);
                        updatingHexFromPalette = true;
                        hexInput.setText(ColorPalette.argbToHex(color));
                        updatingHexFromPalette = false;
                        hexInput.setVisible(true);
                    } else if (blockIndex > 0) {
                        colorPaletteWidget.selectBlock(blockIndex);
                        paletteSwitcherWidget.setMode(PaletteSwitcherWidget.PaletteMode.BLOCKS);
                        pixelPainter.setBlock(blockIndex);
                        hexInput.setVisible(false);
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

    private CanvasImage getHoveredUnlockedImage(double mouseX, double mouseY) {
        double pixelSize = (double) dims.drawingAreaSize / canvasData.canvasSize;
        List<CanvasImage> images = imageLayer.getImages();
        for (int i = images.size() - 1; i >= 0; i--) {
            CanvasImage img = images.get(i);
            if (localPlayerUuid != null && img.isLockedByOther(localPlayerUuid)) continue;
            int sx = dims.drawingAreaX + (int)(img.gridX * pixelSize);
            int sy = dims.drawingAreaY + (int)(img.gridY * pixelSize);
            int sw = (int)(img.gridW * pixelSize);
            int sh = (int)(img.gridH * pixelSize);
            if (mouseX >= sx && mouseX < sx + sw && mouseY >= sy && mouseY < sy + sh) return img;
        }
        return null;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (imageDragging && button == 0) {
            imageController.onDrag(mouseX, mouseY,
                    dims.drawingAreaX, dims.drawingAreaY, dims.drawingAreaSize, canvasData.canvasSize,
                    () -> pendingImageSync = true);
            return true;
        }
        if (isDrawing && button == 0) {
            double scale = (double) dims.drawingAreaSize / canvasData.canvasSize;
            pixelPainter.continueStroke(canvasData, (int) mouseX, (int) mouseY,
                    dims.drawingAreaX, dims.drawingAreaY, scale);
            return true;
        }
        if (button == 0 && hexInput != null && hexInput.isVisible() && hexInput.isFocused()) {
            if (hexInput.mouseDragged(mouseX, mouseY, button, deltaX, deltaY)) return true;
        }
        if (button == 0 && colorPaletteWidget != null) {
            if (colorPaletteWidget.mouseDragged(mouseX, mouseY, button, deltaX, deltaY)) return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (imageDragging && button == 0) {
            imageController.endDrag();
            imageDragging = false;
            flushImageMove();
            return true;
        }
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

    private boolean isHoveringLockedImage() {
        if (localPlayerUuid == null) return false;
        return imageController.isOnLockedByOtherImage(hoverMouseX, hoverMouseY,
                dims.drawingAreaX, dims.drawingAreaY, dims.drawingAreaSize, canvasData.canvasSize,
                localPlayerUuid);
    }

    private void renderHoverHighlight(DrawContext context) {
        if (!canvasData.isSizeChosen()) return;
        if (imageMode) return;
        if (!isInsideDrawingArea(hoverMouseX, hoverMouseY)) return;
        if (pixelPainter.getTool() == DrawingTool.PIPETTE) return;
        if (!imageLayer.getImages().isEmpty()) {
            CanvasImage hov = getHoveredUnlockedImage(hoverMouseX, hoverMouseY);
            if (hov != null) return;
        }

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

    private void updateHexInputBounds() {
        if (hexInput == null) return;
        hexInput.setX(dims.hexInputX);
        hexInput.setY(dims.hexInputY);
        hexInput.setWidth(dims.hexInputW);
        hexInput.setHeight(dims.hexInputH);
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
        if (imageToolWidget != null) {
            imageToolWidget.setPosition(dims.toolSwitchX, dims.toolSwitchY);
            imageToolWidget.setDimensions(dims.toolSwitchW, dims.toolSwitchH);
        }

        updateHexInputBounds();

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

        if (canvasData.isSizeChosen()) {
            CanvasImageRenderer.renderAll(context, imageLayer.getImages(),
                    dims.drawingAreaX, dims.drawingAreaY, dims.drawingAreaSize, canvasData.canvasSize,
                    imageController.getSelectedUuid(), localPlayerUuid);
        }

        renderHoverHighlight(context);

        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean shouldPause() { return false; }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {}
}