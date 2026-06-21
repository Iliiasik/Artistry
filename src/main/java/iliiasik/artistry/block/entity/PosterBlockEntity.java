package iliiasik.artistry.block.entity;

import iliiasik.artistry.data.CanvasData;
import iliiasik.artistry.data.CanvasImageLayer;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.Nullable;

public class PosterBlockEntity extends BlockEntity {
    public final CanvasData canvasData = new CanvasData();
    public final CanvasImageLayer imageLayer = new CanvasImageLayer();
    private boolean dropped = false;

    public PosterBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.POSTER, pos, state);
    }

    @Override
    protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
        super.writeNbt(nbt, registries);
        nbt.put("canvas", canvasData.toNbt());
        nbt.put("images", imageLayer.toNbt());
    }

    @Override
    protected void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
        super.readNbt(nbt, registries);
        if (nbt.contains("canvas")) {
            canvasData.fromNbt(nbt.getCompound("canvas"));
        }
        if (nbt.contains("images")) {
            imageLayer.fromNbt(nbt.getList("images", NbtList.COMPOUND_TYPE));
        }
    }

    @Override
    public NbtCompound toInitialChunkDataNbt(RegistryWrapper.WrapperLookup registries) {
        return createNbt(registries);
    }

    @Override
    public @Nullable Packet<ClientPlayPacketListener> toUpdatePacket() {
        return BlockEntityUpdateS2CPacket.create(this);
    }

    public void loadFromItemStack(ItemStack stack) {
        NbtComponent comp = stack.get(DataComponentTypes.CUSTOM_DATA);
        if (comp == null) return;
        NbtCompound tag = comp.copyNbt();
        if (tag.contains("canvas")) {
            canvasData.fromNbt(tag.getCompound("canvas"));
        }
        if (tag.contains("images")) {
            imageLayer.fromNbt(tag.getList("images", NbtList.COMPOUND_TYPE));
        }
    }

    public void markDirtyAndSync() {
        markDirty();
        if (world != null && !world.isClient()) {
            world.updateListeners(pos, getCachedState(), getCachedState(), 3);
        }
    }

    public void dropWithCanvas(BlockPos dropPos) {
        if (world == null || world.isClient() || dropped) return;
        dropped = true;
        ItemStack stack = new ItemStack(iliiasik.artistry.item.ModItems.POSTER);
        if (canvasData.isSizeChosen()) {
            NbtCompound tag = new NbtCompound();
            tag.put("canvas", canvasData.toNbt());
            NbtList imageNbt = imageLayer.toNbt();
            if (!imageNbt.isEmpty()) {
                tag.put("images", imageNbt);
            }
            stack.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(tag));
            stack.set(DataComponentTypes.MAX_STACK_SIZE, 1);
        }
        net.minecraft.util.ItemScatterer.spawn(world, dropPos,
                new net.minecraft.inventory.SimpleInventory(stack));
    }
}