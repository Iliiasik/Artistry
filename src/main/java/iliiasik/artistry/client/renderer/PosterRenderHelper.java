package iliiasik.artistry.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;

public final class PosterRenderHelper {

    private PosterRenderHelper() {}

    public static void renderQuad(PoseStack matrices, MultiBufferSource vertexConsumers,
                                  ResourceLocation textureId, float z, int light) {
        VertexConsumer vc = vertexConsumers.getBuffer(RenderType.entityCutout(textureId));
        Matrix4f mat = matrices.last().pose();
        int ov = OverlayTexture.NO_OVERLAY;
        vc.vertex(mat, 0f, 1f, z).color(255,255,255,255).uv(1f, 0f).overlayCoords(ov).uv2(light).normal(matrices.last().normal(), 0,0,1).endVertex();
        vc.vertex(mat, 1f, 1f, z).color(255,255,255,255).uv(0f, 0f).overlayCoords(ov).uv2(light).normal(matrices.last().normal(), 0,0,1).endVertex();
        vc.vertex(mat, 1f, 0f, z).color(255,255,255,255).uv(0f, 1f).overlayCoords(ov).uv2(light).normal(matrices.last().normal(), 0,0,1).endVertex();
        vc.vertex(mat, 0f, 0f, z).color(255,255,255,255).uv(1f, 1f).overlayCoords(ov).uv2(light).normal(matrices.last().normal(), 0,0,1).endVertex();
    }
}