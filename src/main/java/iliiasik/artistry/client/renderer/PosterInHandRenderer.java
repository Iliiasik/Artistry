package iliiasik.artistry.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import iliiasik.artistry.client.image.ClientImageCache;
import iliiasik.artistry.client.renderer.ImageOcclusionClipper.VisibleFragment;
import iliiasik.artistry.data.CanvasImage;
import iliiasik.artistry.data.CanvasImageLayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4f;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PosterInHandRenderer {

    private static final ResourceLocation FRAME_TEXTURE =
            new ResourceLocation("artistry", "textures/ui/in_hand_frame.png");

    private static final float PRE_SCALE   = 0.38f;
    private static final float FINAL_SCALE = 0.0078125f;
    private static final int SIZE   = 128;
    private static final int BORDER = 4;
    private static final float Z_IMAGES = -0.01f;

    private static final Map<Integer, CachedItemImages> imageCache = new HashMap<>();

    private record CachedItemImages(CanvasImageLayer layer, int canvasSize, int nbtHash) {
    }

    public static void render(PoseStack matrices, MultiBufferSource vertexConsumers,
                              int light, ItemStack stack) {
        matrices.mulPose(Axis.YP.rotationDegrees(180f));
        matrices.mulPose(Axis.ZP.rotationDegrees(180f));
        matrices.scale(PRE_SCALE, PRE_SCALE, PRE_SCALE);
        matrices.translate(-0.5f, -0.5f, 0f);
        matrices.scale(FINAL_SCALE, FINAL_SCALE, FINAL_SCALE);

        Matrix4f mat = matrices.last().pose();

        ResourceLocation texId = PosterItemCanvasCache.getTexture(stack);
        VertexConsumer vc = vertexConsumers.getBuffer(RenderType.text(texId));
        vc.vertex(mat, BORDER,        SIZE - BORDER, 0).color(255,255,255,255).uv(0f, 1f).uv2(light).endVertex();
        vc.vertex(mat, SIZE - BORDER, SIZE - BORDER, 0).color(255,255,255,255).uv(1f, 1f).uv2(light).endVertex();
        vc.vertex(mat, SIZE - BORDER, BORDER,        0).color(255,255,255,255).uv(1f, 0f).uv2(light).endVertex();
        vc.vertex(mat, BORDER,        BORDER,        0).color(255,255,255,255).uv(0f, 0f).uv2(light).endVertex();

        renderImages(vertexConsumers, light, stack, mat);

        VertexConsumer frame = vertexConsumers.getBuffer(RenderType.text(FRAME_TEXTURE));
        frame.vertex(mat, 0,    SIZE, 0).color(255,255,255,255).uv(0f, 1f).uv2(light).endVertex();
        frame.vertex(mat, SIZE, SIZE, 0).color(255,255,255,255).uv(1f, 1f).uv2(light).endVertex();
        frame.vertex(mat, SIZE, 0,    0).color(255,255,255,255).uv(1f, 0f).uv2(light).endVertex();
        frame.vertex(mat, 0,    0,    0).color(255,255,255,255).uv(0f, 0f).uv2(light).endVertex();
    }

    private static void renderImages(MultiBufferSource vertexConsumers,
                                     int light, ItemStack stack, Matrix4f mat) {
        CompoundTag tag = stack.getTag();
        if (tag == null) return;
        if (!tag.contains("images") || !tag.contains("canvas")) return;

        int nbtHash = tag.toString().hashCode();
        int stackId = System.identityHashCode(stack);

        CachedItemImages cached = imageCache.get(stackId);
        if (cached == null || cached.nbtHash != nbtHash) {
            int canvasSize = tag.getCompound("canvas").getInt("size");
            if (canvasSize <= 0) return;
            CanvasImageLayer layer = new CanvasImageLayer();
            layer.fromNbt(tag.getList("images", Tag.TAG_COMPOUND));
            cached = new CachedItemImages(layer, canvasSize, nbtHash);
            imageCache.put(stackId, cached);
        }

        int drawSize = SIZE - BORDER * 2;

        List<CanvasImage> drawList = cached.layer.getImages();
        List<VisibleFragment> fragments = ImageOcclusionClipper.computeVisibleFragments(drawList);

        for (VisibleFragment frag : fragments) {
            CanvasImage img = frag.image();
            ResourceLocation imgTex;
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

            VertexConsumer imgVc = vertexConsumers.getBuffer(RenderType.text(imgTex));
            imgVc.vertex(mat, x0, y1, Z_IMAGES).color(255,255,255,255).uv(frag.u0(), frag.v1()).uv2(light).endVertex();
            imgVc.vertex(mat, x1, y1, Z_IMAGES).color(255,255,255,255).uv(frag.u1(), frag.v1()).uv2(light).endVertex();
            imgVc.vertex(mat, x1, y0, Z_IMAGES).color(255,255,255,255).uv(frag.u1(), frag.v0()).uv2(light).endVertex();
            imgVc.vertex(mat, x0, y0, Z_IMAGES).color(255,255,255,255).uv(frag.u0(), frag.v0()).uv2(light).endVertex();
        }
    }
}