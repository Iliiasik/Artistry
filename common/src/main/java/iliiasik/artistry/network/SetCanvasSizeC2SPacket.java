package iliiasik.artistry.network;

import iliiasik.artistry.Artistry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

public record SetCanvasSizeC2SPacket(PosterTarget target, int size) implements ArtistryPacket {

    public static final ResourceLocation ID = Artistry.id("set_canvas_size");

    @Override
    public ResourceLocation id() {
        return ID;
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        PosterTarget.write(buf, target);
        buf.writeByte(size);
    }

    public static SetCanvasSizeC2SPacket read(FriendlyByteBuf buf) {
        PosterTarget target = PosterTarget.read(buf);
        return new SetCanvasSizeC2SPacket(target, buf.readByte() & 0xFF);
    }
}
