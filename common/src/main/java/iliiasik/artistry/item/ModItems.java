package iliiasik.artistry.item;

import iliiasik.artistry.platform.Services;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;

import java.util.function.Supplier;

public final class ModItems {

    public static final Supplier<PosterItem> POSTER = Services.REGISTRY.register(Registries.ITEM, "poster",
            () -> new PosterItem(new Item.Properties().stacksTo(64)));

    private ModItems() {}

    public static void init() {
        Services.REGISTRY.addToCreativeTab(CreativeModeTabs.TOOLS_AND_UTILITIES, POSTER);
    }
}
