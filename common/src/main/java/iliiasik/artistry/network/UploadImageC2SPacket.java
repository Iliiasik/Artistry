package iliiasik.artistry.network;

import iliiasik.artistry.Artistry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

public record UploadImageC2SPacket(PosterTarget target, int total, int offset,
                                   byte[] chunk) implements ArtistryPacket {

    public static final ResourceLocation ID = Artistry.id("upload_image");

    @Override
    public ResourceLocation id() {
        return ID;
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        PosterTarget.write(buf, target);
        buf.writeInt(total);
        buf.writeInt(offset);
        buf.writeInt(chunk.length);
        buf.writeBytes(chunk);
    }

    public static UploadImageC2SPacket read(FriendlyByteBuf buf) {
        PosterTarget target = PosterTarget.read(buf);
        int total = buf.readInt();
        int offset = buf.readInt();
        int len = buf.readInt();
        byte[] chunk = new byte[len];
        buf.readBytes(chunk);
        return new UploadImageC2SPacket(target, total, offset, chunk);
    }
}
