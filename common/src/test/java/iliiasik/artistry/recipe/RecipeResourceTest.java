package iliiasik.artistry.recipe;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RecipeResourceTest {

    private static JsonObject read(String path) throws IOException {
        try (InputStream stream = RecipeResourceTest.class.getResourceAsStream(path)) {
            assertNotNull(stream, path + " is not packed into the jar");
            return JsonParser.parseString(new String(stream.readAllBytes(), StandardCharsets.UTF_8))
                    .getAsJsonObject();
        }
    }

    private static JsonObject recipe(String name) throws IOException {
        return read("/data/artistry/recipes/" + name + ".json");
    }

    private static List<String> rows(JsonObject recipe) {
        List<String> pattern = new ArrayList<>();
        for (JsonElement row : recipe.getAsJsonArray("pattern")) {
            pattern.add(row.getAsString());
        }
        return pattern;
    }

    @Test
    @DisplayName("The banner recipe carries a shape so the recipe book can draw it")
    void bannerRecipeIsShaped() throws IOException {
        JsonObject banner = recipe("banner");

        assertEquals("artistry:banner_crafting", banner.get("type").getAsString());
        assertNotNull(banner.get("pattern"), "a shapeless special recipe never reaches the recipe book");
        assertNotNull(banner.get("key"));
        assertEquals("artistry:banner", banner.getAsJsonObject("result").get("item").getAsString());
    }

    @Test
    @DisplayName("The banner is a poster in the middle, sticks in the corners and paper around it")
    void bannerRecipeShape() throws IOException {
        JsonObject banner = recipe("banner");
        List<String> pattern = rows(banner);
        JsonObject key = banner.getAsJsonObject("key");

        assertEquals(3, pattern.size());
        for (String row : pattern) {
            assertEquals(3, row.length());
        }

        char corner = pattern.get(0).charAt(0);
        char centre = pattern.get(1).charAt(1);
        char edge = pattern.get(0).charAt(1);

        assertEquals("minecraft:stick", key.getAsJsonObject(String.valueOf(corner)).get("item").getAsString());
        assertEquals("artistry:poster", key.getAsJsonObject(String.valueOf(centre)).get("item").getAsString());
        assertEquals("minecraft:paper", key.getAsJsonObject(String.valueOf(edge)).get("item").getAsString());

        assertEquals(corner, pattern.get(0).charAt(2));
        assertEquals(corner, pattern.get(2).charAt(0));
        assertEquals(corner, pattern.get(2).charAt(2));
        assertEquals(edge, pattern.get(1).charAt(0));
        assertEquals(edge, pattern.get(1).charAt(2));
        assertEquals(edge, pattern.get(2).charAt(1));
    }

    @Test
    @DisplayName("Both craftable items unlock their recipe through an advancement")
    void recipesAreUnlocked() throws IOException {
        for (String name : List.of("poster", "banner")) {
            JsonObject advancement = read("/data/artistry/advancements/recipes/misc/" + name + ".json");
            JsonArray rewarded = advancement.getAsJsonObject("rewards").getAsJsonArray("recipes");

            assertEquals(1, rewarded.size(), name);
            assertEquals("artistry:" + name, rewarded.get(0).getAsString());
            assertTrue(advancement.getAsJsonObject("criteria").size() > 0, name);
        }
    }

    @Test
    @DisplayName("Cloning stays a special recipe, like vanilla map cloning")
    void cloningStaysSpecial() throws IOException {
        JsonObject cloning = recipe("poster_cloning");

        assertEquals("artistry:poster_cloning", cloning.get("type").getAsString());
        assertEquals(1, cloning.size(), "a special recipe carries nothing but its type");
    }

    @Test
    @DisplayName("The poster recipe still yields a poster from dye, paper and sticks")
    void posterRecipeIsIntact() throws IOException {
        JsonObject poster = recipe("poster");
        JsonObject key = poster.getAsJsonObject("key");

        assertEquals("minecraft:crafting_shaped", poster.get("type").getAsString());
        assertEquals("artistry:poster", poster.getAsJsonObject("result").get("item").getAsString());
        assertEquals(3, rows(poster).size());
        assertEquals(16, key.getAsJsonArray("D").size(), "every vanilla dye must be accepted");
    }
}
