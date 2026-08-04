package com.jvn.lodged.collision;

import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Spider;
import net.minecraft.world.entity.player.Player;

public final class ModelHitboxCache {
    public static final double DEFAULT_INFLATION = 0.03125D;
    private static final double MODEL_ROOT_Y = 1.501D;
    private static final double PLAYER_RENDER_SCALE = 0.9375D;
    private static final double CAVE_SPIDER_RENDER_SCALE = 0.7D;
    private static final int MAX_PARTS = 48;

    private static final ModelHitboxPart[] PLAYER_PARTS = {
            ModelHitboxPart.PLAYER_HEAD,
            ModelHitboxPart.PLAYER_CHEST,
            ModelHitboxPart.PLAYER_RIGHT_ARM,
            ModelHitboxPart.PLAYER_LEFT_ARM,
            ModelHitboxPart.PLAYER_RIGHT_LEG,
            ModelHitboxPart.PLAYER_LEFT_LEG
    };

    private static final ModelHitboxPart[] SPIDER_PARTS = {
            ModelHitboxPart.SPIDER_HEAD,
            ModelHitboxPart.SPIDER_FRONT_BODY,
            ModelHitboxPart.SPIDER_REAR_BODY,
            ModelHitboxPart.SPIDER_RIGHT_HIND_LEG,
            ModelHitboxPart.SPIDER_LEFT_HIND_LEG,
            ModelHitboxPart.SPIDER_RIGHT_MIDDLE_HIND_LEG,
            ModelHitboxPart.SPIDER_LEFT_MIDDLE_HIND_LEG,
            ModelHitboxPart.SPIDER_RIGHT_MIDDLE_FRONT_LEG,
            ModelHitboxPart.SPIDER_LEFT_MIDDLE_FRONT_LEG,
            ModelHitboxPart.SPIDER_RIGHT_FRONT_LEG,
            ModelHitboxPart.SPIDER_LEFT_FRONT_LEG
    };

    private final ModelColliderPart[] parts = new ModelColliderPart[MAX_PARTS];
    private final double[] originX = new double[MAX_PARTS];
    private final double[] originY = new double[MAX_PARTS];
    private final double[] originZ = new double[MAX_PARTS];
    private final double[] axisXX = new double[MAX_PARTS];
    private final double[] axisXY = new double[MAX_PARTS];
    private final double[] axisXZ = new double[MAX_PARTS];
    private final double[] axisYX = new double[MAX_PARTS];
    private final double[] axisYY = new double[MAX_PARTS];
    private final double[] axisYZ = new double[MAX_PARTS];
    private final double[] axisZX = new double[MAX_PARTS];
    private final double[] axisZY = new double[MAX_PARTS];
    private final double[] axisZZ = new double[MAX_PARTS];

    private int preparedTick = Integer.MIN_VALUE;
    private int preparedPartialBits;
    private int preparedKind;
    private int partCount;
    private double scale;
    private double rootCos;
    private double rootSin;
    private double renderOffsetY;

    public boolean prepare(LivingEntity entity, float partialTick) {
        int kind = modelKind(entity);
        if (kind == 0) {
            partCount = 0;
            return false;
        }

        float clampedPartial = Mth.clamp(partialTick, 0.0F, 1.0F);
        int partialBits = Float.floatToRawIntBits(clampedPartial);
        if (preparedKind == kind && preparedTick == entity.tickCount && preparedPartialBits == partialBits) {
            return true;
        }

        preparedKind = kind;
        preparedTick = entity.tickCount;
        preparedPartialBits = partialBits;
        float bodyYaw = Mth.rotLerp(clampedPartial, entity.yBodyRotO, entity.yBodyRot);
        double yawRadians = bodyYaw * Mth.DEG_TO_RAD;
        rootCos = Math.cos(yawRadians);
        rootSin = Math.sin(yawRadians);
        double entityScale = entity.getScale();
        renderOffsetY = 0.0D;

        if (kind == 1) {
            scale = entityScale * PLAYER_RENDER_SCALE;
            if (entity.isCrouching()) {
                renderOffsetY = entityScale * -2.0D / 16.0D;
            }
            preparePlayer(entity, clampedPartial, bodyYaw);
        } else if (kind == 2) {
            scale = entityScale * (entity.getType() == EntityType.CAVE_SPIDER ? CAVE_SPIDER_RENDER_SCALE : 1.0D);
            prepareSpider(entity, clampedPartial, bodyYaw);
        } else {
            scale = entityScale * VanillaModelHitboxRegistry.renderScale(entity);
            prepareGenerated(entity, clampedPartial, bodyYaw);
        }
        return true;
    }

