package iliiasik.artistry.network;

import iliiasik.artistry.Artistry;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;

public record ServerSettingsS2CPacket(boolean disableImages, long batchIntervalMs, long cursorIntervalMs)
        implements CustomPayload {

    public static final Id<ServerSettingsS2CPacket> ID = new Id<>(Artistry.id("server_settings"));
    public static final PacketCodec<PacketByteBuf, ServerSettingsS2CPacket> CODEC =
            PacketCodec.of(ServerSettingsS2CPacket::write, ServerSettingsS2CPacket::read);

    private static void write(ServerSettingsS2CPacket p, PacketByteBuf buf) {
        buf.writeBoolean(p.disableImages);
        buf.writeVarLong(p.batchIntervalMs);
        buf.writeVarLong(p.cursorIntervalMs);
    }

    private static ServerSettingsS2CPacket read(PacketByteBuf buf) {
        boolean disableImages = buf.readBoolean();
        long batch = buf.readVarLong();
        long cursor = buf.readVarLong();
        return new ServerSettingsS2CPacket(disableImages, batch, cursor);
    }

    @Override
    public Id<? extends CustomPayload> getId() { return ID; }
}