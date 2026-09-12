package iliiasik.artistry.client.network;

import iliiasik.artistry.network.ChunkAssembler;
import iliiasik.artistry.network.DeliverImageS2CPacket;
import org.jetbrains.annotations.Nullable;

public final class ClientImageAssembler {

    private static final ChunkAssembler ASSEMBLER = new ChunkAssembler();

    private ClientImageAssembler() {}

    public static byte @Nullable [] accept(DeliverImageS2CPacket packet) {
        return ASSEMBLER.accept(packet.uuid(), packet.total(), packet.offset(), packet.chunk());
    }

    public static void clear() {
        ASSEMBLER.clear();
    }
}
