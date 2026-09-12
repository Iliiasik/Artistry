package iliiasik.artistry.mixin;

import iliiasik.artistry.data.CanvasSignature;
import iliiasik.artistry.item.PosterItem;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemStack.class)
public abstract class ItemStackMixin {

    @Inject(method = "getMaxStackSize", at = @At("HEAD"), cancellable = true)
    private void artistry$signedCanvasNeverStacks(CallbackInfoReturnable<Integer> cir) {
        ItemStack self = (ItemStack) (Object) this;
        if (!(self.getItem() instanceof PosterItem)) return;
        if (CanvasSignature.isSignedStack(self)) {
            cir.setReturnValue(1);
        }
    }
}
