package iliiasik.artistry.network;

import iliiasik.artistry.Artistry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

import java.util.UUID;

public record MoveCanvasImageC2SPacket(PosterTarget target, UUID uuid,
                                       int gridX, int gridY, int gridW, int gridH) implements ArtistryPacket {

    public static final ResourceLocation ID = Artistry.id("move_canvas_image");

    @Override
    public ResourceLocation id() {
        return ID;
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        PosterTarget.write(buf, target);
        buf.writeLong(uuid.getMostSignificantBits());
        buf.writeLong(uuid.getLeastSignificantBits());
        buf.writeInt(gridX);
        buf.writeInt(gridY);
        buf.writeInt(gridW);
        buf.writeInt(gridH);
    }

    public static MoveCanvasImageC2SPacket read(FriendlyByteBuf buf) {
        PosterTarget target = PosterTarget.read(buf);
        UUID uuid = new UUID(buf.readLong(), buf.readLong());
        return new MoveCanvasImageC2SPacket(target, uuid,
                buf.readInt(), buf.readInt(), buf.readInt(), buf.readInt());
    }
}
