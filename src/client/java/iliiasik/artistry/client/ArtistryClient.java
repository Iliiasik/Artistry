package iliiasik.artistry.client;

import iliiasik.artistry.client.ui.screen.PaintScreen;
import iliiasik.artistry.block.PosterBlock;
import iliiasik.artistry.block.entity.ModBlockEntities;
import iliiasik.artistry.block.entity.PosterBlockEntity;
import iliiasik.artistry.client.renderer.PosterBlockEntityRenderer;
import iliiasik.artistry.network.SyncCanvasS2CPacket;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.BlockEntityRendererRegistry;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.ActionResult;

public class ArtistryClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        BlockEntityRendererRegistry.register(ModBlockEntities.POSTER,
                ctx -> new PosterBlockEntityRenderer());

        ClientPlayNetworking.registerGlobalReceiver(SyncCanvasS2CPacket.ID,
                (payload, ctx) -> ctx.client().execute(() -> {
                    MinecraftClient mc = ctx.client();
                    if (mc.world == null) return;
                    if (!(mc.world.getBlockEntity(payload.pos()) instanceof PosterBlockEntity poster)) return;
                    for (var c : payload.changes())
                        poster.canvasData.pixels[c.y() & 0xFF][c.x() & 0xFF] = c.blockIndex();
                    PosterBlockEntityRenderer.invalidate(payload.pos());
                }));

        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            if (world.isClient()) {
                var state = world.getBlockState(hitResult.getBlockPos());
                if (state.getBlock() instanceof PosterBlock) {
                    var be = world.getBlockEntity(hitResult.getBlockPos());
                    if (be instanceof PosterBlockEntity poster) {
                        MinecraftClient.getInstance().setScreen(new PaintScreen(poster));
                        return ActionResult.SUCCESS;
                    }
                }
            }
            return ActionResult.PASS;
        });
    }
}