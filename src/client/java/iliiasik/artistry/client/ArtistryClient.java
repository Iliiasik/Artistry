package iliiasik.artistry.client;

import iliiasik.artistry.block.PosterBlock;
import iliiasik.artistry.block.entity.ModBlockEntities;
import iliiasik.artistry.block.entity.PosterBlockEntity;
import iliiasik.artistry.client.renderer.PosterBlockEntityRenderer;
import iliiasik.artistry.client.ui.screen.PaintScreen;
import iliiasik.artistry.item.PosterItem;
import iliiasik.artistry.network.SyncCanvasS2CPacket;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactories;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;

public class ArtistryClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        BlockEntityRendererFactories.register(ModBlockEntities.POSTER,
                ctx -> new PosterBlockEntityRenderer());

        ClientPlayNetworking.registerGlobalReceiver(SyncCanvasS2CPacket.ID,
                (payload, ctx) -> ctx.client().execute(() -> {
                    MinecraftClient mc = ctx.client();
                    if (mc.world == null) return;

                    BlockPos pos = payload.pos();

                    if (mc.world.getBlockEntity(pos) instanceof PosterBlockEntity poster) {
                        for (var c : payload.changes())
                            poster.canvasData.pixels[c.y() & 0xFF][c.x() & 0xFF] = c.blockIndex();
                        PosterBlockEntityRenderer.invalidate(pos);
                    }

                    if (mc.currentScreen instanceof PaintScreen screen) {
                        BlockPos screenPos = screen.getTargetPos();
                        if (pos.equals(screenPos)) {
                            screen.applyRemoteChanges(payload.changes());
                        }
                    }
                }));

        UseItemCallback.EVENT.register((player, world, hand) -> {
            if (!world.isClient()) return ActionResult.PASS;
            var stack = player.getStackInHand(hand);
            if (!(stack.getItem() instanceof PosterItem)) return ActionResult.PASS;
            MinecraftClient.getInstance().setScreen(new PaintScreen(stack, hand));
            return ActionResult.SUCCESS;
        });

        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            if (!world.isClient()) return ActionResult.PASS;
            var state = world.getBlockState(hitResult.getBlockPos());
            if (state.getBlock() instanceof PosterBlock) {
                var be = world.getBlockEntity(hitResult.getBlockPos());
                if (be instanceof PosterBlockEntity poster) {
                    MinecraftClient.getInstance().setScreen(new PaintScreen(poster));
                    return ActionResult.SUCCESS;
                }
            }
            return ActionResult.PASS;
        });
    }
}