package iliiasik.artistry.block.entity;

import iliiasik.artistry.block.ModBlocks;
import iliiasik.artistry.platform.Services;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;

import java.util.function.Supplier;

public final class ModBlockEntities {

    public static final Supplier<BlockEntityType<PosterBlockEntity>> POSTER =
            Services.REGISTRY.register(Registries.BLOCK_ENTITY_TYPE, "poster",
                    () -> BlockEntityType.Builder.of(PosterBlockEntity::new, ModBlocks.POSTER.get()).build(null));

    private ModBlockEntities() {}

    public static void init() {}
}
