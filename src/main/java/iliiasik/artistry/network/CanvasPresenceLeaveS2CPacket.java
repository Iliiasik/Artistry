package iliiasik.artistry.network;

import iliiasik.artistry.Artistry;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.math.BlockPos;

import java.util.UUID;

public record CanvasPresenceLeaveS2CPacket(BlockPos pos, UUID uuid) implements CustomPayload {

    public static final Id<CanvasPresenceLeaveS2CPacket> ID = new Id<>(Artistry.id("canvas_presence_leave"));
    public static final PacketCodec<PacketByteBuf, CanvasPresenceLeaveS2CPacket> CODEC =
            PacketCodec.of(CanvasPresenceLeaveS2CPacket::write, CanvasPresenceLeaveS2CPacket::read);

    private static void write(CanvasPresenceLeaveS2CPacket p, PacketByteBuf buf) {
        buf.writeBlockPos(p.pos);
        buf.writeLong(p.uuid.getMostSignificantBits());
        buf.writeLong(p.uuid.getLeastSignificantBits());
    }

    private static CanvasPresenceLeaveS2CPacket read(PacketByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        return new CanvasPresenceLeaveS2CPacket(pos, new UUID(buf.readLong(), buf.readLong()));
    }

    @Override
    public Id<? extends CustomPayload> getId() { return ID; }
}