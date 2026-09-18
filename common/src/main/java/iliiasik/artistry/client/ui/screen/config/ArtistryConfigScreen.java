package iliiasik.artistry.client.ui.screen.config;

import iliiasik.artistry.config.ArtistryConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.OptionsList;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

public class ArtistryConfigScreen extends Screen {

    private static final String TITLE_KEY = "screen.artistry.config";
    private static final String NOTE_KEY = "screen.artistry.config.server_controlled";
    private static final String GENERIC_VALUE_KEY = "options.generic_value";
    private static final String BLOCKS_KEY = "option.artistry.blocks";
    private static final String MILLIS_KEY = "option.artistry.milliseconds";
    private static final String KIB_PER_SECOND_KEY = "option.artistry.kib_per_second";
    private static final String UNLIMITED_KEY = "option.artistry.unlimited";

    private static final int LIST_TOP = 32;
    private static final int LIST_BOTTOM = 32;
    private static final int LIST_BOTTOM_WITH_NOTE = 46;
    private static final int ITEM_HEIGHT = 25;
    private static final int TITLE_Y = 20;
    private static final int NOTE_BOTTOM = 42;
    private static final int DONE_WIDTH = 200;
    private static final int DONE_HEIGHT = 20;
    private static final int DONE_BOTTOM = 27;
    private static final int TITLE_COLOR = 0xFFFFFF;
    private static final int NOTE_COLOR = 0xA0A0A0;
    private static final int KIB = 1024;

    @Nullable
    private final Screen parent;
    private OptionsList list;

    public ArtistryConfigScreen(@Nullable Screen parent) {
        super(Component.translatable(TITLE_KEY));
        this.parent = parent;
    }

    private static boolean serverOptionsEditable() {
        Minecraft mc = Minecraft.getInstance();
        return mc.level == null || mc.hasSingleplayerServer();
    }

    @Override
    protected void init() {
        ArtistryConfig config = ArtistryConfig.get();
        boolean editable = serverOptionsEditable();
        int bottom = height - (editable ? LIST_BOTTOM : LIST_BOTTOM_WITH_NOTE);

        list = new OptionsList(Minecraft.getInstance(), width, height, LIST_TOP, bottom, ITEM_HEIGHT);
        list.addBig(viewDistance(config));
        if (editable) {
            list.addSmall(maxEditors(config), disableImages(config));
            list.addSmall(batchInterval(config), cursorInterval(config));
            list.addSmall(worldSyncInterval(config), imageBandwidth(config));
        }
        addWidget(list);

        addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> onClose())
                .bounds((width - DONE_WIDTH) / 2, height - DONE_BOTTOM, DONE_WIDTH, DONE_HEIGHT)
                .build());
    }

    private static Component valueLabel(Component caption, Component value) {
        return Component.translatable(GENERIC_VALUE_KEY, caption, value);
    }

    private static OptionInstance<Integer> viewDistance(ArtistryConfig config) {
        return new OptionInstance<>(
                "option.artistry.poster_view_distance",
                OptionInstance.noTooltip(),
                (caption, value) -> valueLabel(caption, Component.translatable(BLOCKS_KEY, value)),
                new OptionInstance.IntRange(
                        ArtistryConfig.ClientConfig.MIN_VIEW_DISTANCE,
                        ArtistryConfig.ClientConfig.MAX_VIEW_DISTANCE),
                config.client.posterViewDistance,
                value -> config.client.posterViewDistance = value);
    }

    private static OptionInstance<Integer> maxEditors(ArtistryConfig config) {
        return new OptionInstance<>(
                "option.artistry.max_editors",
                OptionInstance.noTooltip(),
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
                "option.artistry.disable_images",
                config.poster.disableImages,
                value -> config.poster.disableImages = value);
    }

    private static OptionInstance<Integer> batchInterval(ArtistryConfig config) {
        return millisOption(
                "option.artistry.batch_interval",
                ArtistryConfig.NetworkConfig.MIN_BATCH_INTERVAL_MS,
                ArtistryConfig.NetworkConfig.MAX_BATCH_INTERVAL_MS,
                config.network.batchIntervalMs,
                value -> config.network.batchIntervalMs = value);
    }

    private static OptionInstance<Integer> cursorInterval(ArtistryConfig config) {
        return millisOption(
                "option.artistry.cursor_interval",
                ArtistryConfig.NetworkConfig.MIN_CURSOR_INTERVAL_MS,
                ArtistryConfig.NetworkConfig.MAX_CURSOR_INTERVAL_MS,
                config.network.cursorIntervalMs,
                value -> config.network.cursorIntervalMs = value);
    }

    private static OptionInstance<Integer> worldSyncInterval(ArtistryConfig config) {
        return millisOption(
                "option.artistry.world_sync_interval",
                ArtistryConfig.NetworkConfig.MIN_WORLD_SYNC_INTERVAL_MS,
                ArtistryConfig.NetworkConfig.MAX_WORLD_SYNC_INTERVAL_MS,
                config.network.worldSyncIntervalMs,
                value -> config.network.worldSyncIntervalMs = value);
    }

    private static OptionInstance<Integer> millisOption(String key, long min, long max, long current,
                                                        LongSetter setter) {
        return new OptionInstance<>(
                key,
                OptionInstance.noTooltip(),
                (caption, value) -> valueLabel(caption, Component.translatable(MILLIS_KEY, value)),
                new OptionInstance.IntRange((int) min, (int) max),
                (int) current,
                value -> setter.set(value.longValue()));
    }

    private static OptionInstance<Integer> imageBandwidth(ArtistryConfig config) {
        return new OptionInstance<>(
                "option.artistry.image_bandwidth",
                OptionInstance.noTooltip(),
                (caption, value) -> valueLabel(caption, Component.translatable(KIB_PER_SECOND_KEY, value)),
                new OptionInstance.IntRange(
                        (int) (ArtistryConfig.NetworkConfig.MIN_IMAGE_BYTES_PER_SECOND / KIB),
                        (int) (ArtistryConfig.NetworkConfig.MAX_IMAGE_BYTES_PER_SECOND / KIB)),
                (int) (config.network.imageBytesPerSecond / KIB),
                value -> config.network.imageBytesPerSecond = value.longValue() * KIB);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        list.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(font, title, width / 2, TITLE_Y, TITLE_COLOR);
        super.render(graphics, mouseX, mouseY, partialTick);
        if (!serverOptionsEditable()) {
            graphics.drawCenteredString(font, Component.translatable(NOTE_KEY),
                    width / 2, height - NOTE_BOTTOM, NOTE_COLOR);
        }
    }

    @Override
    public void onClose() {
        ArtistryConfig.flush();
        Minecraft.getInstance().setScreen(parent);
    }

    @FunctionalInterface
    private interface LongSetter {
        void set(long value);
    }
}
