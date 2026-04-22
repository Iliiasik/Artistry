package iliiasik.artistry.network;

import iliiasik.artistry.block.entity.PosterBlockEntity;
import iliiasik.artistry.data.CanvasData;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkSectionPos;

public class ModNetwork {
    public static void register() {
        PayloadTypeRegistry.playC2S().register(SaveCanvasC2SPacket.ID, SaveCanvasC2SPacket.CODEC);
        PayloadTypeRegistry.playC2S().register(SaveItemCanvasC2SPacket.ID, SaveItemCanvasC2SPacket.CODEC);
        PayloadTypeRegistry.playS2C().register(SyncCanvasS2CPacket.ID, SyncCanvasS2CPacket.CODEC);
        PayloadTypeRegistry.playS2C().register(PosterRemovedS2CPacket.ID, PosterRemovedS2CPacket.CODEC);

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
                    ServerPlayerEntity player = ctx.player();
                    var stack = player.getStackInHand(payload.hand());
                    if (stack.isEmpty()) return;
                    NbtComponent existing = stack.get(DataComponentTypes.CUSTOM_DATA);
                    NbtCompound tag = existing != null ? existing.copyNbt() : new NbtCompound();
                    NbtCompound canvasNbt = tag.getCompound("canvas").orElse(new NbtCompound());
                    CanvasData data = new CanvasData();
                    data.fromNbt(canvasNbt);
                    for (CanvasData.PixelChange c : payload.changes()) {
                        data.pixels[c.y() & 0xFF][c.x() & 0xFF] = c.blockIndex();
                        data.colors[c.y() & 0xFF][c.x() & 0xFF] = c.color();
                    }
                    tag.put("canvas", data.toNbt());
                    stack.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(tag));
                }));
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