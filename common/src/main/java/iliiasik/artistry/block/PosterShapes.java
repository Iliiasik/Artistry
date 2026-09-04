package iliiasik.artistry.block;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class PosterShapes {

    private static final VoxelShape NORTH = Block.box(0, 0, 15.9, 16, 16, 16);
    private static final VoxelShape SOUTH = Block.box(0, 0, 0, 16, 16, 0.1);
    private static final VoxelShape WEST   = Block.box(15.9, 0, 0, 16, 16, 16);
    private static final VoxelShape EAST   = Block.box(0, 0, 0, 0.1, 16, 16);

    private PosterShapes() {}

    public static VoxelShape forFacing(Direction facing) {
        return switch (facing) {
            case SOUTH -> SOUTH;
            case WEST  -> WEST;
            case EAST  -> EAST;
            default    -> NORTH;
        };
    }
}