    public static boolean supports(LivingEntity entity) {
        return modelKind(entity) != 0;
    }

    private static int modelKind(LivingEntity entity) {
        if (entity instanceof Player) {
            return 1;
        }
        if (entity instanceof Spider
                && (entity.getType() == EntityType.SPIDER || entity.getType() == EntityType.CAVE_SPIDER)) {
            return 2;
        }
        if (VanillaModelHitboxRegistry.supports(entity)) {
            return 3;
        }
        return 0;
    }


    private void prepareGenerated(LivingEntity entity, float partialTick, float bodyYaw) {
        ModelColliderPart[] generated = VanillaModelHitboxRegistry.parts(entity);
        partCount = Math.min(generated.length, MAX_PARTS);
        float limbSwing = entity.walkAnimation.position(partialTick);
        float limbAmount = Math.min(entity.walkAnimation.speed(partialTick), 1.0F);
        float headYaw = Mth.wrapDegrees(Mth.rotLerp(partialTick, entity.yHeadRotO, entity.yHeadRot) - bodyYaw)
                * Mth.DEG_TO_RAD;
        float headPitch = Mth.lerp(partialTick, entity.xRotO, entity.getXRot()) * Mth.DEG_TO_RAD;
        float age = entity.tickCount + partialTick;

        for (int index = 0; index < partCount; index++) {
            ModelColliderPart part = generated[index];
            float xRot = 0.0F;
            float yRot = 0.0F;
            float zRot = 0.0F;
            switch (part.bodyPart()) {
                case HEAD -> {
                    xRot = headPitch;
                    yRot = headYaw;
                }
                case RIGHT_ARM ->
                        xRot = Mth.cos(limbSwing * 0.6662F + (float) Math.PI) * limbAmount;
                case LEFT_ARM -> xRot = Mth.cos(limbSwing * 0.6662F) * limbAmount;
                case RIGHT_LEG -> xRot = Mth.cos(limbSwing * 0.6662F) * 1.4F * limbAmount;
                case LEFT_LEG ->
                        xRot = Mth.cos(limbSwing * 0.6662F + (float) Math.PI) * 1.4F * limbAmount;
                case CHEST -> {
                }
            }
            if (part.modelPartPath().contains("wing")) {
                xRot = 0.0F;
                zRot = Mth.cos(age * 0.9F) * 0.45F;
                if (part.modelPartPath().contains("right")) {
                    zRot = -zRot;
                }
            }
            set(index, part, part.pivotX(), part.pivotY(), part.pivotZ(), xRot, yRot, zRot);
        }
    }
    private void preparePlayer(LivingEntity entity, float partialTick, float bodyYaw) {
        partCount = PLAYER_PARTS.length;
        float limbSwing = entity.walkAnimation.position(partialTick);
        float limbAmount = Math.min(entity.walkAnimation.speed(partialTick), 1.0F);
        float headYaw = Mth.wrapDegrees(Mth.rotLerp(partialTick, entity.yHeadRotO, entity.yHeadRot) - bodyYaw)
                * Mth.DEG_TO_RAD;
        float headPitch = Mth.lerp(partialTick, entity.xRotO, entity.getXRot()) * Mth.DEG_TO_RAD;
        if (entity.getFallFlyingTicks() > 4) {
            headPitch = (float) (-Math.PI / 4.0D);
        }

        float movementDivisor = 1.0F;
        if (entity.getFallFlyingTicks() > 4) {
            movementDivisor = (float) entity.getDeltaMovement().lengthSqr() / 0.2F;
            movementDivisor *= movementDivisor * movementDivisor;
            movementDivisor = Math.max(movementDivisor, 1.0F);
        }

        float rightArmXRot = Mth.cos(limbSwing * 0.6662F + (float) Math.PI) * limbAmount / movementDivisor;
        float leftArmXRot = Mth.cos(limbSwing * 0.6662F) * limbAmount / movementDivisor;
        float rightLegXRot = Mth.cos(limbSwing * 0.6662F) * 1.4F * limbAmount / movementDivisor;
        float leftLegXRot = Mth.cos(limbSwing * 0.6662F + (float) Math.PI) * 1.4F * limbAmount / movementDivisor;
        float rightArmYRot = 0.0F;
        float leftArmYRot = 0.0F;
        float rightArmZRot = 0.0F;
        float leftArmZRot = 0.0F;
        float rightLegYRot = 0.005F;
        float leftLegYRot = -0.005F;
        float rightLegZRot = 0.005F;
        float leftLegZRot = -0.005F;

        if (entity.isPassenger()) {
            rightArmXRot -= (float) Math.PI / 5.0F;
            leftArmXRot -= (float) Math.PI / 5.0F;
            rightLegXRot = -1.4137167F;
            leftLegXRot = -1.4137167F;
            rightLegYRot = (float) Math.PI / 10.0F;
            leftLegYRot = (float) -Math.PI / 10.0F;
            rightLegZRot = 0.07853982F;
            leftLegZRot = -0.07853982F;
        }

        if (entity.isBlocking()) {
            HumanoidArm blockingArm = entity.getUsedItemHand() == InteractionHand.MAIN_HAND
                    ? entity.getMainArm()
                    : entity.getMainArm().getOpposite();
            float clampedHeadX = Mth.clamp(headPitch, (float) (-Math.PI * 4.0D / 9.0D), 0.43633232F);
            float clampedHeadY = Mth.clamp(headYaw, (float) -Math.PI / 6.0F, (float) Math.PI / 6.0F);
            if (blockingArm == HumanoidArm.RIGHT) {
                rightArmXRot = rightArmXRot * 0.5F - 0.9424779F + clampedHeadX;
                rightArmYRot = (float) -Math.PI / 6.0F + clampedHeadY;
            } else {
                leftArmXRot = leftArmXRot * 0.5F - 0.9424779F + clampedHeadX;
                leftArmYRot = (float) Math.PI / 6.0F + clampedHeadY;
            }
        }

        float bodyYRot = 0.0F;
        double rightArmPivotX = -5.0D / 16.0D;
        double leftArmPivotX = 5.0D / 16.0D;
        double rightArmPivotZ = 0.0D;
        double leftArmPivotZ = 0.0D;
        float attack = entity.getAttackAnim(partialTick);
        if (attack > 0.0F) {
            HumanoidArm attackArm = entity.swingingArm == InteractionHand.OFF_HAND
                    ? entity.getMainArm().getOpposite()
                    : entity.getMainArm();
            bodyYRot = Mth.sin(Mth.sqrt(attack) * (float) (Math.PI * 2.0D)) * 0.2F;
            if (attackArm == HumanoidArm.LEFT) {
                bodyYRot *= -1.0F;
            }
            rightArmPivotZ = Mth.sin(bodyYRot) * 5.0D / 16.0D;
            rightArmPivotX = -Mth.cos(bodyYRot) * 5.0D / 16.0D;
            leftArmPivotZ = -Mth.sin(bodyYRot) * 5.0D / 16.0D;
            leftArmPivotX = Mth.cos(bodyYRot) * 5.0D / 16.0D;
            rightArmYRot += bodyYRot;
            leftArmYRot += bodyYRot;
            leftArmXRot += bodyYRot;

            float eased = 1.0F - attack;
            eased *= eased;
            eased *= eased;
            eased = 1.0F - eased;
            float swing = Mth.sin(eased * (float) Math.PI);
            float headInfluence = Mth.sin(attack * (float) Math.PI) * -(headPitch - 0.7F) * 0.75F;
            if (attackArm == HumanoidArm.RIGHT) {
                rightArmXRot -= swing * 1.2F + headInfluence;
                rightArmYRot += bodyYRot * 2.0F;
                rightArmZRot += Mth.sin(attack * (float) Math.PI) * -0.4F;
            } else {
                leftArmXRot -= swing * 1.2F + headInfluence;
                leftArmYRot += bodyYRot * 2.0F;
                leftArmZRot += Mth.sin(attack * (float) Math.PI) * -0.4F;
            }
        }

        boolean crouching = entity.isCrouching();
        float bodyXRot = crouching ? 0.5F : 0.0F;
        double headPivotY = crouching ? 4.2D / 16.0D : 0.0D;
        double bodyPivotY = crouching ? 3.2D / 16.0D : 0.0D;
        double armPivotY = crouching ? 5.2D / 16.0D : 2.0D / 16.0D;
        double legPivotY = crouching ? 12.2D / 16.0D : 12.0D / 16.0D;
        double legPivotZ = crouching ? 4.0D / 16.0D : 0.0D;
        if (crouching) {
            rightArmXRot += 0.4F;
            leftArmXRot += 0.4F;
        }

        float age = entity.tickCount + partialTick;
        rightArmZRot += Mth.cos(age * 0.09F) * 0.05F + 0.05F;
        rightArmXRot += Mth.sin(age * 0.067F) * 0.05F;
        leftArmZRot -= Mth.cos(age * 0.09F) * 0.05F + 0.05F;
        leftArmXRot -= Mth.sin(age * 0.067F) * 0.05F;

        set(0, PLAYER_PARTS[0], 0.0D, headPivotY, 0.0D, headPitch, headYaw, 0.0F);
        set(1, PLAYER_PARTS[1], 0.0D, bodyPivotY, 0.0D, bodyXRot, bodyYRot, 0.0F);
        set(2, PLAYER_PARTS[2], rightArmPivotX, armPivotY, rightArmPivotZ,
                rightArmXRot, rightArmYRot, rightArmZRot);
        set(3, PLAYER_PARTS[3], leftArmPivotX, armPivotY, leftArmPivotZ,
                leftArmXRot, leftArmYRot, leftArmZRot);
        set(4, PLAYER_PARTS[4], -1.9D / 16.0D, legPivotY, legPivotZ,
                rightLegXRot, rightLegYRot, rightLegZRot);
        set(5, PLAYER_PARTS[5], 1.9D / 16.0D, legPivotY, legPivotZ,
                leftLegXRot, leftLegYRot, leftLegZRot);
    }

