package iliiasik.artistry.client.ui.screen.config;

import iliiasik.artistry.client.util.ModTextures;
import iliiasik.artistry.config.ArtistryConfig;
import iliiasik.artistry.network.ServerSettingsSync;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.OptionsSubScreen;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.network.chat.Component;

public class ArtistryConfigScreen extends OptionsSubScreen {

    private static final String TITLE_KEY = "screen.artistry.config";
    private static final String NOTE_KEY = "screen.artistry.config.server_controlled";
    private static final String GENERIC_VALUE_KEY = "options.generic_value";
    private static final String BLOCKS_KEY = "option.artistry.blocks";
    private static final String MILLIS_KEY = "option.artistry.milliseconds";
    private static final String KIB_PER_SECOND_KEY = "option.artistry.kib_per_second";
    private static final String UNLIMITED_KEY = "option.artistry.unlimited";

    private static final String VIEW_DISTANCE_KEY = "option.artistry.poster_view_distance";
    private static final String MAX_EDITORS_KEY = "option.artistry.max_editors";
    private static final String DISABLE_IMAGES_KEY = "option.artistry.disable_images";
    private static final String BATCH_INTERVAL_KEY = "option.artistry.batch_interval";
    private static final String CURSOR_INTERVAL_KEY = "option.artistry.cursor_interval";
    private static final String WORLD_SYNC_INTERVAL_KEY = "option.artistry.world_sync_interval";
    private static final String IMAGE_BANDWIDTH_KEY = "option.artistry.image_bandwidth";
    private static final String TOOLTIP_SUFFIX = ".tooltip";

    private static final int HEADER_HEIGHT = 33;
    private static final int ICON_SIZE = 16;
    private static final int ICON_GAP = 4;
    private static final int NOTE_BOTTOM = 48;
    private static final int NOTE_COLOR = 0xA0A0A0;
    private static final int KIB = 1024;

    public ArtistryConfigScreen(Screen parent) {
        super(parent, Minecraft.getInstance().options,
                Component.translatable(TITLE_KEY).withStyle(ChatFormatting.BOLD));
    }

    private static boolean serverOptionsEditable() {
        Minecraft mc = Minecraft.getInstance();
        return mc.level == null || mc.hasSingleplayerServer();
    }

    @Override
    protected void addOptions() {
        if (list == null) return;
        ArtistryConfig config = ArtistryConfig.get();

        list.addBig(viewDistance(config));
        if (serverOptionsEditable()) {
            list.addSmall(maxEditors(config), disableImages(config));
            list.addSmall(batchInterval(config), cursorInterval(config));
            list.addSmall(worldSyncInterval(config), imageBandwidth(config));
        }
    }

    private static <T> OptionInstance.TooltipSupplier<T> tooltip(String key) {
        return OptionInstance.cachedConstantTooltip(Component.translatable(key + TOOLTIP_SUFFIX));
    }

    private static Component valueLabel(Component caption, Component value) {
        return Component.translatable(GENERIC_VALUE_KEY, caption, value);
    }

    private static OptionInstance<Integer> viewDistance(ArtistryConfig config) {
        return new OptionInstance<>(
                VIEW_DISTANCE_KEY,
                tooltip(VIEW_DISTANCE_KEY),
                (caption, value) -> valueLabel(caption, Component.translatable(BLOCKS_KEY, value)),
                new OptionInstance.IntRange(
                        ArtistryConfig.ClientConfig.MIN_VIEW_DISTANCE,
                        ArtistryConfig.ClientConfig.MAX_VIEW_DISTANCE),
                config.client.posterViewDistance,
                value -> config.client.posterViewDistance = value);
    }

    private static OptionInstance<Integer> maxEditors(ArtistryConfig config) {
        return new OptionInstance<>(
                MAX_EDITORS_KEY,
                tooltip(MAX_EDITORS_KEY),
                (caption, value) -> valueLabel(caption, value == ArtistryConfig.PosterConfig.UNLIMITED_EDITORS
                        ? Component.translatable(UNLIMITED_KEY)
                        : Component.literal(String.valueOf(value))),
                new OptionInstance.IntRange(
                        ArtistryConfig.PosterConfig.MIN_EDITORS,
                        ArtistryConfig.PosterConfig.MAX_EDITORS),
                config.poster.maxEditors,
                value -> config.poster.maxEditors = value);
    }

