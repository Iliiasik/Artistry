package iliiasik.artistry.fabric;

import iliiasik.artistry.Artistry;
import iliiasik.artistry.network.ArtistryCommands;
import iliiasik.artistry.network.ArtistryServerEvents;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;

public final class ArtistryFabric implements ModInitializer {

    @Override
    public void onInitialize() {
        Artistry.init();
        FabricModNetwork.register();

        ServerLifecycleEvents.SERVER_STARTED.register(ArtistryServerEvents::onServerStarted);

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) ->
                ArtistryServerEvents.onPlayerJoin(handler.player));

        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) ->
                ArtistryServerEvents.onPlayerLeave(handler.player));

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                ArtistryCommands.register(dispatcher));
    }
}
