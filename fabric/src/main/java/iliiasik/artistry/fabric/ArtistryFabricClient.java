package iliiasik.artistry.fabric;

import iliiasik.artistry.Artistry;
import iliiasik.artistry.block.entity.ModBlockEntities;
import iliiasik.artistry.client.ArtistryClientEvents;
import iliiasik.artistry.client.renderer.PosterBlockEntityRenderer;
import iliiasik.artistry.debug.ArtistryDebug;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.ResourceReloadListenerKeys;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;

public final class ArtistryFabricClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        ArtistryClientEvents.registerItemProperties();

        BlockEntityRenderers.register(ModBlockEntities.POSTER.get(), context -> new PosterBlockEntityRenderer());

        FabricModNetworkClient.register();

        ResourceManagerHelper.get(PackType.CLIENT_RESOURCES).registerReloadListener(
                new SimpleSynchronousResourceReloadListener() {
                    @Override
                    public ResourceLocation getFabricId() {
                        return Artistry.id("canvas_palette");
                    }

                    @Override
                    public Collection<ResourceLocation> getFabricDependencies() {
                        return List.of(ResourceReloadListenerKeys.TEXTURES, ResourceReloadListenerKeys.MODELS);
                    }

                    @Override
                    public void onResourceManagerReload(@NotNull ResourceManager manager) {
                        ArtistryClientEvents.onResourceReload();
                    }
                });

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> ArtistryClientEvents.onDisconnect());

        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> ArtistryClientEvents.onClientStopping());

        KeyMapping profilerKey = ArtistryDebug.hooks().keyMapping();
        if (profilerKey != null) KeyBindingHelper.registerKeyBinding(profilerKey);

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            ArtistryDebug.hooks().handleInput();
            ArtistryClientEvents.onClientTick(client);
        });

        HudRenderCallback.EVENT.register((graphics, tickDelta) -> ArtistryDebug.hooks().renderOverlay(graphics));

        UseItemCallback.EVENT.register((player, level, hand) -> {
            ItemStack stack = player.getItemInHand(hand);
            if (level.isClientSide() && ArtistryClientEvents.openPosterScreen(stack, hand)) {
                return InteractionResultHolder.success(stack);
            }
            return InteractionResultHolder.pass(stack);
        });

        UseBlockCallback.EVENT.register((player, level, hand, hitResult) -> {
            if (level.isClientSide() && ArtistryClientEvents.requestPosterAccess(level, hitResult.getBlockPos())) {
                return InteractionResult.SUCCESS;
            }
            return InteractionResult.PASS;
        });
    }
}
