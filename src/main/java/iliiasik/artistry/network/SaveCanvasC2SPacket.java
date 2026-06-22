package iliiasik.artistry.network;

import iliiasik.artistry.Artistry;
import iliiasik.artistry.data.CanvasData;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.List;

public record SaveCanvasC2SPacket(PosterTarget target, List<CanvasData.PixelChange> changes)
        implements CustomPacketPayload {

    public static final Type<SaveCanvasC2SPacket> TYPE = new Type<>(Artistry.id("save_canvas"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SaveCanvasC2SPacket> CODEC =
            StreamCodec.ofMember(SaveCanvasC2SPacket::write, SaveCanvasC2SPacket::read);

    private static void write(SaveCanvasC2SPacket p, RegistryFriendlyByteBuf buf) {
        PosterTarget.CODEC.encode(buf, p.target);
        CanvasData.PixelChange.writeList(buf, p.changes);
    }

    private static SaveCanvasC2SPacket read(RegistryFriendlyByteBuf buf) {
        PosterTarget target = PosterTarget.CODEC.decode(buf);
        return new SaveCanvasC2SPacket(target, CanvasData.PixelChange.readList(buf));
    }

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}