package iliiasik.artistry.network;

import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

public final class ImageUploadAssembler {

    private static final ChunkAssembler ASSEMBLER = new ChunkAssembler();

    private ImageUploadAssembler() {}

    public static byte @Nullable [] accept(ServerPlayer player, UploadImageC2SPacket packet) {
        return ASSEMBLER.accept(player.getUUID(), packet.total(), packet.offset(), packet.chunk());
    }

    public static void clear(ServerPlayer player) {
        ASSEMBLER.forget(player.getUUID());
    }
}
