package iliiasik.artistry.client.ui.screen.paint;

import iliiasik.artistry.client.image.ImageLayerController;
import iliiasik.artistry.client.palette.PaintSwatches;
import iliiasik.artistry.client.tools.PixelPainter;
import iliiasik.artistry.client.ui.layout.PaintDimensions;
import iliiasik.artistry.data.CanvasImage;
import iliiasik.artistry.network.LockCanvasImageC2SPacket;
import iliiasik.artistry.network.MoveCanvasImageC2SPacket;
import iliiasik.artistry.support.McFixture;
import iliiasik.artistry.support.TestNetworkHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PaintInputTest {

    private static final int CANVAS_SIZE = 32;
    private static final int DRAW_SIZE = 320;
    private static final int PIXEL = DRAW_SIZE / CANVAS_SIZE;
    private static final UUID ME = UUID.fromString("00000000-0000-0000-0000-00000000000a");
    private static final UUID OTHER = UUID.fromString("00000000-0000-0000-0000-00000000000b");

    private PaintSession session;
    private ImageLayerController controller;
    private PaintInput input;
    private CanvasImage first;
    private CanvasImage second;

    @BeforeAll
    static void boot() {
        McFixture.bootstrap();
    }

    @BeforeEach
    void setUp() {
        TestNetworkHelper.reset();
        session = new PaintSession(ItemStack.EMPTY, InteractionHand.MAIN_HAND, CANVAS_SIZE);
        first = new CanvasImage(UUID.randomUUID(), 0, 0, 5, 5);
        second = new CanvasImage(UUID.randomUUID(), 20, 20, 5, 5);
        session.imageLayer().addImage(first);
        session.imageLayer().addImage(second);

        PaintDimensions dims = new PaintDimensions();
        dims.drawingAreaX = 0;
        dims.drawingAreaY = 0;
        dims.drawingAreaSize = DRAW_SIZE;

        controller = new ImageLayerController(session.imageLayer());
        input = new PaintInput(session, dims, new PixelPainter(new PaintSwatches()), controller);
        input.setLocalPlayer(ME);
    }

    private void clickCenter(CanvasImage image) {
        double x = (image.gridX + image.gridW / 2.0) * PIXEL;
        double y = (image.gridY + image.gridH / 2.0) * PIXEL;
        assertTrue(input.mouseClicked(x, y, 0));
        input.mouseReleased(0);
    }

    private List<Object> sent() {
        return new ArrayList<>(TestNetworkHelper.SENT_TO_SERVER);
    }

    private int indexOfMove(UUID uuid) {
        List<Object> packets = sent();
        for (int i = 0; i < packets.size(); i++) {
            if (packets.get(i) instanceof MoveCanvasImageC2SPacket move && move.uuid().equals(uuid)) return i;
        }
        return -1;
    }

    private int indexOfLock(UUID uuid, boolean lock) {
        List<Object> packets = sent();
        for (int i = 0; i < packets.size(); i++) {
            if (packets.get(i) instanceof LockCanvasImageC2SPacket packet
                    && packet.imageUuid().equals(uuid) && packet.lock() == lock) return i;
        }
        return -1;
    }

    @Test
    @DisplayName("A selected image taken by another player drops the editor to normal mode")
    void selectionTakenByOtherPlayerIsDropped() {
        clickCenter(first);
        assertTrue(input.isImageMode());

        session.applyImageLockSync(first.uuid, OTHER);
        input.dropInvalidSelection();

        assertFalse(input.isImageMode());
        assertNull(controller.getSelectedUuid());
    }

    @Test
    @DisplayName("The server confirming our own lock keeps the selection")
    void ownLockKeepsSelection() {
        clickCenter(first);

        session.applyImageLockSync(first.uuid, ME);
        input.dropInvalidSelection();

        assertTrue(input.isImageMode());
        assertEquals(first.uuid, controller.getSelectedUuid());
    }

    @Test
    @DisplayName("A layer sync that arrives with the image already taken drops the selection")
    void layerSyncWithForeignLockDropsSelection() {
        clickCenter(first);

        CanvasImage fromServer = first.copy();
        fromServer.lockedByPlayer = OTHER;
        session.applyImageLayerSync(List.of(fromServer, second.copy()));
        input.dropInvalidSelection();

        assertFalse(input.isImageMode());
        assertNull(controller.getSelectedUuid());
    }

    @Test
    @DisplayName("An evicted selection drops the editor to normal mode")
    void evictedSelectionIsDropped() {
        clickCenter(first);

        session.applyImageLayerSync(List.of(second.copy()));
        input.dropInvalidSelection();

        assertFalse(input.isImageMode());
        assertNull(controller.getSelectedUuid());
    }

    @Test
    @DisplayName("Removing an unselected image keeps the selection")
    void unrelatedEvictionKeepsSelection() {
        clickCenter(first);

        session.applyImageLayerSync(List.of(first.copy()));
        input.dropInvalidSelection();

        assertTrue(input.isImageMode());
        assertEquals(first.uuid, controller.getSelectedUuid());
    }

    @Test
    @DisplayName("Dropping the selection sends nothing to the server")
    void droppingSendsNothing() {
        clickCenter(first);
        TestNetworkHelper.reset();

        session.applyImageLockSync(first.uuid, OTHER);
        input.dropInvalidSelection();

        assertTrue(sent().isEmpty());
    }

    @Test
    @DisplayName("A pending nudge is sent before switching to another image")
    void nudgeIsSentBeforeSwitching() {
        clickCenter(first);
        assertTrue(input.nudgeSelectedImage(1, 0));

        clickCenter(second);

        int move = indexOfMove(first.uuid);
        int unlock = indexOfLock(first.uuid, false);
        assertTrue(move >= 0, "nudge was never sent");
        assertTrue(unlock >= 0, "previous image was never unlocked");
        assertTrue(move < unlock, "nudge must reach the server before the unlock");
        assertEquals(1, ((MoveCanvasImageC2SPacket) sent().get(move)).gridX());
    }

    @Test
    @DisplayName("A pending nudge is sent when leaving image mode")
    void nudgeIsSentOnExit() {
        clickCenter(first);
        assertTrue(input.nudgeSelectedImage(0, 1));

        input.exitImageModeIfActive();

        int move = indexOfMove(first.uuid);
        assertTrue(move >= 0, "nudge was lost on exit");
        assertEquals(1, ((MoveCanvasImageC2SPacket) sent().get(move)).gridY());
    }

    @Test
    @DisplayName("Grabbing the selected image again does not reorder the layer")
    void regrabKeepsOrder() {
        clickCenter(first);
        clickCenter(second);
        session.imageLayer().moveToTop(first.uuid);

        clickCenter(second);

        List<CanvasImage> order = session.imageLayer().getImages();
        assertEquals(first.uuid, order.get(order.size() - 1).uuid);
    }
}
