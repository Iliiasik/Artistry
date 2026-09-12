package iliiasik.artistry.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

public interface ArtistryPacket {

    ResourceLocation id();

    void write(FriendlyByteBuf buf);
}
