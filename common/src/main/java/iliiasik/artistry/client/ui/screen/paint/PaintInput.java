package iliiasik.artistry.client.ui.screen.paint;

import iliiasik.artistry.Artistry;
import iliiasik.artistry.client.image.ClientImageCache;
import iliiasik.artistry.client.image.ImageLayerController;
import iliiasik.artistry.client.tools.DrawingTool;
import iliiasik.artistry.client.tools.PixelPainter;
import iliiasik.artistry.client.ui.layout.PaintDimensions;
import iliiasik.artistry.client.ui.widget.ImageToolWidget;
import iliiasik.artistry.data.CanvasData;
import iliiasik.artistry.data.CanvasImage;
import net.minecraft.client.gui.GuiGraphics;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.UUID;

public class PaintInput {

    private final PaintSession session;
    private final PaintDimensions dims;
    private final PixelPainter pixelPainter;
    private final ImageLayerController imageController;

    private PaintWidgets widgets;
    private UUID localPlayerUuid;

    private boolean isDrawing = false;
    private int drawingButton = -1;
    private DrawingTool toolBeforePipette = null;
    private boolean imageDragging = false;
    private boolean imageMode = false;

    private static final int MAX_IMAGE_DIM = 512;

    public PaintInput(PaintSession session, PaintDimensions dims,
                      PixelPainter pixelPainter, ImageLayerController imageController) {
        this.session = session;
        this.dims = dims;
        this.pixelPainter = pixelPainter;
        this.imageController = imageController;
    }

    public void setWidgets(PaintWidgets widgets) {
        this.widgets = widgets;
        widgets.setImageMode(imageMode);
    }

    public void setLocalPlayer(UUID uuid) {
        this.localPlayerUuid = uuid;
    }

    public void exitImageModeIfActive() {
        if (imageMode) exitImageMode();
    }

    public boolean exitImageModeOnMiss(int button) {
        if (!imageMode || (button != 0 && button != 1)) return false;
        exitImageMode();
        return true;
    }

    public boolean isImageMode() {
        return imageMode;
    }

    public boolean selectTool(DrawingTool tool) {
        if (imageMode) return false;
        toolBeforePipette = null;
        applyTool(tool);
        return true;
    }

    public boolean beginTemporaryPipette() {
        if (imageMode || isDrawing || toolBeforePipette != null) return false;
        if (pixelPainter.getTool() == DrawingTool.PIPETTE) return false;
        toolBeforePipette = pixelPainter.getTool();
        applyTool(DrawingTool.PIPETTE);
        return true;
    }

    public boolean endTemporaryPipette() {
        if (toolBeforePipette == null) return false;
        DrawingTool restored = toolBeforePipette;
        toolBeforePipette = null;
        applyTool(restored);
        return true;
    }

    public boolean deleteSelectedImage() {
        if (!imageMode || imageController.getSelectedUuid() == null) return false;
        handleImageAction(ImageToolWidget.Action.DELETE);
        return true;
    }

    public boolean nudgeSelectedImage(int stepX, int stepY) {
        if (!imageMode) return false;
        UUID selected = imageController.getSelectedUuid();
        if (selected == null) return false;
        if (!imageController.nudge(stepX, stepY, session.canvasData().canvasSize)) return false;
        session.markImageMoved(selected);
        return true;
    }

    private void applyTool(DrawingTool tool) {
        pixelPainter.setTool(tool);
        if (widgets != null) widgets.setActiveTool(tool);
    }

