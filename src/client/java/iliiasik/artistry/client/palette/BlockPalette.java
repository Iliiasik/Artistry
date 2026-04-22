package iliiasik.artistry.client.palette;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.Sprite;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

public final class BlockPalette {

    public static final String[] BLOCK_IDS = {
            "minecraft:white_concrete", "minecraft:white_wool", "minecraft:snow_block", "minecraft:quartz_block", "minecraft:white_terracotta", "minecraft:calcite", "minecraft:bone_block",
            "minecraft:light_gray_concrete", "minecraft:light_gray_wool", "minecraft:light_gray_terracotta", "minecraft:stone", "minecraft:andesite", "minecraft:diorite", "minecraft:polished_diorite",
            "minecraft:gray_concrete", "minecraft:gray_wool", "minecraft:gray_terracotta", "minecraft:cobblestone", "minecraft:polished_andesite", "minecraft:smooth_stone", "minecraft:tuff",
            "minecraft:black_concrete", "minecraft:black_wool", "minecraft:blackstone", "minecraft:obsidian", "minecraft:coal_block", "minecraft:deepslate", "minecraft:cobbled_deepslate",
            "minecraft:brown_concrete", "minecraft:brown_wool", "minecraft:dirt", "minecraft:coarse_dirt", "minecraft:podzol", "minecraft:brown_terracotta", "minecraft:soul_sand",
            "minecraft:red_concrete", "minecraft:red_wool", "minecraft:netherrack", "minecraft:red_terracotta", "minecraft:red_nether_bricks", "minecraft:crimson_nylium", "minecraft:crimson_planks",
            "minecraft:orange_concrete", "minecraft:orange_wool", "minecraft:orange_terracotta", "minecraft:red_sand", "minecraft:pumpkin", "minecraft:acacia_planks", "minecraft:copper_block",
            "minecraft:yellow_concrete", "minecraft:yellow_wool", "minecraft:yellow_terracotta", "minecraft:sponge", "minecraft:sand", "minecraft:hay_block", "minecraft:gold_block",
            "minecraft:lime_concrete", "minecraft:lime_wool", "minecraft:lime_terracotta", "minecraft:melon", "minecraft:slime_block", "minecraft:moss_block", "minecraft:bamboo_planks",
            "minecraft:green_concrete", "minecraft:green_wool", "minecraft:green_terracotta", "minecraft:emerald_block", "minecraft:dried_kelp_block", "minecraft:oak_leaves", "minecraft:green_glazed_terracotta",
            "minecraft:cyan_concrete", "minecraft:cyan_wool", "minecraft:cyan_terracotta", "minecraft:warped_nylium", "minecraft:warped_planks", "minecraft:prismarine", "minecraft:dark_prismarine",
            "minecraft:light_blue_concrete", "minecraft:light_blue_wool", "minecraft:light_blue_terracotta", "minecraft:ice", "minecraft:packed_ice", "minecraft:blue_ice", "minecraft:diamond_block",
            "minecraft:blue_concrete", "minecraft:blue_wool", "minecraft:blue_terracotta", "minecraft:lapis_block", "minecraft:blue_glazed_terracotta", "minecraft:polished_blackstone_bricks", "minecraft:chiseled_stone_bricks",
            "minecraft:purple_concrete", "minecraft:purple_wool", "minecraft:purple_terracotta", "minecraft:amethyst_block", "minecraft:purpur_block", "minecraft:crying_obsidian", "minecraft:mycelium",
            "minecraft:magenta_concrete", "minecraft:magenta_wool", "minecraft:magenta_terracotta", "minecraft:purpur_pillar", "minecraft:magenta_glazed_terracotta", "minecraft:nether_wart_block", "minecraft:warped_hyphae",
            "minecraft:pink_concrete", "minecraft:pink_wool", "minecraft:pink_terracotta", "minecraft:cherry_planks", "minecraft:cherry_log", "minecraft:pink_glazed_terracotta", "minecraft:cherry_leaves",
            "minecraft:white_shulker_box", "minecraft:light_gray_shulker_box", "minecraft:gray_shulker_box", "minecraft:black_shulker_box", "minecraft:brown_shulker_box", "minecraft:red_shulker_box", "minecraft:orange_shulker_box",
            "minecraft:yellow_shulker_box", "minecraft:lime_shulker_box", "minecraft:green_shulker_box", "minecraft:cyan_shulker_box", "minecraft:light_blue_shulker_box", "minecraft:blue_shulker_box", "minecraft:purple_shulker_box",
            "minecraft:magenta_shulker_box", "minecraft:pink_shulker_box",
            "minecraft:oak_planks", "minecraft:spruce_planks", "minecraft:birch_planks", "minecraft:jungle_planks", "minecraft:dark_oak_planks",
            "minecraft:oak_log", "minecraft:spruce_log", "minecraft:birch_log", "minecraft:jungle_log", "minecraft:acacia_log", "minecraft:dark_oak_log", "minecraft:mangrove_log",
            "minecraft:oak_leaves", "minecraft:spruce_leaves", "minecraft:birch_leaves", "minecraft:jungle_leaves", "minecraft:acacia_leaves", "minecraft:dark_oak_leaves", "minecraft:mangrove_leaves",
            "minecraft:stone_bricks", "minecraft:cracked_stone_bricks", "minecraft:mossy_stone_bricks", "minecraft:granite", "minecraft:polished_granite", "minecraft:basalt", "minecraft:polished_basalt",
            "minecraft:smooth_basalt", "minecraft:polished_tuff", "minecraft:dripstone_block", "minecraft:polished_deepslate", "minecraft:deepslate_bricks", "minecraft:deepslate_tiles", "minecraft:chiseled_deepslate",
            "minecraft:iron_block", "minecraft:raw_iron_block", "minecraft:raw_gold_block", "minecraft:raw_copper_block", "minecraft:exposed_copper", "minecraft:weathered_copper", "minecraft:oxidized_copper",
            "minecraft:nether_bricks", "minecraft:red_nether_bricks", "minecraft:cracked_nether_bricks", "minecraft:chiseled_nether_bricks", "minecraft:smooth_quartz", "minecraft:quartz_bricks", "minecraft:quartz_pillar",
            "minecraft:end_stone", "minecraft:end_stone_bricks", "minecraft:polished_blackstone", "minecraft:cracked_polished_blackstone_bricks", "minecraft:chiseled_polished_blackstone", "minecraft:blackstone", "minecraft:gilded_blackstone",
            "minecraft:sandstone", "minecraft:smooth_sandstone", "minecraft:chiseled_sandstone", "minecraft:red_sandstone", "minecraft:smooth_red_sandstone", "minecraft:chiseled_red_sandstone", "minecraft:mud",
            "minecraft:crafting_table", "minecraft:furnace", "minecraft:bookshelf", "minecraft:loom", "minecraft:smithing_table", "minecraft:fletching_table", "minecraft:cartography_table",
            "minecraft:honey_block", "minecraft:magma_block", "minecraft:target", "minecraft:note_block", "minecraft:jukebox", "minecraft:tnt", "minecraft:jack_o_lantern",
            "minecraft:shroomlight", "minecraft:glowstone", "minecraft:sea_lantern", "minecraft:beacon", "minecraft:budding_amethyst", "minecraft:sculk", "minecraft:glass",
            "minecraft:white_glazed_terracotta", "minecraft:light_gray_glazed_terracotta", "minecraft:gray_glazed_terracotta", "minecraft:black_glazed_terracotta", "minecraft:brown_glazed_terracotta", "minecraft:red_glazed_terracotta", "minecraft:orange_glazed_terracotta",
            "minecraft:yellow_glazed_terracotta", "minecraft:lime_glazed_terracotta", "minecraft:cyan_glazed_terracotta", "minecraft:light_blue_glazed_terracotta", "minecraft:blue_glazed_terracotta", "minecraft:purple_glazed_terracotta", "minecraft:magenta_glazed_terracotta",
            "minecraft:pink_glazed_terracotta", "minecraft:azalea_leaves", "minecraft:flowering_azalea_leaves", "minecraft:packed_mud", "minecraft:mangrove_planks", "minecraft:wet_sponge", "minecraft:crimson_hyphae"
    };

    public static final int COUNT = BLOCK_IDS.length;

    private static final Sprite[] SPRITE_CACHE = new Sprite[COUNT + 1];
    private static boolean loaded = false;

    public static void ensureLoaded() {
        if (loaded) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        for (int i = 0; i < COUNT; i++) {
            Identifier blockId = Identifier.of(BLOCK_IDS[i]);
            Block block = Registries.BLOCK.get(blockId);
            BlockState state = block.getDefaultState();
            try {
                SPRITE_CACHE[i + 1] = mc.getBlockRenderManager().getModel(state).particleSprite();
            } catch (Exception ignored) {}
        }
        loaded = true;
    }

    public static Sprite getSprite(int index) {
        if (index <= 0 || index > COUNT) return null;
        return SPRITE_CACHE[index];
    }

    private BlockPalette() {}
}