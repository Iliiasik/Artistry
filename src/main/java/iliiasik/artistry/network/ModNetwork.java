package iliiasik.artistry.network;

import iliiasik.artistry.Artistry;
import iliiasik.artistry.block.entity.PosterBlockEntity;
import iliiasik.artistry.client.network.ClientPacketHandler;
import iliiasik.artistry.config.ArtistryConfig;
import iliiasik.artistry.data.CanvasData;
import iliiasik.artistry.data.CanvasImage;
import iliiasik.artistry.server.ImageStorage;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.ChunkPos;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

@SuppressWarnings("resource")
public class ModNetwork {

    private static final String PROTOCOL = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(Artistry.MOD_ID, "main"),
            () -> PROTOCOL, PROTOCOL::equals, PROTOCOL::equals);

    public static void register() {
        int id = 0;

        playToServer(id++, SetCanvasSizeC2SPacket.class, SetCanvasSizeC2SPacket::encode, SetCanvasSizeC2SPacket::decode, ModNetwork::onSetCanvasSize);
        playToServer(id++, SaveCanvasC2SPacket.class, SaveCanvasC2SPacket::encode, SaveCanvasC2SPacket::decode, ModNetwork::onSaveCanvas);
        playToServer(id++, UploadImageC2SPacket.class, UploadImageC2SPacket::encode, UploadImageC2SPacket::decode, ModNetwork::onUploadImage);
        playToServer(id++, MoveCanvasImageC2SPacket.class, MoveCanvasImageC2SPacket::encode, MoveCanvasImageC2SPacket::decode, ModNetwork::onMoveImage);
        playToServer(id++, DeleteCanvasImageC2SPacket.class, DeleteCanvasImageC2SPacket::encode, DeleteCanvasImageC2SPacket::decode, ModNetwork::onDeleteImage);
        playToServer(id++, TogglePixelizeC2SPacket.class, TogglePixelizeC2SPacket::encode, TogglePixelizeC2SPacket::decode, ModNetwork::onTogglePixelize);
        playToServer(id++, LockCanvasImageC2SPacket.class, LockCanvasImageC2SPacket::encode, LockCanvasImageC2SPacket::decode, ModNetwork::onLockImage);
        playToServer(id++, RequestImageC2SPacket.class, RequestImageC2SPacket::encode, RequestImageC2SPacket::decode, ModNetwork::onRequestImage);
        playToServer(id++, CanvasViewC2SPacket.class, CanvasViewC2SPacket::encode, CanvasViewC2SPacket::decode, ModNetwork::onCanvasView);
        playToServer(id++, CanvasCursorC2SPacket.class, CanvasCursorC2SPacket::encode, CanvasCursorC2SPacket::decode, ModNetwork::onCanvasCursor);
        playToServer(id++, CanvasEnterRequestC2SPacket.class, CanvasEnterRequestC2SPacket::encode, CanvasEnterRequestC2SPacket::decode, ModNetwork::onCanvasEnterRequest);

        playToClient(id++, SyncCanvasS2CPacket.class, SyncCanvasS2CPacket::encode, SyncCanvasS2CPacket::decode, (p, c) -> client(c, () -> ClientPacketHandler.onSyncCanvas(p)));
        playToClient(id++, PosterRemovedS2CPacket.class, PosterRemovedS2CPacket::encode, PosterRemovedS2CPacket::decode, (p, c) -> client(c, () -> ClientPacketHandler.onPosterRemoved(p)));
        playToClient(id++, ImageUploadedS2CPacket.class, ImageUploadedS2CPacket::encode, ImageUploadedS2CPacket::decode, (p, c) -> client(c, () -> ClientPacketHandler.onImageUploaded(p)));
        playToClient(id++, DeliverImageS2CPacket.class, DeliverImageS2CPacket::encode, DeliverImageS2CPacket::decode, (p, c) -> client(c, () -> ClientPacketHandler.onDeliverImage(p)));
        playToClient(id++, SyncImageLayerS2CPacket.class, SyncImageLayerS2CPacket::encode, SyncImageLayerS2CPacket::decode, (p, c) -> client(c, () -> ClientPacketHandler.onSyncImageLayer(p)));
        playToClient(id++, SyncImageLockS2CPacket.class, SyncImageLockS2CPacket::encode, SyncImageLockS2CPacket::decode, (p, c) -> client(c, () -> ClientPacketHandler.onSyncImageLock(p)));
        playToClient(id++, ImageEvictedS2CPacket.class, ImageEvictedS2CPacket::encode, ImageEvictedS2CPacket::decode, (p, c) -> client(c, () -> ClientPacketHandler.onImageEvicted(p)));
        playToClient(id++, CanvasCursorS2CPacket.class, CanvasCursorS2CPacket::encode, CanvasCursorS2CPacket::decode, (p, c) -> client(c, () -> ClientPacketHandler.onCanvasCursor(p)));
        playToClient(id++, CanvasPresenceLeaveS2CPacket.class, CanvasPresenceLeaveS2CPacket::encode, CanvasPresenceLeaveS2CPacket::decode, (p, c) -> client(c, () -> ClientPacketHandler.onCanvasPresenceLeave(p)));
        playToClient(id++, CanvasEnterAllowedS2CPacket.class, CanvasEnterAllowedS2CPacket::encode, CanvasEnterAllowedS2CPacket::decode, (p, c) -> client(c, () -> ClientPacketHandler.onCanvasEnterAllowed(p)));
        playToClient(id, ServerSettingsS2CPacket.class, ServerSettingsS2CPacket::encode, ServerSettingsS2CPacket::decode, (p, c) -> client(c, () -> ClientPacketHandler.onServerSettings(p)));
    }

    private static <T> void playToServer(int id, Class<T> type, java.util.function.BiConsumer<T, net.minecraft.network.FriendlyByteBuf> enc,
                                         java.util.function.Function<net.minecraft.network.FriendlyByteBuf, T> dec,
                                         java.util.function.BiConsumer<T, Supplier<NetworkEvent.Context>> handler) {
        CHANNEL.registerMessage(id, type, enc, dec, handler, Optional.of(NetworkDirection.PLAY_TO_SERVER));
    }

    private static <T> void playToClient(int id, Class<T> type, java.util.function.BiConsumer<T, net.minecraft.network.FriendlyByteBuf> enc,
                                         java.util.function.Function<net.minecraft.network.FriendlyByteBuf, T> dec,
                                         java.util.function.BiConsumer<T, Supplier<NetworkEvent.Context>> handler) {
        CHANNEL.registerMessage(id, type, enc, dec, handler, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
    }

    public static void sendToPlayer(ServerPlayer player, Object packet) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
    }

    public static void sendToServer(Object packet) {
        CHANNEL.sendToServer(packet);
    }

    private static void server(Supplier<NetworkEvent.Context> ctxSup, java.util.function.Consumer<ServerPlayer> body) {
        NetworkEvent.Context ctx = ctxSup.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player != null) body.accept(player);
        });
        ctx.setPacketHandled(true);
    }

    private static void client(Supplier<NetworkEvent.Context> ctxSup, Runnable clientBody) {
        NetworkEvent.Context ctx = ctxSup.get();
        ctx.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> clientBody));
        ctx.setPacketHandled(true);
    }

    private static void onSetCanvasSize(SetCanvasSizeC2SPacket payload, Supplier<NetworkEvent.Context> ctx) {
        server(ctx, player -> {
            PosterAccess access = PosterAccess.resolve(player, payload.target());
            if (access == null) return;
            if (access.canvasData().isSizeChosen()) return;
            access.canvasData().canvasSize = payload.size();
            access.persist();
        });
    }

    private static void onSaveCanvas(SaveCanvasC2SPacket payload, Supplier<NetworkEvent.Context> ctx) {
        server(ctx, player -> {
            if (PacketThrottle.throttled(player.getUUID(), PacketThrottle.Channel.SAVE)) return;
            PosterAccess access = PosterAccess.resolve(player, payload.target());
            if (access == null) return;
            if (!access.canvasData().isSizeChosen()) return;
            CanvasData data = access.canvasData();
            for (CanvasData.PixelChange c : payload.changes()) {
                data.pixels[c.y() & 0xFF][c.x() & 0xFF] = c.blockIndex();
                data.colors[c.y() & 0xFF][c.x() & 0xFF] = c.color();
            }
            access.persist();
            access.syncCanvas(payload.changes());
        });
    }

    private static void onUploadImage(UploadImageC2SPacket payload, Supplier<NetworkEvent.Context> ctx) {
        server(ctx, player -> {
            if (ArtistryConfig.get().poster.disableImages) return;
            PosterAccess access = PosterAccess.resolve(player, payload.target());
            if (access == null) return;
            if (!access.canvasData().isSizeChosen()) return;
            try {
                UUID uuid = ImageStorage.save(payload.bytes());
                int canvasSize = access.canvasData().canvasSize;
                int gridW = Math.max(CanvasImage.MIN_GRID, canvasSize / 2);
                int gridH = Math.max(CanvasImage.MIN_GRID, canvasSize / 2);
                int gridX = (canvasSize - gridW) / 2;
                int gridY = (canvasSize - gridH) / 2;
                CanvasImage img = new CanvasImage(uuid, gridX, gridY, gridW, gridH);
                List<UUID> evicted = access.imageLayer().addImage(img);
                access.persist();
                sendToPlayer(player, new ImageUploadedS2CPacket(access.posOrNull(), uuid, gridX, gridY, gridW, gridH));
                sendToPlayer(player, new DeliverImageS2CPacket(uuid, payload.bytes()));
                access.syncImageLayer();
                if (!evicted.isEmpty()) access.imageEvicted(evicted);
            } catch (IOException e) {
                Artistry.LOGGER.error("Failed to save uploaded image", e);
            }
        });
    }

    private static void onMoveImage(MoveCanvasImageC2SPacket payload, Supplier<NetworkEvent.Context> ctx) {
        server(ctx, player -> {
            PosterAccess access = PosterAccess.resolve(player, payload.target());
            if (access == null) return;
            CanvasImage img = access.imageLayer().findByUuid(payload.uuid());
            if (img == null) return;
            img.gridX = payload.gridX();
            img.gridY = payload.gridY();
            img.gridW = payload.gridW();
            img.gridH = payload.gridH();
            access.persist();
            access.syncImageLayer();
        });
    }

    private static void onDeleteImage(DeleteCanvasImageC2SPacket payload, Supplier<NetworkEvent.Context> ctx) {
        server(ctx, player -> {
            PosterAccess access = PosterAccess.resolve(player, payload.target());
            if (access == null) return;
            access.imageLayer().removeImage(payload.uuid());
            access.persist();
            access.syncImageLayer();
        });
    }

    private static void onTogglePixelize(TogglePixelizeC2SPacket payload, Supplier<NetworkEvent.Context> ctx) {
        server(ctx, player -> {
            PosterAccess access = PosterAccess.resolve(player, payload.target());
            if (access == null) return;
            CanvasImage img = access.imageLayer().findByUuid(payload.uuid());
            if (img == null) return;
            img.pixelized = !img.pixelized;
            access.persist();
            access.syncImageLayer();
        });
    }

    private static void onLockImage(LockCanvasImageC2SPacket payload, Supplier<NetworkEvent.Context> ctx) {
        server(ctx, player -> {
            PosterAccess access = PosterAccess.resolve(player, payload.target());
            if (access == null) return;
            access.lock(payload.imageUuid(), payload.lock());
        });
    }

    private static void onRequestImage(RequestImageC2SPacket payload, Supplier<NetworkEvent.Context> ctx) {
        server(ctx, player -> {
            if (!ImageStorage.exists(payload.uuid())) return;
            try {
                byte[] bytes = ImageStorage.load(payload.uuid());
                sendToPlayer(player, new DeliverImageS2CPacket(payload.uuid(), bytes));
            } catch (IOException e) {
                Artistry.LOGGER.error("Failed to load image {}", payload.uuid(), e);
            }
        });
    }

    private static void onCanvasView(CanvasViewC2SPacket payload, Supplier<NetworkEvent.Context> ctx) {
        server(ctx, player -> {
            if (!(player.level() instanceof ServerLevel level)) return;
            if (!(level.getBlockEntity(payload.pos()) instanceof PosterBlockEntity poster)) return;
            if (payload.open()) {
                PosterPresence.open(player, level, payload.pos());
                sendToPlayer(player, new SyncImageLayerS2CPacket(payload.pos(), poster.imageLayer.getImages()));
            } else {
                PosterPresence.close(player, level, payload.pos());
            }
        });
    }

    private static void onCanvasCursor(CanvasCursorC2SPacket payload, Supplier<NetworkEvent.Context> ctx) {
        server(ctx, player -> {
            if (PacketThrottle.throttled(player.getUUID(), PacketThrottle.Channel.CURSOR)) return;
            if (!(player.level() instanceof ServerLevel level)) return;
            PosterPresence.updateCursor(player, level, payload.pos(), payload.gx(), payload.gy());
        });
    }

    private static void onCanvasEnterRequest(CanvasEnterRequestC2SPacket payload, Supplier<NetworkEvent.Context> ctx) {
        server(ctx, player -> {
            if (!(player.level() instanceof ServerLevel level)) return;
            if (!(level.getBlockEntity(payload.pos()) instanceof PosterBlockEntity poster)) return;
            if (!poster.canvasData.isSizeChosen()) {
                sendToPlayer(player, new CanvasEnterAllowedS2CPacket(payload.pos(), true));
                return;
            }
            int max = ArtistryConfig.get().poster.maxEditors;
            if (!PosterPresence.tryOpen(player, level, payload.pos(), max)) {
                player.displayClientMessage(Component.translatable("message.artistry.too_many_editors"), true);
                return;
            }
            sendToPlayer(player, new CanvasEnterAllowedS2CPacket(payload.pos(), false));
        });
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("artistry")
                .requires(src -> src.hasPermission(2))
                .then(Commands.literal("reload")
                        .executes(cmdCtx -> {
                            ArtistryConfig cfg = ArtistryConfig.reload();
                            MinecraftServer server = cmdCtx.getSource().getServer();
                            ServerSettingsS2CPacket packet = new ServerSettingsS2CPacket(
                                    cfg.poster.disableImages, cfg.network.batchIntervalMs, cfg.network.cursorIntervalMs);
                            for (ServerPlayer p : server.getPlayerList().getPlayers()) {
                                sendToPlayer(p, packet);
                            }
                            cmdCtx.getSource().sendSuccess(
                                    () -> Component.literal("[Artistry] Config reloaded and synced to players"), true);
                            return 1;
                        })));
    }

    @SubscribeEvent
    public static void onLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        ArtistryConfig cfg = ArtistryConfig.get();
        sendToPlayer(player, new ServerSettingsS2CPacket(
                cfg.poster.disableImages, cfg.network.batchIntervalMs, cfg.network.cursorIntervalMs));
    }

    @SubscribeEvent
    public static void onLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        PosterPresence.disconnect(player);
        PacketThrottle.remove(player.getUUID());
    }

    public static void broadcastPosterRemoved(ServerLevel level, BlockPos pos) {
        PosterRemovedS2CPacket packet = new PosterRemovedS2CPacket(pos);
        for (ServerPlayer player : level.getChunkSource().chunkMap.getPlayers(new ChunkPos(pos), false)) {
            sendToPlayer(player, packet);
        }
    }
}