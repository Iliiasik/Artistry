package iliiasik.artistry.network;

import iliiasik.artistry.Artistry;
import iliiasik.artistry.data.CanvasData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

public record SaveCanvasC2SPacket(PosterTarget target,
                                  List<CanvasData.PixelChange> changes) implements ArtistryPacket {

    public static final ResourceLocation ID = Artistry.id("save_canvas");

    @Override
    public ResourceLocation id() {
        return ID;
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        PosterTarget.write(buf, target);
        CanvasData.PixelChange.writeList(buf, changes);
    }

    public static SaveCanvasC2SPacket read(FriendlyByteBuf buf) {
        PosterTarget target = PosterTarget.read(buf);
        return new SaveCanvasC2SPacket(target, CanvasData.PixelChange.readList(buf));
    }
}
