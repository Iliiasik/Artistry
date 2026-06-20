package iliiasik.artistry.network;

import iliiasik.artistry.block.entity.PosterBlockEntity;
import iliiasik.artistry.data.CanvasData;
import iliiasik.artistry.data.CanvasImage;
import iliiasik.artistry.data.CanvasImageLayer;
import iliiasik.artistry.item.ModItems;
import iliiasik.artistry.server.ImageStorage;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkSectionPos;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

public abstract class PosterAccess {

    protected final ServerPlayerEntity player;

    protected PosterAccess(ServerPlayerEntity player) {
        this.player = player;
    }

    public static PosterAccess resolve(ServerPlayerEntity player, PosterTarget target) {
        if (target instanceof PosterTarget.World world) {
            if (!(player.getWorld() instanceof ServerWorld serverWorld)) return null;
            if (!(serverWorld.getBlockEntity(world.pos()) instanceof PosterBlockEntity poster)) return null;
            return new BlockAccess(player, serverWorld, world.pos(), poster);
        }
        if (target instanceof PosterTarget.Held held) {
            ItemStack stack = player.getStackInHand(held.hand());
            if (stack.isEmpty() || !stack.isOf(ModItems.POSTER)) return null;
            return new ItemAccess(player, held.hand(), stack);
        }
        return null;
    }

    public abstract CanvasData canvasData();

    public abstract CanvasImageLayer imageLayer();

    public abstract BlockPos posOrNull();

    public abstract void persist();

    public abstract void syncCanvas(List<CanvasData.PixelChange> changes);

    public abstract void syncImageLayer();

    public abstract void deleteImageBytes(UUID uuid);

    public abstract void imageEvicted(List<UUID> uuids);

    public abstract void lock(UUID imageUuid, boolean doLock);

    protected static void sendNear(ServerWorld world, BlockPos pos,
                                   ServerPlayerEntity exclude, CustomPayload packet) {
        int chunkX = ChunkSectionPos.getSectionCoord(pos.getX());
        int chunkZ = ChunkSectionPos.getSectionCoord(pos.getZ());
        for (ServerPlayerEntity p : world.getPlayers()) {
            if (p == exclude) continue;
            if (p.getChunkFilter().isWithinDistance(chunkX, chunkZ)) {
                ServerPlayNetworking.send(p, packet);
            }
        }
    }

    static final class BlockAccess extends PosterAccess {

        private final ServerWorld world;
        private final BlockPos pos;
        private final PosterBlockEntity poster;

        BlockAccess(ServerPlayerEntity player, ServerWorld world, BlockPos pos, PosterBlockEntity poster) {
            super(player);
            this.world = world;
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
            sendNear(world, pos, player, new SyncCanvasS2CPacket(pos, changes));
        }

        @Override
        public void syncImageLayer() {
            sendNear(world, pos, player, new SyncImageLayerS2CPacket(pos, poster.imageLayer.getImages()));
        }

        @Override
        public void deleteImageBytes(UUID uuid) {
            try {
                ImageStorage.delete(uuid);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void imageEvicted(List<UUID> uuids) {
            sendNear(world, pos, null, new ImageEvictedS2CPacket(uuids));
        }

        @Override
        public void lock(UUID imageUuid, boolean doLock) {
            CanvasImage img = poster.imageLayer.findByUuid(imageUuid);
            if (img == null) return;
            UUID playerUuid = player.getUuid();
            if (doLock) {
                if (img.lockedByPlayer != null && !img.lockedByPlayer.equals(playerUuid)) return;
                img.lockedByPlayer = playerUuid;
                poster.imageLayer.moveToTop(imageUuid);
            } else {
                if (playerUuid.equals(img.lockedByPlayer)) img.lockedByPlayer = null;
            }
            poster.markDirtyAndSync();
            sendNear(world, pos, null, new SyncImageLockS2CPacket(pos, imageUuid, img.lockedByPlayer));
            if (doLock) {
                sendNear(world, pos, player, new SyncImageLayerS2CPacket(pos, poster.imageLayer.getImages()));
            }
        }
    }

    static final class ItemAccess extends PosterAccess {

        private final Hand hand;
        private final ItemStack stack;
        private final CanvasData canvasData = new CanvasData();
        private final CanvasImageLayer imageLayer = new CanvasImageLayer();

        ItemAccess(ServerPlayerEntity player, Hand hand, ItemStack stack) {
            super(player);
            this.hand = hand;
            this.stack = stack;
            NbtComponent comp = stack.get(DataComponentTypes.CUSTOM_DATA);
            NbtCompound tag = comp != null ? comp.copyNbt() : new NbtCompound();
            if (tag.contains("canvas")) canvasData.fromNbt(tag.getCompound("canvas"));
            if (tag.contains("images")) imageLayer.fromNbt(tag.getList("images", NbtList.COMPOUND_TYPE));
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
            NbtComponent comp = stack.get(DataComponentTypes.CUSTOM_DATA);
            NbtCompound tag = comp != null ? comp.copyNbt() : new NbtCompound();
            tag.put("canvas", canvasData.toNbt());
            tag.put("images", imageLayer.toNbt());
            if (stack.getCount() > 1) {
                ItemStack remainder = stack.split(stack.getCount() - 1);
                stack.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(tag));
                stack.set(DataComponentTypes.MAX_STACK_SIZE, 1);
                if (!player.getInventory().insertStack(remainder)) {
                    player.dropItem(remainder, false);
                }
            } else {
                stack.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(tag));
                stack.set(DataComponentTypes.MAX_STACK_SIZE, 1);
            }
        }

        @Override
        public void syncCanvas(List<CanvasData.PixelChange> changes) {
        }

        @Override
        public void syncImageLayer() {
        }

        @Override
        public void deleteImageBytes(UUID uuid) {
        }

        @Override
        public void imageEvicted(List<UUID> uuids) {
            ServerPlayNetworking.send(player, new ImageEvictedS2CPacket(uuids));
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