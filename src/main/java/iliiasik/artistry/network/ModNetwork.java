package iliiasik.artistry.network;

import iliiasik.artistry.Artistry;
import iliiasik.artistry.block.entity.PosterBlockEntity;
import iliiasik.artistry.config.ArtistryConfig;
import iliiasik.artistry.data.CanvasData;
import iliiasik.artistry.data.CanvasImage;
import iliiasik.artistry.server.ImageStorage;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkSectionPos;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

public class ModNetwork {
    public static void register() {
        PayloadTypeRegistry.playC2S().register(SaveCanvasC2SPacket.ID, SaveCanvasC2SPacket.CODEC);
        PayloadTypeRegistry.playC2S().register(SetCanvasSizeC2SPacket.ID, SetCanvasSizeC2SPacket.CODEC);
        PayloadTypeRegistry.playC2S().register(UploadImageC2SPacket.ID, UploadImageC2SPacket.CODEC);
        PayloadTypeRegistry.playC2S().register(RequestImageC2SPacket.ID, RequestImageC2SPacket.CODEC);
        PayloadTypeRegistry.playC2S().register(MoveCanvasImageC2SPacket.ID, MoveCanvasImageC2SPacket.CODEC);
        PayloadTypeRegistry.playC2S().register(DeleteCanvasImageC2SPacket.ID, DeleteCanvasImageC2SPacket.CODEC);
        PayloadTypeRegistry.playC2S().register(TogglePixelizeC2SPacket.ID, TogglePixelizeC2SPacket.CODEC);
        PayloadTypeRegistry.playC2S().register(LockCanvasImageC2SPacket.ID, LockCanvasImageC2SPacket.CODEC);
        PayloadTypeRegistry.playC2S().register(CanvasViewC2SPacket.ID, CanvasViewC2SPacket.CODEC);
        PayloadTypeRegistry.playC2S().register(CanvasCursorC2SPacket.ID, CanvasCursorC2SPacket.CODEC);
        PayloadTypeRegistry.playC2S().register(CanvasEnterRequestC2SPacket.ID, CanvasEnterRequestC2SPacket.CODEC);

        PayloadTypeRegistry.playS2C().register(SyncCanvasS2CPacket.ID, SyncCanvasS2CPacket.CODEC);
        PayloadTypeRegistry.playS2C().register(PosterRemovedS2CPacket.ID, PosterRemovedS2CPacket.CODEC);
        PayloadTypeRegistry.playS2C().register(ImageUploadedS2CPacket.ID, ImageUploadedS2CPacket.CODEC);
        PayloadTypeRegistry.playS2C().register(DeliverImageS2CPacket.ID, DeliverImageS2CPacket.CODEC);
        PayloadTypeRegistry.playS2C().register(SyncImageLayerS2CPacket.ID, SyncImageLayerS2CPacket.CODEC);
        PayloadTypeRegistry.playS2C().register(SyncImageLockS2CPacket.ID, SyncImageLockS2CPacket.CODEC);
        PayloadTypeRegistry.playS2C().register(ImageEvictedS2CPacket.ID, ImageEvictedS2CPacket.CODEC);
        PayloadTypeRegistry.playS2C().register(CanvasCursorS2CPacket.ID, CanvasCursorS2CPacket.CODEC);
        PayloadTypeRegistry.playS2C().register(CanvasPresenceLeaveS2CPacket.ID, CanvasPresenceLeaveS2CPacket.CODEC);
        PayloadTypeRegistry.playS2C().register(CanvasEnterAllowedS2CPacket.ID, CanvasEnterAllowedS2CPacket.CODEC);
        PayloadTypeRegistry.playS2C().register(ServerSettingsS2CPacket.ID, ServerSettingsS2CPacket.CODEC);

        ServerPlayNetworking.registerGlobalReceiver(SetCanvasSizeC2SPacket.ID,
                (payload, ctx) -> ctx.server().execute(() -> {
                    PosterAccess access = PosterAccess.resolve(ctx.player(), payload.target());
                    if (access == null) return;
                    if (access.canvasData().isSizeChosen()) return;
                    access.canvasData().canvasSize = payload.size();
                    access.persist();
                }));

        ServerPlayNetworking.registerGlobalReceiver(SaveCanvasC2SPacket.ID,
                (payload, ctx) -> ctx.server().execute(() -> {
                    if (PacketThrottle.throttled(ctx.player().getUuid(), PacketThrottle.Channel.SAVE)) return;                    PosterAccess access = PosterAccess.resolve(ctx.player(), payload.target());
                    if (access == null) return;
                    if (!access.canvasData().isSizeChosen()) return;
                    CanvasData data = access.canvasData();
                    for (CanvasData.PixelChange c : payload.changes()) {
                        data.pixels[c.y() & 0xFF][c.x() & 0xFF] = c.blockIndex();
                        data.colors[c.y() & 0xFF][c.x() & 0xFF] = c.color();
                    }
                    access.persist();
                    access.syncCanvas(payload.changes());
                }));

        ServerPlayNetworking.registerGlobalReceiver(UploadImageC2SPacket.ID,
                (payload, ctx) -> ctx.server().execute(() -> {
                    if (ArtistryConfig.get().poster.disableImages) return;
                    PosterAccess access = PosterAccess.resolve(ctx.player(), payload.target());
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
                        ServerPlayNetworking.send(ctx.player(),
                                new ImageUploadedS2CPacket(access.posOrNull(), uuid, gridX, gridY, gridW, gridH));
                        ServerPlayNetworking.send(ctx.player(),
                                new DeliverImageS2CPacket(uuid, payload.bytes()));
                        access.syncImageLayer();
                        if (!evicted.isEmpty()) access.imageEvicted(evicted);
                    } catch (IOException e) {
                        Artistry.LOGGER.error("Failed to save uploaded image", e);
                    }
                }));

