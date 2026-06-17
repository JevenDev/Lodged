package com.jvn.lodged.world;

import java.util.Optional;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

public record LodgedArrowVisual(float modelX, float modelY, float modelZ, float directionX, float directionY, float directionZ) {
    private static final String MODEL_X_KEY = "visual_model_x";
    private static final String MODEL_Y_KEY = "visual_model_y";
    private static final String MODEL_Z_KEY = "visual_model_z";
    private static final String DIRECTION_X_KEY = "visual_direction_x";
    private static final String DIRECTION_Y_KEY = "visual_direction_y";
    private static final String DIRECTION_Z_KEY = "visual_direction_z";
    private static final float MODEL_HEAD_TOP = -8.0F / 16.0F;
    private static final float MODEL_HEAD_BOTTOM = 0.0F;
    private static final float MODEL_LEG_TOP = 12.0F / 16.0F;
    private static final float MODEL_FEET_Y = 24.0F / 16.0F;
    private static final float MODEL_HEAD_HALF_WIDTH = 4.0F / 16.0F;
    private static final float MODEL_BODY_HALF_WIDTH = 4.0F / 16.0F;
    private static final float MODEL_ARM_OUTER_X = 8.0F / 16.0F;
    private static final float MODEL_LIMB_HALF_DEPTH = 2.0F / 16.0F;
    private static final float PLAYER_MODEL_RENDER_SCALE = 0.9375F;
    private static final float MODEL_RENDER_ROOT_Y = 1.501F;
    private static final ModelBox[] HUMANOID_MODEL_BOXES = {
            new ModelBox(-MODEL_HEAD_HALF_WIDTH, MODEL_HEAD_TOP, -MODEL_HEAD_HALF_WIDTH, MODEL_HEAD_HALF_WIDTH, MODEL_HEAD_BOTTOM, MODEL_HEAD_HALF_WIDTH),
            new ModelBox(-MODEL_BODY_HALF_WIDTH, MODEL_HEAD_BOTTOM, -MODEL_LIMB_HALF_DEPTH, MODEL_BODY_HALF_WIDTH, MODEL_LEG_TOP, MODEL_LIMB_HALF_DEPTH),
            new ModelBox(-MODEL_ARM_OUTER_X, MODEL_HEAD_BOTTOM, -MODEL_LIMB_HALF_DEPTH, -MODEL_BODY_HALF_WIDTH, MODEL_LEG_TOP, MODEL_LIMB_HALF_DEPTH),
            new ModelBox(MODEL_BODY_HALF_WIDTH, MODEL_HEAD_BOTTOM, -MODEL_LIMB_HALF_DEPTH, MODEL_ARM_OUTER_X, MODEL_LEG_TOP, MODEL_LIMB_HALF_DEPTH),
            new ModelBox((-1.9F - 2.0F) / 16.0F, MODEL_LEG_TOP, -MODEL_LIMB_HALF_DEPTH, (-1.9F + 2.0F) / 16.0F, MODEL_FEET_Y, MODEL_LIMB_HALF_DEPTH),
            new ModelBox((1.9F - 2.0F) / 16.0F, MODEL_LEG_TOP, -MODEL_LIMB_HALF_DEPTH, (1.9F + 2.0F) / 16.0F, MODEL_FEET_Y, MODEL_LIMB_HALF_DEPTH)
    };
    private static final float MIN_DIRECTION_LENGTH = 1.0E-4F;
    private static final double VANILLA_ENTITY_PICK_MARGIN = 0.3D;

