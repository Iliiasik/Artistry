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
import iliiasik.artistry.network.RequestImageC2SPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.network.PacketDistributor;

@Mod(value = Artistry.MOD_ID, dist = Dist.CLIENT)
public final class ArtistryClient {

    private static ItemStack lastMainHandStack = ItemStack.EMPTY;
    private static ItemStack lastOffHandStack = ItemStack.EMPTY;

    public ArtistryClient(IEventBus modEventBus) {
        modEventBus.addListener(ArtistryClient::onClientSetup);
        modEventBus.addListener(ArtistryClient::onRegisterRenderers);
        modEventBus.addListener(ArtistryClient::onRegisterReloadListeners);
        NeoForge.EVENT_BUS.register(ArtistryClient.class);
    }

    private static void onRegisterReloadListeners(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener((ResourceManagerReloadListener) manager -> {
            BlockPalette.reset();
            CanvasCellPainter.reset();
        });
    }

    private static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() ->
                ItemProperties.register(ModItems.POSTER.get(),
                        ResourceLocation.fromNamespaceAndPath("artistry", "has_canvas"),
                        (stack, level, entity, seed) -> CanvasData.isSizeChosenForStack(stack) ? 1.0F : 0.0F));
    }

    private static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ModBlockEntities.POSTER.get(),
                ctx -> new PosterBlockEntityRenderer());
    }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        ClientImageCache.clear();
        PosterBlockEntityRenderer.clearAllPendingRequests();
        ClientServerSettings.reset();
        lastMainHandStack = ItemStack.EMPTY;
        lastOffHandStack = ItemStack.EMPTY;
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) return;

        ItemStack mainHand = client.player.getMainHandItem();
        if (!ItemStack.isSameItemSameComponents(mainHand, lastMainHandStack)) {
            lastMainHandStack = mainHand.copy();
            requestImagesForStackIfPoster(mainHand);
        }

        ItemStack offHand = client.player.getOffhandItem();
        if (!ItemStack.isSameItemSameComponents(offHand, lastOffHandStack)) {
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
                PacketDistributor.sendToServer(new CanvasEnterRequestC2SPacket(poster.getBlockPos()));
                event.setCanceled(true);
                event.setCancellationResult(InteractionResult.SUCCESS);
            }
        }
    }

    private static void requestImagesForStackIfPoster(ItemStack stack) {
        if (stack.isEmpty() || !(stack.getItem() instanceof PosterItem)) return;
        CustomData comp = stack.get(DataComponents.CUSTOM_DATA);
        if (comp == null) return;
        CompoundTag tag = comp.copyTag();
        if (!tag.contains("images")) return;

        CanvasImageLayer layer = new CanvasImageLayer();
        layer.fromNbt(tag.getList("images", Tag.TAG_COMPOUND));
        for (CanvasImage img : layer.getImages()) {
            if (!ClientImageCache.has(img.uuid)) {
                PacketDistributor.sendToServer(new RequestImageC2SPacket(img.uuid));
            }
        }
    }
}