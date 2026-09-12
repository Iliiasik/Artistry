package iliiasik.artistry.fabric;

import iliiasik.artistry.network.ArtistryPacket;
import iliiasik.artistry.platform.services.INetworkHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

public class FabricNetworkHelper implements INetworkHelper {

    @Override
    public void sendToPlayer(ServerPlayer player, ArtistryPacket packet) {
        ServerPlayNetworking.send(player, packet.id(), encode(packet));
    }

    @Override
    public void sendToServer(ArtistryPacket packet) {
        ClientPlayNetworking.send(packet.id(), encode(packet));
    }

    private static FriendlyByteBuf encode(ArtistryPacket packet) {
        FriendlyByteBuf buf = PacketByteBufs.create();
        packet.write(buf);
        return buf;
    }
}
