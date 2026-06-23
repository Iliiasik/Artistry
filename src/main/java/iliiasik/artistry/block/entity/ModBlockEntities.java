package iliiasik.artistry.block.entity;

import iliiasik.artistry.Artistry;
import iliiasik.artistry.block.ModBlocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, Artistry.MOD_ID);

    public static final RegistryObject<BlockEntityType<PosterBlockEntity>> POSTER =
            BLOCK_ENTITIES.register("poster", () ->
                    BlockEntityType.Builder.of(PosterBlockEntity::new, ModBlocks.POSTER.get()).build(null));

    public static void register(IEventBus bus) {
        BLOCK_ENTITIES.register(bus);
    }
}