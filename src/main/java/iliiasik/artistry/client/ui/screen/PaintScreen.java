package iliiasik.artistry.client.ui.screen;

import iliiasik.artistry.block.entity.PosterBlockEntity;
import iliiasik.artistry.client.image.CanvasImageRenderer;
import iliiasik.artistry.client.image.ImageLayerController;
import iliiasik.artistry.client.renderer.CanvasRenderer;
import iliiasik.artistry.client.renderer.PresenceBadgeRenderer;
import iliiasik.artistry.client.tools.PixelPainter;
import iliiasik.artistry.client.ui.layout.PaintDimensions;
import iliiasik.artistry.client.ui.screen.paint.PaintInput;
import iliiasik.artistry.client.ui.screen.paint.PaintSession;
import iliiasik.artistry.client.ui.screen.paint.PaintWidgets;
import iliiasik.artistry.client.util.ModTextures;
import iliiasik.artistry.data.CanvasData;
import iliiasik.artistry.data.CanvasImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

public class PaintScreen extends Screen {

    private final PaintDimensions dims = new PaintDimensions();
    private final PaintSession session;
    private final PixelPainter pixelPainter = new PixelPainter();
    private final CanvasRenderer canvasRenderer = new CanvasRenderer();
    private final ImageLayerController imageController;

    private PaintWidgets widgets;
    private PaintInput input;

    private UUID localPlayerUuid;
    private boolean pendingClose = false;

    private int hoverMouseX = -1;
    private int hoverMouseY = -1;

    public PaintScreen(PosterBlockEntity entity) {
        super(Component.empty());
        this.session = new PaintSession(entity);
        this.imageController = new ImageLayerController(session.imageLayer());
    }

    public PaintScreen(ItemStack stack, InteractionHand hand) {
        this(stack, hand, 0);
    }

    public PaintScreen(ItemStack stack, InteractionHand hand, int chosenSize) {
        super(Component.empty());
        this.session = new PaintSession(stack, hand, chosenSize);
        this.imageController = new ImageLayerController(session.imageLayer());
    }

    public BlockPos getTargetPos() {
        return session.targetPos();
    }

    public void applyRemoteChanges(List<CanvasData.PixelChange> changes) {
        session.applyRemoteChanges(changes);
    }

    public void applyImageLayerSync(List<CanvasImage> images) {
        session.applyImageLayerSync(images);
    }

    public void applyImageLockSync(UUID imageUuid, @Nullable UUID playerUuid) {
        session.applyImageLockSync(imageUuid, playerUuid);
    }

    public void receiveImageBytes(UUID uuid, byte[] bytes) {
        session.receiveImageBytes(uuid, bytes);
    }

    public void onImageUploaded(UUID uuid, int gridX, int gridY, int gridW, int gridH) {
        session.onImageUploaded(uuid, gridX, gridY, gridW, gridH);
    }

    public void receiveCursor(UUID uuid, float gx, float gy) {
        session.receiveCursor(uuid, gx, gy);
    }

    public void removePresence(UUID uuid) {
        session.removePresence(uuid);
    }

    public void scheduledClose() {
        pendingClose = true;
    }

    @Override
    protected void init() {
        super.init();
        dims.calculate(width, height);

        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) localPlayerUuid = mc.player.getUUID();

        if (input == null) {
            input = new PaintInput(session, dims, pixelPainter, imageController);
        }
        input.setLocalPlayer(localPlayerUuid);

        widgets = new PaintWidgets(dims, pixelPainter, input::openFilePicker, input::handleImageAction);
        input.setWidgets(widgets);
        widgets.build(this::addRenderableWidget);

        session.open();
    }

    @Override
    public void removed() {
        if (input != null) input.exitImageModeIfActive();
        canvasRenderer.close();
        session.onScreenClosed();
        super.removed();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (input != null && input.mouseClicked(mouseX, mouseY, button)) return true;
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (input != null && input.mouseDragged(mouseX, mouseY, button)) return true;
        if (widgets != null && widgets.mouseDragged(mouseX, mouseY, button, deltaX, deltaY)) return true;
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (input != null && input.mouseReleased(button)) return true;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        hoverMouseX = (int) mouseX;
        hoverMouseY = (int) mouseY;
        super.mouseMoved(mouseX, mouseY);
    }

    private boolean isInsideDrawingArea(double mouseX, double mouseY) {
        return mouseX >= dims.drawingAreaX && mouseX < dims.drawingAreaX + dims.drawingAreaSize
                && mouseY >= dims.drawingAreaY && mouseY < dims.drawingAreaY + dims.drawingAreaSize;
    }

    private void tickCursor() {
        if (!session.isWorld()) return;
        CanvasData canvasData = session.canvasData();
        if (!canvasData.isSizeChosen()) return;
        if (!isInsideDrawingArea(hoverMouseX, hoverMouseY)) return;
        double ps = (double) dims.drawingAreaSize / canvasData.canvasSize;
        double gx = Mth.clamp((hoverMouseX - dims.drawingAreaX) / ps, 0.0, canvasData.canvasSize);
        double gy = Mth.clamp((hoverMouseY - dims.drawingAreaY) / ps, 0.0, canvasData.canvasSize);
        session.maybeSendCursor((short) Math.round(gx * 16), (short) Math.round(gy * 16));
    }

    @Override
    public void render(GuiGraphics context, int mouseX, int mouseY, float delta) {
        if (pendingClose) {
            Minecraft.getInstance().setScreen(null);
            return;
        }

        session.tickBatch();
        tickCursor();
        dims.calculate(width, height);
        if (widgets != null) widgets.layout();

        context.blit(
                ModTextures.FRAME,
                dims.canvasX, dims.canvasY,
                dims.canvasSize, dims.canvasSize,
                0.0F, 0.0F,
                128, 128,
                128, 128
        );

        CanvasData canvasData = session.canvasData();
        canvasRenderer.update(canvasData);
        canvasRenderer.render(context, dims.drawingAreaX, dims.drawingAreaY, dims.drawingAreaSize);

        if (canvasData.isSizeChosen()) {
            CanvasImageRenderer.renderAll(context, session.imageLayer().getImages(),
                    dims.drawingAreaX, dims.drawingAreaY, dims.drawingAreaSize, canvasData.canvasSize,
                    imageController.getSelectedUuid(), localPlayerUuid);
        }

        if (input != null) input.renderHoverPreview(context, hoverMouseX, hoverMouseY);

        super.render(context, mouseX, mouseY, delta);

        if (canvasData.isSizeChosen()) {
            session.presence().interpolate(0.35f);
            PresenceBadgeRenderer.renderAll(context, session.presence(), session.imageLayer().getImages(),
                    dims.drawingAreaX, dims.drawingAreaY, dims.drawingAreaSize, canvasData.canvasSize,
                    localPlayerUuid);
        }
    }

    @Override
    public boolean isPauseScreen() { return false; }

    @Override
    public void renderBackground(GuiGraphics graphics) {}
}