    private boolean isOutsideDrawingArea(double mouseX, double mouseY) {
        return mouseX < dims.drawingAreaX || mouseX >= dims.drawingAreaX + dims.drawingAreaSize
                || mouseY < dims.drawingAreaY || mouseY >= dims.drawingAreaY + dims.drawingAreaSize;
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if ((button != 0 && button != 1) || isOutsideDrawingArea(mouseX, mouseY)) return false;

        int canvasSize = session.canvasData().canvasSize;
        boolean secondary = button == 1;

        if (imageMode && secondary) return true;

        if (imageMode) {
            UUID previousSelected = imageController.getSelectedUuid();
            boolean hit = imageController.trySelect(mouseX, mouseY,
                    dims.drawingAreaX, dims.drawingAreaY, dims.drawingAreaSize, canvasSize);
            if (!hit) {
                exitImageMode();
            } else {
                UUID newSelected = imageController.getSelectedUuid();
                if (previousSelected != null && !previousSelected.equals(newSelected)) {
                    session.lockImage(previousSelected, false);
                    session.lockImage(newSelected, true);
                }
            }
            imageDragging = hit;
            return true;
        }

        if (!secondary && !session.imageLayer().getImages().isEmpty() &&
                imageController.isOnImage(mouseX, mouseY,
                        dims.drawingAreaX, dims.drawingAreaY, dims.drawingAreaSize, canvasSize)) {
            CanvasImage hovered = getHoveredUnlockedImage(mouseX, mouseY);
            if (hovered != null) {
                boolean hit = imageController.trySelect(mouseX, mouseY,
                        dims.drawingAreaX, dims.drawingAreaY, dims.drawingAreaSize, canvasSize);
                if (hit) {
                    enterImageMode(imageController.getSelectedUuid());
                    imageDragging = true;
                    return true;
                }
            }
        }

        double scale = (double) dims.drawingAreaSize / canvasSize;
        if (pixelPainter.getTool() == DrawingTool.PIPETTE) {
            int[] picked = pixelPainter.pickPixel(session.canvasData(), (int) mouseX, (int) mouseY,
                    dims.drawingAreaX, dims.drawingAreaY, scale);
            if (picked != null) {
                int blockIndex = picked[0];
                int color = picked[1];
                if (color != 0) {
                    widgets.applyPickedColor(color, secondary);
                } else if (blockIndex > 0) {
                    widgets.applyPickedBlock(blockIndex, secondary);
                }
                if (!endTemporaryPipette()) applyTool(DrawingTool.BRUSH);
            }
            return true;
        }

        session.beginStrokeHistory();
        if (pixelPainter.beginStroke(session.canvasData(), (int) mouseX, (int) mouseY,
                dims.drawingAreaX, dims.drawingAreaY, scale, secondary)) {
            isDrawing = true;
            drawingButton = button;
            return true;
        }
        return false;
    }

    public boolean mouseDragged(double mouseX, double mouseY, int button) {
        if (imageDragging && button == 0) {
            imageController.onDrag(mouseX, mouseY,
                    dims.drawingAreaX, dims.drawingAreaY, dims.drawingAreaSize, session.canvasData().canvasSize,
                    () -> session.markImageMoved(imageController.getSelectedUuid()));
            return true;
        }
        if (isDrawing && button == drawingButton) {
            double scale = (double) dims.drawingAreaSize / session.canvasData().canvasSize;
            pixelPainter.continueStroke(session.canvasData(), (int) mouseX, (int) mouseY,
                    dims.drawingAreaX, dims.drawingAreaY, scale);
            return true;
        }
        return false;
    }

    public boolean mouseReleased(int button) {
        if (imageDragging && button == 0) {
            imageController.endDrag();
            imageDragging = false;
            session.flushImageMove();
            return true;
        }
        if (isDrawing && button == drawingButton) {
            pixelPainter.endStroke();
            session.endStrokeHistory(pixelPainter.strokeCells());
            isDrawing = false;
            drawingButton = -1;
            return true;
        }
        return false;
    }

    private void enterImageMode(UUID uuid) {
        imageMode = true;
        if (widgets != null) widgets.setImageMode(true);
        session.lockImage(uuid, true);
    }

    private void exitImageMode() {
        if (imageMode && session.isWorld()) {
            UUID sel = imageController.getSelectedUuid();
            if (sel != null) {
                session.lockImage(sel, false);
            }
        }
        imageMode = false;
        imageController.clearSelection();
        if (widgets != null) widgets.setImageMode(false);
    }

