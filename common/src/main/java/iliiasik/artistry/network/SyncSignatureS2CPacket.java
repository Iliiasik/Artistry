package iliiasik.artistry.network;

import iliiasik.artistry.Artistry;
import iliiasik.artistry.data.CanvasSignature;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.jetbrains.annotations.Nullable;

public record SyncSignatureS2CPacket(@Nullable BlockPos pos,
                                     @Nullable String playerName) implements CustomPacketPayload {

    public static final Type<SyncSignatureS2CPacket> TYPE = new Type<>(Artistry.id("sync_signature"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SyncSignatureS2CPacket> CODEC =
            StreamCodec.ofMember(SyncSignatureS2CPacket::write, SyncSignatureS2CPacket::read);

    private static void write(SyncSignatureS2CPacket p, RegistryFriendlyByteBuf buf) {
        buf.writeBoolean(p.pos != null);
        if (p.pos != null) buf.writeBlockPos(p.pos);
        buf.writeBoolean(p.playerName != null);
        if (p.playerName != null) buf.writeUtf(p.playerName, CanvasSignature.MAX_NAME_LENGTH);
    }

    private static SyncSignatureS2CPacket read(RegistryFriendlyByteBuf buf) {
        BlockPos pos = buf.readBoolean() ? buf.readBlockPos() : null;
        String name = buf.readBoolean() ? buf.readUtf(CanvasSignature.MAX_NAME_LENGTH) : null;
        return new SyncSignatureS2CPacket(pos, name);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
