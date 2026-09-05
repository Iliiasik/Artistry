package iliiasik.artistry.neoforge;

import iliiasik.artistry.Artistry;
import iliiasik.artistry.block.entity.ModBlockEntities;
import iliiasik.artistry.client.ArtistryClientEvents;
import iliiasik.artistry.debug.ArtistryDebug;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.world.InteractionResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

@Mod(value = Artistry.MOD_ID, dist = Dist.CLIENT)
public final class ArtistryNeoForgeClient {

    public ArtistryNeoForgeClient(IEventBus modEventBus) {
        modEventBus.addListener(ArtistryNeoForgeClient::onClientSetup);
        modEventBus.addListener(ArtistryNeoForgeClient::onRegisterRenderers);
        modEventBus.addListener(ArtistryNeoForgeClient::onRegisterReloadListeners);
        modEventBus.addListener(ArtistryNeoForgeClient::onRegisterKeyMappings);
        NeoForge.EVENT_BUS.register(ArtistryNeoForgeClient.class);
    }

    private static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(ArtistryClientEvents::registerItemProperties);
    }

    private static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ModBlockEntities.POSTER.get(), context -> new NeoForgePosterRenderer());
    }

    private static void onRegisterReloadListeners(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener((ResourceManagerReloadListener) manager -> ArtistryClientEvents.onResourceReload());
    }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        ArtistryClientEvents.onDisconnect();
    }

    private static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        KeyMapping profilerKey = ArtistryDebug.hooks().keyMapping();
        if (profilerKey != null) event.register(profilerKey);
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        ArtistryDebug.hooks().handleInput();
        ArtistryClientEvents.onClientTick(Minecraft.getInstance());
    }

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        ArtistryDebug.hooks().renderOverlay(event.getGuiGraphics());
    }

    @SubscribeEvent
    public static void onUseItem(PlayerInteractEvent.RightClickItem event) {
        if (event.getLevel().isClientSide()
                && ArtistryClientEvents.openPosterScreen(event.getItemStack(), event.getHand())) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.SUCCESS);
        }
    }

    @SubscribeEvent
    public static void onUseBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getLevel().isClientSide()
                && ArtistryClientEvents.requestPosterAccess(event.getLevel(), event.getPos())) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.SUCCESS);
        }
    }
}
