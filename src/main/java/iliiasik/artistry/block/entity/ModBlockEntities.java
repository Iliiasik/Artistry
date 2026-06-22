package iliiasik.artistry.block.entity;

import iliiasik.artistry.Artistry;
import iliiasik.artistry.block.ModBlocks;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Artistry.MOD_ID);

    public static final Supplier<BlockEntityType<PosterBlockEntity>> POSTER =
            BLOCK_ENTITIES.register("poster", () ->
                    BlockEntityType.Builder.of(PosterBlockEntity::new, ModBlocks.POSTER.get()).build(null));

    public static void register(IEventBus bus) {
        BLOCK_ENTITIES.register(bus);
    }
}