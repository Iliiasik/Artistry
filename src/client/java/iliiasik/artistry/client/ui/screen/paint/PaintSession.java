package iliiasik.artistry.client.ui.screen.paint;

import iliiasik.artistry.client.ClientServerSettings;
import iliiasik.artistry.client.image.ClientImageCache;
import iliiasik.artistry.client.presence.CanvasPresence;
import iliiasik.artistry.block.entity.PosterBlockEntity;
import iliiasik.artistry.data.CanvasData;
import iliiasik.artistry.data.CanvasImage;
import iliiasik.artistry.data.CanvasImageLayer;
import iliiasik.artistry.network.*;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;

import java.util.List;
import java.util.UUID;

public class PaintSession {

    private static final long IMAGE_SYNC_INTERVAL_MS = 150;

    private final PosterBlockEntity targetEntity;
    private final ItemStack targetStack;
    private final Hand targetHand;

    private final CanvasData canvasData = new CanvasData();
    private final CanvasData lastSentSnapshot = new CanvasData();
    private final CanvasImageLayer imageLayer = new CanvasImageLayer();
    private final CanvasPresence presence = new CanvasPresence();

    private boolean viewRegistered = false;

    private long lastFlushTime = 0;
    private long lastImageSyncTime = 0;
    private UUID pendingMoveUuid = null;

    private long lastCursorSentTime = 0;
    private int lastSentCursorGx = Integer.MIN_VALUE;
    private int lastSentCursorGy = Integer.MIN_VALUE;

    public PaintSession(PosterBlockEntity entity) {
        this.targetEntity = entity;
        this.targetStack = null;
        this.targetHand = null;
        canvasData.copyFrom(entity.canvasData);
        lastSentSnapshot.copyFrom(entity.canvasData);
        imageLayer.copyFrom(entity.imageLayer);
        requestMissingImages();
    }

    public PaintSession(ItemStack stack, Hand hand, int chosenSize) {
        this.targetEntity = null;
        this.targetStack = stack;
        this.targetHand = hand;
        NbtComponent comp = stack.get(DataComponentTypes.CUSTOM_DATA);
        if (comp != null) {
            NbtCompound nbt = comp.copyNbt();
            if (nbt.contains("canvas")) {
                canvasData.fromNbt(nbt.getCompound("canvas"));
            }
            if (nbt.contains("images")) {
                imageLayer.fromNbt(nbt.getList("images", NbtList.COMPOUND_TYPE));
            }
        }
        if (chosenSize > 0) {
            canvasData.canvasSize = chosenSize;
        }
        lastSentSnapshot.copyFrom(canvasData);
        requestMissingImages();
    }

    public CanvasData canvasData() {
        return canvasData;
    }

    public CanvasImageLayer imageLayer() {
        return imageLayer;
    }

    public CanvasPresence presence() {
        return presence;
    }

    public boolean isWorld() {
        return targetEntity != null;
    }

    public BlockPos targetPos() {
        return targetEntity != null ? targetEntity.getPos() : null;
    }

    private PosterTarget target() {
        if (targetEntity != null) return new PosterTarget.World(targetEntity.getPos());
        return new PosterTarget.Held(targetHand);
    }

    private boolean connected() {
        return MinecraftClient.getInstance().getNetworkHandler() != null;
    }

    public void requestMissingImages() {
        for (CanvasImage img : imageLayer.getImages()) {
            if (ClientImageCache.has(img.uuid)) {
                ClientPlayNetworking.send(new RequestImageC2SPacket(img.uuid));
            }
        }
    }

    public void open() {
        if (targetEntity == null || viewRegistered) return;
        if (connected()) {
            ClientPlayNetworking.send(new CanvasViewC2SPacket(targetEntity.getPos(), true));
            viewRegistered = true;
        }
    }

    public void close() {
        if (targetEntity == null || !viewRegistered) return;
        if (connected()) {
            ClientPlayNetworking.send(new CanvasViewC2SPacket(targetEntity.getPos(), false));
        }
        viewRegistered = false;
    }

    public void onScreenClosed() {
        flushPixels();
        close();
        if (targetStack != null) {
            saveToItem();
        }
    }

    public void lockImage(UUID uuid, boolean lock) {
        ClientPlayNetworking.send(new LockCanvasImageC2SPacket(target(), uuid, lock));
    }

    public void deleteImage(UUID uuid) {
        ClientPlayNetworking.send(new DeleteCanvasImageC2SPacket(target(), uuid));
    }

    public void togglePixelize(UUID uuid) {
        ClientPlayNetworking.send(new TogglePixelizeC2SPacket(target(), uuid));
    }

