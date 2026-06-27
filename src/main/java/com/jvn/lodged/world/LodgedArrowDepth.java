package com.jvn.lodged.world;

public enum LodgedArrowDepth {
    SHALLOW(0, "shallow"),
    LODGED(1, "lodged"),
    DEEP_LODGED(2, "deep_lodged");

    private final int id;
    private final String serializedName;

    LodgedArrowDepth(int id, String serializedName) {
        this.id = id;
        this.serializedName = serializedName;
    }

    public int id() {
        return id;
    }

    public String serializedName() {
        return serializedName;
    }

    public static LodgedArrowDepth byId(int id) {
        for (LodgedArrowDepth depth : values()) {
            if (depth.id == id) {
                return depth;
            }
        }
        return LODGED;
    }

    public static LodgedArrowDepth bySerializedName(String serializedName) {
        for (LodgedArrowDepth depth : values()) {
            if (depth.serializedName.equals(serializedName)) {
                return depth;
            }
        }
        return LODGED;
    }
}
