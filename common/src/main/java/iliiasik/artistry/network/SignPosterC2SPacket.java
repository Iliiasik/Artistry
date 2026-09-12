package iliiasik.artistry.network;

import iliiasik.artistry.Artistry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

public record SignPosterC2SPacket(PosterTarget target) implements ArtistryPacket {

    public static final ResourceLocation ID = Artistry.id("sign_poster");

    @Override
    public ResourceLocation id() {
        return ID;
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        PosterTarget.write(buf, target);
    }

    public static SignPosterC2SPacket read(FriendlyByteBuf buf) {
        return new SignPosterC2SPacket(PosterTarget.read(buf));
    }
}
