package iliiasik.artistry.neoforge;

import iliiasik.artistry.block.entity.PosterBlockEntity;
import iliiasik.artistry.client.renderer.PosterBlockEntityRenderer;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.NotNull;

public final class NeoForgePosterRenderer extends PosterBlockEntityRenderer {

    @Override
    public @NotNull AABB getRenderBoundingBox(@NotNull PosterBlockEntity entity) {
        return canvasBounds(entity);
    }
}
