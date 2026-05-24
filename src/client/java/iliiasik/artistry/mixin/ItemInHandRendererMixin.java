package iliiasik.artistry.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import iliiasik.artistry.client.renderer.PosterInHandRenderer;
import iliiasik.artistry.data.CanvasData;
import iliiasik.artistry.item.PosterItem;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HeldItemRenderer.class)
public abstract class ItemInHandRendererMixin {

    @Unique
    private boolean artistry$renderingPoster = false;

    @ModifyExpressionValue(
            method = "renderFirstPersonItem",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/item/ItemStack;isOf(Lnet/minecraft/item/Item;)Z",
                    ordinal = 0))
    private boolean artistry$treatPosterAsMap(boolean original,
                                              @Local(argsOnly = true) ItemStack item) {
        if (item.getItem() instanceof PosterItem && CanvasData.isSizeChosenForStack(item)) {
            artistry$renderingPoster = true;
            return true;
        }
        return original;
    }

    @Inject(method = "renderFirstPersonMap", at = @At("HEAD"), cancellable = true)
    private void artistry$renderPosterInHand(MatrixStack matrices, VertexConsumerProvider vertexConsumers,
                                             int light, ItemStack stack, CallbackInfo ci) {
        if (artistry$renderingPoster) {
            artistry$renderingPoster = false;
            PosterInHandRenderer.render(matrices, vertexConsumers, light, stack);
            ci.cancel();
        }
    }
}