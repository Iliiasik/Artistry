package iliiasik.artistry.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import org.joml.Matrix4f;

public final class PosterRenderHelper {

    private PosterRenderHelper() {}

    public static void renderQuad(PoseStack matrices, MultiBufferSource vertexConsumers,
                                  CanvasAtlas.Slot slot, float size, float z, int light) {
        VertexConsumer vc = vertexConsumers.getBuffer(RenderType.entityCutout(slot.texture()));
        Matrix4f mat = matrices.last().pose();
        int ov = OverlayTexture.NO_OVERLAY;
        vertex(vc, mat, matrices, 0f, size, z, slot.u1(), slot.v0(), ov, light);
        vertex(vc, mat, matrices, size, size, z, slot.u0(), slot.v0(), ov, light);
        vertex(vc, mat, matrices, size, 0f, z, slot.u0(), slot.v1(), ov, light);
        vertex(vc, mat, matrices, 0f, 0f, z, slot.u1(), slot.v1(), ov, light);
    }

    static void vertex(VertexConsumer vc, Matrix4f mat, PoseStack matrices,
                       float x, float y, float z, float u, float v, int overlay, int light) {
        vc.addVertex(mat, x, y, z)
                .setUv(u, v)
                .setColor(255, 255, 255, 255)
                .setOverlay(overlay)
                .setLight(light)
                .setNormal(matrices.last(), 0, 0, 1);
    }
}
