package iliiasik.artistry.network;

import iliiasik.artistry.Artistry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public record ImageEvictedS2CPacket(List<UUID> uuids) implements ArtistryPacket {

    public static final ResourceLocation ID = Artistry.id("image_evicted");

    @Override
    public ResourceLocation id() {
        return ID;
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeInt(uuids.size());
        for (UUID uuid : uuids) {
            buf.writeLong(uuid.getMostSignificantBits());
            buf.writeLong(uuid.getLeastSignificantBits());
        }
    }

    public static ImageEvictedS2CPacket read(FriendlyByteBuf buf) {
        int count = buf.readInt();
        List<UUID> uuids = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            uuids.add(new UUID(buf.readLong(), buf.readLong()));
        }
        return new ImageEvictedS2CPacket(uuids);
    }
}
