package iliiasik.artistry.debug;

import iliiasik.artistry.Artistry;

public final class ArtistryDebug {

    private static final String BOOTSTRAP = "iliiasik.artistry.debug.impl.DebugBootstrap";

    private static DebugHooks hooks = DebugHooks.NOOP;
    private static boolean bootstrapped = false;

    private ArtistryDebug() {}

    public static DebugHooks hooks() {
        if (!bootstrapped) bootstrap();
        return hooks;
    }

    public static void install(DebugHooks implementation) {
        hooks = implementation;
    }

    private static synchronized void bootstrap() {
        if (bootstrapped) return;
        bootstrapped = true;
        try {
            Class.forName(BOOTSTRAP);
            Artistry.LOGGER.info("Artistry debug tools are available");
        } catch (ClassNotFoundException ignored) {
            hooks = DebugHooks.NOOP;
        } catch (Throwable throwable) {
            Artistry.LOGGER.error("Failed to load the Artistry debug tools", throwable);
            hooks = DebugHooks.NOOP;
        }
    }
}
