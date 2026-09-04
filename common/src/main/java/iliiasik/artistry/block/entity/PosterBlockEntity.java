package iliiasik.artistry.block.entity;

import iliiasik.artistry.block.BannerBlock;
import iliiasik.artistry.data.CanvasData;
import iliiasik.artistry.data.CanvasImageLayer;
import iliiasik.artistry.data.CanvasSignature;
import iliiasik.artistry.item.ModItems;
import net.minecraft.world.item.Item;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.Containers;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class PosterBlockEntity extends BlockEntity {
    public final CanvasData canvasData = new CanvasData();
    public final CanvasImageLayer imageLayer = new CanvasImageLayer();
    public final CanvasSignature signature = new CanvasSignature();
    private boolean dropped = false;

    public PosterBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.POSTER.get(), pos, state);
    }

    public float canvasWorldSize() {
        return getBlockState().getBlock() instanceof BannerBlock ? BannerBlock.WORLD_SIZE : 1f;
    }

    private Item asItem() {
        return getBlockState().getBlock() instanceof BannerBlock
                ? ModItems.BANNER.get()
                : ModItems.POSTER.get();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("canvas", canvasData.toNbt());
        tag.put("images", imageLayer.toNbt());
        if (signature.isSigned()) {
            tag.put(CanvasSignature.NBT_KEY, signature.toNbt());
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        readCanvas(tag);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }

    @Override
    public @Nullable Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    public void loadFromItemStack(ItemStack stack) {
        CustomData comp = stack.get(DataComponents.CUSTOM_DATA);
        if (comp == null) return;
        readCanvas(comp.copyTag());
    }

    private void readCanvas(CompoundTag tag) {
        if (tag.contains("canvas")) {
            canvasData.fromNbt(tag.getCompound("canvas"));
        }
        if (tag.contains("images")) {
            imageLayer.fromNbt(tag.getList("images", Tag.TAG_COMPOUND));
        }
        if (tag.contains(CanvasSignature.NBT_KEY)) {
            signature.fromNbt(tag.getCompound(CanvasSignature.NBT_KEY));
        } else {
            signature.clear();
        }
        imageLayer.clampToCanvas(canvasData.canvasSize);
    }

    public void markDirtyAndSync() {
        setChanged();
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    public void dropWithCanvas(BlockPos dropPos) {
        if (level == null || level.isClientSide() || dropped) return;
        dropped = true;
        ItemStack stack = new ItemStack(asItem());
        if (canvasData.isSizeChosen()) {
            CompoundTag tag = new CompoundTag();
            tag.put("canvas", canvasData.toNbt());
            ListTag imageNbt = imageLayer.toNbt();
            if (!imageNbt.isEmpty()) {
                tag.put("images", imageNbt);
            }
            if (signature.isSigned()) {
                tag.put(CanvasSignature.NBT_KEY, signature.toNbt());
            }
            stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
            if (signature.isSigned()) {
                stack.set(DataComponents.MAX_STACK_SIZE, 1);
            }
        }
        Containers.dropContents(level, dropPos, new SimpleContainer(stack));
    }
}
