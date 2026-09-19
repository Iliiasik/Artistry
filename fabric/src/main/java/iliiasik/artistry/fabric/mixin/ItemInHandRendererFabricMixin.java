package iliiasik.artistry.fabric.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import iliiasik.artistry.client.renderer.PosterInHandState;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ItemInHandRenderer.class)
public abstract class ItemInHandRendererFabricMixin {

    @ModifyExpressionValue(
            method = "renderArmWithItem",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/item/ItemStack;is(Lnet/minecraft/world/item/Item;)Z",
                    ordinal = 0))
    private boolean artistry$treatPosterAsMap(boolean original,
                                              @Local(argsOnly = true) ItemStack stack) {
        if (PosterInHandState.shouldRenderAsMap(stack)) {
            PosterInHandState.markRendering();
            return true;
        }
        return original;
    }
}
