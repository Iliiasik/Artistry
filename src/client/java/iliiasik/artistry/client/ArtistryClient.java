package iliiasik.artistry.client;

import iliiasik.artistry.block.PosterBlock;
import iliiasik.artistry.block.entity.ModBlockEntities;
import iliiasik.artistry.block.entity.PosterBlockEntity;
import iliiasik.artistry.client.image.ClientImageCache;
import iliiasik.artistry.client.renderer.PosterBlockEntityRenderer;
import iliiasik.artistry.client.ui.screen.CanvasSizeScreen;
import iliiasik.artistry.client.ui.screen.PaintScreen;
import iliiasik.artistry.data.CanvasData;
import iliiasik.artistry.item.ModItems;
import iliiasik.artistry.item.PosterItem;
import iliiasik.artistry.network.DeliverImageS2CPacket;
import iliiasik.artistry.network.ImageUploadedS2CPacket;
import iliiasik.artistry.network.PosterRemovedS2CPacket;
import iliiasik.artistry.network.RequestImageC2SPacket;
import iliiasik.artistry.network.SyncCanvasS2CPacket;
import iliiasik.artistry.network.SyncImageLayerS2CPacket;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.item.ModelPredicateProviderRegistry;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactories;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Identifier;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.BlockPos;

public class ArtistryClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ModelPredicateProviderRegistry.register(ModItems.POSTER, Identifier.of("artistry", "has_canvas"),
                (stack, world, entity, seed) -> CanvasData.isSizeChosenForStack(stack) ? 1.0F : 0.0F);

        BlockEntityRendererFactories.register(ModBlockEntities.POSTER,
                ctx1 -> new PosterBlockEntityRenderer());

        ClientPlayNetworking.registerGlobalReceiver(SyncCanvasS2CPacket.ID,
                (payload, ctx) -> ctx.client().execute(() -> {
                    MinecraftClient mc = ctx.client();
                    if (mc.world == null) return;
                    BlockPos pos = payload.pos();
                    if (mc.world.getBlockEntity(pos) instanceof PosterBlockEntity poster) {
                        for (var c : payload.changes()) {
                            poster.canvasData.pixels[c.y() & 0xFF][c.x() & 0xFF] = c.blockIndex();
                            poster.canvasData.colors[c.y() & 0xFF][c.x() & 0xFF] = c.color();
                        }
                        PosterBlockEntityRenderer.invalidate(pos);
                    }
                    if (mc.currentScreen instanceof PaintScreen screen) {
                        BlockPos screenPos = screen.getTargetPos();
                        if (pos.equals(screenPos)) {
                            screen.applyRemoteChanges(payload.changes());
                        }
                    }
                }));

        ClientPlayNetworking.registerGlobalReceiver(PosterRemovedS2CPacket.ID,
                (payload, ctx) -> ctx.client().execute(() -> {
                    MinecraftClient mc = ctx.client();
                    PosterBlockEntityRenderer.invalidate(payload.pos());
                    if (mc.currentScreen instanceof PaintScreen screen) {
                        if (payload.pos().equals(screen.getTargetPos())) {
                            screen.scheduledClose();
                        }
                    }
                }));

        ClientPlayNetworking.registerGlobalReceiver(SyncImageLayerS2CPacket.ID,
                (payload, ctx) -> ctx.client().execute(() -> {
                    MinecraftClient mc = ctx.client();
                    if (mc.world == null) return;
                    BlockPos pos = payload.pos();
                    if (mc.world.getBlockEntity(pos) instanceof PosterBlockEntity poster) {
                        poster.imageLayer.getImages().clear();
                        for (var img : payload.images()) {
                            poster.imageLayer.addImage(img);
                        }
                    }
                    if (mc.currentScreen instanceof PaintScreen screen) {
                        if (pos.equals(screen.getTargetPos())) {
                            screen.applyImageLayerSync(payload.images());
                            for (var img : payload.images()) {
                                if (!ClientImageCache.has(img.uuid)) {
                                    ClientPlayNetworking.send(new RequestImageC2SPacket(img.uuid));
                                }
                            }
                        }
                    }
                }));

        ClientPlayNetworking.registerGlobalReceiver(DeliverImageS2CPacket.ID,
                (payload, ctx) -> ctx.client().execute(() -> {
                    ClientImageCache.store(payload.uuid(), payload.bytes());
                    MinecraftClient mc = ctx.client();
                    if (mc.currentScreen instanceof PaintScreen screen) {
                        screen.receiveImageBytes(payload.uuid(), payload.bytes());
                    }
                }));

        ClientPlayNetworking.registerGlobalReceiver(ImageUploadedS2CPacket.ID,
                (payload, ctx) -> ctx.client().execute(() -> {
                    iliiasik.artistry.Artistry.LOGGER.info("ImageUploadedS2CPacket received, uuid: {}", payload.uuid());
                    MinecraftClient mc = ctx.client();
                    if (mc.currentScreen instanceof PaintScreen screen) {
                        if (payload.pos().equals(screen.getTargetPos())) {
                            iliiasik.artistry.Artistry.LOGGER.info("Calling onImageUploaded");
                            screen.onImageUploaded(
                                    payload.uuid(),
                                    payload.gridX(), payload.gridY(),
                                    payload.gridW(), payload.gridH()
                            );
                        } else {
                            iliiasik.artistry.Artistry.LOGGER.info("pos mismatch: {} vs {}", payload.pos(), screen.getTargetPos());
                        }
                    } else {
                        iliiasik.artistry.Artistry.LOGGER.info("currentScreen is not PaintScreen: {}", mc.currentScreen);
                    }
                }));

        UseItemCallback.EVENT.register((player, world, hand) -> {
            if (!world.isClient()) return TypedActionResult.pass(player.getStackInHand(hand));
            var stack = player.getStackInHand(hand);
            if (!(stack.getItem() instanceof PosterItem)) return TypedActionResult.pass(stack);
            MinecraftClient mc = MinecraftClient.getInstance();
            if (!CanvasData.isSizeChosenForStack(stack)) {
                mc.setScreen(new CanvasSizeScreen(stack, hand));
            } else {
                mc.setScreen(new PaintScreen(stack, hand));
            }
            return TypedActionResult.success(stack);
        });

        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            if (!world.isClient()) return ActionResult.PASS;
            var state = world.getBlockState(hitResult.getBlockPos());
            if (state.getBlock() instanceof PosterBlock) {
                var be = world.getBlockEntity(hitResult.getBlockPos());
                if (be instanceof PosterBlockEntity poster) {
                    MinecraftClient mc = MinecraftClient.getInstance();
                    if (!poster.canvasData.isSizeChosen()) {
                        mc.setScreen(new CanvasSizeScreen(poster));
                    } else {
                        mc.setScreen(new PaintScreen(poster));
                    }
                    return ActionResult.SUCCESS;
                }
            }
            return ActionResult.PASS;
        });
    }
}