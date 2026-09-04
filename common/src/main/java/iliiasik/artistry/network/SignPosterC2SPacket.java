package iliiasik.artistry.network;

import iliiasik.artistry.Artistry;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record SignPosterC2SPacket(PosterTarget target) implements CustomPacketPayload {

    public static final Type<SignPosterC2SPacket> TYPE = new Type<>(Artistry.id("sign_poster"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SignPosterC2SPacket> CODEC =
            StreamCodec.ofMember(SignPosterC2SPacket::write, SignPosterC2SPacket::read);

    private static void write(SignPosterC2SPacket p, RegistryFriendlyByteBuf buf) {
        PosterTarget.CODEC.encode(buf, p.target);
    }

    private static SignPosterC2SPacket read(RegistryFriendlyByteBuf buf) {
        return new SignPosterC2SPacket(PosterTarget.CODEC.decode(buf));
    }

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
