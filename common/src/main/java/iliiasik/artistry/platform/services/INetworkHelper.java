package iliiasik.artistry.platform.services;

import iliiasik.artistry.network.ArtistryPacket;
import net.minecraft.server.level.ServerPlayer;

public interface INetworkHelper {

    void sendToPlayer(ServerPlayer player, ArtistryPacket packet);

    void sendToServer(ArtistryPacket packet);
}
