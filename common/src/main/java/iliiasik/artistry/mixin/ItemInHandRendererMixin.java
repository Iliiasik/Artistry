package iliiasik.artistry.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import iliiasik.artistry.client.renderer.PosterInHandRenderer;
import iliiasik.artistry.client.renderer.PosterInHandState;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemInHandRenderer.class)
public abstract class ItemInHandRendererMixin {

    @Inject(method = "renderMap", at = @At("HEAD"), cancellable = true)
    private void artistry$renderPosterInHand(PoseStack poseStack, MultiBufferSource buffer,
                                             int packedLight, ItemStack stack, CallbackInfo ci) {
        if (PosterInHandState.consumeRendering()) {
            PosterInHandRenderer.render(poseStack, buffer, packedLight, stack);
            ci.cancel();
        }
    }
}
