package iliiasik.artistry.client.ui.screen;

import com.mojang.blaze3d.platform.InputConstants;
import iliiasik.artistry.block.entity.PosterBlockEntity;
import iliiasik.artistry.client.image.CanvasImageRenderer;
import iliiasik.artistry.client.image.ImageLayerController;
import iliiasik.artistry.client.renderer.CanvasRenderer;
import iliiasik.artistry.client.palette.PaintSwatches;
import iliiasik.artistry.client.renderer.PresenceBadgeRenderer;
import iliiasik.artistry.client.tools.DrawingTool;
import iliiasik.artistry.client.tools.PixelPainter;
import iliiasik.artistry.client.ui.layout.PaintDimensions;
import iliiasik.artistry.client.ui.screen.paint.PaintInput;
import iliiasik.artistry.client.ui.screen.paint.PaintSession;
import iliiasik.artistry.client.ui.screen.paint.PaintWidgets;
import iliiasik.artistry.client.ui.screen.paint.SignatureRenderer;
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
    private final PaintSwatches swatches = new PaintSwatches();
    private final PixelPainter pixelPainter = new PixelPainter(swatches);
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

    public void applySignature(@Nullable String playerName) {
        session.applySignature(playerName);
        if (input != null) input.exitImageModeIfActive();
        rebuildWidgets();
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

        widgets = new PaintWidgets(dims, swatches, pixelPainter,
                input::openFilePicker, input::handleImageAction, session::sign);
        input.setWidgets(widgets);
        widgets.build(this::addRenderableWidget, session.isSigned(), session.canvasData().isSizeChosen());

        session.open();
    }

    @Override
    public void removed() {
        if (input != null) input.exitImageModeIfActive();
        canvasRenderer.close();
        session.onScreenClosed();
        super.removed();
    }

    private boolean editable() {
        return !session.isSigned();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (editable() && input != null && input.mouseClicked(mouseX, mouseY, button)) return true;
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (editable() && input != null && input.mouseDragged(mouseX, mouseY, button)) return true;
        if (widgets != null && widgets.mouseDragged(mouseX, mouseY, button, deltaX, deltaY)) return true;
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    private boolean shortcutsAllowed() {
        return editable() && input != null && (widgets == null || !widgets.isTextFieldFocused());
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (shortcutsAllowed() && handleShortcut(keyCode)) return true;
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private boolean handleShortcut(int keyCode) {
        if (keyCode == InputConstants.KEY_Z && hasControlDown()) {
            return hasShiftDown() ? session.redo() : session.undo();
        }
        if (hasControlDown()) return false;

        return switch (keyCode) {
            case InputConstants.KEY_B -> input.selectTool(DrawingTool.BRUSH);
            case InputConstants.KEY_E -> input.selectTool(DrawingTool.ERASER);
            case InputConstants.KEY_P -> input.selectTool(DrawingTool.PIPETTE);
            case InputConstants.KEY_X -> swapSwatches();
            case InputConstants.KEY_LALT, InputConstants.KEY_RALT -> input.beginTemporaryPipette();
            case InputConstants.KEY_LBRACKET -> widgets != null && widgets.stepBrushSize(-1);
            case InputConstants.KEY_RBRACKET -> widgets != null && widgets.stepBrushSize(1);
            case InputConstants.KEY_DELETE -> input.deleteSelectedImage();
            case InputConstants.KEY_LEFT -> input.nudgeSelectedImage(-1, 0);
            case InputConstants.KEY_RIGHT -> input.nudgeSelectedImage(1, 0);
            case InputConstants.KEY_UP -> input.nudgeSelectedImage(0, -1);
            case InputConstants.KEY_DOWN -> input.nudgeSelectedImage(0, 1);
            default -> false;
        };
    }

    private boolean swapSwatches() {
        if (widgets == null) return false;
        widgets.swapSwatches();
        return true;
    }

    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        if ((keyCode == InputConstants.KEY_LALT || keyCode == InputConstants.KEY_RALT)
                && input != null && input.endTemporaryPipette()) {
            return true;
        }
        return super.keyReleased(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (shortcutsAllowed() && !input.isImageMode() && scrollY != 0
                && widgets != null && widgets.stepBrushSize(scrollY > 0 ? 1 : -1)) {
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (editable() && input != null && input.mouseReleased(button)) return true;
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
        if (!session.isWorld() || session.isSigned()) return;
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

        if (editable() && input != null) input.renderHoverPreview(context, hoverMouseX, hoverMouseY);

        super.render(context, mouseX, mouseY, delta);

        String signerName = session.signerName();
        if (signerName != null) {
            SignatureRenderer.render(context, this.font, signerName, dims);
            return;
        }

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
    public void renderBackground(GuiGraphics context, int mouseX, int mouseY, float delta) {}
}