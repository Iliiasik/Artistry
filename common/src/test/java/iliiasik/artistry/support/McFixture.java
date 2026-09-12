package iliiasik.artistry.support;

import io.netty.buffer.Unpooled;
import net.minecraft.SharedConstants;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.Bootstrap;

public final class McFixture {

    private static boolean booted = false;

    private McFixture() {}

    public static synchronized void bootstrap() {
        if (booted) return;
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        booted = true;
    }

    public static FriendlyByteBuf buffer() {
        return new FriendlyByteBuf(Unpooled.buffer());
    }

    public static byte[] pattern(int length) {
        byte[] bytes = new byte[length];
        for (int i = 0; i < length; i++) {
            bytes[i] = (byte) (i * 31 + 7);
        }
        return bytes;
    }
}
