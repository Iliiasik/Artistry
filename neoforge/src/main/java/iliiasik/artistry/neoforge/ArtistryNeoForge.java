package iliiasik.artistry.neoforge;

import iliiasik.artistry.Artistry;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;

@Mod(Artistry.MOD_ID)
public final class ArtistryNeoForge {

    public ArtistryNeoForge(IEventBus modEventBus) {
        NeoForgeRegistryHelper.bind(modEventBus);
        Artistry.init();
        modEventBus.addListener(NeoForgeModNetwork::register);
        NeoForge.EVENT_BUS.register(NeoForgeServerEvents.class);
    }
}
