package iliiasik.artistry.client.renderer;

import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import org.joml.Matrix4f;

public final class PosterRenderHelper {

    private PosterRenderHelper() {}

    public static void renderQuad(MatrixStack matrices, VertexConsumerProvider vertexConsumers,
                                  Identifier textureId, float z, int light) {
        VertexConsumer vc = vertexConsumers.getBuffer(RenderLayer.getEntityCutout(textureId));
        Matrix4f mat = matrices.peek().getPositionMatrix();
        int ov = OverlayTexture.DEFAULT_UV;
        vc.vertex(mat, 0f, 1f, z).texture(1f, 0f).color(255,255,255,255).overlay(ov).light(light).normal(matrices.peek(), 0,0,1);
        vc.vertex(mat, 1f, 1f, z).texture(0f, 0f).color(255,255,255,255).overlay(ov).light(light).normal(matrices.peek(), 0,0,1);
        vc.vertex(mat, 1f, 0f, z).texture(0f, 1f).color(255,255,255,255).overlay(ov).light(light).normal(matrices.peek(), 0,0,1);
        vc.vertex(mat, 0f, 0f, z).texture(1f, 1f).color(255,255,255,255).overlay(ov).light(light).normal(matrices.peek(), 0,0,1);
    }
}