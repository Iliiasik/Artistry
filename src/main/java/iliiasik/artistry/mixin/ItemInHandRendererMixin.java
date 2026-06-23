package iliiasik.artistry.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.PoseStack;
import iliiasik.artistry.client.renderer.PosterInHandRenderer;
import iliiasik.artistry.data.CanvasData;
import iliiasik.artistry.item.PosterItem;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemInHandRenderer.class)
public abstract class ItemInHandRendererMixin {

    @Unique
    private boolean artistry$renderingPoster = false;

    @ModifyExpressionValue(
            method = "renderArmWithItem",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/item/ItemStack;is(Lnet/minecraft/world/item/Item;)Z",
                    ordinal = 0))
    private boolean artistry$treatPosterAsMap(boolean original,
                                              @Local(argsOnly = true) ItemStack stack) {
        if (stack.getItem() instanceof PosterItem && CanvasData.isSizeChosenForStack(stack)) {
            artistry$renderingPoster = true;
            return true;
        }
        return original;
    }

    @Inject(method = "renderMap", at = @At("HEAD"), cancellable = true)
    private void artistry$renderPosterInHand(PoseStack matrices, MultiBufferSource vertexConsumers,
                                             int light, ItemStack stack, CallbackInfo ci) {
        if (artistry$renderingPoster) {
            artistry$renderingPoster = false;
            PosterInHandRenderer.render(matrices, vertexConsumers, light, stack);
            ci.cancel();
        }
    }
}