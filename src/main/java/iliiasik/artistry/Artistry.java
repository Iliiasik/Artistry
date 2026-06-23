package iliiasik.artistry;

import iliiasik.artistry.block.ModBlocks;
import iliiasik.artistry.block.entity.ModBlockEntities;
import iliiasik.artistry.config.ArtistryConfig;
import iliiasik.artistry.item.ModItems;
import iliiasik.artistry.network.ModNetwork;
import iliiasik.artistry.recipe.ModRecipes;
import iliiasik.artistry.server.ImageStorage;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(Artistry.MOD_ID)
public class Artistry {
    public static final String MOD_ID = "artistry";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public Artistry() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        ArtistryConfig.get();

        ModBlocks.register(modEventBus);
        ModItems.register(modEventBus);
        ModBlockEntities.register(modEventBus);
        ModRecipes.register(modEventBus);

        ModNetwork.register();

        MinecraftForge.EVENT_BUS.register(ModNetwork.class);
        MinecraftForge.EVENT_BUS.addListener((ServerStartedEvent event) -> ImageStorage.init(event.getServer()));
    }

    public static ResourceLocation id(String path) {
        return new ResourceLocation(MOD_ID, path);
    }
}