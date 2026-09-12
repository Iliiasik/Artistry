package iliiasik.artistry.client.renderer;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

public final class ArtistryRenderStates extends RenderStateShard {

    private static final LayeringStateShard IMAGE_LAYERING = new LayeringStateShard(
            "artistry:image_layering",
            () -> {
                RenderSystem.polygonOffset(-2.0F, -20.0F);
                RenderSystem.enablePolygonOffset();
            },
            () -> {
                RenderSystem.polygonOffset(0.0F, 0.0F);
                RenderSystem.disablePolygonOffset();
            });

    private ArtistryRenderStates() {
        super("artistry:states", () -> {}, () -> {});
    }

    public static RenderType.CompositeState canvas(ResourceLocation texture) {
        return RenderType.CompositeState.builder()
                .setShaderState(RENDERTYPE_ENTITY_CUTOUT_SHADER)
                .setTextureState(new TextureStateShard(texture, false, false))
                .setTransparencyState(NO_TRANSPARENCY)
                .setLightmapState(LIGHTMAP)
                .setOverlayState(OVERLAY)
                .setLayeringState(POLYGON_OFFSET_LAYERING)
                .createCompositeState(true);
    }

    public static RenderType.CompositeState image(ResourceLocation texture) {
        return RenderType.CompositeState.builder()
                .setShaderState(RENDERTYPE_ENTITY_CUTOUT_NO_CULL_SHADER)
                .setTextureState(new TextureStateShard(texture, false, false))
                .setTransparencyState(NO_TRANSPARENCY)
                .setCullState(NO_CULL)
                .setLightmapState(LIGHTMAP)
                .setOverlayState(OVERLAY)
                .setLayeringState(IMAGE_LAYERING)
                .createCompositeState(false);
    }
}
