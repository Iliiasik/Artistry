package iliiasik.artistry.network;

import iliiasik.artistry.Artistry;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;

public record ServerSettingsS2CPacket(boolean disableImages) implements CustomPayload {

    public static final Id<ServerSettingsS2CPacket> ID = new Id<>(Artistry.id("server_settings"));
    public static final PacketCodec<PacketByteBuf, ServerSettingsS2CPacket> CODEC =
            PacketCodec.of(ServerSettingsS2CPacket::write, ServerSettingsS2CPacket::read);

    private static void write(ServerSettingsS2CPacket p, PacketByteBuf buf) {
        buf.writeBoolean(p.disableImages);
    }

    private static ServerSettingsS2CPacket read(PacketByteBuf buf) {
        return new ServerSettingsS2CPacket(buf.readBoolean());
    }

    @Override
    public Id<? extends CustomPayload> getId() { return ID; }
}