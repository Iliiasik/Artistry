package iliiasik.artistry.neoforge.mixin;

import com.llamalad7.mixinextras.expression.Definition;
import com.llamalad7.mixinextras.expression.Expression;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import iliiasik.artistry.client.renderer.PosterInHandState;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.MapItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ItemInHandRenderer.class)
public abstract class ItemInHandRendererNeoForgeMixin {

    @Definition(id = "MapItem", type = MapItem.class)
    @Expression("? instanceof MapItem")
    @ModifyExpressionValue(
            method = "renderArmWithItem",
            at = @At("MIXINEXTRAS:EXPRESSION"))
    private boolean artistry$treatPosterAsMap(boolean original,
                                              @Local(argsOnly = true) ItemStack stack) {
        if (PosterInHandState.shouldRenderAsMap(stack)) {
            PosterInHandState.markRendering();
            return true;
        }
        return original;
    }
}
