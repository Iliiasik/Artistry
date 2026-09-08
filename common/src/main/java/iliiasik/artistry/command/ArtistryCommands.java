package iliiasik.artistry.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import iliiasik.artistry.config.ArtistryConfig;
import iliiasik.artistry.debug.ArtistryDebug;
import iliiasik.artistry.network.ArtistryNetwork;
import iliiasik.artistry.network.ServerSettingsS2CPacket;
import iliiasik.artistry.server.ImageStorage;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

public final class ArtistryCommands {

    private static final ChatFormatting BRAND = ChatFormatting.GOLD;
    private static final ChatFormatting LABEL = ChatFormatting.GRAY;
    private static final ChatFormatting VALUE = ChatFormatting.WHITE;

    private ArtistryCommands() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal("artistry")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("reload")
                        .executes(context -> reloadConfig(context.getSource())))
                .then(Commands.literal("images")
                        .executes(context -> reportImageUsage(context.getSource())));

        ArtistryDebug.hooks().extendCommands(root);
        dispatcher.register(root);
    }

    private static int reloadConfig(CommandSourceStack source) {
        ArtistryConfig cfg = ArtistryConfig.reload();
        ServerSettingsS2CPacket packet = new ServerSettingsS2CPacket(
                cfg.poster.disableImages, cfg.network.batchIntervalMs, cfg.network.cursorIntervalMs);

        List<ServerPlayer> players = source.getServer().getPlayerList().getPlayers();
        for (ServerPlayer player : players) {
            ArtistryNetwork.sendToPlayer(player, packet);
        }

        int synced = players.size();
        source.sendSuccess(() -> brand()
                .append(label("Config reloaded, synced to "))
                .append(value(String.valueOf(synced)))
                .append(label(synced == 1 ? " player" : " players")), true);
        return synced;
    }

    private static int reportImageUsage(CommandSourceStack source) {
        ImageStorage.Usage usage;
        try {
            usage = ImageStorage.usage();
        } catch (IOException e) {
            source.sendFailure(brand()
                    .append(Component.literal("Cannot read the image storage: " + e.getMessage())
                            .withStyle(ChatFormatting.RED)));
            return 0;
        }

        source.sendSuccess(() -> brand()
                .append(label("Images "))
                .append(value(String.valueOf(usage.files())))
                .append(label(usage.files() == 1 ? " file, " : " files, "))
                .append(value(formatBytes(usage.bytes())))
                .append(label(" on disk")), false);
        return usage.files();
    }

    private static MutableComponent brand() {
        return Component.literal("Artistry ").withStyle(BRAND, ChatFormatting.BOLD);
    }

    private static MutableComponent label(String text) {
        return Component.literal(text).withStyle(LABEL);
    }

    private static MutableComponent value(String text) {
        return Component.literal(text).withStyle(VALUE);
    }

    private static String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format(Locale.ROOT, "%.1f KB", bytes / 1024.0);
        return String.format(Locale.ROOT, "%.1f MB", bytes / (1024.0 * 1024.0));
    }
}
