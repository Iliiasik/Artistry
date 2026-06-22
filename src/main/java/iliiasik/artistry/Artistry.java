package iliiasik.artistry;

import iliiasik.artistry.block.ModBlocks;
import iliiasik.artistry.block.entity.ModBlockEntities;
import iliiasik.artistry.config.ArtistryConfig;
import iliiasik.artistry.item.ModItems;
import iliiasik.artistry.network.ModNetwork;
import iliiasik.artistry.recipe.ModRecipes;
import iliiasik.artistry.server.ImageStorage;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(Artistry.MOD_ID)
public class Artistry {
    public static final String MOD_ID = "artistry";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public Artistry(IEventBus modEventBus) {
        ArtistryConfig.get();

        ModBlocks.register(modEventBus);
        ModItems.register(modEventBus);
        ModBlockEntities.register(modEventBus);
        ModRecipes.register(modEventBus);

        modEventBus.addListener(ModNetwork::register);

        NeoForge.EVENT_BUS.register(ModNetwork.class);
        NeoForge.EVENT_BUS.addListener((ServerStartedEvent event) -> ImageStorage.init(event.getServer()));
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }
}