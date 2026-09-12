package iliiasik.artistry.network;

import iliiasik.artistry.block.entity.PosterBlockEntity;
import iliiasik.artistry.data.CanvasData;
import iliiasik.artistry.data.CanvasImage;
import iliiasik.artistry.data.CanvasImageLayer;
import iliiasik.artistry.data.CanvasSignature;
import iliiasik.artistry.item.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

@SuppressWarnings("resource")
public abstract class PosterAccess {

    protected final ServerPlayer player;

    protected PosterAccess(ServerPlayer player) {
        this.player = player;
    }

    @Nullable
    public static PosterAccess resolve(ServerPlayer player, PosterTarget target) {
        if (target instanceof PosterTarget.World world) {
            if (!(player.level() instanceof ServerLevel serverLevel)) return null;
            if (!(serverLevel.getBlockEntity(world.pos()) instanceof PosterBlockEntity poster)) return null;
            return new BlockAccess(player, serverLevel, world.pos(), poster);
        }
        if (target instanceof PosterTarget.Held held) {
            ItemStack stack = player.getItemInHand(held.hand());
            if (stack.isEmpty() || !ModItems.isCanvas(stack)) return null;
            return new ItemAccess(player, stack);
        }
        return null;
    }

    public abstract CanvasData canvasData();

    public abstract CanvasImageLayer imageLayer();

    public abstract CanvasSignature signature();

    @Nullable
    public abstract BlockPos posOrNull();

    public abstract void persist();

    public abstract void persistAndSync();

    public abstract void syncSignature();

    public abstract void syncCanvas(List<CanvasData.PixelChange> changes);

    public abstract void syncImageLayer();

    public abstract void imageEvicted(List<UUID> uuids);

    public abstract void lock(UUID imageUuid, boolean doLock);

    public abstract void releaseAllLocks();

    static final class BlockAccess extends PosterAccess {

        private final ServerLevel level;
        private final BlockPos pos;
        private final PosterBlockEntity poster;

        BlockAccess(ServerPlayer player, ServerLevel level, BlockPos pos, PosterBlockEntity poster) {
            super(player);
            this.level = level;
            this.pos = pos;
            this.poster = poster;
        }

        @Override
        public CanvasData canvasData() {
            return poster.canvasData;
        }

        @Override
        public CanvasImageLayer imageLayer() {
            return poster.imageLayer;
        }

        @Override
        public CanvasSignature signature() {
            return poster.signature;
        }

        @Override
        public BlockPos posOrNull() {
            return pos;
        }

        @Override
        public void persist() {
            poster.setChanged();
        }

        @Override
        public void persistAndSync() {
            poster.markDirtyAndSync();
        }

        @Override
        public void syncSignature() {
            ArtistryNetwork.sendNear(level, pos, null,
                    new SyncSignatureS2CPacket(pos, poster.signature.playerName()));
        }

        @Override
        public void syncCanvas(List<CanvasData.PixelChange> changes) {
            CanvasRoom.publish(level, pos, player, changes);
        }

        @Override
        public void syncImageLayer() {
            ArtistryNetwork.sendNear(level, pos, player,
                    new SyncImageLayerS2CPacket(pos, poster.imageLayer.getImages()));
        }

        @Override
        public void imageEvicted(List<UUID> uuids) {
            ArtistryNetwork.sendNear(level, pos, null, new ImageEvictedS2CPacket(uuids));
        }

        @Override
        public void lock(UUID imageUuid, boolean doLock) {
            CanvasImage img = poster.imageLayer.findByUuid(imageUuid);
            if (img == null) return;
            UUID playerUuid = player.getUUID();
            if (doLock) {
                if (img.lockedByPlayer != null && !img.lockedByPlayer.equals(playerUuid)) return;
                img.lockedByPlayer = playerUuid;
                poster.imageLayer.moveToTop(imageUuid);
            } else {
                if (playerUuid.equals(img.lockedByPlayer)) img.lockedByPlayer = null;
            }
            poster.setChanged();
            ArtistryNetwork.sendNear(level, pos, null, new SyncImageLockS2CPacket(pos, imageUuid, img.lockedByPlayer));
            if (doLock) {
                ArtistryNetwork.sendNear(level, pos, player,
                        new SyncImageLayerS2CPacket(pos, poster.imageLayer.getImages()));
            }
        }

