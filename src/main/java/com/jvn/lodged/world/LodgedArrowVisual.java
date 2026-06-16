package com.jvn.lodged.world;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.phys.Vec3;

public record LodgedArrowVisual(float modelX, float modelY, float modelZ, float directionX, float directionY, float directionZ) {
    private static final String MODEL_X_KEY = "visual_model_x";
    private static final String MODEL_Y_KEY = "visual_model_y";
    private static final String MODEL_Z_KEY = "visual_model_z";
    private static final String DIRECTION_X_KEY = "visual_direction_x";
    private static final String DIRECTION_Y_KEY = "visual_direction_y";
    private static final String DIRECTION_Z_KEY = "visual_direction_z";
    private static final float MODEL_HEIGHT = 24.0F / 16.0F;
    private static final float MODEL_HALF_WIDTH = 6.0F / 16.0F;
    private static final float MODEL_HALF_DEPTH = 5.0F / 16.0F;
    private static final float HEAD_MAX_MODEL_Y = 0.42F;
    private static final float LEG_MIN_MODEL_Y = 0.95F;
    private static final float ARM_MIN_ABS_MODEL_X = 0.27F;
    private static final float MIN_DIRECTION_LENGTH = 1.0E-4F;

    public static final LodgedArrowVisual DEFAULT = new LodgedArrowVisual(
            0.0F,
            MODEL_HEIGHT * 0.5F,
            -MODEL_HALF_DEPTH,
            0.0F,
            0.0F,
            1.0F);

    public static final StreamCodec<RegistryFriendlyByteBuf, LodgedArrowVisual> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.FLOAT,
            LodgedArrowVisual::modelX,
            ByteBufCodecs.FLOAT,
            LodgedArrowVisual::modelY,
            ByteBufCodecs.FLOAT,
            LodgedArrowVisual::modelZ,
            ByteBufCodecs.FLOAT,
            LodgedArrowVisual::directionX,
            ByteBufCodecs.FLOAT,
            LodgedArrowVisual::directionY,
            ByteBufCodecs.FLOAT,
            LodgedArrowVisual::directionZ,
            LodgedArrowVisual::new);

    public static LodgedArrowVisual fromImpact(LivingEntity target, AbstractArrow arrow, Vec3 hitLocation) {
        BodyAxes axes = BodyAxes.of(target.yBodyRot);
        Vec3 relativeHit = hitLocation.subtract(target.getX(), target.getY(), target.getZ());
        float halfWidth = Math.max(target.getBbWidth() * 0.5F, 0.1F);
        float modelX = (float) Mth.clamp(
                -relativeHit.dot(axes.right()) / halfWidth * MODEL_HALF_WIDTH,
                -MODEL_HALF_WIDTH,
                MODEL_HALF_WIDTH);
        float modelY = (float) Mth.clamp(
                MODEL_HEIGHT - (relativeHit.y / Math.max(target.getBbHeight(), 0.1F)) * MODEL_HEIGHT,
                0.05F,
                MODEL_HEIGHT - 0.05F);
        float modelZ = (float) Mth.clamp(
                -relativeHit.dot(axes.forward()) / halfWidth * MODEL_HALF_DEPTH,
                -MODEL_HALF_DEPTH,
                MODEL_HALF_DEPTH);

        Vec3 outward = arrow.getDeltaMovement();
        if (outward.lengthSqr() < MIN_DIRECTION_LENGTH) {
            outward = Vec3.directionFromRotation(arrow.getXRot(), arrow.getYRot());
        }
        outward = outward.normalize().scale(-1.0D);

        float directionX = (float) -outward.dot(axes.right());
        float directionY = (float) -outward.y;
        float directionZ = (float) -outward.dot(axes.forward());
        float directionLength = Mth.sqrt(directionX * directionX + directionY * directionY + directionZ * directionZ);
        if (directionLength < MIN_DIRECTION_LENGTH) {
            return new LodgedArrowVisual(modelX, modelY, modelZ, DEFAULT.directionX, DEFAULT.directionY, DEFAULT.directionZ);
        }

        return new LodgedArrowVisual(
                modelX,
                modelY,
                modelZ,
                directionX / directionLength,
                directionY / directionLength,
                directionZ / directionLength);
    }

    public LodgedArrowBodyPart bodyPart() {
        if (modelY <= HEAD_MAX_MODEL_Y) {
            return LodgedArrowBodyPart.HEAD;
        }

        if (modelY >= LEG_MIN_MODEL_Y) {
            return LodgedArrowBodyPart.LEG;
        }

        if (Math.abs(modelX) >= ARM_MIN_ABS_MODEL_X) {
            return LodgedArrowBodyPart.ARM;
        }

        return LodgedArrowBodyPart.CHEST;
    }

    void save(CompoundTag tag) {
        tag.putFloat(MODEL_X_KEY, modelX);
        tag.putFloat(MODEL_Y_KEY, modelY);
        tag.putFloat(MODEL_Z_KEY, modelZ);
        tag.putFloat(DIRECTION_X_KEY, directionX);
        tag.putFloat(DIRECTION_Y_KEY, directionY);
        tag.putFloat(DIRECTION_Z_KEY, directionZ);
    }

    static LodgedArrowVisual load(CompoundTag tag) {
        if (!tag.contains(MODEL_X_KEY, Tag.TAG_ANY_NUMERIC)
                || !tag.contains(MODEL_Y_KEY, Tag.TAG_ANY_NUMERIC)
                || !tag.contains(MODEL_Z_KEY, Tag.TAG_ANY_NUMERIC)
                || !tag.contains(DIRECTION_X_KEY, Tag.TAG_ANY_NUMERIC)
                || !tag.contains(DIRECTION_Y_KEY, Tag.TAG_ANY_NUMERIC)
                || !tag.contains(DIRECTION_Z_KEY, Tag.TAG_ANY_NUMERIC)) {
            return DEFAULT;
        }

        return new LodgedArrowVisual(
                tag.getFloat(MODEL_X_KEY),
                tag.getFloat(MODEL_Y_KEY),
                tag.getFloat(MODEL_Z_KEY),
                tag.getFloat(DIRECTION_X_KEY),
                tag.getFloat(DIRECTION_Y_KEY),
                tag.getFloat(DIRECTION_Z_KEY));
    }

    private record BodyAxes(Vec3 right, Vec3 forward) {
        static BodyAxes of(float bodyYaw) {
            float yawRadians = bodyYaw * Mth.DEG_TO_RAD;
            Vec3 right = new Vec3(Math.cos(yawRadians), 0.0D, Math.sin(yawRadians));
            Vec3 forward = new Vec3(-Math.sin(yawRadians), 0.0D, Math.cos(yawRadians));
            return new BodyAxes(right, forward);
        }
    }
}
