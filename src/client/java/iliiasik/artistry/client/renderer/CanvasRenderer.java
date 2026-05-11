package iliiasik.artistry.client.renderer;

import iliiasik.artistry.client.palette.BlockPalette;
import iliiasik.artistry.data.CanvasData;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Identifier;

import java.util.UUID;

public class CanvasRenderer {

    private static final int TEX_SIZE = 512;

    private final CanvasTextureHolder holder;

    public CanvasRenderer() {
        Identifier id = Identifier.of("artistry", "canvas_" + UUID.randomUUID().toString().replace("-", ""));
        holder = new CanvasTextureHolder(id, TEX_SIZE);
    }

    public void update(CanvasData data) {
        if (!data.isSizeChosen()) return;
        BlockPalette.ensureLoaded();
        holder.updatePixels(data, TEX_SIZE);
    }

    public void render(DrawContext ctx, int x, int y, int size) {
        ctx.drawTexture(
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