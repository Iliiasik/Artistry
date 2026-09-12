package iliiasik.artistry.debug.impl;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import iliiasik.artistry.data.CanvasData;
import iliiasik.artistry.data.CanvasImageLayer;
import iliiasik.artistry.debug.ArtistryDebug;
import iliiasik.artistry.debug.DebugHooks;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("unused")
public final class DebugBootstrap implements DebugHooks {

    private static final int DEFAULT_CANVAS_SIZE = 32;
    private static final int DEFAULT_IMAGES = 2;
    private static final int CLEAR_RADIUS = 64;

    static {
        ArtistryDebug.install(new DebugBootstrap());
    }

    private DebugBootstrap() {}

    @Override
    public boolean isRecording() {
        return RenderProfiler.isEnabled();
    }

    @Override
    public void poster(long nanos) {
        RenderProfiler.recordPoster(nanos);
    }

    @Override
    public void textureCreated(int texSize) {
        RenderProfiler.textureCreated(texSize);
    }

    @Override
    public void textureClosed(int texSize) {
        RenderProfiler.textureClosed(texSize);
    }

    @Override
    public void textureUploaded(int texSize) {
        RenderProfiler.textureUploaded(texSize);
    }

    @Override
    public void handleInput() {
        ProfilerKey.handleInput();
    }

    @Override
    public void renderOverlay(GuiGraphics graphics) {
        RenderProfiler.renderOverlay(graphics);
    }

    @Override
    public @NotNull KeyMapping keyMapping() {
        return ProfilerKey.TOGGLE;
    }

    @Override
    public void extendCommands(LiteralArgumentBuilder<CommandSourceStack> root) {
        root.then(Commands.literal("benchmark")
                .then(Commands.literal("clear")
                        .executes(DebugBootstrap::clearBenchmark))
                .then(countArgument(BenchmarkKind.POSTERS))
                .then(Commands.literal("posters").then(countArgument(BenchmarkKind.POSTERS)))
                .then(Commands.literal("banners").then(countArgument(BenchmarkKind.BANNERS)))
                .then(Commands.literal("mixed").then(countArgument(BenchmarkKind.MIXED))));
    }

    private static RequiredArgumentBuilder<CommandSourceStack, Integer> countArgument(BenchmarkKind kind) {
        return Commands.argument("count", IntegerArgumentType.integer(1, 4096))
                .executes(context -> placeBenchmark(context, DEFAULT_CANVAS_SIZE, DEFAULT_IMAGES, kind))
                .then(Commands.argument("canvasSize", IntegerArgumentType.integer(1, CanvasData.MAX_SIZE))
                        .executes(context -> placeBenchmark(context,
                                IntegerArgumentType.getInteger(context, "canvasSize"), DEFAULT_IMAGES, kind))
                        .then(Commands.argument("images", IntegerArgumentType.integer(0, CanvasImageLayer.MAX_IMAGES))
                                .executes(context -> placeBenchmark(context,
                                        IntegerArgumentType.getInteger(context, "canvasSize"),
                                        IntegerArgumentType.getInteger(context, "images"), kind))));
    }

    private static int placeBenchmark(CommandContext<CommandSourceStack> context,
                                      int canvasSize, int images, BenchmarkKind kind)
            throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        int count = IntegerArgumentType.getInteger(context, "count");

        long start = System.nanoTime();
        int placed = PosterBenchmark.place(context.getSource().getLevel(), player, count, canvasSize, images, kind);
        long millis = (System.nanoTime() - start) / 1_000_000;

        context.getSource().sendSuccess(() -> Component.literal(
                "[Artistry] Placed " + placed + " " + kind.name().toLowerCase(java.util.Locale.ROOT)
                        + ", " + canvasSize + "x" + canvasSize
                        + " canvas, " + images + " images each, in " + millis + " ms"), true);
        return placed;
    }

    private static int clearBenchmark(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        int removed = PosterBenchmark.clear(context.getSource().getLevel(), player.blockPosition(), CLEAR_RADIUS);

        context.getSource().sendSuccess(
                () -> Component.literal("[Artistry] Removed " + removed + " posters"), true);
        return removed;
    }
}
