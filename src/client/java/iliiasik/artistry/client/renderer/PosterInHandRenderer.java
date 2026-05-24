package iliiasik.artistry.client.renderer;

import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;
import org.joml.Matrix4f;

public class PosterInHandRenderer {

    private static final Identifier FRAME_TEXTURE = Identifier.of("artistry", "textures/ui/in_hand_frame.png");

    private static final float PRE_SCALE = 0.38f;
    private static final float FINAL_SCALE = 0.0078125f;
    private static final int SIZE = 128;
    private static final int BORDER = 4;

    public static void render(MatrixStack matrices, VertexConsumerProvider vertexConsumers,
                              int light, ItemStack stack) {
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180f));
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(180f));
        matrices.scale(PRE_SCALE, PRE_SCALE, PRE_SCALE);
        matrices.translate(-0.5f, -0.5f, 0f);
        matrices.scale(FINAL_SCALE, FINAL_SCALE, FINAL_SCALE);

        Matrix4f mat = matrices.peek().getPositionMatrix();

        Identifier texId = PosterItemCanvasCache.getTexture(stack);
        VertexConsumer vc = vertexConsumers.getBuffer(RenderLayer.getText(texId));
        vc.vertex(mat, BORDER,        SIZE - BORDER, 0).color(255,255,255,255).texture(0f, 1f).light(light);
        vc.vertex(mat, SIZE - BORDER, SIZE - BORDER, 0).color(255,255,255,255).texture(1f, 1f).light(light);
        vc.vertex(mat, SIZE - BORDER, BORDER,        0).color(255,255,255,255).texture(1f, 0f).light(light);
        vc.vertex(mat, BORDER,        BORDER,        0).color(255,255,255,255).texture(0f, 0f).light(light);

        VertexConsumer frame = vertexConsumers.getBuffer(RenderLayer.getText(FRAME_TEXTURE));
        frame.vertex(mat, 0,    SIZE, 0).color(255,255,255,255).texture(0f, 1f).light(light);
        frame.vertex(mat, SIZE, SIZE, 0).color(255,255,255,255).texture(1f, 1f).light(light);
        frame.vertex(mat, SIZE, 0,    0).color(255,255,255,255).texture(1f, 0f).light(light);
        frame.vertex(mat, 0,    0,    0).color(255,255,255,255).texture(0f, 0f).light(light);
    }
}