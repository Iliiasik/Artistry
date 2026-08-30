package iliiasik.artistry.support;

import iliiasik.artistry.platform.services.IPlatformHelper;

import java.nio.file.Path;

public class TestPlatformHelper implements IPlatformHelper {

    public static final Path CONFIG_DIRECTORY =
            Path.of(System.getProperty("artistry.test.configDir", "build/test-config")).toAbsolutePath();

    @Override
    public String getPlatformName() {
        return "JUnit";
    }

    @Override
    public boolean isDevelopmentEnvironment() {
        return true;
    }

    @Override
    public Path getConfigDirectory() {
        return CONFIG_DIRECTORY;
    }
}
