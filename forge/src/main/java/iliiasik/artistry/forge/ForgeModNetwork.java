package iliiasik.artistry.forge;

import iliiasik.artistry.Artistry;
import iliiasik.artistry.client.network.ClientPacketHandler;
import iliiasik.artistry.network.ArtistryPacket;
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
import iliiasik.artistry.network.SyncCanvasS2CPacket;
import iliiasik.artistry.network.SyncImageLayerS2CPacket;
import iliiasik.artistry.network.SyncImageLockS2CPacket;
import iliiasik.artistry.network.SyncSignatureS2CPacket;
import iliiasik.artistry.network.TogglePixelizeC2SPacket;
import iliiasik.artistry.network.UploadImageC2SPacket;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

public final class ForgeModNetwork {

    private static final String PROTOCOL = "1";

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            Artistry.id("main"), () -> PROTOCOL, PROTOCOL::equals, PROTOCOL::equals);

    private ForgeModNetwork() {}

    @SuppressWarnings("Convert2MethodRef")
    public static void register() {
        int id = 0;

        toServer(id++, SetCanvasSizeC2SPacket.class, SetCanvasSizeC2SPacket::read,
                (packet, player) -> ServerPacketHandlers.onSetCanvasSize(packet, player));
        toServer(id++, SaveCanvasC2SPacket.class, SaveCanvasC2SPacket::read,
                (packet, player) -> ServerPacketHandlers.onSaveCanvas(packet, player));
        toServer(id++, UploadImageC2SPacket.class, UploadImageC2SPacket::read,
                (packet, player) -> ServerPacketHandlers.onUploadImage(packet, player));
        toServer(id++, MoveCanvasImageC2SPacket.class, MoveCanvasImageC2SPacket::read,
                (packet, player) -> ServerPacketHandlers.onMoveImage(packet, player));
        toServer(id++, DeleteCanvasImageC2SPacket.class, DeleteCanvasImageC2SPacket::read,
                (packet, player) -> ServerPacketHandlers.onDeleteImage(packet, player));
        toServer(id++, TogglePixelizeC2SPacket.class, TogglePixelizeC2SPacket::read,
                (packet, player) -> ServerPacketHandlers.onTogglePixelize(packet, player));
        toServer(id++, LockCanvasImageC2SPacket.class, LockCanvasImageC2SPacket::read,
                (packet, player) -> ServerPacketHandlers.onLockImage(packet, player));
        toServer(id++, RequestImageC2SPacket.class, RequestImageC2SPacket::read,
                (packet, player) -> ServerPacketHandlers.onRequestImage(packet, player));
        toServer(id++, CanvasViewC2SPacket.class, CanvasViewC2SPacket::read,
                (packet, player) -> ServerPacketHandlers.onCanvasView(packet, player));
        toServer(id++, CanvasCursorC2SPacket.class, CanvasCursorC2SPacket::read,
                (packet, player) -> ServerPacketHandlers.onCanvasCursor(packet, player));
        toServer(id++, CanvasEnterRequestC2SPacket.class, CanvasEnterRequestC2SPacket::read,
                (packet, player) -> ServerPacketHandlers.onCanvasEnterRequest(packet, player));
        toServer(id++, SignPosterC2SPacket.class, SignPosterC2SPacket::read,
                (packet, player) -> ServerPacketHandlers.onSignPoster(packet, player));

        toClient(id++, SyncCanvasS2CPacket.class, SyncCanvasS2CPacket::read,
                packet -> ClientPacketHandler.onSyncCanvas(packet));
        toClient(id++, PosterRemovedS2CPacket.class, PosterRemovedS2CPacket::read,
                packet -> ClientPacketHandler.onPosterRemoved(packet));
        toClient(id++, ImageUploadedS2CPacket.class, ImageUploadedS2CPacket::read,
                packet -> ClientPacketHandler.onImageUploaded(packet));
        toClient(id++, DeliverImageS2CPacket.class, DeliverImageS2CPacket::read,
                packet -> ClientPacketHandler.onDeliverImage(packet));
        toClient(id++, SyncImageLayerS2CPacket.class, SyncImageLayerS2CPacket::read,
                packet -> ClientPacketHandler.onSyncImageLayer(packet));
        toClient(id++, SyncImageLockS2CPacket.class, SyncImageLockS2CPacket::read,
                packet -> ClientPacketHandler.onSyncImageLock(packet));
        toClient(id++, ImageEvictedS2CPacket.class, ImageEvictedS2CPacket::read,
                packet -> ClientPacketHandler.onImageEvicted(packet));
        toClient(id++, CanvasCursorS2CPacket.class, CanvasCursorS2CPacket::read,
                packet -> ClientPacketHandler.onCanvasCursor(packet));
        toClient(id++, CanvasPresenceLeaveS2CPacket.class, CanvasPresenceLeaveS2CPacket::read,
                packet -> ClientPacketHandler.onCanvasPresenceLeave(packet));
        toClient(id++, CanvasEnterAllowedS2CPacket.class, CanvasEnterAllowedS2CPacket::read,
                packet -> ClientPacketHandler.onCanvasEnterAllowed(packet));
        toClient(id++, ServerSettingsS2CPacket.class, ServerSettingsS2CPacket::read,
                packet -> ClientPacketHandler.onServerSettings(packet));
        toClient(id, SyncSignatureS2CPacket.class, SyncSignatureS2CPacket::read,
                packet -> ClientPacketHandler.onSyncSignature(packet));
    }

    private static <T extends ArtistryPacket> void toServer(int id, Class<T> type,
                                                            Function<FriendlyByteBuf, T> decoder,
                                                            BiConsumer<T, ServerPlayer> handler) {
        CHANNEL.registerMessage(id, type, ArtistryPacket::write, decoder,
                (packet, ctxSupplier) -> {
                    NetworkEvent.Context ctx = ctxSupplier.get();
                    ctx.enqueueWork(() -> {
                        ServerPlayer player = ctx.getSender();
                        if (player != null) handler.accept(packet, player);
                    });
                    ctx.setPacketHandled(true);
                },
                Optional.of(NetworkDirection.PLAY_TO_SERVER));
    }

    private static <T extends ArtistryPacket> void toClient(int id, Class<T> type,
                                                            Function<FriendlyByteBuf, T> decoder,
                                                            Consumer<T> handler) {
        CHANNEL.registerMessage(id, type, ArtistryPacket::write, decoder,
                (packet, ctxSupplier) -> {
                    NetworkEvent.Context ctx = ctxSupplier.get();
                    ctx.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                            () -> () -> handler.accept(packet)));
                    ctx.setPacketHandled(true);
                },
                Optional.of(NetworkDirection.PLAY_TO_CLIENT));
    }
}
