package iliiasik.artistry.client;

import iliiasik.artistry.Artistry;
import iliiasik.artistry.block.PosterBlocks;
import iliiasik.artistry.block.entity.PosterBlockEntity;
import iliiasik.artistry.client.image.ClientImageCache;
import iliiasik.artistry.client.network.ClientImageAssembler;
import iliiasik.artistry.client.palette.BlockPalette;
import iliiasik.artistry.client.renderer.CanvasCellPainter;
import iliiasik.artistry.client.renderer.PosterBlockEntityRenderer;
import iliiasik.artistry.client.renderer.PosterInHandRenderer;
import iliiasik.artistry.client.renderer.PosterItemCanvasCache;
import iliiasik.artistry.client.ui.screen.CanvasSizeScreen;
import iliiasik.artistry.client.ui.screen.PaintScreen;
import iliiasik.artistry.data.CanvasData;
import iliiasik.artistry.data.CanvasImage;
import iliiasik.artistry.data.CanvasImageLayer;
import iliiasik.artistry.data.CanvasSignature;
import iliiasik.artistry.item.ModItems;
import iliiasik.artistry.item.PosterItem;
import iliiasik.artistry.mixin.ItemPropertiesAccessor;
import iliiasik.artistry.network.ArtistryNetwork;
import iliiasik.artistry.network.CanvasEnterRequestC2SPacket;
import iliiasik.artistry.network.RequestImageC2SPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public final class ArtistryClientEvents {

    private static ItemStack lastMainHandStack = ItemStack.EMPTY;
    private static ItemStack lastOffHandStack = ItemStack.EMPTY;

    private ArtistryClientEvents() {}

    public static void registerItemProperties() {
        ItemPropertiesAccessor.artistry$register(ModItems.POSTER.get(), Artistry.id("has_canvas"),
                (stack, level, entity, seed) -> CanvasData.isSizeChosenForStack(stack) ? 1.0F : 0.0F);
        ItemPropertiesAccessor.artistry$register(ModItems.POSTER.get(), Artistry.id("signed"),
                (stack, level, entity, seed) -> CanvasSignature.isSignedStack(stack) ? 1.0F : 0.0F);
        ItemPropertiesAccessor.artistry$register(ModItems.BANNER.get(), Artistry.id("has_canvas"),
                (stack, level, entity, seed) -> CanvasData.isSizeChosenForStack(stack) ? 1.0F : 0.0F);
        ItemPropertiesAccessor.artistry$register(ModItems.BANNER.get(), Artistry.id("signed"),
                (stack, level, entity, seed) -> CanvasSignature.isSignedStack(stack) ? 1.0F : 0.0F);
    }

    public static void onResourceReload() {
        BlockPalette.reset();
        CanvasCellPainter.reset();
    }

    public static void onDisconnect() {
        ClientImageCache.clear();
        ClientImageAssembler.clear();
        PosterBlockEntityRenderer.clearAll();
        PosterBlockEntityRenderer.clearAllPendingRequests();
        PosterItemCanvasCache.clear();
        PosterInHandRenderer.clear();
        ClientServerSettings.reset();
        lastMainHandStack = ItemStack.EMPTY;
        lastOffHandStack = ItemStack.EMPTY;
    }

    public static void onClientStopping() {
        ClientImageCache.clear();
    }

    public static void onClientTick(Minecraft client) {
        ClientImageCache.sweep();
        PosterBlockEntityRenderer.tick();
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

    public static boolean openPosterScreen(ItemStack stack, InteractionHand hand) {
        if (!(stack.getItem() instanceof PosterItem)) return false;
        Minecraft mc = Minecraft.getInstance();
        if (!CanvasData.isSizeChosenForStack(stack)) {
            mc.setScreen(new CanvasSizeScreen(stack, hand));
        } else {
            mc.setScreen(new PaintScreen(stack, hand));
        }
        return true;
    }

    public static boolean requestPosterAccess(Level level, BlockPos pos) {
        BlockPos origin = PosterBlocks.originOf(level, pos);
        if (origin == null) return false;
        if (!(level.getBlockEntity(origin) instanceof PosterBlockEntity poster)) return false;
        ArtistryNetwork.sendToServer(new CanvasEnterRequestC2SPacket(poster.getBlockPos()));
        return true;
    }

    private static void requestImagesForStackIfPoster(ItemStack stack) {
        if (stack.isEmpty() || !(stack.getItem() instanceof PosterItem)) return;
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains("images")) return;

        CanvasImageLayer layer = new CanvasImageLayer();
        layer.fromNbt(tag.getList("images", Tag.TAG_COMPOUND));
        for (CanvasImage img : layer.getImages()) {
            if (!ClientImageCache.has(img.uuid)) {
                ArtistryNetwork.sendToServer(new RequestImageC2SPacket(img.uuid));
            }
        }
    }
}
