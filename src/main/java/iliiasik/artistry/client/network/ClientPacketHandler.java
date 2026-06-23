package iliiasik.artistry.client.network;

import iliiasik.artistry.block.entity.PosterBlockEntity;
import iliiasik.artistry.client.ClientServerSettings;
import iliiasik.artistry.client.image.ClientImageCache;
import iliiasik.artistry.client.renderer.PosterBlockEntityRenderer;
import iliiasik.artistry.client.ui.screen.CanvasSizeScreen;
import iliiasik.artistry.client.ui.screen.PaintScreen;
import iliiasik.artistry.data.CanvasImage;
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
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;

import java.util.UUID;

public final class ClientPacketHandler {

    private ClientPacketHandler() {}

    public static void onSyncCanvas(SyncCanvasS2CPacket payload) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        BlockPos pos = payload.pos();
        if (mc.level.getBlockEntity(pos) instanceof PosterBlockEntity poster) {
            for (var c : payload.changes()) {
                poster.canvasData.pixels[c.y() & 0xFF][c.x() & 0xFF] = c.blockIndex();
                poster.canvasData.colors[c.y() & 0xFF][c.x() & 0xFF] = c.color();
            }
            PosterBlockEntityRenderer.invalidate(pos);
        }
        if (mc.screen instanceof PaintScreen screen) {
            BlockPos screenPos = screen.getTargetPos();
            if (pos.equals(screenPos)) {
                screen.applyRemoteChanges(payload.changes());
            }
        }
    }

    public static void onPosterRemoved(PosterRemovedS2CPacket payload) {
        Minecraft mc = Minecraft.getInstance();
        PosterBlockEntityRenderer.invalidate(payload.pos());
        if (mc.screen instanceof PaintScreen screen) {
            if (payload.pos().equals(screen.getTargetPos())) {
                screen.scheduledClose();
            }
        }
    }

    public static void onSyncImageLayer(SyncImageLayerS2CPacket payload) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        BlockPos pos = payload.pos();
        if (mc.level.getBlockEntity(pos) instanceof PosterBlockEntity poster) {
            poster.imageLayer.getImages().clear();
            for (var img : payload.images()) {
                poster.imageLayer.addImage(img);
            }
            PosterBlockEntityRenderer.clearPendingRequests(
                    payload.images().stream().map(i -> i.uuid).toList()
            );
        }
        if (mc.screen instanceof PaintScreen screen) {
            if (pos.equals(screen.getTargetPos())) {
                screen.applyImageLayerSync(payload.images());
            }
        }
    }

    public static void onDeliverImage(DeliverImageS2CPacket payload) {
        byte[] full = ClientImageAssembler.accept(payload);
        if (full == null) return;
        UUID uuid = payload.uuid();
        ClientImageCache.store(uuid, full);
        PosterBlockEntityRenderer.onImageReceived(uuid);
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen instanceof PaintScreen screen) {
            screen.receiveImageBytes(uuid, full);
        }
    }

    public static void onImageUploaded(ImageUploadedS2CPacket payload) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen instanceof PaintScreen screen) {
            if (payload.pos() == null || payload.pos().equals(screen.getTargetPos())) {
                screen.onImageUploaded(
                        payload.uuid(),
                        payload.gridX(), payload.gridY(),
                        payload.gridW(), payload.gridH()
                );
            }
        }
    }

    public static void onImageEvicted(ImageEvictedS2CPacket payload) {
        for (UUID uuid : payload.uuids()) {
            ClientImageCache.evict(uuid);
        }
    }

    public static void onSyncImageLock(SyncImageLockS2CPacket payload) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        BlockPos pos = payload.pos();
        if (mc.level.getBlockEntity(pos) instanceof PosterBlockEntity poster) {
            CanvasImage img = poster.imageLayer.findByUuid(payload.imageUuid());
            if (img != null) img.lockedByPlayer = payload.playerUuid();
        }
        if (mc.screen instanceof PaintScreen screen) {
            if (pos.equals(screen.getTargetPos())) {
                screen.applyImageLockSync(payload.imageUuid(), payload.playerUuid());
            }
        }
    }

    public static void onCanvasCursor(CanvasCursorS2CPacket payload) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen instanceof PaintScreen screen) {
            if (payload.pos().equals(screen.getTargetPos())) {
                screen.receiveCursor(payload.uuid(), payload.gx() / 16f, payload.gy() / 16f);
            }
        }
    }

    public static void onCanvasPresenceLeave(CanvasPresenceLeaveS2CPacket payload) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen instanceof PaintScreen screen) {
            if (payload.pos().equals(screen.getTargetPos())) {
                screen.removePresence(payload.uuid());
            }
        }
    }

    public static void onCanvasEnterAllowed(CanvasEnterAllowedS2CPacket payload) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        if (mc.level.getBlockEntity(payload.pos()) instanceof PosterBlockEntity poster) {
            if (payload.needsSize()) {
                mc.setScreen(new CanvasSizeScreen(poster));
            } else {
                mc.setScreen(new PaintScreen(poster));
            }
        }
    }

    public static void onServerSettings(ServerSettingsS2CPacket payload) {
        ClientServerSettings.apply(payload.disableImages(),
                payload.batchIntervalMs(), payload.cursorIntervalMs());
    }
}