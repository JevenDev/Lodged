package com.jvn.lodged.world;

public enum LodgedArrowBodyPart {
    HEAD(0, "head"),
    CHEST(1, "chest"),
    LEFT_ARM(2, "left_arm"),
    RIGHT_ARM(3, "right_arm"),
    LEFT_LEG(4, "left_leg"),
    RIGHT_LEG(5, "right_leg");

    private final int id;
    private final String serializedName;

    LodgedArrowBodyPart(int id, String serializedName) {
        this.id = id;
        this.serializedName = serializedName;
    }

    public int id() {
        return id;
    }

    public String serializedName() {
        return serializedName;
    }

    public boolean isArm() {
        return this == LEFT_ARM || this == RIGHT_ARM;
    }

    public boolean isLeg() {
        return this == LEFT_LEG || this == RIGHT_LEG;
    }

    public static LodgedArrowBodyPart byId(int id) {
        for (LodgedArrowBodyPart bodyPart : values()) {
            if (bodyPart.id == id) {
                return bodyPart;
            }
        }
        return CHEST;
    }

    public static LodgedArrowBodyPart bySerializedName(String serializedName) {
        for (LodgedArrowBodyPart bodyPart : values()) {
            if (bodyPart.serializedName.equals(serializedName)) {
                return bodyPart;
            }
        }
        return CHEST;
    }
}
