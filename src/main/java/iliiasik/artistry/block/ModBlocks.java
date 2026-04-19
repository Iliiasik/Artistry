package iliiasik.artistry.block;

import iliiasik.artistry.Artistry;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.piston.PistonBehavior;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;

public class ModBlocks {
    public static final RegistryKey<Block> POSTER_KEY = RegistryKey.of(RegistryKeys.BLOCK, Artistry.id("poster"));

    public static final PosterBlock POSTER = new PosterBlock(
            AbstractBlock.Settings.create()
                    .registryKey(POSTER_KEY)
                    .noCollision()
                    .breakInstantly()
                    .pistonBehavior(PistonBehavior.DESTROY));

    public static void register() {
        Registry.register(Registries.BLOCK, POSTER_KEY, POSTER);
    }
}