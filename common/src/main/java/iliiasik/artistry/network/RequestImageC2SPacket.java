package iliiasik.artistry.network;

import iliiasik.artistry.Artistry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

import java.util.UUID;

public record RequestImageC2SPacket(UUID uuid) implements ArtistryPacket {

    public static final ResourceLocation ID = Artistry.id("request_image");

    @Override
    public ResourceLocation id() {
        return ID;
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeLong(uuid.getMostSignificantBits());
        buf.writeLong(uuid.getLeastSignificantBits());
    }

    public static RequestImageC2SPacket read(FriendlyByteBuf buf) {
        return new RequestImageC2SPacket(new UUID(buf.readLong(), buf.readLong()));
    }
}
