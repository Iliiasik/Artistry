package iliiasik.artistry.client.renderer;

import iliiasik.artistry.data.CanvasData;
import iliiasik.artistry.item.PosterItem;
import net.minecraft.world.item.ItemStack;

public final class PosterInHandState {

    private static boolean rendering = false;

    private PosterInHandState() {}

    public static boolean shouldRenderAsMap(ItemStack stack) {
        return stack.getItem() instanceof PosterItem && CanvasData.isSizeChosenForStack(stack);
    }

    public static void markRendering() {
        rendering = true;
    }

    public static boolean consumeRendering() {
        boolean value = rendering;
        rendering = false;
        return value;
    }
}