    private void prepareSpider(LivingEntity entity, float partialTick, float bodyYaw) {
        partCount = SPIDER_PARTS.length;
        float limbSwing = entity.walkAnimation.position(partialTick);
        float limbAmount = Math.min(entity.walkAnimation.speed(partialTick), 1.0F);
        float headYaw = Mth.wrapDegrees(Mth.rotLerp(partialTick, entity.yHeadRotO, entity.yHeadRot) - bodyYaw)
                * Mth.DEG_TO_RAD;
        float headPitch = Mth.lerp(partialTick, entity.xRotO, entity.getXRot()) * Mth.DEG_TO_RAD;

        set(0, SPIDER_PARTS[0], 0.0D, 15.0D / 16.0D, -3.0D / 16.0D, headPitch, headYaw, 0.0F);
        set(1, SPIDER_PARTS[1], 0.0D, 15.0D / 16.0D, 0.0D, 0.0F, 0.0F, 0.0F);
        set(2, SPIDER_PARTS[2], 0.0D, 15.0D / 16.0D, 9.0D / 16.0D, 0.0F, 0.0F, 0.0F);

        float[] baseY = {
                (float) Math.PI / 4.0F, (float) -Math.PI / 4.0F,
                (float) Math.PI / 8.0F, (float) -Math.PI / 8.0F,
                (float) -Math.PI / 8.0F, (float) Math.PI / 8.0F,
                (float) -Math.PI / 4.0F, (float) Math.PI / 4.0F
        };
        float[] baseZ = {
                (float) -Math.PI / 4.0F, (float) Math.PI / 4.0F,
                -0.58119464F, 0.58119464F, -0.58119464F, 0.58119464F,
                (float) -Math.PI / 4.0F, (float) Math.PI / 4.0F
        };
        float phase = limbSwing * 0.6662F;
        float[] yMotion = {
                -Mth.cos(phase * 2.0F) * 0.4F * limbAmount,
                Mth.cos(phase * 2.0F) * 0.4F * limbAmount,
                -Mth.cos(phase * 2.0F + (float) Math.PI) * 0.4F * limbAmount,
                Mth.cos(phase * 2.0F + (float) Math.PI) * 0.4F * limbAmount,
                -Mth.cos(phase * 2.0F + (float) Math.PI / 2.0F) * 0.4F * limbAmount,
                Mth.cos(phase * 2.0F + (float) Math.PI / 2.0F) * 0.4F * limbAmount,
                -Mth.cos(phase * 2.0F + (float) Math.PI * 1.5F) * 0.4F * limbAmount,
                Mth.cos(phase * 2.0F + (float) Math.PI * 1.5F) * 0.4F * limbAmount
        };
        float[] zMotion = {
                Math.abs(Mth.sin(phase) * 0.4F) * limbAmount,
                -Math.abs(Mth.sin(phase) * 0.4F) * limbAmount,
                Math.abs(Mth.sin(phase + (float) Math.PI) * 0.4F) * limbAmount,
                -Math.abs(Mth.sin(phase + (float) Math.PI) * 0.4F) * limbAmount,
                Math.abs(Mth.sin(phase + (float) Math.PI / 2.0F) * 0.4F) * limbAmount,
                -Math.abs(Mth.sin(phase + (float) Math.PI / 2.0F) * 0.4F) * limbAmount,
                Math.abs(Mth.sin(phase + (float) Math.PI * 1.5F) * 0.4F) * limbAmount,
                -Math.abs(Mth.sin(phase + (float) Math.PI * 1.5F) * 0.4F) * limbAmount
        };
        double[] pivotZ = {2, 2, 1, 1, 0, 0, -1, -1};
        for (int index = 0; index < 8; index++) {
            double pivotX = (index & 1) == 0 ? -4.0D / 16.0D : 4.0D / 16.0D;
            set(index + 3, SPIDER_PARTS[index + 3], pivotX, 15.0D / 16.0D, pivotZ[index] / 16.0D,
                    0.0F, baseY[index] + yMotion[index], baseZ[index] + zMotion[index]);
        }
    }

