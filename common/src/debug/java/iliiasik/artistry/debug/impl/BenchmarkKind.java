package iliiasik.artistry.debug.impl;

public enum BenchmarkKind {

    POSTERS(1),
    BANNERS(2),
    MIXED(2);

    private final int slotSize;

    BenchmarkKind(int slotSize) {
        this.slotSize = slotSize;
    }

    public int slotSize() {
        return slotSize;
    }

    public boolean bannerAt(int index) {
        return switch (this) {
            case POSTERS -> false;
            case BANNERS -> true;
            case MIXED -> index % 2 == 1;
        };
    }
}