        @Override
        public void releaseAllLocks() {
            for (CanvasImage img : poster.imageLayer.getImages()) {
                if (img.lockedByPlayer == null) continue;
                img.lockedByPlayer = null;
                ArtistryNetwork.sendNear(level, pos, null, new SyncImageLockS2CPacket(pos, img.uuid, null));
            }
        }
    }

    static final class ItemAccess extends PosterAccess {

        private final ItemStack stack;
        private final CanvasData canvasData = new CanvasData();
        private final CanvasImageLayer imageLayer = new CanvasImageLayer();
        private final CanvasSignature signature = new CanvasSignature();

        ItemAccess(ServerPlayer player, ItemStack stack) {
            super(player);
            this.stack = stack;
            CompoundTag tag = stack.getTag();
            if (tag != null) {
                if (tag.contains("canvas")) canvasData.fromNbt(tag.getCompound("canvas"));
                if (tag.contains("images")) imageLayer.fromNbt(tag.getList("images", Tag.TAG_COMPOUND));
                if (tag.contains(CanvasSignature.NBT_KEY)) signature.fromNbt(tag.getCompound(CanvasSignature.NBT_KEY));
            }
            imageLayer.clampToCanvas(canvasData.canvasSize);
        }

        @Override
        public CanvasData canvasData() {
            return canvasData;
        }

        @Override
        public CanvasImageLayer imageLayer() {
            return imageLayer;
        }

        @Override
        public CanvasSignature signature() {
            return signature;
        }

        @Override
        @Nullable
        public BlockPos posOrNull() {
            return null;
        }

        @Override
        public void persist() {
            CompoundTag existing = stack.getTag();
            CompoundTag tag = existing != null ? existing.copy() : new CompoundTag();
            tag.put("canvas", canvasData.toNbt());
            ListTag imageNbt = imageLayer.toNbt();
            if (imageNbt.isEmpty()) {
                tag.remove("images");
            } else {
                tag.put("images", imageNbt);
            }
            if (signature.isSigned()) {
                tag.put(CanvasSignature.NBT_KEY, signature.toNbt());
            } else {
                tag.remove(CanvasSignature.NBT_KEY);
            }

            ItemStack remainder = stack.getCount() > 1
                    ? stack.split(stack.getCount() - 1)
                    : ItemStack.EMPTY;

            stack.setTag(tag);

            if (!remainder.isEmpty() && !player.getInventory().add(remainder)) {
                player.drop(remainder, false);
            }
        }

        @Override
        public void persistAndSync() {
            persist();
        }

        @Override
        public void syncSignature() {
            ArtistryNetwork.sendToPlayer(player, new SyncSignatureS2CPacket(null, signature.playerName()));
        }

        @Override
        public void syncCanvas(List<CanvasData.PixelChange> changes) {
        }

        @Override
        public void syncImageLayer() {
        }

        @Override
        public void imageEvicted(List<UUID> uuids) {
            ArtistryNetwork.sendToPlayer(player, new ImageEvictedS2CPacket(uuids));
        }

        @Override
        public void lock(UUID imageUuid, boolean doLock) {
            CanvasImage img = imageLayer.findByUuid(imageUuid);
            if (img == null) return;
            if (doLock) imageLayer.moveToTop(imageUuid);
            persist();
        }

        @Override
        public void releaseAllLocks() {
            for (CanvasImage img : imageLayer.getImages()) {
                img.lockedByPlayer = null;
            }
        }
    }
}
