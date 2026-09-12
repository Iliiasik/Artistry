package iliiasik.artistry.forge;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import iliiasik.artistry.Artistry;
import iliiasik.artistry.block.entity.PosterBlockEntity;
import iliiasik.artistry.platform.services.INetworkHelper;
import iliiasik.artistry.platform.services.IPlatformHelper;
import iliiasik.artistry.platform.services.IRegistryHelper;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraftforge.common.extensions.IForgeBlockEntity;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
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

class ForgePlatformTest {

    @BeforeAll
    static void allowRegistryKeys() throws Exception {
        Field bootstrapped = Bootstrap.class.getDeclaredField("isBootstrapped");
        bootstrapped.setAccessible(true);
        bootstrapped.setBoolean(null, true);
        Class.forName(BuiltInRegistries.class.getName(), true, ForgePlatformTest.class.getClassLoader());
    }

    private static <T> List<T> services(Class<T> type) {
        List<T> found = new ArrayList<>();
        for (T service : ServiceLoader.load(type)) {
            found.add(service);
        }
        return found;
    }

    private static String resource(String path) throws IOException {
        try (InputStream stream = ForgePlatformTest.class.getResourceAsStream(path)) {
            assertNotNull(stream, "missing resource " + path);
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    @Test
    @DisplayName("Every platform service resolves to exactly one Forge implementation")
    void servicesResolveToForgeImplementations() {
        List<IPlatformHelper> platforms = services(IPlatformHelper.class);
        assertEquals(1, platforms.size(), "expected exactly one platform helper");
        assertTrue(platforms.get(0) instanceof ForgePlatformHelper);

        List<IRegistryHelper> registries = services(IRegistryHelper.class);
        assertEquals(1, registries.size(), "expected exactly one registry helper");
        assertTrue(registries.get(0) instanceof ForgeRegistryHelper);

        List<INetworkHelper> networks = services(INetworkHelper.class);
        assertEquals(1, networks.size(), "expected exactly one network helper");
        assertTrue(networks.get(0) instanceof ForgeNetworkHelper);
    }

    @Test
    @DisplayName("The platform reports itself as Forge")
    void platformNameIsForge() {
        assertEquals("Forge", new ForgePlatformHelper().getPlatformName());
    }

    @Test
    @DisplayName("Creative tab entries are collected until the mod bus is available")
    void creativeTabEntriesAreCollected() throws Exception {
        ResourceKey<CreativeModeTab> tab = ResourceKey.create(Registries.CREATIVE_MODE_TAB,
                Artistry.id("test_tab"));

        new ForgeRegistryHelper().addToCreativeTab(tab, () -> null);

        Field field = ForgeRegistryHelper.class.getDeclaredField("TAB_ENTRIES");
        field.setAccessible(true);
        Map<?, ?> entries = (Map<?, ?>) field.get(null);

        assertTrue(entries.containsKey(tab), "the entry must be remembered for the tab event");
    }

    @Test
    @DisplayName("The poster block entity really overrides the Forge render bounding box hook")
    void posterOverridesRenderBoundingBox() throws NoSuchMethodException {
        assertTrue(IForgeBlockEntity.class.isAssignableFrom(PosterBlockEntity.class),
                "block entities must inherit the Forge extension");

        Method ours = PosterBlockEntity.class.getDeclaredMethod("getRenderBoundingBox");
        Method forge = IForgeBlockEntity.class.getMethod("getRenderBoundingBox");

        assertEquals(forge.getReturnType(), ours.getReturnType(),
                "a different return type makes this a new method instead of an override");
    }

    @Test
    @DisplayName("The mod metadata is fully expanded")
    void modMetadataIsExpanded() throws IOException {
        String toml = resource("/META-INF/mods.toml");

        assertFalse(toml.contains("${"), "mods.toml still contains unexpanded placeholders");
        assertTrue(toml.contains("modId = \"artistry\""), "the mod id is missing");
        assertTrue(toml.contains("modLoader = \"javafml\""), "the loader is missing");
        assertTrue(toml.contains("logoFile = \"artistry_banner.png\""), "the logo path is missing");
        assertTrue(toml.contains("issueTrackerURL = \"https://github.com/Iliiasik/Artistry/issues\""),
                "the issue tracker is missing");
    }

    @Test
    @DisplayName("Catalogue finds the icon and the background at the jar root")
    void catalogueMetadataIsDeclared() throws IOException {
        String toml = resource("/META-INF/mods.toml");

        assertFalse(toml.contains("credits ="), "an empty credits line must not reach the manifest");
        assertTrue(toml.contains("[modproperties.\"artistry\"]"), "the modproperties table is missing");
        assertTrue(toml.contains("catalogueImageIcon = \"artistry_icon.png\""));
        assertTrue(toml.contains("catalogueBackground = \"artistry_background.png\""));

        for (String file : List.of("artistry_banner.png", "artistry_icon.png", "artistry_background.png")) {
            assertNotNull(ForgePlatformTest.class.getResourceAsStream("/" + file),
                    file + " must sit at the jar root");
        }
    }

    @Test
    @DisplayName("The mod declares its dependencies on Forge and Minecraft")
    void modDeclaresDependencies() throws IOException {
        String toml = resource("/META-INF/mods.toml");

        assertTrue(toml.contains("modId = \"forge\""));
        assertTrue(toml.contains("modId = \"minecraft\""));
    }

    @Test
    @DisplayName("The Forge requirement is a range of its own, not the version we built against")
    void forgeRangeIsDecoupledFromTheBuildVersion() throws IOException {
        String toml = resource("/META-INF/mods.toml");

        assertTrue(toml.contains("versionRange = \"[47,)\""),
                "the floor must stay open so older 1.20.1 servers keep working");
        assertFalse(toml.contains("47.4.23"),
                "the build version must not leak into the requirement");
    }

    @Test
    @DisplayName("The access transformer is shipped next to the manifest")
    void accessTransformerIsShipped() throws IOException {
        String at = resource("/META-INF/accesstransformer.cfg");
        assertTrue(at.contains("net.minecraft.client.renderer.RenderStateShard"),
                "the render state entries are missing");
    }

    @Test
    @DisplayName("Both mixin configurations are declared and loadable")
    void mixinConfigsAreDeclaredAndLoadable() throws IOException, ClassNotFoundException {
        String toml = resource("/META-INF/mods.toml");
        assertTrue(toml.contains("config = \"artistry.mixins.json\""));
        assertTrue(toml.contains("config = \"artistry.forge.mixins.json\""));

        for (String config : List.of("artistry.mixins.json", "artistry.forge.mixins.json")) {
            JsonObject mixinJson = JsonParser.parseString(resource("/" + config)).getAsJsonObject();
            String pkg = mixinJson.get("package").getAsString();
            assertEquals("JAVA_17", mixinJson.get("compatibilityLevel").getAsString());

            for (String side : List.of("mixins", "client")) {
                for (JsonElement element : mixinJson.getAsJsonArray(side)) {
                    Class.forName(pkg + "." + element.getAsString());
                }
            }
        }
    }

    @Test
    @DisplayName("The in hand mixin is shared by both loaders instead of being duplicated")
    void inHandMixinLivesInCommon() throws IOException {
        assertDoesNotThrow(() -> Class.forName("iliiasik.artistry.mixin.ItemInHandRendererMixin"));

        JsonObject forgeMixins =
                JsonParser.parseString(resource("/artistry.forge.mixins.json")).getAsJsonObject();
        assertEquals(0, forgeMixins.getAsJsonArray("mixins").size(),
                "the forge config must stay empty while the mixin lives in common");
        assertEquals(0, forgeMixins.getAsJsonArray("client").size(),
                "the forge config must stay empty while the mixin lives in common");
    }

    @Test
    @DisplayName("The shared assets of the common module end up in the loader jar")
    void commonResourcesArePresent() throws IOException {
        assertNotNull(ForgePlatformTest.class.getResourceAsStream("/artistry_icon.png"));
        assertFalse(resource("/artistry.mixins.json").isBlank());
    }
}