    public void handleImageAction(ImageToolWidget.Action action) {
        UUID sel = imageController.getSelectedUuid();
        if (sel == null) return;
        if (action == ImageToolWidget.Action.DELETE) {
            session.imageLayer().removeImage(sel);
            exitImageMode();
            session.deleteImage(sel);
        } else if (action == ImageToolWidget.Action.PIXELIZE) {
            CanvasImage img = session.imageLayer().findByUuid(sel);
            if (img != null) {
                img.pixelized = !img.pixelized;
                if (img.pixelized) {
                    ClientImageCache.rebuildPixelizedTexture(sel, img.gridW, img.gridH);
                }
            }
            session.togglePixelize(sel);
        }
    }

    public void openFilePicker() {
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
                    session.uploadImage(bytes);
                } catch (IOException e) {
                    Artistry.LOGGER.error("Failed to read selected image {}", path, e);
                }
            }
            if (widgets != null) widgets.setActiveTool(DrawingTool.BRUSH);
            pixelPainter.setTool(DrawingTool.BRUSH);
        }, "artistry-file-picker").start();
    }

    private byte[] convertToPng(File file) throws IOException {
        BufferedImage img = ImageIO.read(file);
        if (img == null) throw new IOException("Cannot read image: " + file.getName());
        img = downscale(img);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(img, "png", out);
        return out.toByteArray();
    }

    private BufferedImage downscale(BufferedImage src) {
        int w = src.getWidth();
        int h = src.getHeight();
        if (w <= MAX_IMAGE_DIM && h <= MAX_IMAGE_DIM) return src;
        double scale = (double) MAX_IMAGE_DIM / Math.max(w, h);
        int nw = Math.max(1, (int) Math.round(w * scale));
        int nh = Math.max(1, (int) Math.round(h * scale));
        BufferedImage dst = new BufferedImage(nw, nh, BufferedImage.TYPE_INT_ARGB);
        java.awt.Graphics2D g = dst.createGraphics();
        g.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION,
                java.awt.RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(src, 0, 0, nw, nh, null);
        g.dispose();
        return dst;
    }

    private CanvasImage getHoveredUnlockedImage(double mouseX, double mouseY) {
        int canvasSize = session.canvasData().canvasSize;
        double pixelSize = (double) dims.drawingAreaSize / canvasSize;
        List<CanvasImage> images = session.imageLayer().getImages();
        for (int i = images.size() - 1; i >= 0; i--) {
            CanvasImage img = images.get(i);
            if (localPlayerUuid != null && img.isLockedByOther(localPlayerUuid)) continue;
            int sx = dims.drawingAreaX + (int) (img.gridX * pixelSize);
            int sy = dims.drawingAreaY + (int) (img.gridY * pixelSize);
            int sw = (int) (img.gridW * pixelSize);
            int sh = (int) (img.gridH * pixelSize);
            if (mouseX >= sx && mouseX < sx + sw && mouseY >= sy && mouseY < sy + sh) return img;
        }
        return null;
    }

    public void renderHoverPreview(GuiGraphics context, int hoverX, int hoverY) {
        CanvasData canvasData = session.canvasData();
        if (!canvasData.isSizeChosen()) return;
        if (imageMode) return;
        if (isOutsideDrawingArea(hoverX, hoverY)) return;
        if (pixelPainter.getTool() == DrawingTool.PIPETTE) return;
        if (!session.imageLayer().getImages().isEmpty()) {
            if (getHoveredUnlockedImage(hoverX, hoverY) != null) return;
        }

        int canvasSize = canvasData.canvasSize;
        double scale = (double) dims.drawingAreaSize / canvasSize;

        int[] bounds = pixelPainter.getBrushGridBounds(
                hoverX, hoverY,
                dims.drawingAreaX, dims.drawingAreaY,
                scale, canvasSize
        );

        int px0 = dims.drawingAreaX + (int) (bounds[0] * scale);
        int py0 = dims.drawingAreaY + (int) (bounds[1] * scale);
        int px1 = dims.drawingAreaX + (int) ((bounds[2] + 1) * scale);
        int py1 = dims.drawingAreaY + (int) ((bounds[3] + 1) * scale);

        context.fill(px0, py0, px1, py1, 0x40000000);
        context.fill(px0, py0, px1, py1, 0x18FFFFFF);
    }
}