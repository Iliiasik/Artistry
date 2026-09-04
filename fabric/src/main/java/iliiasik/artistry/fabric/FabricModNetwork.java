package iliiasik.artistry.fabric;

import iliiasik.artistry.network.CanvasCursorC2SPacket;
import iliiasik.artistry.network.CanvasCursorS2CPacket;
import iliiasik.artistry.network.CanvasEnterAllowedS2CPacket;
import iliiasik.artistry.network.CanvasEnterRequestC2SPacket;
import iliiasik.artistry.network.CanvasPresenceLeaveS2CPacket;
import iliiasik.artistry.network.CanvasViewC2SPacket;
import iliiasik.artistry.network.DeleteCanvasImageC2SPacket;
import iliiasik.artistry.network.DeliverImageS2CPacket;
import iliiasik.artistry.network.ImageEvictedS2CPacket;
import iliiasik.artistry.network.ImageUploadedS2CPacket;
import iliiasik.artistry.network.LockCanvasImageC2SPacket;
import iliiasik.artistry.network.MoveCanvasImageC2SPacket;
import iliiasik.artistry.network.PosterRemovedS2CPacket;
import iliiasik.artistry.network.RequestImageC2SPacket;
import iliiasik.artistry.network.SaveCanvasC2SPacket;
import iliiasik.artistry.network.ServerPacketHandlers;
import iliiasik.artistry.network.ServerSettingsS2CPacket;
import iliiasik.artistry.network.SetCanvasSizeC2SPacket;
import iliiasik.artistry.network.SignPosterC2SPacket;
import iliiasik.artistry.network.SyncSignatureS2CPacket;
import iliiasik.artistry.network.SyncCanvasS2CPacket;
import iliiasik.artistry.network.SyncImageLayerS2CPacket;
import iliiasik.artistry.network.SyncImageLockS2CPacket;
import iliiasik.artistry.network.TogglePixelizeC2SPacket;
import iliiasik.artistry.network.UploadImageC2SPacket;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

@SuppressWarnings("resource")
public final class FabricModNetwork {

    private FabricModNetwork() {}

