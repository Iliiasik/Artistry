package iliiasik.artistry.block;

import iliiasik.artistry.support.McFixture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BannerGeometryTest {

    private static final BlockPos ORIGIN = new BlockPos(10, 70, -4);

    @BeforeAll
    static void boot() {
        McFixture.bootstrap();
    }

    @Test
    @DisplayName("Every part points back at the origin it was placed from")
    void partsResolveBackToOrigin() {
        for (Direction facing : Direction.Plane.HORIZONTAL) {
            for (BannerPart part : BannerPart.values()) {
                BlockPos partPos = BannerBlock.partPos(ORIGIN, facing, part);
                assertEquals(ORIGIN, BannerBlock.originOf(partPos, facing, part),
                        facing + " " + part);
            }
        }
    }

    @Test
    @DisplayName("A banner always covers four distinct cells")
    void fourDistinctCells() {
        for (Direction facing : Direction.Plane.HORIZONTAL) {
            Set<BlockPos> cells = new HashSet<>();
            for (BannerPart part : BannerPart.values()) {
                cells.add(BannerBlock.partPos(ORIGIN, facing, part));
            }
            assertEquals(BannerPart.values().length, cells.size(), facing.toString());
        }
    }

    @Test
    @DisplayName("The banner grows clockwise from the facing and upwards")
    void growsClockwiseAndUp() {
        Direction facing = Direction.NORTH;
        assertEquals(ORIGIN, BannerBlock.partPos(ORIGIN, facing, BannerPart.ORIGIN));
        assertEquals(ORIGIN.east(), BannerBlock.partPos(ORIGIN, facing, BannerPart.SIDE));
        assertEquals(ORIGIN.above(), BannerBlock.partPos(ORIGIN, facing, BannerPart.TOP));
        assertEquals(ORIGIN.east().above(), BannerBlock.partPos(ORIGIN, facing, BannerPart.CORNER));
    }

    @Test
    @DisplayName("All four cells lean on their own support behind the same wall")
    void everyCellHasItsOwnSupport() {
        for (Direction facing : Direction.Plane.HORIZONTAL) {
            Set<BlockPos> supports = new HashSet<>();
            for (BannerPart part : BannerPart.values()) {
                BlockPos partPos = BannerBlock.partPos(ORIGIN, facing, part);
                BlockPos support = partPos.relative(facing.getOpposite());
                assertTrue(supports.add(support), facing + " " + part + " shares a support");
            }
        }
    }
}
