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
import net.minecraft.util.Hand;
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
                    ServerWorld world = ctx.player().getEntityWorld();
                    if (!(world.getBlockEntity(payload.pos()) instanceof PosterBlockEntity poster)) return;
                    if (poster.canvasData.isSizeChosen()) return;
                    poster.canvasData.canvasSize = payload.size();
                    poster.markDirtyAndSync();
                }));

        ServerPlayNetworking.registerGlobalReceiver(SetItemCanvasSizeC2SPacket.ID,
                (payload, ctx) -> ctx.server().execute(() -> {
                    NbtCompound[] result = getItemCanvasNbt(ctx.player(), payload.hand());
                    if (result == null) return;
                    NbtCompound tag = result[0], canvasNbt = result[1];
                    if (canvasNbt.getInt("size", 0) > 0) return;
                    canvasNbt.putInt("size", payload.size());
                    tag.put("canvas", canvasNbt);
                    ctx.player().getStackInHand(payload.hand())
                            .set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(tag));
                }));

        ServerPlayNetworking.registerGlobalReceiver(SaveCanvasC2SPacket.ID,
                (payload, ctx) -> ctx.server().execute(() -> {
                    ServerWorld world = ctx.player().getEntityWorld();
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
                    NbtCompound[] result = getItemCanvasNbt(ctx.player(), payload.hand());
                    if (result == null) return;
                    NbtCompound tag = result[0], canvasNbt = result[1];
                    CanvasData data = new CanvasData();
                    data.fromNbt(canvasNbt);
                    for (CanvasData.PixelChange c : payload.changes()) {
                        data.pixels[c.y() & 0xFF][c.x() & 0xFF] = c.blockIndex();
                        data.colors[c.y() & 0xFF][c.x() & 0xFF] = c.color();
                    }
                    tag.put("canvas", data.toNbt());
                    ctx.player().getStackInHand(payload.hand())
                            .set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(tag));
                }));
    }

    private static NbtCompound[] getItemCanvasNbt(ServerPlayerEntity player, Hand hand) {
        ItemStack stack = player.getStackInHand(hand);
        if (stack.isEmpty()) return null;
        NbtComponent existing = stack.get(DataComponentTypes.CUSTOM_DATA);
        NbtCompound tag = existing != null ? existing.copyNbt() : new NbtCompound();
        NbtCompound canvasNbt = tag.getCompound("canvas").orElse(new NbtCompound());
        return new NbtCompound[]{tag, canvasNbt};
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