        ServerPlayNetworking.registerGlobalReceiver(MoveCanvasImageC2SPacket.ID,
                (payload, ctx) -> ctx.server().execute(() -> {
                    PosterAccess access = PosterAccess.resolve(ctx.player(), payload.target());
                    if (access == null) return;
                    CanvasImage img = access.imageLayer().findByUuid(payload.uuid());
                    if (img == null) return;
                    img.gridX = payload.gridX();
                    img.gridY = payload.gridY();
                    img.gridW = payload.gridW();
                    img.gridH = payload.gridH();
                    access.persist();
                    access.syncImageLayer();
                }));

        ServerPlayNetworking.registerGlobalReceiver(DeleteCanvasImageC2SPacket.ID,
                (payload, ctx) -> ctx.server().execute(() -> {
                    PosterAccess access = PosterAccess.resolve(ctx.player(), payload.target());
                    if (access == null) return;
                    access.imageLayer().removeImage(payload.uuid());
                    access.persist();
                    access.syncImageLayer();
                }));

        ServerPlayNetworking.registerGlobalReceiver(TogglePixelizeC2SPacket.ID,
                (payload, ctx) -> ctx.server().execute(() -> {
                    PosterAccess access = PosterAccess.resolve(ctx.player(), payload.target());
                    if (access == null) return;
                    CanvasImage img = access.imageLayer().findByUuid(payload.uuid());
                    if (img == null) return;
                    img.pixelized = !img.pixelized;
                    access.persist();
                    access.syncImageLayer();
                }));

        ServerPlayNetworking.registerGlobalReceiver(LockCanvasImageC2SPacket.ID,
                (payload, ctx) -> ctx.server().execute(() -> {
                    PosterAccess access = PosterAccess.resolve(ctx.player(), payload.target());
                    if (access == null) return;
                    access.lock(payload.imageUuid(), payload.lock());
                }));

        ServerPlayNetworking.registerGlobalReceiver(RequestImageC2SPacket.ID,
                (payload, ctx) -> ctx.server().execute(() -> {
                    if (!ImageStorage.exists(payload.uuid())) return;
                    try {
                        byte[] bytes = ImageStorage.load(payload.uuid());
                        ServerPlayNetworking.send(ctx.player(),
                                new DeliverImageS2CPacket(payload.uuid(), bytes));
                    } catch (IOException e) {
                        Artistry.LOGGER.error("Failed to load image {}", payload.uuid(), e);
                    }
                }));

