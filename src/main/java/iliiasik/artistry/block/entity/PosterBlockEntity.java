package iliiasik.artistry.block.entity;

import iliiasik.artistry.data.CanvasData;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.Nullable;

public class PosterBlockEntity extends BlockEntity {
    public final CanvasData canvasData = new CanvasData();

    public PosterBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.POSTER, pos, state);
    }

    @Override
    protected void writeData(WriteView view) {
        super.writeData(view);
        view.put("canvas", NbtCompound.CODEC, canvasData.toNbt());
    }

    @Override
    protected void readData(ReadView view) {
        super.readData(view);
        view.read("canvas", NbtCompound.CODEC).ifPresent(canvasData::fromNbt);
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
        if (comp != null) {
            NbtCompound tag = comp.copyNbt();
            tag.getCompound("canvas").ifPresent(canvasData::fromNbt);
        }
    }

    public void markDirtyAndSync() {
        markDirty();
        if (world != null && !world.isClient()) {
            world.updateListeners(pos, getCachedState(), getCachedState(), 3);
        }
    }
}