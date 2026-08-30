package iliiasik.artistry.network;

import iliiasik.artistry.Artistry;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record ServerSettingsS2CPacket(boolean disableImages, long batchIntervalMs, long cursorIntervalMs)
        implements CustomPacketPayload {

    public static final Type<ServerSettingsS2CPacket> TYPE = new Type<>(Artistry.id("server_settings"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ServerSettingsS2CPacket> CODEC =
            StreamCodec.ofMember(ServerSettingsS2CPacket::write, ServerSettingsS2CPacket::read);

    private static void write(ServerSettingsS2CPacket p, RegistryFriendlyByteBuf buf) {
        buf.writeBoolean(p.disableImages);
        buf.writeVarLong(p.batchIntervalMs);
        buf.writeVarLong(p.cursorIntervalMs);
    }

    private static ServerSettingsS2CPacket read(RegistryFriendlyByteBuf buf) {
        boolean disableImages = buf.readBoolean();
        long batch = buf.readVarLong();
        long cursor = buf.readVarLong();
        return new ServerSettingsS2CPacket(disableImages, batch, cursor);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}