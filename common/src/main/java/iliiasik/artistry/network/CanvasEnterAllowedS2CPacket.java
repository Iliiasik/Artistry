package iliiasik.artistry.network;

import iliiasik.artistry.Artistry;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record CanvasEnterAllowedS2CPacket(BlockPos pos, boolean needsSize) implements CustomPacketPayload {

    public static final Type<CanvasEnterAllowedS2CPacket> TYPE = new Type<>(Artistry.id("canvas_enter_allowed"));
    public static final StreamCodec<RegistryFriendlyByteBuf, CanvasEnterAllowedS2CPacket> CODEC =
            StreamCodec.ofMember(CanvasEnterAllowedS2CPacket::write, CanvasEnterAllowedS2CPacket::read);

    private static void write(CanvasEnterAllowedS2CPacket p, RegistryFriendlyByteBuf buf) {
        buf.writeBlockPos(p.pos);
        buf.writeBoolean(p.needsSize);
    }

    private static CanvasEnterAllowedS2CPacket read(RegistryFriendlyByteBuf buf) {
        return new CanvasEnterAllowedS2CPacket(buf.readBlockPos(), buf.readBoolean());
    }

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}