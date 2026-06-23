package iliiasik.artistry.network;

import net.minecraft.network.FriendlyByteBuf;

public record ServerSettingsS2CPacket(boolean disableImages, long batchIntervalMs, long cursorIntervalMs) {
    public static void encode(ServerSettingsS2CPacket p, FriendlyByteBuf buf) {
        buf.writeBoolean(p.disableImages);
        buf.writeVarLong(p.batchIntervalMs);
        buf.writeVarLong(p.cursorIntervalMs);
    }
    public static ServerSettingsS2CPacket decode(FriendlyByteBuf buf) {
        boolean disableImages = buf.readBoolean();
        long batch = buf.readVarLong();
        long cursor = buf.readVarLong();
        return new ServerSettingsS2CPacket(disableImages, batch, cursor);
    }
}