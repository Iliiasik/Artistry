package iliiasik.artistry.client.image;

import iliiasik.artistry.data.CanvasImage;
import iliiasik.artistry.data.CanvasImageLayer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ImageLayerControllerTest {

    private static final int CANVAS_SIZE = 32;
    private static final int DRAW_SIZE = 320;
    private static final int PIXEL = DRAW_SIZE / CANVAS_SIZE;

    private static CanvasImageLayer layerWith(CanvasImage image) {
        CanvasImageLayer layer = new CanvasImageLayer();
        layer.addImage(image);
        return layer;
    }

    private static CanvasImage image(int x, int y, int w, int h) {
        return new CanvasImage(UUID.randomUUID(), x, y, w, h);
    }

    private static void grabTopLeft(ImageLayerController controller, CanvasImage image) {
        boolean selected = controller.trySelect(
                image.gridX * PIXEL + 1, image.gridY * PIXEL + 1, 0, 0, DRAW_SIZE, CANVAS_SIZE);
        assertTrue(selected, "handle not grabbed");
    }

    private static void grabBottomRight(ImageLayerController controller, CanvasImage image) {
        boolean selected = controller.trySelect(
                (image.gridX + image.gridW) * PIXEL - 1, (image.gridY + image.gridH) * PIXEL - 1,
                0, 0, DRAW_SIZE, CANVAS_SIZE);
        assertTrue(selected, "handle not grabbed");
    }

    private static void drag(ImageLayerController controller, int gridX, int gridY) {
        controller.onDrag(gridX * PIXEL + 1, gridY * PIXEL + 1, 0, 0, DRAW_SIZE, CANVAS_SIZE, () -> {});
    }

    @Test
    @DisplayName("resize from the far edge does not throw")
    void resizeAtCanvasEdge() {
        CanvasImage img = image(CANVAS_SIZE - 10, CANVAS_SIZE - 10, 10, 10);
        ImageLayerController controller = new ImageLayerController(layerWith(img));

        grabTopLeft(controller, img);

        assertDoesNotThrow(() -> drag(controller, 5, 5));
        assertDoesNotThrow(() -> drag(controller, 0, 0));
        assertDoesNotThrow(() -> drag(controller, CANVAS_SIZE - 1, CANVAS_SIZE - 1));
    }

    @Test
    @DisplayName("dragging a top left handle keeps the opposite edge fixed")
    void resizeKeepsOppositeEdge() {
        CanvasImage img = image(10, 10, 10, 10);
        ImageLayerController controller = new ImageLayerController(layerWith(img));

        grabTopLeft(controller, img);
        drag(controller, 5, 5);

        assertEquals(5, img.gridX);
        assertEquals(5, img.gridY);
        assertEquals(20, img.gridX + img.gridW);
        assertEquals(20, img.gridY + img.gridH);
    }

    @Test
    @DisplayName("dragging a bottom right handle keeps the origin fixed")
    void resizeKeepsOrigin() {
        CanvasImage img = image(10, 10, 10, 10);
        ImageLayerController controller = new ImageLayerController(layerWith(img));

        grabBottomRight(controller, img);
        drag(controller, 25, 25);

        assertEquals(10, img.gridX);
        assertEquals(10, img.gridY);
        assertEquals(25, img.gridX + img.gridW);
        assertEquals(25, img.gridY + img.gridH);
    }

    @Test
    @DisplayName("grabbing a handle without moving it keeps the geometry")
    void resizeIsStable() {
        CanvasImage img = image(10, 10, 10, 10);
        ImageLayerController controller = new ImageLayerController(layerWith(img));

        for (int attempt = 0; attempt < 5; attempt++) {
            grabTopLeft(controller, img);
            drag(controller, img.gridX, img.gridY);
            assertEquals(10, img.gridX);
            assertEquals(10, img.gridW);
            assertEquals(10, img.gridY);
            assertEquals(10, img.gridH);
        }
    }

    @Test
    @DisplayName("dragging a handle past the anchor pins it instead of flipping the image")
    void resizePastAnchorDoesNotFlip() {
        CanvasImage topLeftGrab = image(10, 10, 10, 10);
        ImageLayerController first = new ImageLayerController(layerWith(topLeftGrab));
        grabTopLeft(first, topLeftGrab);
        drag(first, 25, 25);

        assertEquals(CanvasImage.MIN_GRID, topLeftGrab.gridW);
        assertEquals(CanvasImage.MIN_GRID, topLeftGrab.gridH);
        assertEquals(20, topLeftGrab.gridX + topLeftGrab.gridW);
        assertEquals(20, topLeftGrab.gridY + topLeftGrab.gridH);

        CanvasImage bottomRightGrab = image(10, 10, 10, 10);
        ImageLayerController second = new ImageLayerController(layerWith(bottomRightGrab));
        grabBottomRight(second, bottomRightGrab);
        drag(second, 2, 2);

        assertEquals(CanvasImage.MIN_GRID, bottomRightGrab.gridW);
        assertEquals(CanvasImage.MIN_GRID, bottomRightGrab.gridH);
        assertEquals(10, bottomRightGrab.gridX);
        assertEquals(10, bottomRightGrab.gridY);
    }

    @Test
    @DisplayName("resize never leaves the canvas or shrinks below the minimum")
    void resizeStaysInsideCanvas() {
        for (int x = 0; x <= CANVAS_SIZE - CanvasImage.MIN_GRID; x++) {
            CanvasImage img = image(x, 0, CanvasImage.MIN_GRID, CanvasImage.MIN_GRID);
            ImageLayerController controller = new ImageLayerController(layerWith(img));
            grabTopLeft(controller, img);
            for (int target = 0; target < CANVAS_SIZE; target++) {
                int finalTarget = target;
                assertDoesNotThrow(() -> drag(controller, finalTarget, finalTarget));
                assertTrue(img.gridX >= 0 && img.gridY >= 0, "negative origin");
                assertTrue(img.gridX + img.gridW <= CANVAS_SIZE, "width past the canvas");
                assertTrue(img.gridY + img.gridH <= CANVAS_SIZE, "height past the canvas");
                assertTrue(img.gridW >= CanvasImage.MIN_GRID, "width below the minimum");
                assertTrue(img.gridH >= CanvasImage.MIN_GRID, "height below the minimum");
            }
        }
    }

    @Test
    @DisplayName("moving an image keeps it inside the canvas")
    void moveStaysInsideCanvas() {
        CanvasImage img = image(4, 4, 8, 8);
        ImageLayerController controller = new ImageLayerController(layerWith(img));

        boolean selected = controller.trySelect(
                8 * PIXEL, 8 * PIXEL, 0, 0, DRAW_SIZE, CANVAS_SIZE);
        assertTrue(selected, "image not selected");

        drag(controller, CANVAS_SIZE + 10, CANVAS_SIZE + 10);
        assertEquals(CANVAS_SIZE - img.gridW, img.gridX);
        assertEquals(CANVAS_SIZE - img.gridH, img.gridY);

        drag(controller, -10, -10);
        assertEquals(0, img.gridX);
        assertEquals(0, img.gridY);
    }
}
