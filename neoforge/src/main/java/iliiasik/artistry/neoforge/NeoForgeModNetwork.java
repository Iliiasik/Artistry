package iliiasik.artistry.neoforge;

import iliiasik.artistry.client.network.ClientPacketHandler;
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
import iliiasik.artistry.network.SyncCanvasS2CPacket;
import iliiasik.artistry.network.SyncImageLayerS2CPacket;
import iliiasik.artistry.network.SyncImageLockS2CPacket;
import iliiasik.artistry.network.TogglePixelizeC2SPacket;
import iliiasik.artistry.network.UploadImageC2SPacket;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class NeoForgeModNetwork {

    private static final String PROTOCOL = "1";

    private NeoForgeModNetwork() {}

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(PROTOCOL);

        registrar.playToServer(SetCanvasSizeC2SPacket.TYPE, SetCanvasSizeC2SPacket.CODEC, (payload, context) -> {
            ServerPlayer player = (ServerPlayer) context.player();
            context.enqueueWork(() -> ServerPacketHandlers.onSetCanvasSize(payload, player));
        });
        registrar.playToServer(SaveCanvasC2SPacket.TYPE, SaveCanvasC2SPacket.CODEC, (payload, context) -> {
            ServerPlayer player = (ServerPlayer) context.player();
            context.enqueueWork(() -> ServerPacketHandlers.onSaveCanvas(payload, player));
        });
        registrar.playToServer(UploadImageC2SPacket.TYPE, UploadImageC2SPacket.CODEC, (payload, context) -> {
            ServerPlayer player = (ServerPlayer) context.player();
            context.enqueueWork(() -> ServerPacketHandlers.onUploadImage(payload, player));
        });
        registrar.playToServer(MoveCanvasImageC2SPacket.TYPE, MoveCanvasImageC2SPacket.CODEC, (payload, context) -> {
            ServerPlayer player = (ServerPlayer) context.player();
            context.enqueueWork(() -> ServerPacketHandlers.onMoveImage(payload, player));
        });
        registrar.playToServer(DeleteCanvasImageC2SPacket.TYPE, DeleteCanvasImageC2SPacket.CODEC, (payload, context) -> {
            ServerPlayer player = (ServerPlayer) context.player();
            context.enqueueWork(() -> ServerPacketHandlers.onDeleteImage(payload, player));
        });
        registrar.playToServer(TogglePixelizeC2SPacket.TYPE, TogglePixelizeC2SPacket.CODEC, (payload, context) -> {
            ServerPlayer player = (ServerPlayer) context.player();
            context.enqueueWork(() -> ServerPacketHandlers.onTogglePixelize(payload, player));
        });
        registrar.playToServer(LockCanvasImageC2SPacket.TYPE, LockCanvasImageC2SPacket.CODEC, (payload, context) -> {
            ServerPlayer player = (ServerPlayer) context.player();
            context.enqueueWork(() -> ServerPacketHandlers.onLockImage(payload, player));
        });
        registrar.playToServer(RequestImageC2SPacket.TYPE, RequestImageC2SPacket.CODEC, (payload, context) -> {
            ServerPlayer player = (ServerPlayer) context.player();
            context.enqueueWork(() -> ServerPacketHandlers.onRequestImage(payload, player));
        });
        registrar.playToServer(CanvasViewC2SPacket.TYPE, CanvasViewC2SPacket.CODEC, (payload, context) -> {
            ServerPlayer player = (ServerPlayer) context.player();
            context.enqueueWork(() -> ServerPacketHandlers.onCanvasView(payload, player));
        });
        registrar.playToServer(CanvasCursorC2SPacket.TYPE, CanvasCursorC2SPacket.CODEC, (payload, context) -> {
            ServerPlayer player = (ServerPlayer) context.player();
            context.enqueueWork(() -> ServerPacketHandlers.onCanvasCursor(payload, player));
        });
        registrar.playToServer(CanvasEnterRequestC2SPacket.TYPE, CanvasEnterRequestC2SPacket.CODEC, (payload, context) -> {
            ServerPlayer player = (ServerPlayer) context.player();
            context.enqueueWork(() -> ServerPacketHandlers.onCanvasEnterRequest(payload, player));
        });

        registrar.playToClient(SyncCanvasS2CPacket.TYPE, SyncCanvasS2CPacket.CODEC,
                (payload, context) -> context.enqueueWork(() -> ClientPacketHandler.onSyncCanvas(payload)));
        registrar.playToClient(PosterRemovedS2CPacket.TYPE, PosterRemovedS2CPacket.CODEC,
                (payload, context) -> context.enqueueWork(() -> ClientPacketHandler.onPosterRemoved(payload)));
        registrar.playToClient(ImageUploadedS2CPacket.TYPE, ImageUploadedS2CPacket.CODEC,
                (payload, context) -> context.enqueueWork(() -> ClientPacketHandler.onImageUploaded(payload)));
        registrar.playToClient(DeliverImageS2CPacket.TYPE, DeliverImageS2CPacket.CODEC,
                (payload, context) -> context.enqueueWork(() -> ClientPacketHandler.onDeliverImage(payload)));
        registrar.playToClient(SyncImageLayerS2CPacket.TYPE, SyncImageLayerS2CPacket.CODEC,
                (payload, context) -> context.enqueueWork(() -> ClientPacketHandler.onSyncImageLayer(payload)));
        registrar.playToClient(SyncImageLockS2CPacket.TYPE, SyncImageLockS2CPacket.CODEC,
                (payload, context) -> context.enqueueWork(() -> ClientPacketHandler.onSyncImageLock(payload)));
        registrar.playToClient(ImageEvictedS2CPacket.TYPE, ImageEvictedS2CPacket.CODEC,
                (payload, context) -> context.enqueueWork(() -> ClientPacketHandler.onImageEvicted(payload)));
        registrar.playToClient(CanvasCursorS2CPacket.TYPE, CanvasCursorS2CPacket.CODEC,
                (payload, context) -> context.enqueueWork(() -> ClientPacketHandler.onCanvasCursor(payload)));
        registrar.playToClient(CanvasPresenceLeaveS2CPacket.TYPE, CanvasPresenceLeaveS2CPacket.CODEC,
                (payload, context) -> context.enqueueWork(() -> ClientPacketHandler.onCanvasPresenceLeave(payload)));
        registrar.playToClient(CanvasEnterAllowedS2CPacket.TYPE, CanvasEnterAllowedS2CPacket.CODEC,
                (payload, context) -> context.enqueueWork(() -> ClientPacketHandler.onCanvasEnterAllowed(payload)));
        registrar.playToClient(ServerSettingsS2CPacket.TYPE, ServerSettingsS2CPacket.CODEC,
                (payload, context) -> context.enqueueWork(() -> ClientPacketHandler.onServerSettings(payload)));
    }
}
