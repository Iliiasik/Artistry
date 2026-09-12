package iliiasik.artistry.forge;

import iliiasik.artistry.network.ArtistryPacket;
import iliiasik.artistry.platform.services.INetworkHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.PacketDistributor;

public class ForgeNetworkHelper implements INetworkHelper {

    @Override
    public void sendToPlayer(ServerPlayer player, ArtistryPacket packet) {
        ForgeModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
    }

    @Override
    public void sendToServer(ArtistryPacket packet) {
        ForgeModNetwork.CHANNEL.sendToServer(packet);
    }
}
