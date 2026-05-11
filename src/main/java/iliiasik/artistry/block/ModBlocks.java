package iliiasik.artistry.block;

import iliiasik.artistry.Artistry;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.piston.PistonBehavior;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;

public class ModBlocks {
    public static final PosterBlock POSTER = new PosterBlock(
            AbstractBlock.Settings.create()
                    .noCollision()
                    .breakInstantly()
                    .pistonBehavior(PistonBehavior.DESTROY));

    public static void register() {
        Registry.register(Registries.BLOCK, Artistry.id("poster"), POSTER);
    }
}