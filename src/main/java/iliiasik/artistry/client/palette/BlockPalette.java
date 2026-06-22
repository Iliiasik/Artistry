package iliiasik.artistry.client.palette;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public final class BlockPalette {

    public static final String[] BLOCK_IDS = {
            "minecraft:oak_planks",             "minecraft:spruce_planks",          "minecraft:birch_planks",
            "minecraft:jungle_planks",          "minecraft:acacia_planks",          "minecraft:dark_oak_planks",
            "minecraft:mangrove_planks",

            "minecraft:cherry_planks",          "minecraft:bamboo_planks",          "minecraft:crimson_planks",
            "minecraft:warped_planks",          "minecraft:oak_log",                "minecraft:spruce_log",
            "minecraft:birch_log",

            "minecraft:jungle_log",             "minecraft:acacia_log",             "minecraft:dark_oak_log",
            "minecraft:mangrove_log",           "minecraft:cherry_log",             "minecraft:stripped_oak_log",
            "minecraft:stripped_spruce_log",

            "minecraft:stripped_birch_log",     "minecraft:stripped_jungle_log",    "minecraft:stripped_acacia_log",
            "minecraft:stripped_dark_oak_log",  "minecraft:stripped_mangrove_log",  "minecraft:stripped_cherry_log",
            "minecraft:bamboo_block",

            "minecraft:stripped_bamboo_block",  "minecraft:oak_wood",               "minecraft:spruce_wood",
            "minecraft:birch_wood",             "minecraft:jungle_wood",            "minecraft:acacia_wood",
            "minecraft:dark_oak_wood",

            "minecraft:oak_leaves",             "minecraft:spruce_leaves",          "minecraft:birch_leaves",
            "minecraft:jungle_leaves",          "minecraft:acacia_leaves",          "minecraft:dark_oak_leaves",
            "minecraft:mangrove_leaves",

            "minecraft:cherry_leaves",          "minecraft:azalea_leaves",          "minecraft:flowering_azalea_leaves",
            "minecraft:grass_block",            "minecraft:dirt",                   "minecraft:coarse_dirt",
            "minecraft:podzol",

            "minecraft:mycelium",               "minecraft:mud",                    "minecraft:packed_mud",
            "minecraft:mud_bricks",             "minecraft:sand",                   "minecraft:red_sand",
            "minecraft:gravel",

            "minecraft:clay",                   "minecraft:soul_sand",              "minecraft:soul_soil",
            "minecraft:moss_block",             "minecraft:snow_block",             "minecraft:ice",
            "minecraft:packed_ice",

            "minecraft:blue_ice",               "minecraft:stone",                  "minecraft:cobblestone",
            "minecraft:smooth_stone",           "minecraft:stone_bricks",           "minecraft:cracked_stone_bricks",
            "minecraft:mossy_stone_bricks",

            "minecraft:chiseled_stone_bricks",  "minecraft:mossy_cobblestone",      "minecraft:granite",
            "minecraft:polished_granite",       "minecraft:diorite",                "minecraft:polished_diorite",
            "minecraft:andesite",

            "minecraft:polished_andesite",      "minecraft:calcite",                "minecraft:tuff",
            "minecraft:polished_tuff",          "minecraft:tuff_bricks",            "minecraft:chiseled_tuff",
            "minecraft:chiseled_tuff_bricks",

            "minecraft:deepslate",              "minecraft:cobbled_deepslate",      "minecraft:polished_deepslate",
            "minecraft:deepslate_bricks",       "minecraft:cracked_deepslate_bricks", "minecraft:deepslate_tiles",
            "minecraft:cracked_deepslate_tiles",

            "minecraft:chiseled_deepslate",     "minecraft:blackstone",             "minecraft:polished_blackstone",
            "minecraft:polished_blackstone_bricks", "minecraft:cracked_polished_blackstone_bricks", "minecraft:chiseled_polished_blackstone",
            "minecraft:gilded_blackstone",

            "minecraft:obsidian",               "minecraft:crying_obsidian",        "minecraft:sandstone",
            "minecraft:smooth_sandstone",       "minecraft:chiseled_sandstone",     "minecraft:cut_sandstone",
            "minecraft:red_sandstone",

            "minecraft:smooth_red_sandstone",   "minecraft:chiseled_red_sandstone", "minecraft:cut_red_sandstone",
            "minecraft:basalt",                 "minecraft:polished_basalt",        "minecraft:smooth_basalt",
            "minecraft:dripstone_block",

            "minecraft:netherrack",             "minecraft:nether_bricks",          "minecraft:red_nether_bricks",
            "minecraft:cracked_nether_bricks",  "minecraft:chiseled_nether_bricks", "minecraft:nether_wart_block",
            "minecraft:crimson_nylium",

            "minecraft:warped_nylium",          "minecraft:quartz_block",           "minecraft:smooth_quartz",
            "minecraft:quartz_bricks",          "minecraft:quartz_pillar",          "minecraft:chiseled_quartz_block",
            "minecraft:purpur_block",

            "minecraft:purpur_pillar",          "minecraft:end_stone",              "minecraft:end_stone_bricks",
            "minecraft:prismarine",             "minecraft:prismarine_bricks",      "minecraft:dark_prismarine",
            "minecraft:amethyst_block",

            "minecraft:iron_ore",               "minecraft:deepslate_iron_ore",     "minecraft:gold_ore",
            "minecraft:deepslate_gold_ore",     "minecraft:copper_ore",             "minecraft:deepslate_copper_ore",
            "minecraft:coal_ore",

            "minecraft:deepslate_coal_ore",     "minecraft:diamond_ore",            "minecraft:deepslate_diamond_ore",
            "minecraft:emerald_ore",            "minecraft:deepslate_emerald_ore",  "minecraft:lapis_ore",
            "minecraft:deepslate_lapis_ore",

            "minecraft:nether_gold_ore",        "minecraft:nether_quartz_ore",      "minecraft:ancient_debris",
            "minecraft:iron_block",             "minecraft:raw_iron_block",         "minecraft:gold_block",
            "minecraft:raw_gold_block",

            "minecraft:copper_block",           "minecraft:exposed_copper",         "minecraft:weathered_copper",
            "minecraft:oxidized_copper",        "minecraft:raw_copper_block",       "minecraft:diamond_block",
            "minecraft:emerald_block",

            "minecraft:lapis_block",            "minecraft:coal_block",             "minecraft:netherite_block",
            "minecraft:amethyst_block",         "minecraft:bone_block",             "minecraft:hay_block",
            "minecraft:dried_kelp_block",

            "minecraft:slime_block",            "minecraft:honey_block",            "minecraft:magma_block",
            "minecraft:glowstone",              "minecraft:sea_lantern",            "minecraft:shroomlight",
            "minecraft:sculk",

            "minecraft:glass",                  "minecraft:bookshelf",              "minecraft:terracotta",
            "minecraft:white_terracotta",       "minecraft:light_gray_terracotta",  "minecraft:gray_terracotta",
            "minecraft:black_terracotta",

            "minecraft:brown_terracotta",       "minecraft:red_terracotta",         "minecraft:orange_terracotta",
            "minecraft:yellow_terracotta",      "minecraft:lime_terracotta",        "minecraft:green_terracotta",
            "minecraft:cyan_terracotta",

            "minecraft:light_blue_terracotta",  "minecraft:blue_terracotta",        "minecraft:purple_terracotta",
            "minecraft:magenta_terracotta",     "minecraft:pink_terracotta",        "minecraft:white_concrete",
            "minecraft:light_gray_concrete",

            "minecraft:gray_concrete",          "minecraft:black_concrete",         "minecraft:brown_concrete",
            "minecraft:red_concrete",           "minecraft:orange_concrete",        "minecraft:yellow_concrete",
            "minecraft:lime_concrete",

            "minecraft:green_concrete",         "minecraft:cyan_concrete",          "minecraft:light_blue_concrete",
            "minecraft:blue_concrete",          "minecraft:purple_concrete",        "minecraft:magenta_concrete",
            "minecraft:pink_concrete",

            "minecraft:white_wool",             "minecraft:light_gray_wool",        "minecraft:gray_wool",
            "minecraft:black_wool",             "minecraft:brown_wool",             "minecraft:red_wool",
            "minecraft:orange_wool",

            "minecraft:yellow_wool",            "minecraft:lime_wool",              "minecraft:green_wool",
            "minecraft:cyan_wool",              "minecraft:light_blue_wool",        "minecraft:blue_wool",
            "minecraft:purple_wool",

            "minecraft:magenta_wool",           "minecraft:pink_wool",              "minecraft:white_shulker_box",
            "minecraft:light_gray_shulker_box", "minecraft:gray_shulker_box",       "minecraft:black_shulker_box",
            "minecraft:brown_shulker_box",

            "minecraft:red_shulker_box",        "minecraft:orange_shulker_box",     "minecraft:yellow_shulker_box",
            "minecraft:lime_shulker_box",       "minecraft:green_shulker_box",      "minecraft:cyan_shulker_box",
            "minecraft:light_blue_shulker_box",

            "minecraft:blue_shulker_box",       "minecraft:purple_shulker_box",     "minecraft:magenta_shulker_box",
            "minecraft:pink_shulker_box",       "minecraft:moss_block",             "minecraft:mycelium",
            "minecraft:soul_soil",
    };

    public static final int COUNT = BLOCK_IDS.length;

    private static final ResourceLocation BLOCK_ATLAS = ResourceLocation.withDefaultNamespace("textures/atlas/blocks.png");
    private static final TextureAtlasSprite[] SPRITE_CACHE = new TextureAtlasSprite[COUNT + 1];
    private static boolean loaded = false;

    @SuppressWarnings("resource")
    public static void ensureLoaded() {
        if (loaded) return;
        Minecraft mc = Minecraft.getInstance();
        TextureAtlas atlas = (TextureAtlas) mc.getTextureManager().getTexture(BLOCK_ATLAS);
        for (int i = 0; i < COUNT; i++) {
            try {
                Block block = BuiltInRegistries.BLOCK.get(ResourceLocation.parse(BLOCK_IDS[i]));
                BlockState state = block.defaultBlockState();
                TextureAtlasSprite particle = mc.getBlockRenderer().getBlockModel(state).getParticleIcon();
                SPRITE_CACHE[i + 1] = atlas.getSprite(particle.contents().name());
            } catch (Exception ignored) {}
        }
        loaded = true;
    }

    public static TextureAtlasSprite getSprite(int index) {
        if (index <= 0 || index > COUNT) return null;
        return SPRITE_CACHE[index];
    }

    private BlockPalette() {}
}