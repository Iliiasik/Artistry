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
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemInHandRenderer.class)
public abstract class ItemInHandRendererMixin {

    @ModifyVariable(
            method = "renderArmWithItem",
            at = @At("HEAD"),
            argsOnly = true)
    private ItemStack artistry$treatPosterAsMap(ItemStack stack) {
        return PosterInHandState.asMapProxy(stack);
    }

    @Inject(
            method = "renderMap(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/world/item/ItemStack;)V",
            at = @At("HEAD"),
            cancellable = true)
    private void artistry$renderPosterInHand(PoseStack poseStack, MultiBufferSource buffer,
                                             int combinedLight, ItemStack stack, CallbackInfo ci) {
        ItemStack poster = PosterInHandState.consumePoster(stack);
        if (!poster.isEmpty()) {
            PosterInHandRenderer.render(poseStack, buffer, combinedLight, poster);
            ci.cancel();
        }
    }
}
