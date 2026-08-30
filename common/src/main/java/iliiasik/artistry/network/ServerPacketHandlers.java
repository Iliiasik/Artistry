package iliiasik.artistry.network;

import iliiasik.artistry.Artistry;
import iliiasik.artistry.block.entity.PosterBlockEntity;
import iliiasik.artistry.config.ArtistryConfig;
import iliiasik.artistry.data.CanvasData;
import iliiasik.artistry.data.CanvasImage;
import iliiasik.artistry.server.ImageStorage;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

public final class ServerPacketHandlers {

    private ServerPacketHandlers() {}

    public static void onSetCanvasSize(SetCanvasSizeC2SPacket payload, ServerPlayer player) {
        PosterAccess access = PosterAccess.resolve(player, payload.target());
        if (access == null) return;
        if (access.canvasData().isSizeChosen()) return;
        if (!CanvasData.isValidSize(payload.size())) return;
        access.canvasData().canvasSize = payload.size();
        access.persist();
    }

    public static void onSaveCanvas(SaveCanvasC2SPacket payload, ServerPlayer player) {
        if (PacketThrottle.throttled(player.getUUID(), PacketThrottle.Channel.SAVE)) return;
        PosterAccess access = PosterAccess.resolve(player, payload.target());
        if (access == null) return;
        if (!access.canvasData().isSizeChosen()) return;
        CanvasData data = access.canvasData();
        for (CanvasData.PixelChange c : payload.changes()) {
            int x = c.x() & 0xFF;
            int y = c.y() & 0xFF;
            if (!data.inBounds(x, y)) continue;
            data.pixels[y][x] = c.blockIndex();
            data.colors[y][x] = c.color();
        }
        access.persist();
        access.syncCanvas(payload.changes());
    }

    public static void onUploadImage(UploadImageC2SPacket payload, ServerPlayer player) {
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
            ArtistryNetwork.sendToPlayer(player,
                    new ImageUploadedS2CPacket(access.posOrNull(), uuid, gridX, gridY, gridW, gridH));
            ArtistryNetwork.sendToPlayer(player, new DeliverImageS2CPacket(uuid, payload.bytes()));
            access.syncImageLayer();
            if (!evicted.isEmpty()) access.imageEvicted(evicted);
        } catch (IOException e) {
            Artistry.LOGGER.error("Failed to save uploaded image", e);
        }
    }

    public static void onMoveImage(MoveCanvasImageC2SPacket payload, ServerPlayer player) {
        PosterAccess access = PosterAccess.resolve(player, payload.target());
        if (access == null) return;
        CanvasImage img = access.imageLayer().findByUuid(payload.uuid());
        if (img == null) return;
        img.gridX = payload.gridX();
        img.gridY = payload.gridY();
        img.gridW = payload.gridW();
        img.gridH = payload.gridH();
        access.imageLayer().clampToCanvas(access.canvasData().canvasSize);
        access.persist();
        access.syncImageLayer();
    }

    public static void onDeleteImage(DeleteCanvasImageC2SPacket payload, ServerPlayer player) {
        PosterAccess access = PosterAccess.resolve(player, payload.target());
        if (access == null) return;
        access.imageLayer().removeImage(payload.uuid());
        access.persist();
        access.syncImageLayer();
    }

    public static void onTogglePixelize(TogglePixelizeC2SPacket payload, ServerPlayer player) {
        PosterAccess access = PosterAccess.resolve(player, payload.target());
        if (access == null) return;
        CanvasImage img = access.imageLayer().findByUuid(payload.uuid());
        if (img == null) return;
        img.pixelized = !img.pixelized;
        access.persist();
        access.syncImageLayer();
    }

    public static void onLockImage(LockCanvasImageC2SPacket payload, ServerPlayer player) {
        PosterAccess access = PosterAccess.resolve(player, payload.target());
        if (access == null) return;
        access.lock(payload.imageUuid(), payload.lock());
    }

    public static void onRequestImage(RequestImageC2SPacket payload, ServerPlayer player) {
        if (!ImageStorage.exists(payload.uuid())) return;
        try {
            byte[] bytes = ImageStorage.load(payload.uuid());
            ArtistryNetwork.sendToPlayer(player, new DeliverImageS2CPacket(payload.uuid(), bytes));
        } catch (IOException e) {
            Artistry.LOGGER.error("Failed to load image {}", payload.uuid(), e);
        }
    }

    @SuppressWarnings("resource")
    public static void onCanvasView(CanvasViewC2SPacket payload, ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel level)) return;
        if (!(level.getBlockEntity(payload.pos()) instanceof PosterBlockEntity poster)) return;
        if (payload.open()) {
            PosterPresence.open(player, level, payload.pos());
            ArtistryNetwork.sendToPlayer(player,
                    new SyncImageLayerS2CPacket(payload.pos(), poster.imageLayer.getImages()));
        } else {
            PosterPresence.close(player, level, payload.pos());
        }
    }

    @SuppressWarnings("resource")
    public static void onCanvasCursor(CanvasCursorC2SPacket payload, ServerPlayer player) {
        if (PacketThrottle.throttled(player.getUUID(), PacketThrottle.Channel.CURSOR)) return;
        if (!(player.level() instanceof ServerLevel level)) return;
        PosterPresence.updateCursor(player, level, payload.pos(), payload.gx(), payload.gy());
    }

    @SuppressWarnings("resource")
    public static void onCanvasEnterRequest(CanvasEnterRequestC2SPacket payload, ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel level)) return;
        if (!(level.getBlockEntity(payload.pos()) instanceof PosterBlockEntity poster)) return;
        if (!poster.canvasData.isSizeChosen()) {
            ArtistryNetwork.sendToPlayer(player, new CanvasEnterAllowedS2CPacket(payload.pos(), true));
            return;
        }
        int max = ArtistryConfig.get().poster.maxEditors;
        if (!PosterPresence.tryOpen(player, level, payload.pos(), max)) {
            player.displayClientMessage(Component.translatable("message.artistry.too_many_editors"), true);
            return;
        }
        ArtistryNetwork.sendToPlayer(player, new CanvasEnterAllowedS2CPacket(payload.pos(), false));
    }
}
