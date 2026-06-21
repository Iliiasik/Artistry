package iliiasik.artistry;

import iliiasik.artistry.block.ModBlocks;
import iliiasik.artistry.block.entity.ModBlockEntities;
import iliiasik.artistry.config.ArtistryConfig;
import iliiasik.artistry.item.ModItems;
import iliiasik.artistry.network.ModNetwork;
import iliiasik.artistry.recipe.ModRecipes;
import iliiasik.artistry.server.ImageStorage;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Artistry implements ModInitializer {
    public static final String MOD_ID = "artistry";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static Identifier id(String path) {
        return Identifier.of(MOD_ID, path);
    }

    @Override
    public void onInitialize() {
        ArtistryConfig.get();
        ModBlocks.register();
        ModItems.register();
        ModBlockEntities.register();
        ModNetwork.register();
        ModRecipes.register();
        ServerLifecycleEvents.SERVER_STARTED.register(ImageStorage::init);
    }
}