package iliiasik.artistry.network;

import iliiasik.artistry.data.CanvasData;
import net.minecraft.network.FriendlyByteBuf;

import java.util.List;

public record SaveCanvasC2SPacket(PosterTarget target, List<CanvasData.PixelChange> changes) {
    public static void encode(SaveCanvasC2SPacket p, FriendlyByteBuf buf) {
        PosterTarget.write(buf, p.target);
        CanvasData.PixelChange.writeList(buf, p.changes);
    }
    public static SaveCanvasC2SPacket decode(FriendlyByteBuf buf) {
        PosterTarget target = PosterTarget.read(buf);
        return new SaveCanvasC2SPacket(target, CanvasData.PixelChange.readList(buf));
    }
}