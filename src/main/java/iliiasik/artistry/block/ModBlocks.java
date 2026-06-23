package iliiasik.artistry.block;

import iliiasik.artistry.Artistry;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.PushReaction;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, Artistry.MOD_ID);

    public static final RegistryObject<PosterBlock> POSTER = BLOCKS.register("poster",
            () -> new PosterBlock(BlockBehaviour.Properties.of()
                    .noCollission()
                    .instabreak()
                    .pushReaction(PushReaction.DESTROY)));

    public static void register(IEventBus bus) {
        BLOCKS.register(bus);
    }
}