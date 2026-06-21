package iliiasik.artistry.client.renderer;

import iliiasik.artistry.client.image.ClientImageCache;
import iliiasik.artistry.client.renderer.ImageOcclusionClipper.VisibleFragment;
import iliiasik.artistry.data.CanvasImage;
import iliiasik.artistry.data.CanvasImageLayer;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;
import org.joml.Matrix4f;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PosterInHandRenderer {

    private static final Identifier FRAME_TEXTURE = Identifier.of("artistry", "textures/ui/in_hand_frame.png");

    private static final float PRE_SCALE   = 0.38f;
    private static final float FINAL_SCALE = 0.0078125f;
    private static final int SIZE   = 128;
    private static final int BORDER = 4;
    private static final float Z_IMAGES = -0.01f;

    private static final Map<Integer, CachedItemImages> imageCache = new HashMap<>();

    private record CachedItemImages(CanvasImageLayer layer, int canvasSize, int nbtHash) {
    }

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

        renderImages(vertexConsumers, light, stack, mat);

        VertexConsumer frame = vertexConsumers.getBuffer(RenderLayer.getText(FRAME_TEXTURE));
        frame.vertex(mat, 0,    SIZE, 0).color(255,255,255,255).texture(0f, 1f).light(light);
        frame.vertex(mat, SIZE, SIZE, 0).color(255,255,255,255).texture(1f, 1f).light(light);
        frame.vertex(mat, SIZE, 0,    0).color(255,255,255,255).texture(1f, 0f).light(light);
        frame.vertex(mat, 0,    0,    0).color(255,255,255,255).texture(0f, 0f).light(light);
    }

    private static void renderImages(VertexConsumerProvider vertexConsumers,
                                     int light, ItemStack stack, Matrix4f mat) {
        NbtComponent comp = stack.get(DataComponentTypes.CUSTOM_DATA);
        if (comp == null) return;
        NbtCompound tag = comp.copyNbt();
        if (!tag.contains("images") || !tag.contains("canvas")) return;

        int nbtHash = tag.toString().hashCode();
        int stackId = System.identityHashCode(stack);

        CachedItemImages cached = imageCache.get(stackId);
        if (cached == null || cached.nbtHash != nbtHash) {
            int canvasSize = tag.getCompound("canvas").getInt("size");
            if (canvasSize <= 0) return;
            CanvasImageLayer layer = new CanvasImageLayer();
            layer.fromNbt(tag.getList("images", net.minecraft.nbt.NbtList.COMPOUND_TYPE));
            cached = new CachedItemImages(layer, canvasSize, nbtHash);
            imageCache.put(stackId, cached);
        }

        int drawSize = SIZE - BORDER * 2;

        List<CanvasImage> drawList = cached.layer.getImages();
        List<VisibleFragment> fragments = ImageOcclusionClipper.computeVisibleFragments(drawList);

        for (VisibleFragment frag : fragments) {
            CanvasImage img = frag.image();
            Identifier imgTex;
            if (img.pixelized) {
                imgTex = ClientImageCache.getOrBuildPixelizedTexture(img.uuid, img.gridW, img.gridH);
            } else {
                imgTex = ClientImageCache.getTexture(img.uuid);
            }
            if (imgTex == null) continue;

            float x0 = BORDER + (float) frag.destRect().x0() / cached.canvasSize * drawSize;
            float y0 = BORDER + (float) frag.destRect().y0() / cached.canvasSize * drawSize;
            float x1 = BORDER + (float) frag.destRect().x1() / cached.canvasSize * drawSize;
            float y1 = BORDER + (float) frag.destRect().y1() / cached.canvasSize * drawSize;

            VertexConsumer imgVc = vertexConsumers.getBuffer(RenderLayer.getText(imgTex));
            imgVc.vertex(mat, x0, y1, Z_IMAGES).color(255,255,255,255).texture(frag.u0(), frag.v1()).light(light);
            imgVc.vertex(mat, x1, y1, Z_IMAGES).color(255,255,255,255).texture(frag.u1(), frag.v1()).light(light);
            imgVc.vertex(mat, x1, y0, Z_IMAGES).color(255,255,255,255).texture(frag.u1(), frag.v0()).light(light);
            imgVc.vertex(mat, x0, y0, Z_IMAGES).color(255,255,255,255).texture(frag.u0(), frag.v0()).light(light);
        }
    }
}