    private void set(
            int index,
            ModelColliderPart part,
            double pivotX,
            double pivotY,
            double pivotZ,
            float xRot,
            float yRot,
            float zRot) {
        parts[index] = part;
        double sx = Math.sin(xRot);
        double cx = Math.cos(xRot);
        double sy = Math.sin(yRot);
        double cy = Math.cos(yRot);
        double sz = Math.sin(zRot);
        double cz = Math.cos(zRot);

        double m00 = cz * cy;
        double m01 = cz * sy * sx - sz * cx;
        double m02 = cz * sy * cx + sz * sx;
        double m10 = sz * cy;
        double m11 = sz * sy * sx + cz * cx;
        double m12 = sz * sy * cx - cz * sx;
        double m20 = -sy;
        double m21 = cy * sx;
        double m22 = cy * cx;

        originX[index] = scale * (rootCos * pivotX + rootSin * pivotZ);
        originY[index] = renderOffsetY + scale * (MODEL_ROOT_Y - pivotY);
        originZ[index] = scale * (rootSin * pivotX - rootCos * pivotZ);

        axisXX[index] = rootCos * m00 + rootSin * m20;
        axisXY[index] = -m10;
        axisXZ[index] = rootSin * m00 - rootCos * m20;
        axisYX[index] = rootCos * m01 + rootSin * m21;
        axisYY[index] = -m11;
        axisYZ[index] = rootSin * m01 - rootCos * m21;
        axisZX[index] = rootCos * m02 + rootSin * m22;
        axisZY[index] = -m12;
        axisZZ[index] = rootSin * m02 - rootCos * m22;
    }

    public int partCount() { return partCount; }
    public ModelColliderPart part(int index) { return parts[index]; }
    public double scale() { return scale; }
    public double originX(int index) { return originX[index]; }
    public double originY(int index) { return originY[index]; }
    public double originZ(int index) { return originZ[index]; }
    public double axisXX(int index) { return axisXX[index]; }
    public double axisXY(int index) { return axisXY[index]; }
    public double axisXZ(int index) { return axisXZ[index]; }
    public double axisYX(int index) { return axisYX[index]; }
    public double axisYY(int index) { return axisYY[index]; }
    public double axisYZ(int index) { return axisYZ[index]; }
    public double axisZX(int index) { return axisZX[index]; }
    public double axisZY(int index) { return axisZY[index]; }
    public double axisZZ(int index) { return axisZZ[index]; }
}
