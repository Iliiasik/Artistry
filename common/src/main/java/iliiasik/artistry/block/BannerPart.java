package iliiasik.artistry.block;

import net.minecraft.util.StringRepresentable;

public enum BannerPart implements StringRepresentable {

    ORIGIN("origin", 0, 0),
    SIDE("side", 1, 0),
    TOP("top", 0, 1),
    CORNER("corner", 1, 1);

    private final String name;
    private final int sideSteps;
    private final int upSteps;

    BannerPart(String name, int sideSteps, int upSteps) {
        this.name = name;
        this.sideSteps = sideSteps;
        this.upSteps = upSteps;
    }

    public int sideSteps() {
        return sideSteps;
    }

    public int upSteps() {
        return upSteps;
    }

    @Override
    public String getSerializedName() {
        return name;
    }
}
