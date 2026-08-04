package com.jvn.lodged.collision;

import com.jvn.lodged.world.LodgedArrowBodyPart;
import java.util.Locale;

public enum ModelHitboxPart implements ModelColliderPart {
    PLAYER_HEAD(LodgedArrowBodyPart.HEAD, 0, 0, 0, -4, -8, -4, 4, 0, 4),
    PLAYER_CHEST(LodgedArrowBodyPart.CHEST, 0, 0, 0, -4, 0, -2, 4, 12, 2),
    PLAYER_RIGHT_ARM(LodgedArrowBodyPart.RIGHT_ARM, -5, 2, 0, -3, -2, -2, 1, 10, 2),
    PLAYER_LEFT_ARM(LodgedArrowBodyPart.LEFT_ARM, 5, 2, 0, -1, -2, -2, 3, 10, 2),
    PLAYER_RIGHT_LEG(LodgedArrowBodyPart.RIGHT_LEG, -1.9, 12, 0, -2, 0, -2, 2, 12, 2),
    PLAYER_LEFT_LEG(LodgedArrowBodyPart.LEFT_LEG, 1.9, 12, 0, -2, 0, -2, 2, 12, 2),

    SPIDER_HEAD(LodgedArrowBodyPart.HEAD, 0, 15, -3, -4, -4, -8, 4, 4, 0),
    SPIDER_FRONT_BODY(LodgedArrowBodyPart.CHEST, 0, 15, 0, -3, -3, -3, 3, 3, 3),
    SPIDER_REAR_BODY(LodgedArrowBodyPart.CHEST, 0, 15, 9, -5, -4, -6, 5, 4, 6),
    SPIDER_RIGHT_HIND_LEG(LodgedArrowBodyPart.RIGHT_LEG, -4, 15, 2, -15, -1, -1, 1, 1, 1),
    SPIDER_LEFT_HIND_LEG(LodgedArrowBodyPart.LEFT_LEG, 4, 15, 2, -1, -1, -1, 15, 1, 1),
    SPIDER_RIGHT_MIDDLE_HIND_LEG(LodgedArrowBodyPart.RIGHT_LEG, -4, 15, 1, -15, -1, -1, 1, 1, 1),
    SPIDER_LEFT_MIDDLE_HIND_LEG(LodgedArrowBodyPart.LEFT_LEG, 4, 15, 1, -1, -1, -1, 15, 1, 1),
    SPIDER_RIGHT_MIDDLE_FRONT_LEG(LodgedArrowBodyPart.RIGHT_LEG, -4, 15, 0, -15, -1, -1, 1, 1, 1),
    SPIDER_LEFT_MIDDLE_FRONT_LEG(LodgedArrowBodyPart.LEFT_LEG, 4, 15, 0, -1, -1, -1, 15, 1, 1),
    SPIDER_RIGHT_FRONT_LEG(LodgedArrowBodyPart.RIGHT_LEG, -4, 15, -1, -15, -1, -1, 1, 1, 1),
    SPIDER_LEFT_FRONT_LEG(LodgedArrowBodyPart.LEFT_LEG, 4, 15, -1, -1, -1, -1, 15, 1, 1);

    private static final double PIXEL = 1.0D / 16.0D;

    private final LodgedArrowBodyPart bodyPart;
    private final double pivotX;
    private final double pivotY;
    private final double pivotZ;
    private final double minX;
    private final double minY;
    private final double minZ;
    private final double maxX;
    private final double maxY;
    private final double maxZ;

    ModelHitboxPart(
            LodgedArrowBodyPart bodyPart,
            double pivotX,
            double pivotY,
            double pivotZ,
            double minX,
            double minY,
            double minZ,
            double maxX,
            double maxY,
            double maxZ) {
        this.bodyPart = bodyPart;
        this.pivotX = pivotX * PIXEL;
        this.pivotY = pivotY * PIXEL;
        this.pivotZ = pivotZ * PIXEL;
        this.minX = minX * PIXEL;
        this.minY = minY * PIXEL;
        this.minZ = minZ * PIXEL;
        this.maxX = maxX * PIXEL;
        this.maxY = maxY * PIXEL;
        this.maxZ = maxZ * PIXEL;
    }

    public LodgedArrowBodyPart bodyPart() { return bodyPart; }
    public double pivotX() { return pivotX; }
    public double pivotY() { return pivotY; }
    public double pivotZ() { return pivotZ; }
    public double minX() { return minX; }
    public double minY() { return minY; }
    public double minZ() { return minZ; }
    public double maxX() { return maxX; }
    public double maxY() { return maxY; }
    public double maxZ() { return maxZ; }

    public int id() {
        return ordinal();
    }

    public String serializedName() {
        return name().toLowerCase(Locale.ROOT);
    }

    public String modelPartPath() {
        return switch (this) {
            case PLAYER_HEAD, SPIDER_HEAD -> "head";
            case PLAYER_CHEST -> "body";
            case PLAYER_RIGHT_ARM -> "right_arm";
            case PLAYER_LEFT_ARM -> "left_arm";
            case PLAYER_RIGHT_LEG -> "right_leg";
            case PLAYER_LEFT_LEG -> "left_leg";
            case SPIDER_FRONT_BODY -> "body0";
            case SPIDER_REAR_BODY -> "body1";
            case SPIDER_RIGHT_HIND_LEG -> "right_hind_leg";
            case SPIDER_LEFT_HIND_LEG -> "left_hind_leg";
            case SPIDER_RIGHT_MIDDLE_HIND_LEG -> "right_middle_hind_leg";
            case SPIDER_LEFT_MIDDLE_HIND_LEG -> "left_middle_hind_leg";
            case SPIDER_RIGHT_MIDDLE_FRONT_LEG -> "right_middle_front_leg";
            case SPIDER_LEFT_MIDDLE_FRONT_LEG -> "left_middle_front_leg";
            case SPIDER_RIGHT_FRONT_LEG -> "right_front_leg";
            case SPIDER_LEFT_FRONT_LEG -> "left_front_leg";
        };
    }

    public static ModelHitboxPart byId(int id) {
        ModelHitboxPart[] parts = values();
        return id >= 0 && id < parts.length ? parts[id] : null;
    }

    public static ModelHitboxPart bySerializedName(String name) {
        for (ModelHitboxPart part : values()) {
            if (part.serializedName().equals(name)) {
                return part;
            }
        }
        return null;
    }
}
