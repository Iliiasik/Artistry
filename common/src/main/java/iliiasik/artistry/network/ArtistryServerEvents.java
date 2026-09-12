package iliiasik.artistry.network;

import iliiasik.artistry.config.ArtistryConfig;
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
        ArtistryConfig cfg = ArtistryConfig.get();
        ArtistryNetwork.sendToPlayer(player, new ServerSettingsS2CPacket(
                cfg.poster.disableImages, cfg.network.batchIntervalMs, cfg.network.cursorIntervalMs));
    }

    public static void onPlayerLeave(ServerPlayer player) {
        PosterPresence.disconnect(player);
        PacketThrottle.remove(player.getUUID());
        ImageUploadAssembler.clear(player);
    }
}
