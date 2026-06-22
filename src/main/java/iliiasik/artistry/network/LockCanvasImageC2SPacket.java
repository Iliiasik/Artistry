package iliiasik.artistry.network;

import iliiasik.artistry.Artistry;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.UUID;

public record LockCanvasImageC2SPacket(PosterTarget target, UUID imageUuid, boolean lock) implements CustomPacketPayload {

    public static final Type<LockCanvasImageC2SPacket> TYPE = new Type<>(Artistry.id("lock_canvas_image"));
    public static final StreamCodec<RegistryFriendlyByteBuf, LockCanvasImageC2SPacket> CODEC =
            StreamCodec.ofMember(LockCanvasImageC2SPacket::write, LockCanvasImageC2SPacket::read);

    private static void write(LockCanvasImageC2SPacket p, RegistryFriendlyByteBuf buf) {
        PosterTarget.CODEC.encode(buf, p.target);
        buf.writeLong(p.imageUuid.getMostSignificantBits());
        buf.writeLong(p.imageUuid.getLeastSignificantBits());
        buf.writeBoolean(p.lock);
    }

    private static LockCanvasImageC2SPacket read(RegistryFriendlyByteBuf buf) {
        PosterTarget target = PosterTarget.CODEC.decode(buf);
        UUID uuid = new UUID(buf.readLong(), buf.readLong());
        return new LockCanvasImageC2SPacket(target, uuid, buf.readBoolean());
    }

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}