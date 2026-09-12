package iliiasik.artistry.debug;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.commands.CommandSourceStack;
import org.jetbrains.annotations.Nullable;

public interface DebugHooks {

    DebugHooks NOOP = new DebugHooks() {};

    default boolean isRecording() {
        return false;
    }

    default void poster(long nanos) {}

    default void textureCreated(int texSize) {}

    default void textureClosed(int texSize) {}

    default void textureUploaded(int texSize) {}

    default void handleInput() {}

    default void renderOverlay(GuiGraphics graphics) {}

    @Nullable
    default KeyMapping keyMapping() {
        return null;
    }

    default void extendCommands(LiteralArgumentBuilder<CommandSourceStack> root) {}
}
