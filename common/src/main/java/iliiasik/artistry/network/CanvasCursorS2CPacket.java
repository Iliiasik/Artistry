package iliiasik.artistry.network;

import iliiasik.artistry.Artistry;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.UUID;

public record CanvasCursorS2CPacket(BlockPos pos, UUID uuid, short gx, short gy) implements CustomPacketPayload {

    public static final Type<CanvasCursorS2CPacket> TYPE = new Type<>(Artistry.id("canvas_cursor_sync"));
    public static final StreamCodec<RegistryFriendlyByteBuf, CanvasCursorS2CPacket> CODEC =
            StreamCodec.ofMember(CanvasCursorS2CPacket::write, CanvasCursorS2CPacket::read);

    private static void write(CanvasCursorS2CPacket p, RegistryFriendlyByteBuf buf) {
        buf.writeBlockPos(p.pos);
        buf.writeLong(p.uuid.getMostSignificantBits());
        buf.writeLong(p.uuid.getLeastSignificantBits());
        buf.writeShort(p.gx);
        buf.writeShort(p.gy);
    }

    private static CanvasCursorS2CPacket read(RegistryFriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        UUID uuid = new UUID(buf.readLong(), buf.readLong());
        return new CanvasCursorS2CPacket(pos, uuid, buf.readShort(), buf.readShort());
    }

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}