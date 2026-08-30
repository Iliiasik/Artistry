package iliiasik.artistry.client.ui.screen.paint;

import iliiasik.artistry.client.ClientServerSettings;
import iliiasik.artistry.client.image.ClientImageCache;
import iliiasik.artistry.client.presence.CanvasPresence;
import iliiasik.artistry.block.entity.PosterBlockEntity;
import iliiasik.artistry.data.CanvasData;
import iliiasik.artistry.data.CanvasImage;
import iliiasik.artistry.data.CanvasImageLayer;
import iliiasik.artistry.network.ArtistryNetwork;
import iliiasik.artistry.network.*;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

public class PaintSession {

    private static final long IMAGE_SYNC_INTERVAL_MS = 150;

    private final PosterBlockEntity targetEntity;
    private final ItemStack targetStack;
    private final InteractionHand targetHand;

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

    public PaintSession(ItemStack stack, InteractionHand hand, int chosenSize) {
        this.targetEntity = null;
        this.targetStack = stack;
        this.targetHand = hand;
        CustomData comp = stack.get(DataComponents.CUSTOM_DATA);
        if (comp != null) {
            CompoundTag nbt = comp.copyTag();
            if (nbt.contains("canvas")) {
                canvasData.fromNbt(nbt.getCompound("canvas"));
            }
            if (nbt.contains("images")) {
                imageLayer.fromNbt(nbt.getList("images", Tag.TAG_COMPOUND));
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
        return targetEntity != null ? targetEntity.getBlockPos() : null;
    }

    private PosterTarget target() {
        if (targetEntity != null) return new PosterTarget.World(targetEntity.getBlockPos());
        return new PosterTarget.Held(targetHand);
    }

    private boolean connected() {
        return Minecraft.getInstance().getConnection() != null;
    }

    public void requestMissingImages() {
        for (CanvasImage img : imageLayer.getImages()) {
            if (!ClientImageCache.has(img.uuid)) {
                ArtistryNetwork.sendToServer(new RequestImageC2SPacket(img.uuid));
            }
        }
    }

    public void open() {
        if (targetEntity == null || viewRegistered) return;
        if (connected()) {
            ArtistryNetwork.sendToServer(new CanvasViewC2SPacket(targetEntity.getBlockPos(), true));
            viewRegistered = true;
        }
    }

    public void close() {
        if (targetEntity == null || !viewRegistered) return;
        if (connected()) {
            ArtistryNetwork.sendToServer(new CanvasViewC2SPacket(targetEntity.getBlockPos(), false));
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
        ArtistryNetwork.sendToServer(new LockCanvasImageC2SPacket(target(), uuid, lock));
    }

    public void deleteImage(UUID uuid) {
        ArtistryNetwork.sendToServer(new DeleteCanvasImageC2SPacket(target(), uuid));
    }

    public void togglePixelize(UUID uuid) {
        ArtistryNetwork.sendToServer(new TogglePixelizeC2SPacket(target(), uuid));
    }

    public void uploadImage(byte[] bytes) {
        ArtistryNetwork.sendToServer(new UploadImageC2SPacket(target(), bytes));
    }

    public void markImageMoved(UUID uuid) {
        pendingMoveUuid = uuid;
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
        ArtistryNetwork.sendToServer(new MoveCanvasImageC2SPacket(
                target(), uuid, img.gridX, img.gridY, img.gridW, img.gridH));
        lastImageSyncTime = System.currentTimeMillis();
    }

    public void tickBatch() {
        long now = System.currentTimeMillis();
        if (targetEntity != null && now - lastFlushTime >= ClientServerSettings.batchIntervalMs()) {
            flushPixels();
        }
        if (targetEntity != null && pendingMoveUuid != null && now - lastImageSyncTime >= IMAGE_SYNC_INTERVAL_MS) {
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
            ArtistryNetwork.sendToServer(new SaveCanvasC2SPacket(target(), changes));
        }
    }

    private void saveToItem() {
        CustomData comp = targetStack.get(DataComponents.CUSTOM_DATA);
        CompoundTag tag = comp != null ? comp.copyTag() : new CompoundTag();
        List<CanvasData.PixelChange> changes = canvasData.diff(lastSentSnapshot);
        if (!changes.isEmpty()) {
            tag.put("canvas", canvasData.toNbt());
        }
        tag.put("images", imageLayer.toNbt());
        targetStack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        if (!changes.isEmpty() && connected()) {
            ArtistryNetwork.sendToServer(new SaveCanvasC2SPacket(target(), changes));
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
            ArtistryNetwork.sendToServer(new CanvasCursorC2SPacket(targetEntity.getBlockPos(), gx, gy));
        }
    }

    public void applyRemoteChanges(List<CanvasData.PixelChange> changes) {
        for (CanvasData.PixelChange c : changes) {
            int x = c.x() & 0xFF;
            int y = c.y() & 0xFF;
            if (!canvasData.inBounds(x, y)) continue;
            canvasData.pixels[y][x] = c.blockIndex();
            canvasData.colors[y][x] = c.color();
            lastSentSnapshot.pixels[y][x] = c.blockIndex();
            lastSentSnapshot.colors[y][x] = c.color();
        }
        canvasData.markChanged();
    }

    public void applyImageLayerSync(List<CanvasImage> images) {
        imageLayer.getImages().clear();
        for (CanvasImage img : images) {
            imageLayer.addImage(img);
        }
        imageLayer.clampToCanvas(canvasData.canvasSize);
        requestMissingImages();
    }

    public void applyImageLockSync(UUID imageUuid, @Nullable UUID playerUuid) {
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
