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
import iliiasik.artistry.network.SyncSignatureS2CPacket;
import iliiasik.artistry.network.SyncCanvasS2CPacket;
import iliiasik.artistry.network.SyncImageLayerS2CPacket;
import iliiasik.artistry.network.SyncImageLockS2CPacket;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

@SuppressWarnings("resource")
public final class FabricModNetworkClient {

    private FabricModNetworkClient() {}

    public static void register() {
        ClientPlayNetworking.registerGlobalReceiver(SyncCanvasS2CPacket.TYPE, (payload, context) ->
                context.client().execute(() -> ClientPacketHandler.onSyncCanvas(payload)));
        ClientPlayNetworking.registerGlobalReceiver(PosterRemovedS2CPacket.TYPE, (payload, context) ->
                context.client().execute(() -> ClientPacketHandler.onPosterRemoved(payload)));
        ClientPlayNetworking.registerGlobalReceiver(ImageUploadedS2CPacket.TYPE, (payload, context) ->
                context.client().execute(() -> ClientPacketHandler.onImageUploaded(payload)));
        ClientPlayNetworking.registerGlobalReceiver(DeliverImageS2CPacket.TYPE, (payload, context) ->
                context.client().execute(() -> ClientPacketHandler.onDeliverImage(payload)));
        ClientPlayNetworking.registerGlobalReceiver(SyncImageLayerS2CPacket.TYPE, (payload, context) ->
                context.client().execute(() -> ClientPacketHandler.onSyncImageLayer(payload)));
        ClientPlayNetworking.registerGlobalReceiver(SyncImageLockS2CPacket.TYPE, (payload, context) ->
                context.client().execute(() -> ClientPacketHandler.onSyncImageLock(payload)));
        ClientPlayNetworking.registerGlobalReceiver(ImageEvictedS2CPacket.TYPE, (payload, context) ->
                context.client().execute(() -> ClientPacketHandler.onImageEvicted(payload)));
        ClientPlayNetworking.registerGlobalReceiver(CanvasCursorS2CPacket.TYPE, (payload, context) ->
                context.client().execute(() -> ClientPacketHandler.onCanvasCursor(payload)));
        ClientPlayNetworking.registerGlobalReceiver(CanvasPresenceLeaveS2CPacket.TYPE, (payload, context) ->
                context.client().execute(() -> ClientPacketHandler.onCanvasPresenceLeave(payload)));
        ClientPlayNetworking.registerGlobalReceiver(CanvasEnterAllowedS2CPacket.TYPE, (payload, context) ->
                context.client().execute(() -> ClientPacketHandler.onCanvasEnterAllowed(payload)));
        ClientPlayNetworking.registerGlobalReceiver(ServerSettingsS2CPacket.TYPE, (payload, context) ->
                context.client().execute(() -> ClientPacketHandler.onServerSettings(payload)));
        ClientPlayNetworking.registerGlobalReceiver(SyncSignatureS2CPacket.TYPE, (payload, context) ->
                context.client().execute(() -> ClientPacketHandler.onSyncSignature(payload)));
    }
}
