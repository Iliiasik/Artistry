package iliiasik.artistry.block.entity;

import iliiasik.artistry.Artistry;
import iliiasik.artistry.block.ModBlocks;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;

public class ModBlockEntities {
    public static BlockEntityType<PosterBlockEntity> POSTER;

    public static void register() {
        POSTER = Registry.register(Registries.BLOCK_ENTITY_TYPE,
                Artistry.id("poster"),
                BlockEntityType.Builder.create(
                        PosterBlockEntity::new,
                        ModBlocks.POSTER
                ).build(null));
    }
}