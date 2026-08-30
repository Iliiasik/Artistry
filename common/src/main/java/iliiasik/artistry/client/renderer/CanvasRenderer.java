package iliiasik.artistry.client.renderer;

import iliiasik.artistry.client.palette.BlockPalette;
import iliiasik.artistry.data.CanvasData;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

import java.util.UUID;

public class CanvasRenderer {

    private static final int TEX_SIZE = 512;

    private final CanvasTextureHolder holder;
    private int canvasRevision = -1;
    private int paletteGeneration = -1;

    public CanvasRenderer() {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(
                "artistry", "canvas_" + UUID.randomUUID().toString().replace("-", ""));
        holder = new CanvasTextureHolder(id, TEX_SIZE);
    }

    public void update(CanvasData data) {
        if (!data.isSizeChosen()) return;
        if (paletteGeneration != CanvasCellPainter.generation()) {
            paletteGeneration = CanvasCellPainter.generation();
            BlockPalette.ensureLoaded();
            holder.markPaletteChanged();
        }
        if (canvasRevision != data.revision()) {
            canvasRevision = data.revision();
            holder.markDirty();
        }
        holder.updatePixels(data);
    }

    public void render(GuiGraphics ctx, int x, int y, int size) {
        ctx.blit(
                holder.getTextureId(),
                x, y,
                size, size,
                0.0F, 0.0F,
                TEX_SIZE, TEX_SIZE,
                TEX_SIZE, TEX_SIZE
        );
    }

    public void close() {
        holder.close();
    }
}