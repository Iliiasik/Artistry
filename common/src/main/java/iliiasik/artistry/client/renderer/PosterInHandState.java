package iliiasik.artistry.client.renderer;

import iliiasik.artistry.data.CanvasData;
import iliiasik.artistry.item.PosterItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class PosterInHandState {

    private static ItemStack mapProxy = ItemStack.EMPTY;
    private static ItemStack pending = ItemStack.EMPTY;

    private PosterInHandState() {}

    public static boolean shouldRenderAsMap(ItemStack stack) {
        return stack.getItem() instanceof PosterItem && CanvasData.isSizeChosenForStack(stack);
    }

    public static ItemStack asMapProxy(ItemStack stack) {
        if (!shouldRenderAsMap(stack)) {
            pending = ItemStack.EMPTY;
            return stack;
        }
        pending = stack;
        if (mapProxy.isEmpty()) mapProxy = new ItemStack(Items.FILLED_MAP);
        return mapProxy;
    }

    public static ItemStack consumePoster(ItemStack rendered) {
        ItemStack poster = pending;
        pending = ItemStack.EMPTY;
        if (poster.isEmpty()) return ItemStack.EMPTY;
        return rendered == poster || rendered == mapProxy ? poster : ItemStack.EMPTY;
    }
}
