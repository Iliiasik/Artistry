package iliiasik.artistry;

import iliiasik.artistry.block.ModBlocks;
import iliiasik.artistry.block.entity.ModBlockEntities;
import iliiasik.artistry.config.ArtistryConfig;
import iliiasik.artistry.item.ModItems;
import iliiasik.artistry.platform.Services;
import iliiasik.artistry.recipe.ModRecipes;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Artistry {

    public static final String MOD_ID = "artistry";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private Artistry() {}

    public static void init() {
        LOGGER.info("Artistry is loading on {} in a {} environment",
                Services.PLATFORM.getPlatformName(), Services.PLATFORM.getEnvironmentName());
        ArtistryConfig.get();
        ModBlocks.init();
        ModItems.init();
        ModBlockEntities.init();
        ModRecipes.init();
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }
}
