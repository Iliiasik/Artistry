package iliiasik.artistry.fabric;

import iliiasik.artistry.client.network.ClientPacketHandler;
import iliiasik.artistry.network.CanvasCursorS2CPacket;
import iliiasik.artistry.network.CanvasEnterAllowedS2CPacket;
import iliiasik.artistry.network.CanvasPresenceLeaveS2CPacket;
import iliiasik.artistry.network.DeliverImageS2CPacket;
import iliiasik.artistry.network.ImageEvictedS2CPacket;
import iliiasik.artistry.network.ImageUploadedS2CPacket;
import iliiasik.artistry.network.PosterRemovedS2CPacket;
import iliiasik.artistry.network.ServerSettingsS2CPacket;
import iliiasik.artistry.network.SyncCanvasS2CPacket;
import iliiasik.artistry.network.SyncImageLayerS2CPacket;
import iliiasik.artistry.network.SyncImageLockS2CPacket;
import iliiasik.artistry.network.SyncSignatureS2CPacket;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

import java.util.function.Consumer;
import java.util.function.Function;

public final class FabricModNetworkClient {

    private FabricModNetworkClient() {}

    public static void register() {
        receive(SyncCanvasS2CPacket.ID, SyncCanvasS2CPacket::read, ClientPacketHandler::onSyncCanvas);
        receive(PosterRemovedS2CPacket.ID, PosterRemovedS2CPacket::read, ClientPacketHandler::onPosterRemoved);
        receive(ImageUploadedS2CPacket.ID, ImageUploadedS2CPacket::read, ClientPacketHandler::onImageUploaded);
        receive(DeliverImageS2CPacket.ID, DeliverImageS2CPacket::read, ClientPacketHandler::onDeliverImage);
        receive(SyncImageLayerS2CPacket.ID, SyncImageLayerS2CPacket::read, ClientPacketHandler::onSyncImageLayer);
        receive(SyncImageLockS2CPacket.ID, SyncImageLockS2CPacket::read, ClientPacketHandler::onSyncImageLock);
        receive(ImageEvictedS2CPacket.ID, ImageEvictedS2CPacket::read, ClientPacketHandler::onImageEvicted);
        receive(CanvasCursorS2CPacket.ID, CanvasCursorS2CPacket::read, ClientPacketHandler::onCanvasCursor);
        receive(CanvasPresenceLeaveS2CPacket.ID, CanvasPresenceLeaveS2CPacket::read, ClientPacketHandler::onCanvasPresenceLeave);
        receive(CanvasEnterAllowedS2CPacket.ID, CanvasEnterAllowedS2CPacket::read, ClientPacketHandler::onCanvasEnterAllowed);
        receive(ServerSettingsS2CPacket.ID, ServerSettingsS2CPacket::read, ClientPacketHandler::onServerSettings);
        receive(SyncSignatureS2CPacket.ID, SyncSignatureS2CPacket::read, ClientPacketHandler::onSyncSignature);
    }

    private static <T> void receive(ResourceLocation channel,
                                    Function<FriendlyByteBuf, T> decoder,
                                    Consumer<T> handler) {
        ClientPlayNetworking.registerGlobalReceiver(channel, (client, listener, buf, responseSender) -> {
            T packet = decoder.apply(buf);
            client.execute(() -> handler.accept(packet));
        });
    }
}
