package iliiasik.artistry.network;

import iliiasik.artistry.Artistry;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record CanvasEnterRequestC2SPacket(BlockPos pos) implements CustomPacketPayload {

    public static final Type<CanvasEnterRequestC2SPacket> TYPE = new Type<>(Artistry.id("canvas_enter_request"));
    public static final StreamCodec<RegistryFriendlyByteBuf, CanvasEnterRequestC2SPacket> CODEC =
            StreamCodec.ofMember(CanvasEnterRequestC2SPacket::write, CanvasEnterRequestC2SPacket::read);

    private static void write(CanvasEnterRequestC2SPacket p, RegistryFriendlyByteBuf buf) {
        buf.writeBlockPos(p.pos);
    }

    private static CanvasEnterRequestC2SPacket read(RegistryFriendlyByteBuf buf) {
        return new CanvasEnterRequestC2SPacket(buf.readBlockPos());
    }

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}