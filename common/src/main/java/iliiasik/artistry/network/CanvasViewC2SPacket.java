package iliiasik.artistry.network;

import iliiasik.artistry.Artistry;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record CanvasViewC2SPacket(BlockPos pos, boolean open) implements CustomPacketPayload {

    public static final Type<CanvasViewC2SPacket> TYPE = new Type<>(Artistry.id("canvas_view"));
    public static final StreamCodec<RegistryFriendlyByteBuf, CanvasViewC2SPacket> CODEC =
            StreamCodec.ofMember(CanvasViewC2SPacket::write, CanvasViewC2SPacket::read);

    private static void write(CanvasViewC2SPacket p, RegistryFriendlyByteBuf buf) {
        buf.writeBlockPos(p.pos);
        buf.writeBoolean(p.open);
    }

    private static CanvasViewC2SPacket read(RegistryFriendlyByteBuf buf) {
        return new CanvasViewC2SPacket(buf.readBlockPos(), buf.readBoolean());
    }

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}