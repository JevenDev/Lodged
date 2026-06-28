package com.jvn.lodged.world;

import com.jvn.lodged.config.LodgedConfig;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.HumanoidArm;

public final class LodgedArrowRemovalScoring {
    private static final double HIDDEN_ARROW_PRIORITY_PENALTY = 1000.0D;
    private static final double ARMOR_SLOT_PRIORITY_WEIGHT = 100.0D;
    private static final double BODY_PART_PRIORITY_WEIGHT = 100.0D;
    private static final double ARM_REACH_X = 5.0D / 16.0D;
    private static final double ARM_REACH_Y = 6.0D / 16.0D;
    private static final float SIDE_THRESHOLD = 0.05F;

    private LodgedArrowRemovalScoring() {
    }

    public static double armorArrowPriorityScore(
            EquipmentSlot slot,
            LodgedArrowVisual arrow,
            HumanoidArm fallbackArm) {
        double visibleScore = arrow.modelZ() <= 0.0F ? 0.0D : HIDDEN_ARROW_PRIORITY_PENALTY;
        return visibleScore
                + (armorSlotPriority(slot) * ARMOR_SLOT_PRIORITY_WEIGHT)
                + arrowReachDistance(arrow, fallbackArm);
    }

    public static double bodyArrowPriorityScore(LodgedArrowVisual arrow, double successChance) {
        return (bodyPartRemovalPriority(arrow.bodyPart()) * BODY_PART_PRIORITY_WEIGHT)
                - Mth.clamp(successChance, 0.0D, 1.0D);
    }

    public static HumanoidArm armorPullingArm(HumanoidArm fallbackArm, LodgedArrowVisual arrow) {
        if (arrow.bodyPart().isArm()) {
            return arrow.modelX() < 0.0F ? HumanoidArm.LEFT : HumanoidArm.RIGHT;
        }
        if (Math.abs(arrow.modelX()) > SIDE_THRESHOLD) {
            return arrow.modelX() < 0.0F ? HumanoidArm.RIGHT : HumanoidArm.LEFT;
        }
        return fallbackArm;
    }

    private static int armorSlotPriority(EquipmentSlot slot) {
        return switch (slot) {
            case HEAD -> 0;
            case CHEST -> 1;
            case LEGS -> 2;
            case FEET -> 3;
            default -> 4;
        };
    }

    private static int bodyPartRemovalPriority(LodgedArrowBodyPart bodyPart) {
        return switch (bodyPart) {
            case LEFT_ARM, RIGHT_ARM, LEFT_LEG, RIGHT_LEG -> 0;
            case CHEST -> 1;
            case HEAD -> 2;
        };
    }

    private static double arrowReachDistance(LodgedArrowVisual arrow, HumanoidArm fallbackArm) {
        HumanoidArm arm = armorPullingArm(fallbackArm, arrow);
        double armX = arm == HumanoidArm.RIGHT ? -ARM_REACH_X : ARM_REACH_X;
        double dx = arrow.modelX() - armX;
        double dy = arrow.modelY() - ARM_REACH_Y;
        return (dx * dx) + (dy * dy);
    }
}