    public static void register() {
        PayloadTypeRegistry.playC2S().register(SetCanvasSizeC2SPacket.TYPE, SetCanvasSizeC2SPacket.CODEC);
        PayloadTypeRegistry.playC2S().register(SaveCanvasC2SPacket.TYPE, SaveCanvasC2SPacket.CODEC);
        PayloadTypeRegistry.playC2S().register(UploadImageC2SPacket.TYPE, UploadImageC2SPacket.CODEC);
        PayloadTypeRegistry.playC2S().register(MoveCanvasImageC2SPacket.TYPE, MoveCanvasImageC2SPacket.CODEC);
        PayloadTypeRegistry.playC2S().register(DeleteCanvasImageC2SPacket.TYPE, DeleteCanvasImageC2SPacket.CODEC);
        PayloadTypeRegistry.playC2S().register(TogglePixelizeC2SPacket.TYPE, TogglePixelizeC2SPacket.CODEC);
        PayloadTypeRegistry.playC2S().register(LockCanvasImageC2SPacket.TYPE, LockCanvasImageC2SPacket.CODEC);
        PayloadTypeRegistry.playC2S().register(RequestImageC2SPacket.TYPE, RequestImageC2SPacket.CODEC);
        PayloadTypeRegistry.playC2S().register(CanvasViewC2SPacket.TYPE, CanvasViewC2SPacket.CODEC);
        PayloadTypeRegistry.playC2S().register(CanvasCursorC2SPacket.TYPE, CanvasCursorC2SPacket.CODEC);
        PayloadTypeRegistry.playC2S().register(CanvasEnterRequestC2SPacket.TYPE, CanvasEnterRequestC2SPacket.CODEC);
        PayloadTypeRegistry.playC2S().register(SignPosterC2SPacket.TYPE, SignPosterC2SPacket.CODEC);

        PayloadTypeRegistry.playS2C().register(SyncCanvasS2CPacket.TYPE, SyncCanvasS2CPacket.CODEC);
        PayloadTypeRegistry.playS2C().register(PosterRemovedS2CPacket.TYPE, PosterRemovedS2CPacket.CODEC);
        PayloadTypeRegistry.playS2C().register(ImageUploadedS2CPacket.TYPE, ImageUploadedS2CPacket.CODEC);
        PayloadTypeRegistry.playS2C().register(DeliverImageS2CPacket.TYPE, DeliverImageS2CPacket.CODEC);
        PayloadTypeRegistry.playS2C().register(SyncImageLayerS2CPacket.TYPE, SyncImageLayerS2CPacket.CODEC);
        PayloadTypeRegistry.playS2C().register(SyncImageLockS2CPacket.TYPE, SyncImageLockS2CPacket.CODEC);
        PayloadTypeRegistry.playS2C().register(ImageEvictedS2CPacket.TYPE, ImageEvictedS2CPacket.CODEC);
        PayloadTypeRegistry.playS2C().register(CanvasCursorS2CPacket.TYPE, CanvasCursorS2CPacket.CODEC);
        PayloadTypeRegistry.playS2C().register(CanvasPresenceLeaveS2CPacket.TYPE, CanvasPresenceLeaveS2CPacket.CODEC);
        PayloadTypeRegistry.playS2C().register(CanvasEnterAllowedS2CPacket.TYPE, CanvasEnterAllowedS2CPacket.CODEC);
        PayloadTypeRegistry.playS2C().register(ServerSettingsS2CPacket.TYPE, ServerSettingsS2CPacket.CODEC);
        PayloadTypeRegistry.playS2C().register(SyncSignatureS2CPacket.TYPE, SyncSignatureS2CPacket.CODEC);

        ServerPlayNetworking.registerGlobalReceiver(SetCanvasSizeC2SPacket.TYPE, (payload, context) ->
                context.server().execute(() -> ServerPacketHandlers.onSetCanvasSize(payload, context.player())));
        ServerPlayNetworking.registerGlobalReceiver(SaveCanvasC2SPacket.TYPE, (payload, context) ->
                context.server().execute(() -> ServerPacketHandlers.onSaveCanvas(payload, context.player())));
        ServerPlayNetworking.registerGlobalReceiver(UploadImageC2SPacket.TYPE, (payload, context) ->
                context.server().execute(() -> ServerPacketHandlers.onUploadImage(payload, context.player())));
        ServerPlayNetworking.registerGlobalReceiver(MoveCanvasImageC2SPacket.TYPE, (payload, context) ->
                context.server().execute(() -> ServerPacketHandlers.onMoveImage(payload, context.player())));
        ServerPlayNetworking.registerGlobalReceiver(DeleteCanvasImageC2SPacket.TYPE, (payload, context) ->
                context.server().execute(() -> ServerPacketHandlers.onDeleteImage(payload, context.player())));
        ServerPlayNetworking.registerGlobalReceiver(TogglePixelizeC2SPacket.TYPE, (payload, context) ->
                context.server().execute(() -> ServerPacketHandlers.onTogglePixelize(payload, context.player())));
        ServerPlayNetworking.registerGlobalReceiver(LockCanvasImageC2SPacket.TYPE, (payload, context) ->
                context.server().execute(() -> ServerPacketHandlers.onLockImage(payload, context.player())));
        ServerPlayNetworking.registerGlobalReceiver(RequestImageC2SPacket.TYPE, (payload, context) ->
                context.server().execute(() -> ServerPacketHandlers.onRequestImage(payload, context.player())));
        ServerPlayNetworking.registerGlobalReceiver(CanvasViewC2SPacket.TYPE, (payload, context) ->
                context.server().execute(() -> ServerPacketHandlers.onCanvasView(payload, context.player())));
        ServerPlayNetworking.registerGlobalReceiver(CanvasCursorC2SPacket.TYPE, (payload, context) ->
                context.server().execute(() -> ServerPacketHandlers.onCanvasCursor(payload, context.player())));
        ServerPlayNetworking.registerGlobalReceiver(CanvasEnterRequestC2SPacket.TYPE, (payload, context) ->
                context.server().execute(() -> ServerPacketHandlers.onCanvasEnterRequest(payload, context.player())));
        ServerPlayNetworking.registerGlobalReceiver(SignPosterC2SPacket.TYPE, (payload, context) ->
                context.server().execute(() -> ServerPacketHandlers.onSignPoster(payload, context.player())));
    }
}
