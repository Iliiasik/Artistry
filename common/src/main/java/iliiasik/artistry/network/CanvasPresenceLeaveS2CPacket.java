package iliiasik.artistry.network;

import iliiasik.artistry.Artistry;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.UUID;

public record CanvasPresenceLeaveS2CPacket(BlockPos pos, UUID uuid) implements CustomPacketPayload {

    public static final Type<CanvasPresenceLeaveS2CPacket> TYPE = new Type<>(Artistry.id("canvas_presence_leave"));
    public static final StreamCodec<RegistryFriendlyByteBuf, CanvasPresenceLeaveS2CPacket> CODEC =
            StreamCodec.ofMember(CanvasPresenceLeaveS2CPacket::write, CanvasPresenceLeaveS2CPacket::read);

    private static void write(CanvasPresenceLeaveS2CPacket p, RegistryFriendlyByteBuf buf) {
        buf.writeBlockPos(p.pos);
        buf.writeLong(p.uuid.getMostSignificantBits());
        buf.writeLong(p.uuid.getLeastSignificantBits());
    }

    private static CanvasPresenceLeaveS2CPacket read(RegistryFriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        return new CanvasPresenceLeaveS2CPacket(pos, new UUID(buf.readLong(), buf.readLong()));
    }

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}