package iliiasik.artistry.network;

import iliiasik.artistry.block.entity.PosterBlockEntity;
import iliiasik.artistry.data.CanvasData;
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

public class ModNetwork {
    public static void register() {
        PayloadTypeRegistry.playC2S().register(SaveCanvasC2SPacket.ID, SaveCanvasC2SPacket.CODEC);
        PayloadTypeRegistry.playC2S().register(SaveItemCanvasC2SPacket.ID, SaveItemCanvasC2SPacket.CODEC);
        PayloadTypeRegistry.playC2S().register(SetCanvasSizeC2SPacket.ID, SetCanvasSizeC2SPacket.CODEC);
        PayloadTypeRegistry.playC2S().register(SetItemCanvasSizeC2SPacket.ID, SetItemCanvasSizeC2SPacket.CODEC);
        PayloadTypeRegistry.playS2C().register(SyncCanvasS2CPacket.ID, SyncCanvasS2CPacket.CODEC);
        PayloadTypeRegistry.playS2C().register(PosterRemovedS2CPacket.ID, PosterRemovedS2CPacket.CODEC);

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
                    if (stack.isEmpty()) return;
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
                    if (stack.isEmpty()) return;
                    NbtCompound tag = getOrCreateCustomData(stack);
                    NbtCompound canvasNbt = getOrCreateCanvasNbt(tag);
                    CanvasData data = new CanvasData();
                    data.fromNbt(canvasNbt);
                    for (CanvasData.PixelChange c : payload.changes()) {
                        data.pixels[c.y() & 0xFF][c.x() & 0xFF] = c.blockIndex();
                        data.colors[c.y() & 0xFF][c.x() & 0xFF] = c.color();
                    }
                    tag.put("canvas", data.toNbt());
                    applyCanvasDataToStack(ctx.player(), stack, tag);
                }));
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
}