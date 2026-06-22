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
import net.minecraft.core.SectionPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

public class ModNetwork {

    @SuppressWarnings("Convert2MethodRef")
    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");

        registrar.playToServer(SetCanvasSizeC2SPacket.TYPE, SetCanvasSizeC2SPacket.CODEC, (payload, context) -> {
            ServerPlayer player = (ServerPlayer) context.player();
            context.enqueueWork(() -> {
                PosterAccess access = PosterAccess.resolve(player, payload.target());
                if (access == null) return;
                if (access.canvasData().isSizeChosen()) return;
                access.canvasData().canvasSize = payload.size();
                access.persist();
            });
        });

        registrar.playToServer(SaveCanvasC2SPacket.TYPE, SaveCanvasC2SPacket.CODEC, (payload, context) -> {
            ServerPlayer player = (ServerPlayer) context.player();
            context.enqueueWork(() -> {
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
        });

        registrar.playToServer(UploadImageC2SPacket.TYPE, UploadImageC2SPacket.CODEC, (payload, context) -> {
            ServerPlayer player = (ServerPlayer) context.player();
            context.enqueueWork(() -> {
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
                    PacketDistributor.sendToPlayer(player,
                            new ImageUploadedS2CPacket(access.posOrNull(), uuid, gridX, gridY, gridW, gridH));
                    PacketDistributor.sendToPlayer(player,
                            new DeliverImageS2CPacket(uuid, payload.bytes()));
                    access.syncImageLayer();
                    if (!evicted.isEmpty()) access.imageEvicted(evicted);
                } catch (IOException e) {
                    Artistry.LOGGER.error("Failed to save uploaded image", e);
                }
            });
        });

        registrar.playToServer(MoveCanvasImageC2SPacket.TYPE, MoveCanvasImageC2SPacket.CODEC, (payload, context) -> {
            ServerPlayer player = (ServerPlayer) context.player();
            context.enqueueWork(() -> {
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
        });

        registrar.playToServer(DeleteCanvasImageC2SPacket.TYPE, DeleteCanvasImageC2SPacket.CODEC, (payload, context) -> {
            ServerPlayer player = (ServerPlayer) context.player();
            context.enqueueWork(() -> {
                PosterAccess access = PosterAccess.resolve(player, payload.target());
                if (access == null) return;
                access.imageLayer().removeImage(payload.uuid());
                access.persist();
                access.syncImageLayer();
            });
        });

        registrar.playToServer(TogglePixelizeC2SPacket.TYPE, TogglePixelizeC2SPacket.CODEC, (payload, context) -> {
            ServerPlayer player = (ServerPlayer) context.player();
            context.enqueueWork(() -> {
                PosterAccess access = PosterAccess.resolve(player, payload.target());
                if (access == null) return;
                CanvasImage img = access.imageLayer().findByUuid(payload.uuid());
                if (img == null) return;
                img.pixelized = !img.pixelized;
                access.persist();
                access.syncImageLayer();
            });
        });

        registrar.playToServer(LockCanvasImageC2SPacket.TYPE, LockCanvasImageC2SPacket.CODEC, (payload, context) -> {
            ServerPlayer player = (ServerPlayer) context.player();
            context.enqueueWork(() -> {
                PosterAccess access = PosterAccess.resolve(player, payload.target());
                if (access == null) return;
                access.lock(payload.imageUuid(), payload.lock());
            });
        });

        registrar.playToServer(RequestImageC2SPacket.TYPE, RequestImageC2SPacket.CODEC, (payload, context) -> {
            ServerPlayer player = (ServerPlayer) context.player();
            context.enqueueWork(() -> {
                if (!ImageStorage.exists(payload.uuid())) return;
                try {
                    byte[] bytes = ImageStorage.load(payload.uuid());
                    PacketDistributor.sendToPlayer(player, new DeliverImageS2CPacket(payload.uuid(), bytes));
                } catch (IOException e) {
                    Artistry.LOGGER.error("Failed to load image {}", payload.uuid(), e);
                }
            });
        });

        registrar.playToServer(CanvasViewC2SPacket.TYPE, CanvasViewC2SPacket.CODEC, (payload, context) -> {
            ServerPlayer player = (ServerPlayer) context.player();
            context.enqueueWork(() -> {
                if (!(player.level() instanceof ServerLevel level)) return;
                if (!(level.getBlockEntity(payload.pos()) instanceof PosterBlockEntity poster)) return;
                if (payload.open()) {
                    PosterPresence.open(player, level, payload.pos());
                    PacketDistributor.sendToPlayer(player,
                            new SyncImageLayerS2CPacket(payload.pos(), poster.imageLayer.getImages()));
                } else {
                    PosterPresence.close(player, level, payload.pos());
                }
            });
        });

        registrar.playToServer(CanvasCursorC2SPacket.TYPE, CanvasCursorC2SPacket.CODEC, (payload, context) -> {
            ServerPlayer player = (ServerPlayer) context.player();
            context.enqueueWork(() -> {
                if (PacketThrottle.throttled(player.getUUID(), PacketThrottle.Channel.CURSOR)) return;
                if (!(player.level() instanceof ServerLevel level)) return;
                PosterPresence.updateCursor(player, level, payload.pos(), payload.gx(), payload.gy());
            });
        });

        registrar.playToServer(CanvasEnterRequestC2SPacket.TYPE, CanvasEnterRequestC2SPacket.CODEC, (payload, context) -> {
            ServerPlayer player = (ServerPlayer) context.player();
            context.enqueueWork(() -> {
                if (!(player.level() instanceof ServerLevel level)) return;
                if (!(level.getBlockEntity(payload.pos()) instanceof PosterBlockEntity poster)) return;
                if (!poster.canvasData.isSizeChosen()) {
                    PacketDistributor.sendToPlayer(player, new CanvasEnterAllowedS2CPacket(payload.pos(), true));
                    return;
                }
                int max = ArtistryConfig.get().poster.maxEditors;
                if (!PosterPresence.tryOpen(player, level, payload.pos(), max)) {
                    player.displayClientMessage(Component.translatable("message.artistry.too_many_editors"), true);
                    return;
                }
                PacketDistributor.sendToPlayer(player, new CanvasEnterAllowedS2CPacket(payload.pos(), false));
            });
        });

        registrar.playToClient(SyncCanvasS2CPacket.TYPE, SyncCanvasS2CPacket.CODEC,
                (payload, context) -> ClientPacketHandler.onSyncCanvas(payload, context));
        registrar.playToClient(PosterRemovedS2CPacket.TYPE, PosterRemovedS2CPacket.CODEC,
                (payload, context) -> ClientPacketHandler.onPosterRemoved(payload, context));
        registrar.playToClient(ImageUploadedS2CPacket.TYPE, ImageUploadedS2CPacket.CODEC,
                (payload, context) -> ClientPacketHandler.onImageUploaded(payload, context));
        registrar.playToClient(DeliverImageS2CPacket.TYPE, DeliverImageS2CPacket.CODEC,
                (payload, context) -> ClientPacketHandler.onDeliverImage(payload, context));
        registrar.playToClient(SyncImageLayerS2CPacket.TYPE, SyncImageLayerS2CPacket.CODEC,
                (payload, context) -> ClientPacketHandler.onSyncImageLayer(payload, context));
        registrar.playToClient(SyncImageLockS2CPacket.TYPE, SyncImageLockS2CPacket.CODEC,
                (payload, context) -> ClientPacketHandler.onSyncImageLock(payload, context));
        registrar.playToClient(ImageEvictedS2CPacket.TYPE, ImageEvictedS2CPacket.CODEC,
                (payload, context) -> ClientPacketHandler.onImageEvicted(payload, context));
        registrar.playToClient(CanvasCursorS2CPacket.TYPE, CanvasCursorS2CPacket.CODEC,
                (payload, context) -> ClientPacketHandler.onCanvasCursor(payload, context));
        registrar.playToClient(CanvasPresenceLeaveS2CPacket.TYPE, CanvasPresenceLeaveS2CPacket.CODEC,
                (payload, context) -> ClientPacketHandler.onCanvasPresenceLeave(payload, context));
        registrar.playToClient(CanvasEnterAllowedS2CPacket.TYPE, CanvasEnterAllowedS2CPacket.CODEC,
                (payload, context) -> ClientPacketHandler.onCanvasEnterAllowed(payload, context));
        registrar.playToClient(ServerSettingsS2CPacket.TYPE, ServerSettingsS2CPacket.CODEC,
                (payload, context) -> ClientPacketHandler.onServerSettings(payload, context));
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
                                PacketDistributor.sendToPlayer(p, packet);
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
        PacketDistributor.sendToPlayer(player, new ServerSettingsS2CPacket(
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
        int chunkX = SectionPos.blockToSectionCoord(pos.getX());
        int chunkZ = SectionPos.blockToSectionCoord(pos.getZ());
        for (ServerPlayer player : level.players()) {
            if (player.getChunkTrackingView().isInViewDistance(chunkX, chunkZ)) {
                PacketDistributor.sendToPlayer(player, packet);
            }
        }
    }
}