package iliiasik.artistry.network;

import iliiasik.artistry.Artistry;
import iliiasik.artistry.data.CanvasData;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.List;

public record SyncCanvasS2CPacket(BlockPos pos, List<CanvasData.PixelChange> changes)
        implements CustomPacketPayload {

    public static final Type<SyncCanvasS2CPacket> TYPE = new Type<>(Artistry.id("sync_canvas"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SyncCanvasS2CPacket> CODEC =
            StreamCodec.ofMember(SyncCanvasS2CPacket::write, SyncCanvasS2CPacket::read);

    private static void write(SyncCanvasS2CPacket p, RegistryFriendlyByteBuf buf) {
        buf.writeBlockPos(p.pos);
        CanvasData.PixelChange.writeList(buf, p.changes);
    }

    private static SyncCanvasS2CPacket read(RegistryFriendlyByteBuf buf) {
        return new SyncCanvasS2CPacket(buf.readBlockPos(), CanvasData.PixelChange.readList(buf));
    }

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}