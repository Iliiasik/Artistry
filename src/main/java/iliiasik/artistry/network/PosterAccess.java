package iliiasik.artistry.network;

import iliiasik.artistry.block.entity.PosterBlockEntity;
import iliiasik.artistry.data.CanvasData;
import iliiasik.artistry.data.CanvasImage;
import iliiasik.artistry.data.CanvasImageLayer;
import iliiasik.artistry.item.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;
import java.util.UUID;

public abstract class PosterAccess {

    protected final ServerPlayer player;

    protected PosterAccess(ServerPlayer player) {
        this.player = player;
    }

    public static PosterAccess resolve(ServerPlayer player, PosterTarget target) {
        if (target instanceof PosterTarget.World world) {
            if (!(player.level() instanceof ServerLevel serverLevel)) return null;
            if (!(serverLevel.getBlockEntity(world.pos()) instanceof PosterBlockEntity poster)) return null;
            return new BlockAccess(player, serverLevel, world.pos(), poster);
        }
        if (target instanceof PosterTarget.Held held) {
            ItemStack stack = player.getItemInHand(held.hand());
            if (stack.isEmpty() || !stack.is(ModItems.POSTER.get())) return null;
            return new ItemAccess(player, stack);
        }
        return null;
    }

    public abstract CanvasData canvasData();

    public abstract CanvasImageLayer imageLayer();

    public abstract BlockPos posOrNull();

    public abstract void persist();

    public abstract void syncCanvas(List<CanvasData.PixelChange> changes);

    public abstract void syncImageLayer();

    public abstract void imageEvicted(List<UUID> uuids);

    public abstract void lock(UUID imageUuid, boolean doLock);

    protected static void sendNear(ServerLevel level, BlockPos pos,
                                   ServerPlayer exclude, CustomPacketPayload packet) {
        int chunkX = SectionPos.blockToSectionCoord(pos.getX());
        int chunkZ = SectionPos.blockToSectionCoord(pos.getZ());
        for (ServerPlayer p : level.players()) {
            if (p == exclude) continue;
            if (p.getChunkTrackingView().isInViewDistance(chunkX, chunkZ)) {
                PacketDistributor.sendToPlayer(p, packet);
            }
        }
    }

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
        public BlockPos posOrNull() {
            return pos;
        }

        @Override
        public void persist() {
            poster.markDirtyAndSync();
        }

        @Override
        public void syncCanvas(List<CanvasData.PixelChange> changes) {
            sendNear(level, pos, player, new SyncCanvasS2CPacket(pos, changes));
        }

        @Override
        public void syncImageLayer() {
            sendNear(level, pos, player, new SyncImageLayerS2CPacket(pos, poster.imageLayer.getImages()));
        }

        @Override
        public void imageEvicted(List<UUID> uuids) {
            sendNear(level, pos, null, new ImageEvictedS2CPacket(uuids));
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
            poster.markDirtyAndSync();
            sendNear(level, pos, null, new SyncImageLockS2CPacket(pos, imageUuid, img.lockedByPlayer));
            if (doLock) {
                sendNear(level, pos, player, new SyncImageLayerS2CPacket(pos, poster.imageLayer.getImages()));
            }
        }
    }

    static final class ItemAccess extends PosterAccess {

        private final ItemStack stack;
        private final CanvasData canvasData = new CanvasData();
        private final CanvasImageLayer imageLayer = new CanvasImageLayer();

        ItemAccess(ServerPlayer player, ItemStack stack) {
            super(player);
            this.stack = stack;
            CustomData comp = stack.get(DataComponents.CUSTOM_DATA);
            CompoundTag tag = comp != null ? comp.copyTag() : new CompoundTag();
            if (tag.contains("canvas")) canvasData.fromNbt(tag.getCompound("canvas"));
            if (tag.contains("images")) imageLayer.fromNbt(tag.getList("images", Tag.TAG_COMPOUND));
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
        public BlockPos posOrNull() {
            return null;
        }

        @Override
        public void persist() {
            CustomData comp = stack.get(DataComponents.CUSTOM_DATA);
            CompoundTag tag = comp != null ? comp.copyTag() : new CompoundTag();
            tag.put("canvas", canvasData.toNbt());
            tag.put("images", imageLayer.toNbt());
            if (stack.getCount() > 1) {
                ItemStack remainder = stack.split(stack.getCount() - 1);
                stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
                stack.set(DataComponents.MAX_STACK_SIZE, 1);
                if (!player.getInventory().add(remainder)) {
                    player.drop(remainder, false);
                }
            } else {
                stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
                stack.set(DataComponents.MAX_STACK_SIZE, 1);
            }
        }

        @Override
        public void syncCanvas(List<CanvasData.PixelChange> changes) {
        }

        @Override
        public void syncImageLayer() {
        }

        @Override
        public void imageEvicted(List<UUID> uuids) {
            PacketDistributor.sendToPlayer(player, new ImageEvictedS2CPacket(uuids));
        }

        @Override
        public void lock(UUID imageUuid, boolean doLock) {
            CanvasImage img = imageLayer.findByUuid(imageUuid);
            if (img == null) return;
            if (doLock) imageLayer.moveToTop(imageUuid);
            persist();
        }
    }
}