package iliiasik.artistry.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import iliiasik.artistry.client.renderer.PosterInHandRenderer;
import iliiasik.artistry.client.renderer.PosterInHandState;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemInHandRenderer.class)
public abstract class ItemInHandRendererMixin {

    @Redirect(
            method = "renderArmWithItem",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/item/ItemStack;is(Lnet/minecraft/world/item/Item;)Z",
                    ordinal = 0))
    private boolean artistry$treatPosterAsMap(ItemStack instance, Item item) {
        if (PosterInHandState.shouldRenderAsMap(instance)) {
            PosterInHandState.markRendering();
            return true;
        }
        return instance.is(item);
    }

    @Inject(
            method = "renderMap(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/world/item/ItemStack;)V",
            at = @At("HEAD"),
            cancellable = true)
    private void artistry$renderPosterInHand(PoseStack poseStack, MultiBufferSource buffer,
                                             int combinedLight, ItemStack stack, CallbackInfo ci) {
        if (PosterInHandState.consumeRendering()) {
            PosterInHandRenderer.render(poseStack, buffer, combinedLight, stack);
            ci.cancel();
        }
    }
}
