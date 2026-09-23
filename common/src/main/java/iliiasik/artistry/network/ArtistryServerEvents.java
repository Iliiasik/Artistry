package iliiasik.artistry.network;

import iliiasik.artistry.server.ImageStorage;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

public final class ArtistryServerEvents {

    private ArtistryServerEvents() {}

    public static void onServerStarted(MinecraftServer server) {
        ImageStorage.init(server);
        CanvasRoom.clear();
    }

    @SuppressWarnings("unused")
    public static void onServerTick(MinecraftServer server) {
        CanvasRoom.tick();
    }

    public static void onPlayerJoin(ServerPlayer player) {
        ServerSettingsSync.sendTo(player);
    }

    public static void onPlayerLeave(ServerPlayer player) {
        player.server.execute(() -> {
            PosterPresence.disconnect(player);
            PacketThrottle.remove(player.getUUID());
            ImageUploadAssembler.clear(player);
        });
    }
}
