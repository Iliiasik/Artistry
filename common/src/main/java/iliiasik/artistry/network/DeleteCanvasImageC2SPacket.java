package iliiasik.artistry.network;

import iliiasik.artistry.Artistry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

import java.util.UUID;

public record DeleteCanvasImageC2SPacket(PosterTarget target, UUID uuid) implements ArtistryPacket {

    public static final ResourceLocation ID = Artistry.id("delete_canvas_image");

    @Override
    public ResourceLocation id() {
        return ID;
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        PosterTarget.write(buf, target);
        buf.writeLong(uuid.getMostSignificantBits());
        buf.writeLong(uuid.getLeastSignificantBits());
    }

    public static DeleteCanvasImageC2SPacket read(FriendlyByteBuf buf) {
        PosterTarget target = PosterTarget.read(buf);
        return new DeleteCanvasImageC2SPacket(target, new UUID(buf.readLong(), buf.readLong()));
    }
}
