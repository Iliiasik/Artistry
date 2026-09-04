package iliiasik.artistry.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import iliiasik.artistry.config.ArtistryConfig;
import iliiasik.artistry.debug.ArtistryDebug;
import iliiasik.artistry.network.ArtistryNetwork;
import iliiasik.artistry.network.ServerSettingsS2CPacket;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

public final class ArtistryCommands {

    private ArtistryCommands() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal("artistry")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("reload")
                        .executes(context -> {
                            ArtistryConfig cfg = ArtistryConfig.reload();
                            MinecraftServer server = context.getSource().getServer();
                            ServerSettingsS2CPacket packet = new ServerSettingsS2CPacket(
                                    cfg.poster.disableImages, cfg.network.batchIntervalMs, cfg.network.cursorIntervalMs);
                            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                                ArtistryNetwork.sendToPlayer(player, packet);
                            }
                            context.getSource().sendSuccess(
                                    () -> Component.literal("[Artistry] Config reloaded and synced to players"), true);
                            return 1;
                        }));

        ArtistryDebug.hooks().extendCommands(root);
        dispatcher.register(root);
    }
}