    public static final LodgedArrowVisual DEFAULT = new LodgedArrowVisual(
            0.0F,
            (MODEL_HEAD_BOTTOM + MODEL_LEG_TOP) * 0.5F,
            -MODEL_LIMB_HALF_DEPTH,
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

    public static LodgedArrowVisual fromImpact(LivingEntity target, AbstractArrow arrow, EntityHitResult hitResult) {
        BodyAxes axes = BodyAxes.of(target.yBodyRot);
        Vec3 motion = impactMotion(arrow);
        AABB boundingBox = target.getBoundingBox();
        TraceSegment trace = impactTrace(arrow, boundingBox, motion);
        Vec3 modelStart = toModelSpace(target, boundingBox, axes, trace.start());
        Vec3 modelEnd = toModelSpace(target, boundingBox, axes, trace.end());
        ModelHit modelHit = traceModelHit(modelStart, modelEnd)
                .orElseGet(() -> fallbackModelHit(target, arrow, hitResult, motion, boundingBox, axes, modelStart, modelEnd));
        Vec3 modelPosition = modelHit.position();
        float modelX = (float) modelPosition.x;
        float modelY = (float) modelPosition.y;
        float modelZ = (float) modelPosition.z;

        Vec3 modelDirection = modelHit.direction();
        float directionX = (float) modelDirection.x;
        float directionY = (float) modelDirection.y;
        float directionZ = (float) modelDirection.z;
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
        if (modelY < MODEL_HEAD_BOTTOM) {
            return LodgedArrowBodyPart.HEAD;
        }

        if (modelY >= MODEL_LEG_TOP) {
            return LodgedArrowBodyPart.LEG;
        }

        if (Math.abs(modelX) > MODEL_BODY_HALF_WIDTH) {
            return LodgedArrowBodyPart.ARM;
        }

        return LodgedArrowBodyPart.CHEST;
    }

    public Vec3 toEntityLocalPosition(LivingEntity target) {
        double modelScale = Math.max(PLAYER_MODEL_RENDER_SCALE * target.getScale(), 0.1D);
        return new Vec3(
                modelX * modelScale,
                MODEL_RENDER_ROOT_Y - (modelY * modelScale),
                -modelZ * modelScale);
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

    private static Vec3 impactMotion(AbstractArrow arrow) {
        Vec3 motion = arrow.getDeltaMovement();
        if (motion.lengthSqr() >= MIN_DIRECTION_LENGTH) {
            return motion;
        }

        Vec3 rotationMotion = Vec3.directionFromRotation(arrow.getXRot(), arrow.getYRot());
        if (rotationMotion.lengthSqr() >= MIN_DIRECTION_LENGTH) {
            return rotationMotion;
        }

        return new Vec3(DEFAULT.directionX, DEFAULT.directionY, DEFAULT.directionZ);
    }

    private static TraceSegment impactTrace(AbstractArrow arrow, AABB boundingBox, Vec3 motion) {
        Vec3 direction = normalizedOrDefault(motion);
        double padding = Math.max(boundingBox.getSize(), 1.0D);
        double forwardLength = Math.sqrt(Math.max(motion.lengthSqr(), MIN_DIRECTION_LENGTH)) + padding;
        Vec3 arrowPosition = arrow.position();
        return new TraceSegment(
                arrowPosition.subtract(direction.scale(padding)),
                arrowPosition.add(direction.scale(forwardLength)));
    }

    private static Vec3 toModelSpace(LivingEntity target, AABB boundingBox, BodyAxes axes, Vec3 worldPosition) {
        double modelScale = Math.max(PLAYER_MODEL_RENDER_SCALE * target.getScale(), 0.1D);
        Vec3 relative = worldPosition.subtract(target.getX(), boundingBox.minY, target.getZ());
        return new Vec3(
                relative.dot(axes.right()) / modelScale,
                MODEL_RENDER_ROOT_Y - (relative.y / modelScale),
                -relative.dot(axes.forward()) / modelScale);
    }

    private static Optional<ModelHit> traceModelHit(Vec3 modelStart, Vec3 modelEnd) {
        Vec3 modelDelta = modelEnd.subtract(modelStart);
        Vec3 modelDirection = normalizedOrDefault(modelDelta);
        ModelHit closestHit = null;
        double closestT = Double.MAX_VALUE;
        for (ModelBox box : HUMANOID_MODEL_BOXES) {
            Optional<Double> hitT = box.intersect(modelStart, modelDelta);
            if (hitT.isPresent() && hitT.get() < closestT) {
                closestT = hitT.get();
                closestHit = new ModelHit(modelStart.add(modelDelta.scale(hitT.get())), modelDirection);
            }
        }

        return Optional.ofNullable(closestHit);
    }

    private static ModelHit fallbackModelHit(
            LivingEntity target,
            AbstractArrow arrow,
            EntityHitResult hitResult,
            Vec3 motion,
            AABB boundingBox,
            BodyAxes axes,
            Vec3 modelStart,
            Vec3 modelEnd) {
        Vec3 hitLocation = resolveImpactLocation(target, arrow, hitResult, motion);
        Vec3 modelPosition = snapToHumanoidSurface(toModelSpace(target, boundingBox, axes, hitLocation));
        return new ModelHit(modelPosition, normalizedOrDefault(modelEnd.subtract(modelStart)));
    }

    private static Vec3 normalizedOrDefault(Vec3 vector) {
        if (vector.lengthSqr() < MIN_DIRECTION_LENGTH) {
            return new Vec3(DEFAULT.directionX, DEFAULT.directionY, DEFAULT.directionZ);
        }

        return vector.normalize();
    }

    private static Vec3 resolveImpactLocation(
            LivingEntity target,
            AbstractArrow arrow,
            EntityHitResult hitResult,
            Vec3 motion) {
        AABB boundingBox = target.getBoundingBox();
        TraceSegment trace = impactTrace(arrow, boundingBox, motion);

        return boundingBox.clip(trace.start(), trace.end())
                .or(() -> boundingBox.inflate(VANILLA_ENTITY_PICK_MARGIN).clip(trace.start(), trace.end())
                        .map(inflatedHit -> closestPoint(boundingBox, inflatedHit)))
                .or(() -> usableReportedImpact(boundingBox, hitResult.getLocation(), trace.start())
                        ? Optional.of(hitResult.getLocation())
                        : Optional.empty())
                .orElseGet(() -> closestPoint(boundingBox, trace.end()));
    }

    private static boolean usableReportedImpact(AABB boundingBox, Vec3 reportedImpact, Vec3 arrowPosition) {
        if (!boundingBox.inflate(VANILLA_ENTITY_PICK_MARGIN).contains(reportedImpact)) {
            return false;
        }

        double reportedY = reportedImpact.y - boundingBox.minY;
        double arrowY = arrowPosition.y - boundingBox.minY;
        double footThreshold = Math.max(0.1D, boundingBox.getYsize() * 0.12D);
        return reportedY > footThreshold || arrowY <= footThreshold;
    }

    private static Vec3 closestPoint(AABB boundingBox, Vec3 point) {
        return new Vec3(
                Mth.clamp(point.x, boundingBox.minX, boundingBox.maxX),
                Mth.clamp(point.y, boundingBox.minY, boundingBox.maxY),
                Mth.clamp(point.z, boundingBox.minZ, boundingBox.maxZ));
    }

    private static Vec3 snapToHumanoidSurface(Vec3 modelPosition) {
        ModelBox closestBox = HUMANOID_MODEL_BOXES[0];
        double closestDistance = closestBox.distanceToSqr(modelPosition);
        for (int index = 1; index < HUMANOID_MODEL_BOXES.length; index++) {
            ModelBox box = HUMANOID_MODEL_BOXES[index];
            double distance = box.distanceToSqr(modelPosition);
            if (distance < closestDistance) {
                closestBox = box;
                closestDistance = distance;
            }
        }

        return closestBox.clampToSurface(modelPosition);
    }

    private record BodyAxes(Vec3 right, Vec3 forward) {
        static BodyAxes of(float bodyYaw) {
            float yawRadians = bodyYaw * Mth.DEG_TO_RAD;
            Vec3 right = new Vec3(Math.cos(yawRadians), 0.0D, Math.sin(yawRadians));
            Vec3 forward = new Vec3(-Math.sin(yawRadians), 0.0D, Math.cos(yawRadians));
            return new BodyAxes(right, forward);
        }
    }

    private record ModelBox(float minX, float minY, float minZ, float maxX, float maxY, float maxZ) {
        Optional<Double> intersect(Vec3 start, Vec3 delta) {
            double tMin = 0.0D;
            double tMax = 1.0D;

            AxisIntersection x = intersectAxis(start.x, delta.x, minX, maxX, tMin, tMax);
            if (!x.intersects()) {
                return Optional.empty();
            }
            tMin = x.tMin();
            tMax = x.tMax();

            AxisIntersection y = intersectAxis(start.y, delta.y, minY, maxY, tMin, tMax);
            if (!y.intersects()) {
                return Optional.empty();
            }
            tMin = y.tMin();
            tMax = y.tMax();

            AxisIntersection z = intersectAxis(start.z, delta.z, minZ, maxZ, tMin, tMax);
            if (!z.intersects()) {
                return Optional.empty();
            }
            tMin = z.tMin();
            tMax = z.tMax();

            return Optional.of(contains(start) ? tMax : tMin);
        }

        double distanceToSqr(Vec3 point) {
            double x = Mth.clamp(point.x, minX, maxX);
            double y = Mth.clamp(point.y, minY, maxY);
            double z = Mth.clamp(point.z, minZ, maxZ);
            double dx = point.x - x;
            double dy = point.y - y;
            double dz = point.z - z;
            return dx * dx + dy * dy + dz * dz;
        }

        Vec3 clampToSurface(Vec3 point) {
            double x = Mth.clamp(point.x, minX, maxX);
            double y = Mth.clamp(point.y, minY, maxY);
            double z = Mth.clamp(point.z, minZ, maxZ);
            Axis axis = surfaceAxis(point);
            return switch (axis) {
                case X -> new Vec3(point.x < centerX() ? minX : maxX, y, z);
                case Y -> new Vec3(x, point.y < centerY() ? minY : maxY, z);
                case Z -> new Vec3(x, y, point.z < centerZ() ? minZ : maxZ);
            };
        }

        private boolean contains(Vec3 point) {
            return point.x >= minX
                    && point.x <= maxX
                    && point.y >= minY
                    && point.y <= maxY
                    && point.z >= minZ
                    && point.z <= maxZ;
        }

        private Axis surfaceAxis(Vec3 point) {
            double outsideX = outsideDistance(point.x, minX, maxX);
            double outsideY = outsideDistance(point.y, minY, maxY);
            double outsideZ = outsideDistance(point.z, minZ, maxZ);
            if (outsideX > 0.0D || outsideY > 0.0D || outsideZ > 0.0D) {
                if (outsideX >= outsideY && outsideX >= outsideZ) {
                    return Axis.X;
                }
                return outsideY >= outsideZ ? Axis.Y : Axis.Z;
            }

            double nearestX = Math.min(point.x - minX, maxX - point.x);
            double nearestY = Math.min(point.y - minY, maxY - point.y);
            double nearestZ = Math.min(point.z - minZ, maxZ - point.z);
            if (nearestX <= nearestY && nearestX <= nearestZ) {
                return Axis.X;
            }
            return nearestY <= nearestZ ? Axis.Y : Axis.Z;
        }

        private double centerX() {
            return (minX + maxX) * 0.5D;
        }

        private double centerY() {
            return (minY + maxY) * 0.5D;
        }

        private double centerZ() {
            return (minZ + maxZ) * 0.5D;
        }

        private static double outsideDistance(double value, double min, double max) {
            if (value < min) {
                return min - value;
            }
            return value > max ? value - max : 0.0D;
        }
    }

    private static AxisIntersection intersectAxis(double start, double delta, double min, double max, double tMin, double tMax) {
        if (Math.abs(delta) < 1.0E-7D) {
            return new AxisIntersection(start >= min && start <= max, tMin, tMax);
        }

        double invDelta = 1.0D / delta;
        double near = (min - start) * invDelta;
        double far = (max - start) * invDelta;
        if (near > far) {
            double swap = near;
            near = far;
            far = swap;
        }

        double nextTMin = Math.max(tMin, near);
        double nextTMax = Math.min(tMax, far);
        return new AxisIntersection(nextTMin <= nextTMax, nextTMin, nextTMax);
    }

    private record TraceSegment(Vec3 start, Vec3 end) {
    }

    private record ModelHit(Vec3 position, Vec3 direction) {
    }

    private record AxisIntersection(boolean intersects, double tMin, double tMax) {
    }

    private enum Axis {
        X,
        Y,
        Z
    }
}
