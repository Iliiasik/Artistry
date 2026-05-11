package iliiasik.artistry;

import iliiasik.artistry.block.ModBlocks;
import iliiasik.artistry.block.entity.ModBlockEntities;
import iliiasik.artistry.config.ArtistryConfig;
import iliiasik.artistry.item.ModItems;
import iliiasik.artistry.network.ModNetwork;
import iliiasik.artistry.recipe.ModRecipes;
import net.fabricmc.api.ModInitializer;
import net.minecraft.util.Identifier;

public class Artistry implements ModInitializer {
    public static final String MOD_ID = "artistry";

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
    }
}