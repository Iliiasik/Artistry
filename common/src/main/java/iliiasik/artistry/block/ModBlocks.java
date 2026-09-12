package iliiasik.artistry.block;

import iliiasik.artistry.platform.Services;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.PushReaction;

import java.util.function.Supplier;

public final class ModBlocks {

    public static final Supplier<PosterBlock> POSTER = Services.REGISTRY.register(Registries.BLOCK, "poster",
            () -> new PosterBlock(BlockBehaviour.Properties.of()
                    .noCollission()
                    .instabreak()
                    .pushReaction(PushReaction.DESTROY)));

    public static final Supplier<BannerBlock> BANNER = Services.REGISTRY.register(Registries.BLOCK, "banner",
            () -> new BannerBlock(BlockBehaviour.Properties.of()
                    .noCollission()
                    .instabreak()
                    .pushReaction(PushReaction.DESTROY)));

    private ModBlocks() {}

    public static void init() {}
}
