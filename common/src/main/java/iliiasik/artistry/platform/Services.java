package iliiasik.artistry.platform;

import iliiasik.artistry.Artistry;
import iliiasik.artistry.platform.services.INetworkHelper;
import iliiasik.artistry.platform.services.IPlatformHelper;
import iliiasik.artistry.platform.services.IRegistryHelper;

import java.util.ServiceLoader;

public final class Services {

    public static final IPlatformHelper PLATFORM = load(IPlatformHelper.class);
    public static final IRegistryHelper REGISTRY = load(IRegistryHelper.class);
    public static final INetworkHelper NETWORK = load(INetworkHelper.class);

    private Services() {}

    public static <T> T load(Class<T> clazz) {
        final T loadedService = ServiceLoader.load(clazz)
                .findFirst()
                .orElseThrow(() -> new NullPointerException("Failed to load service for " + clazz.getName()));
        Artistry.LOGGER.debug("Loaded {} for service {}", loadedService, clazz);
        return loadedService;
    }
}
