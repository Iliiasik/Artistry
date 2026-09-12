package iliiasik.artistry.fabric;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import iliiasik.artistry.platform.services.INetworkHelper;
import iliiasik.artistry.platform.services.IPlatformHelper;
import iliiasik.artistry.platform.services.IRegistryHelper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FabricPlatformTest {

    private static <T> List<T> services(Class<T> type) {
        List<T> found = new ArrayList<>();
        for (T service : ServiceLoader.load(type)) {
            found.add(service);
        }
        return found;
    }

    private static String resource(String path) throws IOException {
        try (InputStream stream = FabricPlatformTest.class.getResourceAsStream(path)) {
            assertNotNull(stream, "missing resource " + path);
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    @Test
    @DisplayName("Every platform service resolves to exactly one Fabric implementation")
    void servicesResolveToFabricImplementations() {
        List<IPlatformHelper> platforms = services(IPlatformHelper.class);
        assertEquals(1, platforms.size(), "expected exactly one platform helper");
        assertTrue(platforms.get(0) instanceof FabricPlatformHelper);

        List<IRegistryHelper> registries = services(IRegistryHelper.class);
        assertEquals(1, registries.size(), "expected exactly one registry helper");
        assertTrue(registries.get(0) instanceof FabricRegistryHelper);

        List<INetworkHelper> networks = services(INetworkHelper.class);
        assertEquals(1, networks.size(), "expected exactly one network helper");
        assertTrue(networks.get(0) instanceof FabricNetworkHelper);
    }

    @Test
    @DisplayName("The platform reports itself as Fabric")
    void platformNameIsFabric() {
        assertEquals("Fabric", new FabricPlatformHelper().getPlatformName());
    }

    @Test
    @DisplayName("The mod metadata is fully expanded and points at the real entrypoints")
    void modMetadataIsValid() throws IOException, ClassNotFoundException {
        String raw = resource("/fabric.mod.json");
        assertFalse(raw.contains("${"), "fabric.mod.json still contains unexpanded placeholders");

        JsonObject json = JsonParser.parseString(raw).getAsJsonObject();
        assertEquals("artistry", json.get("id").getAsString());
        assertFalse(json.get("version").getAsString().isBlank());
        assertFalse(json.get("name").getAsString().isBlank());
        assertFalse(json.get("description").getAsString().isBlank());

        JsonObject entrypoints = json.getAsJsonObject("entrypoints");
        for (String side : List.of("main", "client")) {
            JsonArray entries = entrypoints.getAsJsonArray(side);
            assertEquals(1, entries.size(), side + " must declare exactly one entrypoint");
            Class.forName(entries.get(0).getAsString());
        }
    }

    @Test
    @DisplayName("The mod declares its dependencies on the loader and the api")
    void modDeclaresDependencies() throws IOException {
        JsonObject json = JsonParser.parseString(resource("/fabric.mod.json")).getAsJsonObject();
        JsonObject depends = json.getAsJsonObject("depends");

        assertTrue(depends.has("fabricloader"));
        assertTrue(depends.has("fabric-api"));
        assertTrue(depends.has("minecraft"));
        assertTrue(depends.has("java"));
    }

    @Test
    @DisplayName("The loader requirement is a range of its own, not the version we built against")
    void loaderRangeIsDecoupledFromTheBuildVersion() throws IOException {
        JsonObject json = JsonParser.parseString(resource("/fabric.mod.json")).getAsJsonObject();
        String range = json.getAsJsonObject("depends").get("fabricloader").getAsString();

        assertTrue(range.startsWith(">="),
                "the floor must stay open so older launchers keep working, was " + range);
        assertFalse(range.contains("0.19.5"),
                "the build version must not leak into the requirement");
    }

    @Test
    @DisplayName("The access widener is declared and shipped")
    void accessWidenerIsDeclared() throws IOException {
        JsonObject json = JsonParser.parseString(resource("/fabric.mod.json")).getAsJsonObject();
        String widener = json.get("accessWidener").getAsString();

        assertEquals("artistry.accesswidener", widener);
        assertTrue(resource("/" + widener).startsWith("accessWidener"));
    }

    @Test
    @DisplayName("Both mixin configurations are declared and loadable")
    void mixinConfigsAreDeclaredAndLoadable() throws IOException, ClassNotFoundException {
        JsonObject json = JsonParser.parseString(resource("/fabric.mod.json")).getAsJsonObject();
        JsonArray mixins = json.getAsJsonArray("mixins");

        List<String> configs = new ArrayList<>();
        for (JsonElement element : mixins) {
            configs.add(element.getAsString());
        }
        assertEquals(List.of("artistry.mixins.json", "artistry.fabric.mixins.json"), configs);

        for (String config : configs) {
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

        JsonObject fabricMixins =
                JsonParser.parseString(resource("/artistry.fabric.mixins.json")).getAsJsonObject();
        assertEquals(0, fabricMixins.getAsJsonArray("mixins").size(),
                "the fabric config must stay empty while the mixin lives in common");
        assertEquals(0, fabricMixins.getAsJsonArray("client").size(),
                "the fabric config must stay empty while the mixin lives in common");
    }

    @Test
    @DisplayName("The shared assets of the common module end up in the loader jar")
    void commonResourcesArePresent() throws IOException {
        assertNotNull(FabricPlatformTest.class.getResourceAsStream("/artistry_icon.png"));
        assertFalse(resource("/artistry.mixins.json").isBlank());
    }

    @Test
    @DisplayName("The manifest points at the icon and the issue tracker")
    void modMetadataCarriesIconAndContact() throws IOException {
        JsonObject json = JsonParser.parseString(resource("/fabric.mod.json")).getAsJsonObject();

        assertEquals("artistry_icon.png", json.get("icon").getAsString());

        JsonObject contact = json.getAsJsonObject("contact");
        assertEquals("https://github.com/Iliiasik/Artistry/issues", contact.get("issues").getAsString());
        assertEquals("https://github.com/Iliiasik/Artistry", contact.get("sources").getAsString());
    }

    @Test
    @DisplayName("Catalogue finds the banner and the background at the jar root")
    void catalogueMetadataIsDeclared() throws IOException {
        JsonObject json = JsonParser.parseString(resource("/fabric.mod.json")).getAsJsonObject();
        JsonObject catalogue = json.getAsJsonObject("custom").getAsJsonObject("catalogue");

        assertEquals("artistry_banner.png", catalogue.get("banner").getAsString());
        assertEquals("artistry_background.png", catalogue.get("background").getAsString());

        for (String file : List.of("artistry_banner.png", "artistry_background.png", "artistry_icon.png")) {
            assertNotNull(FabricPlatformTest.class.getResourceAsStream("/" + file),
                    file + " must sit at the jar root");
        }
    }
}
