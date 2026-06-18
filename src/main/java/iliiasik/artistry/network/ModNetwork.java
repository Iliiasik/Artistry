package iliiasik.artistry.network;

import iliiasik.artistry.block.entity.PosterBlockEntity;
import iliiasik.artistry.data.CanvasData;
import iliiasik.artistry.data.CanvasImage;
import iliiasik.artistry.server.ImageStorage;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkSectionPos;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

public class ModNetwork {
    public static void register() {
        PayloadTypeRegistry.playC2S().register(SaveCanvasC2SPacket.ID, SaveCanvasC2SPacket.CODEC);
        PayloadTypeRegistry.playC2S().register(SaveItemCanvasC2SPacket.ID, SaveItemCanvasC2SPacket.CODEC);
        PayloadTypeRegistry.playC2S().register(SetCanvasSizeC2SPacket.ID, SetCanvasSizeC2SPacket.CODEC);
        PayloadTypeRegistry.playC2S().register(SetItemCanvasSizeC2SPacket.ID, SetItemCanvasSizeC2SPacket.CODEC);
        PayloadTypeRegistry.playC2S().register(UploadImageC2SPacket.ID, UploadImageC2SPacket.CODEC);
        PayloadTypeRegistry.playC2S().register(RequestImageC2SPacket.ID, RequestImageC2SPacket.CODEC);
        PayloadTypeRegistry.playC2S().register(MoveCanvasImageC2SPacket.ID, MoveCanvasImageC2SPacket.CODEC);
        PayloadTypeRegistry.playC2S().register(DeleteCanvasImageC2SPacket.ID, DeleteCanvasImageC2SPacket.CODEC);
        PayloadTypeRegistry.playC2S().register(TogglePixelizeC2SPacket.ID, TogglePixelizeC2SPacket.CODEC);
        PayloadTypeRegistry.playC2S().register(LockCanvasImageC2SPacket.ID, LockCanvasImageC2SPacket.CODEC);
        PayloadTypeRegistry.playC2S().register(UploadItemImageC2SPacket.ID, UploadItemImageC2SPacket.CODEC);
        PayloadTypeRegistry.playC2S().register(DeleteItemImageC2SPacket.ID, DeleteItemImageC2SPacket.CODEC);
        PayloadTypeRegistry.playC2S().register(ToggleItemPixelizeC2SPacket.ID, ToggleItemPixelizeC2SPacket.CODEC);
        PayloadTypeRegistry.playC2S().register(MoveItemImageC2SPacket.ID, MoveItemImageC2SPacket.CODEC);
        PayloadTypeRegistry.playC2S().register(MoveItemImageToTopC2SPacket.ID, MoveItemImageToTopC2SPacket.CODEC);

        PayloadTypeRegistry.playS2C().register(SyncCanvasS2CPacket.ID, SyncCanvasS2CPacket.CODEC);
        PayloadTypeRegistry.playS2C().register(PosterRemovedS2CPacket.ID, PosterRemovedS2CPacket.CODEC);
        PayloadTypeRegistry.playS2C().register(ImageUploadedS2CPacket.ID, ImageUploadedS2CPacket.CODEC);
        PayloadTypeRegistry.playS2C().register(DeliverImageS2CPacket.ID, DeliverImageS2CPacket.CODEC);
        PayloadTypeRegistry.playS2C().register(SyncImageLayerS2CPacket.ID, SyncImageLayerS2CPacket.CODEC);
        PayloadTypeRegistry.playS2C().register(SyncImageLockS2CPacket.ID, SyncImageLockS2CPacket.CODEC);
        PayloadTypeRegistry.playS2C().register(ImageEvictedS2CPacket.ID, ImageEvictedS2CPacket.CODEC);

        ServerPlayNetworking.registerGlobalReceiver(MoveItemImageToTopC2SPacket.ID,
                (payload, ctx) -> ctx.server().execute(() -> {
                    ItemStack stack = ctx.player().getStackInHand(payload.hand());
                    if (stack.isEmpty() || !stack.isOf(iliiasik.artistry.item.ModItems.POSTER)) return;
                    NbtCompound tag = getOrCreateCustomData(stack);
                    if (!tag.contains("images")) return;
                    iliiasik.artistry.data.CanvasImageLayer layer = new iliiasik.artistry.data.CanvasImageLayer();
                    layer.fromNbt(tag.getList("images", net.minecraft.nbt.NbtList.COMPOUND_TYPE));
                    layer.moveToTop(payload.uuid());
                    tag.put("images", layer.toNbt());
                    applyCanvasDataToStack(ctx.player(), stack, tag);
                }));

        ServerPlayNetworking.registerGlobalReceiver(MoveItemImageC2SPacket.ID,
                (payload, ctx) -> ctx.server().execute(() -> {
                    ItemStack stack = ctx.player().getStackInHand(payload.hand());
                    if (stack.isEmpty() || !stack.isOf(iliiasik.artistry.item.ModItems.POSTER)) return;
                    NbtCompound tag = getOrCreateCustomData(stack);
                    if (!tag.contains("images")) return;
                    iliiasik.artistry.data.CanvasImageLayer layer = new iliiasik.artistry.data.CanvasImageLayer();
                    layer.fromNbt(tag.getList("images", net.minecraft.nbt.NbtList.COMPOUND_TYPE));
                    CanvasImage img = layer.findByUuid(payload.uuid());
                    if (img == null) return;
                    img.gridX = payload.gridX();
                    img.gridY = payload.gridY();
                    img.gridW = payload.gridW();
                    img.gridH = payload.gridH();
                    tag.put("images", layer.toNbt());
                    applyCanvasDataToStack(ctx.player(), stack, tag);
                }));

        ServerPlayNetworking.registerGlobalReceiver(UploadItemImageC2SPacket.ID,
                (payload, ctx) -> ctx.server().execute(() -> {
                    ItemStack stack = ctx.player().getStackInHand(payload.hand());
                    if (stack.isEmpty() || !stack.isOf(iliiasik.artistry.item.ModItems.POSTER)) return;
                    try {
                        UUID uuid = ImageStorage.save(payload.bytes());
                        NbtComponent comp = stack.get(DataComponentTypes.CUSTOM_DATA);
                        NbtCompound tag = comp != null ? comp.copyNbt() : new NbtCompound();
                        NbtCompound canvasNbt = tag.contains("canvas") ? tag.getCompound("canvas") : new NbtCompound();
                        int canvasSize = canvasNbt.getInt("size");
                        if (canvasSize <= 0) canvasSize = 16;
                        int gridW = Math.max(CanvasImage.MIN_GRID, canvasSize / 2);
                        int gridH = Math.max(CanvasImage.MIN_GRID, canvasSize / 2);
                        int gridX = (canvasSize - gridW) / 2;
                        int gridY = (canvasSize - gridH) / 2;

                        iliiasik.artistry.data.CanvasImageLayer layer = new iliiasik.artistry.data.CanvasImageLayer();
                        if (tag.contains("images")) {
                            layer.fromNbt(tag.getList("images", net.minecraft.nbt.NbtList.COMPOUND_TYPE));
                        }
                        CanvasImage img = new CanvasImage(uuid, gridX, gridY, gridW, gridH);
                        List<UUID> evicted = layer.addImage(img);
                        tag.put("images", layer.toNbt());
                        applyCanvasDataToStack(ctx.player(), stack, tag);

                        deleteEvictedImages(evicted);

                        ServerPlayNetworking.send(ctx.player(),
                                new ImageUploadedS2CPacket(null, uuid, gridX, gridY, gridW, gridH));
                        ServerPlayNetworking.send(ctx.player(),
                                new DeliverImageS2CPacket(uuid, payload.bytes()));
                        if (!evicted.isEmpty()) {
                            ServerPlayNetworking.send(ctx.player(), new ImageEvictedS2CPacket(evicted));
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }));

        ServerPlayNetworking.registerGlobalReceiver(LockCanvasImageC2SPacket.ID,
                (payload, ctx) -> ctx.server().execute(() -> {
                    if (!(ctx.player().getWorld() instanceof ServerWorld world)) return;
                    if (!(world.getBlockEntity(payload.pos()) instanceof PosterBlockEntity poster)) return;
                    CanvasImage img = poster.imageLayer.findByUuid(payload.imageUuid());
                    if (img == null) return;
                    UUID playerUuid = ctx.player().getUuid();
                    if (payload.lock()) {
                        if (img.lockedByPlayer != null && !img.lockedByPlayer.equals(playerUuid)) return;
                        img.lockedByPlayer = playerUuid;
                        poster.imageLayer.moveToTop(payload.imageUuid());
                    } else {
                        if (playerUuid.equals(img.lockedByPlayer)) img.lockedByPlayer = null;
                    }
                    poster.markDirtyAndSync();
                    broadcastImageLockToAll(world, payload.pos(),
                            new SyncImageLockS2CPacket(payload.pos(), payload.imageUuid(), img.lockedByPlayer));
                    if (payload.lock()) {
                        broadcastImageLayerToWatchers(world, payload.pos(), ctx.player(),
                                new SyncImageLayerS2CPacket(payload.pos(), poster.imageLayer.getImages()));
                    }
                }));

        ServerPlayNetworking.registerGlobalReceiver(SetCanvasSizeC2SPacket.ID,
                (payload, ctx) -> ctx.server().execute(() -> {
                    if (!(ctx.player().getWorld() instanceof ServerWorld world)) return;
                    if (!(world.getBlockEntity(payload.pos()) instanceof PosterBlockEntity poster)) return;
                    if (poster.canvasData.isSizeChosen()) return;
                    poster.canvasData.canvasSize = payload.size();
                    poster.markDirtyAndSync();
                }));

        ServerPlayNetworking.registerGlobalReceiver(SetItemCanvasSizeC2SPacket.ID,
                (payload, ctx) -> ctx.server().execute(() -> {
                    ItemStack stack = ctx.player().getStackInHand(payload.hand());
                    if (stack.isEmpty() || !stack.isOf(iliiasik.artistry.item.ModItems.POSTER)) return;
                    NbtCompound tag = getOrCreateCustomData(stack);
                    NbtCompound canvasNbt = getOrCreateCanvasNbt(tag);
                    if (canvasNbt.getInt("size") > 0) return;
                    canvasNbt.putInt("size", payload.size());
                    tag.put("canvas", canvasNbt);
                    applyCanvasDataToStack(ctx.player(), stack, tag);
                }));

        ServerPlayNetworking.registerGlobalReceiver(SaveCanvasC2SPacket.ID,
                (payload, ctx) -> ctx.server().execute(() -> {
                    if (!(ctx.player().getWorld() instanceof ServerWorld world)) return;
                    BlockPos pos = payload.pos();
                    if (!(world.getBlockEntity(pos) instanceof PosterBlockEntity poster)) return;
                    if (!poster.canvasData.isSizeChosen()) return;
                    for (CanvasData.PixelChange c : payload.changes()) {
                        poster.canvasData.pixels[c.y() & 0xFF][c.x() & 0xFF] = c.blockIndex();
                        poster.canvasData.colors[c.y() & 0xFF][c.x() & 0xFF] = c.color();
                    }
                    poster.markDirtyAndSync();
                    broadcastToWatchers(world, pos, ctx.player(),
                            new SyncCanvasS2CPacket(pos, payload.changes()));
                }));

        ServerPlayNetworking.registerGlobalReceiver(SaveItemCanvasC2SPacket.ID,
                (payload, ctx) -> ctx.server().execute(() -> {
                    ItemStack stack = ctx.player().getStackInHand(payload.hand());
                    if (stack.isEmpty() || !stack.isOf(iliiasik.artistry.item.ModItems.POSTER)) return;
                    NbtCompound tag = getOrCreateCustomData(stack);
                    NbtCompound canvasNbt = getOrCreateCanvasNbt(tag);
                    if (canvasNbt.getInt("size") <= 0) return;
                    CanvasData data = new CanvasData();
                    data.fromNbt(canvasNbt);
                    for (CanvasData.PixelChange c : payload.changes()) {
                        data.pixels[c.y() & 0xFF][c.x() & 0xFF] = c.blockIndex();
                        data.colors[c.y() & 0xFF][c.x() & 0xFF] = c.color();
                    }
                    tag.put("canvas", data.toNbt());
                    applyCanvasDataToStack(ctx.player(), stack, tag);
                }));

        ServerPlayNetworking.registerGlobalReceiver(UploadImageC2SPacket.ID,
                (payload, ctx) -> ctx.server().execute(() -> {
                    if (!(ctx.player().getWorld() instanceof ServerWorld world)) return;
                    BlockPos pos = payload.pos();
                    if (!(world.getBlockEntity(pos) instanceof PosterBlockEntity poster)) return;
                    if (!poster.canvasData.isSizeChosen()) return;
                    try {
                        UUID uuid = ImageStorage.save(payload.bytes());
                        int canvasSize = poster.canvasData.canvasSize;
                        int gridW = Math.max(CanvasImage.MIN_GRID, canvasSize / 2);
                        int gridH = Math.max(CanvasImage.MIN_GRID, canvasSize / 2);
                        int gridX = (canvasSize - gridW) / 2;
                        int gridY = (canvasSize - gridH) / 2;

                        CanvasImage img = new CanvasImage(uuid, gridX, gridY, gridW, gridH);
                        List<UUID> evicted = poster.imageLayer.addImage(img);
                        poster.markDirtyAndSync();

                        deleteEvictedImages(evicted);

                        ServerPlayNetworking.send(ctx.player(),
                                new ImageUploadedS2CPacket(pos, uuid, gridX, gridY, gridW, gridH));
                        ServerPlayNetworking.send(ctx.player(),
                                new DeliverImageS2CPacket(uuid, payload.bytes()));

                        broadcastImageLayerToWatchers(world, pos, ctx.player(),
                                new SyncImageLayerS2CPacket(pos, poster.imageLayer.getImages()));

                        if (!evicted.isEmpty()) {
                            broadcastImageEvictedToAll(world, pos, new ImageEvictedS2CPacket(evicted));
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }));

        ServerPlayNetworking.registerGlobalReceiver(RequestImageC2SPacket.ID,
                (payload, ctx) -> ctx.server().execute(() -> {
                    if (!ImageStorage.exists(payload.uuid())) return;
                    try {
                        byte[] bytes = ImageStorage.load(payload.uuid());
                        ServerPlayNetworking.send(ctx.player(),
                                new DeliverImageS2CPacket(payload.uuid(), bytes));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }));

        ServerPlayNetworking.registerGlobalReceiver(MoveCanvasImageC2SPacket.ID,
                (payload, ctx) -> ctx.server().execute(() -> {
                    if (!(ctx.player().getWorld() instanceof ServerWorld world)) return;
                    if (!(world.getBlockEntity(payload.pos()) instanceof PosterBlockEntity poster)) return;
                    CanvasImage img = poster.imageLayer.findByUuid(payload.uuid());
                    if (img == null) return;
                    img.gridX = payload.gridX();
                    img.gridY = payload.gridY();
                    img.gridW = payload.gridW();
                    img.gridH = payload.gridH();
                    poster.markDirtyAndSync();
                    broadcastImageLayerToWatchers(world, payload.pos(), ctx.player(),
                            new SyncImageLayerS2CPacket(payload.pos(), poster.imageLayer.getImages()));
                }));

        ServerPlayNetworking.registerGlobalReceiver(DeleteCanvasImageC2SPacket.ID,
                (payload, ctx) -> ctx.server().execute(() -> {
                    if (!(ctx.player().getWorld() instanceof ServerWorld world)) return;
                    if (!(world.getBlockEntity(payload.pos()) instanceof PosterBlockEntity poster)) return;
                    poster.imageLayer.removeImage(payload.uuid());
                    poster.markDirtyAndSync();
                    try {
                        ImageStorage.delete(payload.uuid());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    broadcastImageLayerToWatchers(world, payload.pos(), ctx.player(),
                            new SyncImageLayerS2CPacket(payload.pos(), poster.imageLayer.getImages()));
                }));

        ServerPlayNetworking.registerGlobalReceiver(TogglePixelizeC2SPacket.ID,
                (payload, ctx) -> ctx.server().execute(() -> {
                    if (!(ctx.player().getWorld() instanceof ServerWorld world)) return;
                    if (!(world.getBlockEntity(payload.pos()) instanceof PosterBlockEntity poster)) return;
                    CanvasImage img = poster.imageLayer.findByUuid(payload.uuid());
                    if (img == null) return;
                    img.pixelized = !img.pixelized;
                    poster.markDirtyAndSync();
                    broadcastImageLayerToWatchers(world, payload.pos(), ctx.player(),
                            new SyncImageLayerS2CPacket(payload.pos(), poster.imageLayer.getImages()));
                }));

        ServerPlayNetworking.registerGlobalReceiver(DeleteItemImageC2SPacket.ID,
                (payload, ctx) -> ctx.server().execute(() -> {
                    ItemStack stack = ctx.player().getStackInHand(payload.hand());
                    if (stack.isEmpty() || !stack.isOf(iliiasik.artistry.item.ModItems.POSTER)) return;
                    NbtCompound tag = getOrCreateCustomData(stack);
                    if (!tag.contains("images")) return;
                    iliiasik.artistry.data.CanvasImageLayer layer = new iliiasik.artistry.data.CanvasImageLayer();
                    layer.fromNbt(tag.getList("images", net.minecraft.nbt.NbtList.COMPOUND_TYPE));
                    layer.removeImage(payload.uuid());
                    tag.put("images", layer.toNbt());
                    applyCanvasDataToStack(ctx.player(), stack, tag);
                }));

        ServerPlayNetworking.registerGlobalReceiver(ToggleItemPixelizeC2SPacket.ID,
                (payload, ctx) -> ctx.server().execute(() -> {
                    ItemStack stack = ctx.player().getStackInHand(payload.hand());
                    if (stack.isEmpty() || !stack.isOf(iliiasik.artistry.item.ModItems.POSTER)) return;
                    NbtCompound tag = getOrCreateCustomData(stack);
                    if (!tag.contains("images")) return;
                    iliiasik.artistry.data.CanvasImageLayer layer = new iliiasik.artistry.data.CanvasImageLayer();
                    layer.fromNbt(tag.getList("images", net.minecraft.nbt.NbtList.COMPOUND_TYPE));
                    CanvasImage img = layer.findByUuid(payload.uuid());
                    if (img == null) return;
                    img.pixelized = !img.pixelized;
                    tag.put("images", layer.toNbt());
                    applyCanvasDataToStack(ctx.player(), stack, tag);
                }));
    }

    private static void deleteEvictedImages(List<UUID> evicted) {
        for (UUID uuid : evicted) {
            try {
                ImageStorage.delete(uuid);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private static NbtCompound getOrCreateCustomData(ItemStack stack) {
        NbtComponent existing = stack.get(DataComponentTypes.CUSTOM_DATA);
        return existing != null ? existing.copyNbt() : new NbtCompound();
    }

    private static NbtCompound getOrCreateCanvasNbt(NbtCompound tag) {
        return tag.contains("canvas") ? tag.getCompound("canvas") : new NbtCompound();
    }

    private static void applyCanvasDataToStack(ServerPlayerEntity player, ItemStack stack, NbtCompound tag) {
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

    private static void broadcastToWatchers(ServerWorld world, BlockPos pos,
                                            ServerPlayerEntity exclude,
                                            SyncCanvasS2CPacket packet) {
        int chunkX = ChunkSectionPos.getSectionCoord(pos.getX());
        int chunkZ = ChunkSectionPos.getSectionCoord(pos.getZ());
        for (ServerPlayerEntity player : world.getPlayers()) {
            if (player == exclude) continue;
            if (player.getChunkFilter().isWithinDistance(chunkX, chunkZ)) {
                ServerPlayNetworking.send(player, packet);
            }
        }
    }

    private static void broadcastImageLayerToWatchers(ServerWorld world, BlockPos pos,
                                                      ServerPlayerEntity exclude,
                                                      SyncImageLayerS2CPacket packet) {
        int chunkX = ChunkSectionPos.getSectionCoord(pos.getX());
        int chunkZ = ChunkSectionPos.getSectionCoord(pos.getZ());
        for (ServerPlayerEntity player : world.getPlayers()) {
            if (player == exclude) continue;
            if (player.getChunkFilter().isWithinDistance(chunkX, chunkZ)) {
                ServerPlayNetworking.send(player, packet);
            }
        }
    }

    private static void broadcastImageLockToAll(ServerWorld world, BlockPos pos,
                                                SyncImageLockS2CPacket packet) {
        int chunkX = ChunkSectionPos.getSectionCoord(pos.getX());
        int chunkZ = ChunkSectionPos.getSectionCoord(pos.getZ());
        for (ServerPlayerEntity player : world.getPlayers()) {
            if (player.getChunkFilter().isWithinDistance(chunkX, chunkZ)) {
                ServerPlayNetworking.send(player, packet);
            }
        }
    }

    private static void broadcastImageEvictedToAll(ServerWorld world, BlockPos pos,
                                                   ImageEvictedS2CPacket packet) {
        int chunkX = ChunkSectionPos.getSectionCoord(pos.getX());
        int chunkZ = ChunkSectionPos.getSectionCoord(pos.getZ());
        for (ServerPlayerEntity player : world.getPlayers()) {
            if (player.getChunkFilter().isWithinDistance(chunkX, chunkZ)) {
                ServerPlayNetworking.send(player, packet);
            }
        }
    }
}