    private static OptionInstance<Boolean> disableImages(ArtistryConfig config) {
        return OptionInstance.createBoolean(
                DISABLE_IMAGES_KEY,
                tooltip(DISABLE_IMAGES_KEY),
                config.poster.disableImages,
                value -> config.poster.disableImages = value);
    }

    private static OptionInstance<Integer> batchInterval(ArtistryConfig config) {
        return millisOption(
                BATCH_INTERVAL_KEY,
                ArtistryConfig.NetworkConfig.MIN_BATCH_INTERVAL_MS,
                ArtistryConfig.NetworkConfig.MAX_BATCH_INTERVAL_MS,
                config.network.batchIntervalMs,
                value -> config.network.batchIntervalMs = value);
    }

    private static OptionInstance<Integer> cursorInterval(ArtistryConfig config) {
        return millisOption(
                CURSOR_INTERVAL_KEY,
                ArtistryConfig.NetworkConfig.MIN_CURSOR_INTERVAL_MS,
                ArtistryConfig.NetworkConfig.MAX_CURSOR_INTERVAL_MS,
                config.network.cursorIntervalMs,
                value -> config.network.cursorIntervalMs = value);
    }

    private static OptionInstance<Integer> worldSyncInterval(ArtistryConfig config) {
        return millisOption(
                WORLD_SYNC_INTERVAL_KEY,
                ArtistryConfig.NetworkConfig.MIN_WORLD_SYNC_INTERVAL_MS,
                ArtistryConfig.NetworkConfig.MAX_WORLD_SYNC_INTERVAL_MS,
                config.network.worldSyncIntervalMs,
                value -> config.network.worldSyncIntervalMs = value);
    }

    private static OptionInstance<Integer> millisOption(String key, long min, long max, long current,
                                                        LongSetter setter) {
        return new OptionInstance<>(
                key,
                tooltip(key),
                (caption, value) -> valueLabel(caption, Component.translatable(MILLIS_KEY, value)),
                new OptionInstance.IntRange((int) min, (int) max),
                (int) current,
                value -> setter.set(value.longValue()));
    }

    private static OptionInstance<Integer> imageBandwidth(ArtistryConfig config) {
        return new OptionInstance<>(
                IMAGE_BANDWIDTH_KEY,
                tooltip(IMAGE_BANDWIDTH_KEY),
                (caption, value) -> valueLabel(caption, Component.translatable(KIB_PER_SECOND_KEY, value)),
                new OptionInstance.IntRange(
                        (int) (ArtistryConfig.NetworkConfig.MIN_IMAGE_BYTES_PER_SECOND / KIB),
                        (int) (ArtistryConfig.NetworkConfig.MAX_IMAGE_BYTES_PER_SECOND / KIB)),
                (int) (config.network.imageBytesPerSecond / KIB),
                value -> config.network.imageBytesPerSecond = value.longValue() * KIB);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        renderIcon(graphics);
        if (!serverOptionsEditable()) {
            graphics.drawCenteredString(font, Component.translatable(NOTE_KEY),
                    width / 2, height - NOTE_BOTTOM, NOTE_COLOR);
        }
    }

    private void renderIcon(GuiGraphics graphics) {
        int iconX = width / 2 - font.width(title) / 2 - ICON_GAP - ICON_SIZE;
        int iconY = (HEADER_HEIGHT - ICON_SIZE) / 2;
        graphics.blit(ModTextures.ICON,
                iconX, iconY,
                ICON_SIZE, ICON_SIZE,
                0f, 0f,
                ModTextures.ICON_TEXTURE_SIZE, ModTextures.ICON_TEXTURE_SIZE,
                ModTextures.ICON_TEXTURE_SIZE, ModTextures.ICON_TEXTURE_SIZE);
    }

    @Override
    public void removed() {
        super.removed();
        ArtistryConfig.flush();
        IntegratedServer server = Minecraft.getInstance().getSingleplayerServer();
        if (server != null) server.execute(() -> ServerSettingsSync.broadcast(server));
    }

    @FunctionalInterface
    private interface LongSetter {
        void set(long value);
    }
}
