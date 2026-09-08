package iliiasik.artistry.item;

import iliiasik.artistry.platform.Services;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.function.Supplier;

public final class ModItems {

    public static final Supplier<PosterItem> POSTER = Services.REGISTRY.register(Registries.ITEM, "poster",
            () -> new PosterItem(new Item.Properties().stacksTo(64)));

    public static final Supplier<BannerItem> BANNER = Services.REGISTRY.register(Registries.ITEM, "banner",
            () -> new BannerItem(new Item.Properties().stacksTo(64)));

    private ModItems() {}

    public static boolean isCanvas(ItemStack stack) {
        return stack.getItem() instanceof PosterItem;
    }

    public static void init() {
        Services.REGISTRY.addToCreativeTab(CreativeModeTabs.FUNCTIONAL_BLOCKS, POSTER);
        Services.REGISTRY.addToCreativeTab(CreativeModeTabs.FUNCTIONAL_BLOCKS, BANNER);
    }
}
