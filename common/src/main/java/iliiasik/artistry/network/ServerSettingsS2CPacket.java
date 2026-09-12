package iliiasik.artistry.network;

import iliiasik.artistry.Artistry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

public record ServerSettingsS2CPacket(boolean disableImages, long batchIntervalMs,
                                      long cursorIntervalMs) implements ArtistryPacket {

    public static final ResourceLocation ID = Artistry.id("server_settings");

    @Override
    public ResourceLocation id() {
        return ID;
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeBoolean(disableImages);
        buf.writeVarLong(batchIntervalMs);
        buf.writeVarLong(cursorIntervalMs);
    }

    public static ServerSettingsS2CPacket read(FriendlyByteBuf buf) {
        boolean disableImages = buf.readBoolean();
        long batch = buf.readVarLong();
        long cursor = buf.readVarLong();
        return new ServerSettingsS2CPacket(disableImages, batch, cursor);
    }
}