        ServerPlayNetworking.registerGlobalReceiver(CanvasViewC2SPacket.ID,
                (payload, ctx) -> ctx.server().execute(() -> {
                    if (!(ctx.player().getWorld() instanceof ServerWorld world)) return;
                    if (!(world.getBlockEntity(payload.pos()) instanceof PosterBlockEntity poster)) return;
                    if (payload.open()) {
                        PosterPresence.open(ctx.player(), world, payload.pos());
                        ServerPlayNetworking.send(ctx.player(),
                                new SyncImageLayerS2CPacket(payload.pos(), poster.imageLayer.getImages()));
                    } else {
                        PosterPresence.close(ctx.player(), world, payload.pos());
                    }
                }));

        ServerPlayNetworking.registerGlobalReceiver(CanvasCursorC2SPacket.ID,
                (payload, ctx) -> ctx.server().execute(() -> {
                    if (PacketThrottle.throttled(ctx.player().getUuid(), PacketThrottle.Channel.CURSOR)) return;                    if (!(ctx.player().getWorld() instanceof ServerWorld world)) return;
                    PosterPresence.updateCursor(ctx.player(), world, payload.pos(), payload.gx(), payload.gy());
                }));

        ServerPlayNetworking.registerGlobalReceiver(CanvasEnterRequestC2SPacket.ID,
                (payload, ctx) -> ctx.server().execute(() -> {
                    if (!(ctx.player().getWorld() instanceof ServerWorld world)) return;
                    if (!(world.getBlockEntity(payload.pos()) instanceof PosterBlockEntity)) return;
                    int max = ArtistryConfig.get().poster.maxEditors;
                    if (!PosterPresence.tryOpen(ctx.player(), world, payload.pos(), max)) {
                        ctx.player().sendMessage(Text.translatable("message.artistry.too_many_editors"), true);
                        return;
                    }
                    ServerPlayNetworking.send(ctx.player(), new CanvasEnterAllowedS2CPacket(payload.pos()));
                }));

        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            PosterPresence.disconnect(handler.player);
            PacketThrottle.remove(handler.player.getUuid());
        });

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ArtistryConfig cfg = ArtistryConfig.get();
            ServerPlayNetworking.send(handler.player, new ServerSettingsS2CPacket(
                    cfg.poster.disableImages, cfg.network.batchIntervalMs, cfg.network.cursorIntervalMs));
        });

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                dispatcher.register(CommandManager.literal("artistry")
                        .requires(src -> src.hasPermissionLevel(2))
                        .then(CommandManager.literal("reload")
                                .executes(cmdCtx -> {
                                    ArtistryConfig cfg = ArtistryConfig.reload();
                                    MinecraftServer server = cmdCtx.getSource().getServer();
                                    ServerSettingsS2CPacket packet = new ServerSettingsS2CPacket(
                                            cfg.poster.disableImages, cfg.network.batchIntervalMs, cfg.network.cursorIntervalMs);
                                    for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) {
                                        ServerPlayNetworking.send(p, packet);
                                    }
                                    cmdCtx.getSource().sendFeedback(
                                            () -> Text.literal("[Artistry] Config reloaded and synced to players"), true);
                                    return 1;
                                }))));
    }

    public static void broadcastPosterRemoved(ServerWorld world, BlockPos pos) {
        PosterRemovedS2CPacket packet = new PosterRemovedS2CPacket(pos);
        int chunkX = ChunkSectionPos.getSectionCoord(pos.getX());
        int chunkZ = ChunkSectionPos.getSectionCoord(pos.getZ());
        for (ServerPlayerEntity player : world.getPlayers()) {
            if (player.getChunkFilter().isWithinDistance(chunkX, chunkZ)) {
                ServerPlayNetworking.send(player, packet);
            }
        }
    }
}