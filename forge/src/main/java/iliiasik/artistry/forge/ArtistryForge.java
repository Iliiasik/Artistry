package iliiasik.artistry.forge;

import iliiasik.artistry.Artistry;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(Artistry.MOD_ID)
public final class ArtistryForge {

    @SuppressWarnings({"removal"})
    public ArtistryForge() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        ForgeRegistryHelper.bind(modEventBus);
        Artistry.init();
        ForgeModNetwork.register();
        MinecraftForge.EVENT_BUS.register(ForgeServerEvents.class);
    }
}
