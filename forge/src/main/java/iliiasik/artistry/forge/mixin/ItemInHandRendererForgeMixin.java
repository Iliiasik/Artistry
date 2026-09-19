package iliiasik.artistry.forge.mixin;

import iliiasik.artistry.client.renderer.PosterInHandState;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ItemInHandRenderer.class)
public abstract class ItemInHandRendererForgeMixin {

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
}
