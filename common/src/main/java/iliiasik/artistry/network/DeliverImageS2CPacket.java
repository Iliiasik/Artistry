package iliiasik.artistry.network;

import iliiasik.artistry.Artistry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

import java.util.UUID;

public record DeliverImageS2CPacket(UUID uuid, int total, int offset,
                                    byte[] chunk) implements ArtistryPacket {

    public static final ResourceLocation ID = Artistry.id("deliver_image");

    @Override
    public ResourceLocation id() {
        return ID;
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeLong(uuid.getMostSignificantBits());
        buf.writeLong(uuid.getLeastSignificantBits());
        buf.writeInt(total);
        buf.writeInt(offset);
        buf.writeInt(chunk.length);
        buf.writeBytes(chunk);
    }

    public static DeliverImageS2CPacket read(FriendlyByteBuf buf) {
        UUID uuid = new UUID(buf.readLong(), buf.readLong());
        int total = buf.readInt();
        int offset = buf.readInt();
        int len = buf.readInt();
        byte[] chunk = new byte[len];
        buf.readBytes(chunk);
        return new DeliverImageS2CPacket(uuid, total, offset, chunk);
    }
}