    public void uploadImage(byte[] bytes) {
        ClientPlayNetworking.send(new UploadImageC2SPacket(target(), bytes));
    }

    public void markImageMoved(UUID uuid) {
        if (targetEntity != null) pendingMoveUuid = uuid;
    }

    public void flushImageMove() {
        if (pendingMoveUuid == null) return;
        sendImageMove(pendingMoveUuid);
        pendingMoveUuid = null;
    }

    private void sendImageMove(UUID uuid) {
        CanvasImage img = imageLayer.findByUuid(uuid);
        if (img == null) return;
        if (img.pixelized) {
            ClientImageCache.rebuildPixelizedTexture(uuid, img.gridW, img.gridH);
        }
        ClientPlayNetworking.send(new MoveCanvasImageC2SPacket(
                target(), uuid, img.gridX, img.gridY, img.gridW, img.gridH));
        lastImageSyncTime = System.currentTimeMillis();
    }

    public void tickBatch() {
        if (targetEntity == null) return;
        long now = System.currentTimeMillis();
        if (now - lastFlushTime >= ClientServerSettings.batchIntervalMs()) {
            flushPixels();
        }
        if (pendingMoveUuid != null && now - lastImageSyncTime >= IMAGE_SYNC_INTERVAL_MS) {
            flushImageMove();
        }
    }

    private void flushPixels() {
        if (targetEntity == null) return;
        List<CanvasData.PixelChange> changes = canvasData.diff(lastSentSnapshot);
        if (changes.isEmpty()) return;
        lastSentSnapshot.copyFrom(canvasData);
        lastFlushTime = System.currentTimeMillis();
        if (connected()) {
            ClientPlayNetworking.send(new SaveCanvasC2SPacket(target(), changes));
        }
    }

    private void saveToItem() {
        NbtComponent comp = targetStack.get(DataComponentTypes.CUSTOM_DATA);
        NbtCompound tag = comp != null ? comp.copyNbt() : new NbtCompound();
        List<CanvasData.PixelChange> changes = canvasData.diff(lastSentSnapshot);
        if (!changes.isEmpty()) {
            tag.put("canvas", canvasData.toNbt());
        }
        tag.put("images", imageLayer.toNbt());
        targetStack.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(tag));
        if (!changes.isEmpty() && connected()) {
            ClientPlayNetworking.send(new SaveCanvasC2SPacket(target(), changes));
        }
    }

    public void maybeSendCursor(short gx, short gy) {
        if (targetEntity == null || !viewRegistered) return;
        long now = System.currentTimeMillis();
        if (now - lastCursorSentTime < ClientServerSettings.cursorIntervalMs()) return;
        if (gx == lastSentCursorGx && gy == lastSentCursorGy) return;
        lastSentCursorGx = gx;
        lastSentCursorGy = gy;
        lastCursorSentTime = now;
        if (connected()) {
            ClientPlayNetworking.send(new CanvasCursorC2SPacket(targetEntity.getPos(), gx, gy));
        }
    }

    public void applyRemoteChanges(List<CanvasData.PixelChange> changes) {
        for (CanvasData.PixelChange c : changes) {
            int x = c.x() & 0xFF;
            int y = c.y() & 0xFF;
            canvasData.pixels[y][x] = c.blockIndex();
            canvasData.colors[y][x] = c.color();
            lastSentSnapshot.pixels[y][x] = c.blockIndex();
            lastSentSnapshot.colors[y][x] = c.color();
        }
    }

    public void applyImageLayerSync(List<CanvasImage> images) {
        imageLayer.getImages().clear();
        for (CanvasImage img : images) {
            imageLayer.addImage(img);
        }
        requestMissingImages();
    }

    public void applyImageLockSync(UUID imageUuid, UUID playerUuid) {
        CanvasImage img = imageLayer.findByUuid(imageUuid);
        if (img != null) img.lockedByPlayer = playerUuid;
    }

    public void receiveImageBytes(UUID uuid, byte[] bytes) {
        ClientImageCache.store(uuid, bytes);
    }

    public void onImageUploaded(UUID uuid, int gridX, int gridY, int gridW, int gridH) {
        if (imageLayer.findByUuid(uuid) != null) return;
        imageLayer.addImage(new CanvasImage(uuid, gridX, gridY, gridW, gridH));
    }

    public void receiveCursor(UUID uuid, float gx, float gy) {
        presence.updateCursor(uuid, gx, gy);
    }

    public void removePresence(UUID uuid) {
        presence.remove(uuid);
    }
}