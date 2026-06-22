package iliiasik.artistry.network;

import iliiasik.artistry.Artistry;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record PosterRemovedS2CPacket(BlockPos pos) implements CustomPacketPayload {

    public static final Type<PosterRemovedS2CPacket> TYPE = new Type<>(Artistry.id("poster_removed"));
    public static final StreamCodec<RegistryFriendlyByteBuf, PosterRemovedS2CPacket> CODEC =
            StreamCodec.ofMember(PosterRemovedS2CPacket::write, PosterRemovedS2CPacket::read);

    private static void write(PosterRemovedS2CPacket p, RegistryFriendlyByteBuf buf) {
        buf.writeBlockPos(p.pos);
    }

    private static PosterRemovedS2CPacket read(RegistryFriendlyByteBuf buf) {
        return new PosterRemovedS2CPacket(buf.readBlockPos());
    }

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}