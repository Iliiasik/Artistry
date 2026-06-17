package iliiasik.artistry.client;

import iliiasik.artistry.block.PosterBlock;
import iliiasik.artistry.block.entity.ModBlockEntities;
import iliiasik.artistry.block.entity.PosterBlockEntity;
import iliiasik.artistry.client.image.ClientImageCache;
import iliiasik.artistry.client.renderer.PosterBlockEntityRenderer;
import iliiasik.artistry.client.ui.screen.CanvasSizeScreen;
import iliiasik.artistry.client.ui.screen.PaintScreen;
import iliiasik.artistry.data.CanvasData;
import iliiasik.artistry.data.CanvasImage;
import iliiasik.artistry.data.CanvasImageLayer;
import iliiasik.artistry.item.ModItems;
import iliiasik.artistry.item.PosterItem;
import iliiasik.artistry.network.*;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.item.ModelPredicateProviderRegistry;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactories;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.BlockPos;

import java.util.UUID;

public class ArtistryClient implements ClientModInitializer {

    private ItemStack lastMainHandStack = ItemStack.EMPTY;
    private ItemStack lastOffHandStack = ItemStack.EMPTY;

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
                        PosterBlockEntityRenderer.clearPendingRequests(
                                payload.images().stream().map(i -> i.uuid).toList()
                        );
                    }
                    if (mc.currentScreen instanceof PaintScreen screen) {
                        if (pos.equals(screen.getTargetPos())) {
                            screen.applyImageLayerSync(payload.images());
                        }
                    }
                }));

        ClientPlayNetworking.registerGlobalReceiver(DeliverImageS2CPacket.ID,
                (payload, ctx) -> ctx.client().execute(() -> {
                    ClientImageCache.store(payload.uuid(), payload.bytes());
                    PosterBlockEntityRenderer.onImageReceived(payload.uuid());
                    MinecraftClient mc = ctx.client();
                    if (mc.currentScreen instanceof PaintScreen screen) {
                        screen.receiveImageBytes(payload.uuid(), payload.bytes());
                    }
                }));

        ClientPlayNetworking.registerGlobalReceiver(ImageUploadedS2CPacket.ID,
                (payload, ctx) -> ctx.client().execute(() -> {
                    MinecraftClient mc = ctx.client();
                    if (mc.currentScreen instanceof PaintScreen screen) {
                        if (payload.pos() == null || payload.pos().equals(screen.getTargetPos())) {
                            screen.onImageUploaded(
                                    payload.uuid(),
                                    payload.gridX(), payload.gridY(),
                                    payload.gridW(), payload.gridH()
                            );
                        }
                    }
                }));

        ClientPlayNetworking.registerGlobalReceiver(ImageEvictedS2CPacket.ID,
                (payload, ctx) -> ctx.client().execute(() -> {
                    for (UUID uuid : payload.uuids()) {
                        ClientImageCache.evict(uuid);
                    }
                }));

        ClientPlayNetworking.registerGlobalReceiver(SyncImageLockS2CPacket.ID,
                (payload, ctx) -> ctx.client().execute(() -> {
                    MinecraftClient mc = ctx.client();
                    if (mc.world == null) return;
                    BlockPos pos = payload.pos();
                    if (mc.world.getBlockEntity(pos) instanceof PosterBlockEntity poster) {
                        CanvasImage img = poster.imageLayer.findByUuid(payload.imageUuid());
                        if (img != null) img.lockedByPlayer = payload.playerUuid();
                    }
                    if (mc.currentScreen instanceof PaintScreen screen) {
                        if (pos.equals(screen.getTargetPos())) {
                            screen.applyImageLockSync(payload.imageUuid(), payload.playerUuid());
                        }
                    }
                }));

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            ClientImageCache.clear();
            PosterBlockEntityRenderer.clearAllPendingRequests();
            lastMainHandStack = ItemStack.EMPTY;
            lastOffHandStack = ItemStack.EMPTY;
        });

        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> {
            ClientImageCache.clear();
        });

        ClientTickEvents.END_CLIENT_TICK.register(this::onClientTick);

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

    private void onClientTick(MinecraftClient client) {
        if (client.player == null) return;

        ItemStack mainHand = client.player.getMainHandStack();
        if (!ItemStack.areItemsAndComponentsEqual(mainHand, lastMainHandStack)) {
            lastMainHandStack = mainHand.copy();
            requestImagesForStackIfPoster(mainHand);
        }

        ItemStack offHand = client.player.getOffHandStack();
        if (!ItemStack.areItemsAndComponentsEqual(offHand, lastOffHandStack)) {
            lastOffHandStack = offHand.copy();
            requestImagesForStackIfPoster(offHand);
        }
    }

    private void requestImagesForStackIfPoster(ItemStack stack) {
        if (stack.isEmpty() || !(stack.getItem() instanceof PosterItem)) return;
        NbtComponent comp = stack.get(DataComponentTypes.CUSTOM_DATA);
        if (comp == null) return;
        NbtCompound tag = comp.copyNbt();
        if (!tag.contains("images")) return;

        CanvasImageLayer layer = new CanvasImageLayer();
        layer.fromNbt(tag.getList("images", NbtList.COMPOUND_TYPE));
        for (CanvasImage img : layer.getImages()) {
            if (!ClientImageCache.has(img.uuid)) {
                ClientPlayNetworking.send(new RequestImageC2SPacket(img.uuid));
            }
        }
    }
}