package iliiasik.artistry.network;

import iliiasik.artistry.config.ArtistryConfig;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;

public final class ServerSettingsSync {

    private ServerSettingsSync() {}

    public static ServerSettingsS2CPacket packet() {
        ArtistryConfig config = ArtistryConfig.get();
        return new ServerSettingsS2CPacket(
                config.poster.disableImages,
                config.network.batchIntervalMs,
                config.network.cursorIntervalMs);
    }

    public static void sendTo(ServerPlayer player) {
        ArtistryNetwork.sendToPlayer(player, packet());
    }

    public static int broadcast(MinecraftServer server) {
        ServerSettingsS2CPacket settings = packet();
        List<ServerPlayer> players = server.getPlayerList().getPlayers();
        for (ServerPlayer player : players) {
            ArtistryNetwork.sendToPlayer(player, settings);
        }
        return players.size();
    }
}
