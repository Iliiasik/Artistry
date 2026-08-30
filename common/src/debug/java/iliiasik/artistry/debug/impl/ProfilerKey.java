package iliiasik.artistry.debug.impl;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import org.lwjgl.glfw.GLFW;

public final class ProfilerKey {

    public static final String CATEGORY = "key.categories.artistry";

    public static final KeyMapping TOGGLE = new KeyMapping(
            "key.artistry.profiler",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_F6,
            CATEGORY);

    private ProfilerKey() {}

    public static void handleInput() {
        while (TOGGLE.consumeClick()) {
            RenderProfiler.toggle();
        }
    }
}
