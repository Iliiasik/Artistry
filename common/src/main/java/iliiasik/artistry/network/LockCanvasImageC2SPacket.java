package iliiasik.artistry.network;

import iliiasik.artistry.Artistry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

import java.util.UUID;

public record LockCanvasImageC2SPacket(PosterTarget target, UUID imageUuid,
                                       boolean lock) implements ArtistryPacket {

    public static final ResourceLocation ID = Artistry.id("lock_canvas_image");

    @Override
    public ResourceLocation id() {
        return ID;
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        PosterTarget.write(buf, target);
        buf.writeLong(imageUuid.getMostSignificantBits());
        buf.writeLong(imageUuid.getLeastSignificantBits());
        buf.writeBoolean(lock);
    }

    public static LockCanvasImageC2SPacket read(FriendlyByteBuf buf) {
        PosterTarget target = PosterTarget.read(buf);
        UUID uuid = new UUID(buf.readLong(), buf.readLong());
        return new LockCanvasImageC2SPacket(target, uuid, buf.readBoolean());
    }
}
