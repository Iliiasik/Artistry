package iliiasik.artistry.client;

import iliiasik.artistry.Artistry;
import iliiasik.artistry.block.PosterBlock;
import iliiasik.artistry.block.entity.ModBlockEntities;
import iliiasik.artistry.block.entity.PosterBlockEntity;
import iliiasik.artistry.client.image.ClientImageCache;
import iliiasik.artistry.client.palette.BlockPalette;
import iliiasik.artistry.client.renderer.CanvasCellPainter;
import iliiasik.artistry.client.renderer.PosterBlockEntityRenderer;
import iliiasik.artistry.client.ui.screen.CanvasSizeScreen;
import iliiasik.artistry.client.ui.screen.PaintScreen;
import iliiasik.artistry.data.CanvasData;
import iliiasik.artistry.data.CanvasImage;
import iliiasik.artistry.data.CanvasImageLayer;
import iliiasik.artistry.item.ModItems;
import iliiasik.artistry.item.PosterItem;
import iliiasik.artistry.network.CanvasEnterRequestC2SPacket;
import iliiasik.artistry.network.ModNetwork;
import iliiasik.artistry.network.RequestImageC2SPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

public final class ArtistryClient {

    private ArtistryClient() {}

    @Mod.EventBusSubscriber(modid = Artistry.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static final class ModBus {

        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            event.enqueueWork(() ->
                    ItemProperties.register(ModItems.POSTER.get(),
                            new ResourceLocation("artistry", "has_canvas"),
                            (stack, level, entity, seed) -> CanvasData.isSizeChosenForStack(stack) ? 1.0F : 0.0F));
        }

        @SubscribeEvent
        public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
            event.registerBlockEntityRenderer(ModBlockEntities.POSTER.get(),
                    ctx -> new PosterBlockEntityRenderer());
        }

        @SubscribeEvent
        public static void onRegisterReloadListeners(RegisterClientReloadListenersEvent event) {
            event.registerReloadListener((ResourceManagerReloadListener) manager -> {
                BlockPalette.reset();
                CanvasCellPainter.reset();
            });
        }
    }

    @Mod.EventBusSubscriber(modid = Artistry.MOD_ID, value = Dist.CLIENT)
    public static final class ForgeBus {

        private static ItemStack lastMainHandStack = ItemStack.EMPTY;
        private static ItemStack lastOffHandStack = ItemStack.EMPTY;

        @SubscribeEvent
        public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
            ClientImageCache.clear();
            PosterBlockEntityRenderer.clearAllPendingRequests();
            ClientServerSettings.reset();
            lastMainHandStack = ItemStack.EMPTY;
            lastOffHandStack = ItemStack.EMPTY;
        }

        @SubscribeEvent
        public static void onClientTick(TickEvent.ClientTickEvent event) {
            if (event.phase != TickEvent.Phase.END) return;
            Minecraft client = Minecraft.getInstance();
            if (client.player == null) return;

            ItemStack mainHand = client.player.getMainHandItem();
            if (!ItemStack.isSameItemSameTags(mainHand, lastMainHandStack)) {
                lastMainHandStack = mainHand.copy();
                requestImagesForStackIfPoster(mainHand);
            }

            ItemStack offHand = client.player.getOffhandItem();
            if (!ItemStack.isSameItemSameTags(offHand, lastOffHandStack)) {
                lastOffHandStack = offHand.copy();
                requestImagesForStackIfPoster(offHand);
            }
        }

        @SubscribeEvent
        public static void onUseItem(PlayerInteractEvent.RightClickItem event) {
            if (!event.getLevel().isClientSide()) return;
            ItemStack stack = event.getItemStack();
            if (!(stack.getItem() instanceof PosterItem)) return;
            Minecraft mc = Minecraft.getInstance();
            if (!CanvasData.isSizeChosenForStack(stack)) {
                mc.setScreen(new CanvasSizeScreen(stack, event.getHand()));
            } else {
                mc.setScreen(new PaintScreen(stack, event.getHand()));
            }
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.SUCCESS);
        }

        @SubscribeEvent
        public static void onUseBlock(PlayerInteractEvent.RightClickBlock event) {
            if (!event.getLevel().isClientSide()) return;
            BlockState state = event.getLevel().getBlockState(event.getPos());
            if (state.getBlock() instanceof PosterBlock) {
                if (event.getLevel().getBlockEntity(event.getPos()) instanceof PosterBlockEntity poster) {
                    ModNetwork.sendToServer(new CanvasEnterRequestC2SPacket(poster.getBlockPos()));
                    event.setCanceled(true);
                    event.setCancellationResult(InteractionResult.SUCCESS);
                }
            }
        }

        private static void requestImagesForStackIfPoster(ItemStack stack) {
            if (stack.isEmpty() || !(stack.getItem() instanceof PosterItem)) return;
            CompoundTag tag = stack.getTag();
            if (tag == null || !tag.contains("images")) return;

            CanvasImageLayer layer = new CanvasImageLayer();
            layer.fromNbt(tag.getList("images", Tag.TAG_COMPOUND));
            for (CanvasImage img : layer.getImages()) {
                if (!ClientImageCache.has(img.uuid)) {
                    ModNetwork.sendToServer(new RequestImageC2SPacket(img.uuid));
                }
            }
        }
    }
}