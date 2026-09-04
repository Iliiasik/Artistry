package iliiasik.artistry.neoforge;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import iliiasik.artistry.platform.services.INetworkHelper;
import iliiasik.artistry.platform.services.IPlatformHelper;
import iliiasik.artistry.platform.services.IRegistryHelper;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NeoForgePlatformTest {

    private static <T> List<T> services(Class<T> type) {
        List<T> found = new ArrayList<>();
        for (T service : ServiceLoader.load(type)) {
            found.add(service);
        }
        return found;
    }

    private static String resource(String path) throws IOException {
        try (InputStream stream = NeoForgePlatformTest.class.getResourceAsStream(path)) {
            assertNotNull(stream, "missing resource " + path);
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    @Test
    @DisplayName("Every platform service resolves to exactly one NeoForge implementation")
    void servicesResolveToNeoForgeImplementations() {
        List<IPlatformHelper> platforms = services(IPlatformHelper.class);
        assertEquals(1, platforms.size(), "expected exactly one platform helper");
        assertTrue(platforms.get(0) instanceof NeoForgePlatformHelper);

        List<IRegistryHelper> registries = services(IRegistryHelper.class);
        assertEquals(1, registries.size(), "expected exactly one registry helper");
        assertTrue(registries.get(0) instanceof NeoForgeRegistryHelper);

        List<INetworkHelper> networks = services(INetworkHelper.class);
        assertEquals(1, networks.size(), "expected exactly one network helper");
        assertTrue(networks.get(0) instanceof NeoForgeNetworkHelper);
    }

    @Test
    @DisplayName("The platform reports itself as NeoForge")
    void platformNameIsNeoForge() {
        assertEquals("NeoForge", new NeoForgePlatformHelper().getPlatformName());
    }

    @Test
    @DisplayName("Creative tab entries are collected until the mod bus is available")
    void creativeTabEntriesAreCollected() throws Exception {
        ResourceKey<CreativeModeTab> tab = ResourceKey.create(Registries.CREATIVE_MODE_TAB,
                ResourceLocation.fromNamespaceAndPath("artistry", "test_tab"));

        new NeoForgeRegistryHelper().addToCreativeTab(tab, () -> null);

        Field field = NeoForgeRegistryHelper.class.getDeclaredField("TAB_ENTRIES");
        field.setAccessible(true);
        Map<?, ?> entries = (Map<?, ?>) field.get(null);

        assertTrue(entries.containsKey(tab), "the entry must be remembered for the tab event");
    }

    @Test
    @DisplayName("The mod metadata is fully expanded")
    void modMetadataIsExpanded() throws IOException {
        String toml = resource("/META-INF/neoforge.mods.toml");

        assertFalse(toml.contains("${"), "neoforge.mods.toml still contains unexpanded placeholders");
        assertTrue(toml.contains("modId = \"artistry\""), "the mod id is missing");
        assertTrue(toml.contains("modLoader = \"javafml\""), "the loader is missing");
        assertTrue(toml.contains("logoFile = \"artistry_banner.png\""), "the logo path is missing");
        assertTrue(toml.contains("issueTrackerURL = \"https://github.com/Iliiasik/Artistry/issues\""),
                "the issue tracker is missing");
    }

    @Test
    @DisplayName("Catalogue finds the icon and the background at the jar root")
    void catalogueMetadataIsDeclared() throws IOException {
        String toml = resource("/META-INF/neoforge.mods.toml");

        assertFalse(toml.contains("credits ="), "an empty credits line must not reach the manifest");
        assertTrue(toml.contains("[modproperties.\"artistry\"]"), "the modproperties table is missing");
        assertTrue(toml.contains("catalogueImageIcon = \"artistry_icon.png\""));
        assertTrue(toml.contains("catalogueBackground = \"artistry_background.png\""));

        for (String file : List.of("artistry_banner.png", "artistry_icon.png", "artistry_background.png")) {
            assertNotNull(NeoForgePlatformTest.class.getResourceAsStream("/" + file),
                    file + " must sit at the jar root");
        }
    }

    @Test
    @DisplayName("The mod declares its dependencies on NeoForge and Minecraft")
    void modDeclaresDependencies() throws IOException {
        String toml = resource("/META-INF/neoforge.mods.toml");

        assertTrue(toml.contains("modId = \"neoforge\""));
        assertTrue(toml.contains("modId = \"minecraft\""));
    }

    @Test
    @DisplayName("The NeoForge requirement is a range of its own, not the version we built against")
    void neoForgeRangeIsDecoupledFromTheBuildVersion() throws IOException {
        String toml = resource("/META-INF/neoforge.mods.toml");

        assertTrue(toml.contains("versionRange = \"[21.1.0,)\""),
                "the floor must stay open so older 1.21.1 servers keep working");
        assertFalse(toml.contains("versionRange = \"[21.1.234,)\""),
                "the build version must not leak into the requirement");
    }

    @Test
    @DisplayName("Both mixin configurations are declared and loadable")
    void mixinConfigsAreDeclaredAndLoadable() throws IOException, ClassNotFoundException {
        String toml = resource("/META-INF/neoforge.mods.toml");
        assertTrue(toml.contains("config = \"artistry.mixins.json\""));
        assertTrue(toml.contains("config = \"artistry.neoforge.mixins.json\""));

        for (String config : List.of("artistry.mixins.json", "artistry.neoforge.mixins.json")) {
            JsonObject mixinJson = JsonParser.parseString(resource("/" + config)).getAsJsonObject();
            String pkg = mixinJson.get("package").getAsString();
            assertEquals("JAVA_21", mixinJson.get("compatibilityLevel").getAsString());

            for (JsonElement element : mixinJson.getAsJsonArray("client")) {
                Class.forName(pkg + "." + element.getAsString());
            }
        }
    }

    @Test
    @DisplayName("The NeoForge mixin config requires a MixinExtras that understands expressions")
    void neoForgeMixinRequiresModernMixinExtras() throws IOException {
        JsonObject mixinJson = JsonParser.parseString(resource("/artistry.neoforge.mixins.json")).getAsJsonObject();
        JsonObject extras = mixinJson.getAsJsonObject("mixinextras");

        assertNotNull(extras, "the expression based mixin needs a mixinextras requirement");
        assertEquals("0.5.0", extras.get("minVersion").getAsString());
    }

    @Test
    @DisplayName("The NeoForge mixin targets the patched map branch")
    void neoForgeMixinTargetsPatchedBranch() {
        assertDoesNotThrow(() -> Class.forName(
                "iliiasik.artistry.neoforge.mixin.ItemInHandRendererNeoForgeMixin"));
    }

    @Test
    @DisplayName("The shared assets of the common module end up in the loader jar")
    void commonResourcesArePresent() throws IOException {
        assertNotNull(NeoForgePlatformTest.class.getResourceAsStream("/artistry_icon.png"));
        assertFalse(resource("/artistry.mixins.json").isBlank());
    }
}
