package com.jvn.lodged.world;

import java.util.Optional;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

public record LodgedArrowVisual(
        float modelX,
        float modelY,
        float modelZ,
        float directionX,
        float directionY,
        float directionZ,
        LodgedArrowBodyPart bodyPart,
        LodgedArrowDepth depth,
        ItemStack stack) {
    private static final String MODEL_X_KEY = "visual_model_x";
    private static final String MODEL_Y_KEY = "visual_model_y";
    private static final String MODEL_Z_KEY = "visual_model_z";
    private static final String DIRECTION_X_KEY = "visual_direction_x";
    private static final String DIRECTION_Y_KEY = "visual_direction_y";
    private static final String DIRECTION_Z_KEY = "visual_direction_z";
    private static final String BODY_PART_KEY = "visual_body_part";
    private static final String DEPTH_KEY = "visual_depth";
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
    private static final double MAX_MODEL_TRACE_INFLATE = 1.0D / 16.0D;
    private static final double SHIELD_CENTER_HEIGHT_RATIO = 0.55D;
    private static final double SHIELD_CENTER_FORWARD_OFFSET = 0.35D;
    private static final double SHIELD_HALF_WIDTH = 6.0D / 16.0D;
    private static final double SHIELD_HALF_HEIGHT = 11.0D / 16.0D;
    private static final double SHIELD_FRONT_Z = -2.0D / 16.0D;
    private static final BodyPartBox[] HUMANOID_MODEL_BOXES = {
            new BodyPartBox(LodgedArrowBodyPart.HEAD,
                    new ModelBox(-MODEL_HEAD_HALF_WIDTH, MODEL_HEAD_TOP, -MODEL_HEAD_HALF_WIDTH,
                            MODEL_HEAD_HALF_WIDTH, MODEL_HEAD_BOTTOM, MODEL_HEAD_HALF_WIDTH)),
            new BodyPartBox(LodgedArrowBodyPart.CHEST,
                    new ModelBox(-MODEL_BODY_HALF_WIDTH, MODEL_HEAD_BOTTOM, -MODEL_LIMB_HALF_DEPTH,
                            MODEL_BODY_HALF_WIDTH, MODEL_LEG_TOP, MODEL_LIMB_HALF_DEPTH)),
            new BodyPartBox(LodgedArrowBodyPart.RIGHT_ARM,
                    new ModelBox(-MODEL_ARM_OUTER_X, MODEL_HEAD_BOTTOM, -MODEL_LIMB_HALF_DEPTH,
                            -MODEL_BODY_HALF_WIDTH, MODEL_LEG_TOP, MODEL_LIMB_HALF_DEPTH)),
            new BodyPartBox(LodgedArrowBodyPart.LEFT_ARM,
                    new ModelBox(MODEL_BODY_HALF_WIDTH, MODEL_HEAD_BOTTOM, -MODEL_LIMB_HALF_DEPTH,
                            MODEL_ARM_OUTER_X, MODEL_LEG_TOP, MODEL_LIMB_HALF_DEPTH)),
            new BodyPartBox(LodgedArrowBodyPart.RIGHT_LEG,
                    new ModelBox((-1.9F - 2.0F) / 16.0F, MODEL_LEG_TOP, -MODEL_LIMB_HALF_DEPTH,
                            (-1.9F + 2.0F) / 16.0F, MODEL_FEET_Y, MODEL_LIMB_HALF_DEPTH)),
            new BodyPartBox(LodgedArrowBodyPart.LEFT_LEG,
                    new ModelBox((1.9F - 2.0F) / 16.0F, MODEL_LEG_TOP, -MODEL_LIMB_HALF_DEPTH,
                            (1.9F + 2.0F) / 16.0F, MODEL_FEET_Y, MODEL_LIMB_HALF_DEPTH))
    };
    private static final float MIN_DIRECTION_LENGTH = 1.0E-4F;
    private static final double MODEL_HIT_TIE_EPSILON = 1.0E-7D;
    private static final double VANILLA_ENTITY_PICK_MARGIN = 0.3D;

    public static final LodgedArrowVisual DEFAULT = new LodgedArrowVisual(
            0.0F,
            (MODEL_HEAD_BOTTOM + MODEL_LEG_TOP) * 0.5F,
            -MODEL_LIMB_HALF_DEPTH,
            0.0F,
            0.0F,
            1.0F);

    public static final StreamCodec<RegistryFriendlyByteBuf, LodgedArrowVisual> STREAM_CODEC = StreamCodec.ofMember(
            LodgedArrowVisual::encode,
            LodgedArrowVisual::decode);

    public LodgedArrowVisual {
        modelX = finiteOrDefault(modelX, 0.0F);
        modelY = finiteOrDefault(modelY, (MODEL_HEAD_BOTTOM + MODEL_LEG_TOP) * 0.5F);
        modelZ = finiteOrDefault(modelZ, -MODEL_LIMB_HALF_DEPTH);
        directionX = finiteOrDefault(directionX, 0.0F);
        directionY = finiteOrDefault(directionY, 0.0F);
        directionZ = finiteOrDefault(directionZ, 1.0F);
        float directionLength = Mth.sqrt(
                directionX * directionX + directionY * directionY + directionZ * directionZ);
        if (directionLength < MIN_DIRECTION_LENGTH) {
            directionX = 0.0F;
            directionY = 0.0F;
            directionZ = 1.0F;
        } else {
            directionX /= directionLength;
            directionY /= directionLength;
            directionZ /= directionLength;
        }
        bodyPart = bodyPart == null ? classifyBodyPart(modelX, modelY) : bodyPart;
        depth = depth == null ? LodgedArrowDepth.LODGED : depth;
        stack = renderStack(stack);
    }

    public LodgedArrowVisual(
            float modelX,
            float modelY,
            float modelZ,
            float directionX,
            float directionY,
            float directionZ) {
        this(modelX, modelY, modelZ, directionX, directionY, directionZ, null,
                LodgedArrowDepth.LODGED, new ItemStack(Items.ARROW));
    }

    public LodgedArrowVisual(
            float modelX,
            float modelY,
            float modelZ,
            float directionX,
            float directionY,
            float directionZ,
            LodgedArrowDepth depth,
            ItemStack stack) {
        this(modelX, modelY, modelZ, directionX, directionY, directionZ, null, depth, stack);
    }

    public LodgedArrowVisual withStack(ItemStack stack) {
        return new LodgedArrowVisual(
                modelX, modelY, modelZ, directionX, directionY, directionZ, bodyPart, depth, stack);
    }

    public LodgedArrowVisual withDepth(LodgedArrowDepth depth) {
        return new LodgedArrowVisual(
                modelX, modelY, modelZ, directionX, directionY, directionZ, bodyPart, depth, stack);
    }

    public boolean matches(LodgedArrowVisual other) {
        return other != null
                && Float.compare(modelX, other.modelX) == 0
                && Float.compare(modelY, other.modelY) == 0
                && Float.compare(modelZ, other.modelZ) == 0
                && Float.compare(directionX, other.directionX) == 0
                && Float.compare(directionY, other.directionY) == 0
                && Float.compare(directionZ, other.directionZ) == 0
                && bodyPart == other.bodyPart
                && depth == other.depth
                && ItemStack.isSameItemSameComponents(stack, other.stack);
    }

    public static LodgedArrowVisual fromImpact(LivingEntity target, AbstractArrow arrow, EntityHitResult hitResult) {
        BodyAxes axes = BodyAxes.of(target.yBodyRot);
        Vec3 motion = impactMotion(arrow);
        AABB boundingBox = target.getBoundingBox();
        double modelScale = modelRenderScale(target);
        TraceSegment trace = impactTrace(arrow, boundingBox, motion);
        Vec3 modelStart = toModelSpace(target, boundingBox, axes, trace.start(), modelScale);
        Vec3 modelEnd = toModelSpace(target, boundingBox, axes, trace.end(), modelScale);
        double modelPickMargin = Math.min(VANILLA_ENTITY_PICK_MARGIN / modelScale, MAX_MODEL_TRACE_INFLATE);
        ModelHit modelHit = traceModelHit(modelStart, modelEnd)
                .or(() -> traceModelHit(modelStart, modelEnd, modelPickMargin))
                .orElseGet(() -> fallbackModelHit(target, arrow, hitResult, motion, boundingBox, axes, modelScale, modelStart, modelEnd));
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
            return new LodgedArrowVisual(
                    modelX, modelY, modelZ,
                    DEFAULT.directionX, DEFAULT.directionY, DEFAULT.directionZ,
                    modelHit.bodyPart(), LodgedArrowDepth.LODGED, new ItemStack(Items.ARROW));
        }

        return new LodgedArrowVisual(
                modelX,
                modelY,
                modelZ,
                directionX / directionLength,
                directionY / directionLength,
                directionZ / directionLength,
                modelHit.bodyPart(),
                LodgedArrowDepth.LODGED,
                new ItemStack(Items.ARROW));
    }

    public static LodgedArrowVisual fromShieldImpact(LivingEntity target, AbstractArrow arrow, EntityHitResult hitResult) {
        BodyAxes axes = BodyAxes.of(target.getYHeadRot());
        Vec3 motion = impactMotion(arrow);
        Vec3 modelDirection = normalizedOrDefault(new Vec3(
                motion.dot(axes.right()),
                -motion.y,
                -motion.dot(axes.forward())));
        Vec3 center = shieldCenter(target, axes);
        TraceSegment trace = impactTrace(arrow, target.getBoundingBox(), motion);
        Vec3 hitLocation = traceShieldPlane(trace, center, axes.forward())
                .orElseGet(() -> resolveImpactLocation(target, arrow, hitResult, motion));
        Vec3 relative = hitLocation.subtract(center);

        return new LodgedArrowVisual(
                (float) Mth.clamp(relative.dot(axes.right()), -SHIELD_HALF_WIDTH, SHIELD_HALF_WIDTH),
                (float) Mth.clamp(-relative.y, -SHIELD_HALF_HEIGHT, SHIELD_HALF_HEIGHT),
                (float) SHIELD_FRONT_Z,
                (float) modelDirection.x,
                (float) modelDirection.y,
                (float) modelDirection.z,
                LodgedArrowBodyPart.CHEST,
                LodgedArrowDepth.LODGED,
                new ItemStack(Items.ARROW));
    }

    public EquipmentSlot armorSlot() {
        return switch (bodyPart()) {
            case HEAD -> EquipmentSlot.HEAD;
            case CHEST, LEFT_ARM, RIGHT_ARM -> EquipmentSlot.CHEST;
            case LEFT_LEG, RIGHT_LEG -> modelY > 20.0F / 16.0F ? EquipmentSlot.FEET : EquipmentSlot.LEGS;
        };
    }

    public Vec3 toEntityLocalPosition(LivingEntity target) {
        double modelScale = modelRenderScale(target);
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
        tag.putString(BODY_PART_KEY, bodyPart.serializedName());
        tag.putString(DEPTH_KEY, depth.serializedName());
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
                tag.getFloat(DIRECTION_Z_KEY),
                tag.contains(BODY_PART_KEY, Tag.TAG_STRING)
                        ? LodgedArrowBodyPart.bySerializedName(tag.getString(BODY_PART_KEY))
                        : null,
                tag.contains(DEPTH_KEY, Tag.TAG_STRING)
                        ? LodgedArrowDepth.bySerializedName(tag.getString(DEPTH_KEY))
                        : LodgedArrowDepth.LODGED,
                new ItemStack(Items.ARROW));
    }

    private void encode(RegistryFriendlyByteBuf buffer) {
        buffer.writeFloat(modelX);
        buffer.writeFloat(modelY);
        buffer.writeFloat(modelZ);
        buffer.writeFloat(directionX);
        buffer.writeFloat(directionY);
        buffer.writeFloat(directionZ);
        ByteBufCodecs.idMapper(LodgedArrowBodyPart::byId, LodgedArrowBodyPart::id).encode(buffer, bodyPart);
        ByteBufCodecs.idMapper(LodgedArrowDepth::byId, LodgedArrowDepth::id).encode(buffer, depth);
        ItemStack.OPTIONAL_STREAM_CODEC.encode(buffer, stack);
    }

    private static LodgedArrowVisual decode(RegistryFriendlyByteBuf buffer) {
        return new LodgedArrowVisual(
                buffer.readFloat(),
                buffer.readFloat(),
                buffer.readFloat(),
                buffer.readFloat(),
                buffer.readFloat(),
                buffer.readFloat(),
                ByteBufCodecs.idMapper(LodgedArrowBodyPart::byId, LodgedArrowBodyPart::id).decode(buffer),
                ByteBufCodecs.idMapper(LodgedArrowDepth::byId, LodgedArrowDepth::id).decode(buffer),
                ItemStack.OPTIONAL_STREAM_CODEC.decode(buffer));
    }

    private static ItemStack renderStack(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return new ItemStack(Items.ARROW);
        }

        return stack.copyWithCount(1);
    }

    private static float finiteOrDefault(float value, float fallback) {
        return Float.isFinite(value) ? value : fallback;
    }

    private static LodgedArrowBodyPart classifyBodyPart(float modelX, float modelY) {
        if (modelY < MODEL_HEAD_BOTTOM) {
            return LodgedArrowBodyPart.HEAD;
        }
        if (modelY >= MODEL_LEG_TOP) {
            return modelX < 0.0F ? LodgedArrowBodyPart.RIGHT_LEG : LodgedArrowBodyPart.LEFT_LEG;
        }
        if (Math.abs(modelX) > MODEL_BODY_HALF_WIDTH) {
            return modelX < 0.0F ? LodgedArrowBodyPart.RIGHT_ARM : LodgedArrowBodyPart.LEFT_ARM;
        }
        return LodgedArrowBodyPart.CHEST;
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
        double padding = Math.max(boundingBox.getSize() + VANILLA_ENTITY_PICK_MARGIN, 1.0D);
        Vec3 arrowPosition = arrow.position();
        return new TraceSegment(
                arrowPosition.subtract(direction.scale(padding)),
                arrowPosition.add(motion).add(direction.scale(padding)));
    }

    private static Vec3 toModelSpace(LivingEntity target, AABB boundingBox, BodyAxes axes, Vec3 worldPosition, double modelScale) {
        Vec3 relative = worldPosition.subtract(target.getX(), boundingBox.minY, target.getZ());
        return new Vec3(
                relative.dot(axes.right()) / modelScale,
                MODEL_RENDER_ROOT_Y - (relative.y / modelScale),
                -relative.dot(axes.forward()) / modelScale);
    }

    private static double modelRenderScale(LivingEntity target) {
        double rendererScale = target instanceof Player ? PLAYER_MODEL_RENDER_SCALE : 1.0D;
        return Math.max(rendererScale * target.getScale(), 0.1D);
    }

    private static Vec3 shieldCenter(LivingEntity target, BodyAxes axes) {
        return new Vec3(
                target.getX(),
                target.getY() + target.getBbHeight() * SHIELD_CENTER_HEIGHT_RATIO,
                target.getZ()).add(axes.forward().scale(SHIELD_CENTER_FORWARD_OFFSET));
    }

    private static Optional<Vec3> traceShieldPlane(TraceSegment trace, Vec3 center, Vec3 forward) {
        Vec3 delta = trace.end().subtract(trace.start());
        double denominator = delta.dot(forward);
        if (Math.abs(denominator) < 1.0E-7D) {
            return Optional.empty();
        }

        double t = center.subtract(trace.start()).dot(forward) / denominator;
        if (t < 0.0D || t > 1.0D) {
            return Optional.empty();
        }

        return Optional.of(trace.start().add(delta.scale(t)));
    }

    private static Optional<ModelHit> traceModelHit(Vec3 modelStart, Vec3 modelEnd) {
        return traceModelHit(modelStart, modelEnd, 0.0D);
    }

    private static Optional<ModelHit> traceModelHit(Vec3 modelStart, Vec3 modelEnd, double inflate) {
        Vec3 modelDelta = modelEnd.subtract(modelStart);
        Vec3 modelDirection = normalizedOrDefault(modelDelta);
        ModelHit closestHit = null;
        double closestT = Double.MAX_VALUE;
        double closestCenterDistance = Double.MAX_VALUE;
        for (BodyPartBox candidate : HUMANOID_MODEL_BOXES) {
            ModelBox box = candidate.box();
            Optional<Double> hitT = box.inflate(inflate).intersect(modelStart, modelDelta);
            if (hitT.isEmpty()) {
                continue;
            }

            Vec3 hitPosition = modelStart.add(modelDelta.scale(hitT.get()));
            if (inflate > 0.0D) {
                hitPosition = box.clampToSurface(hitPosition, modelDirection);
            }
            double centerDistance = box.centerDistanceToSqr(hitPosition);
            if (hitT.get() < closestT - MODEL_HIT_TIE_EPSILON
                    || Math.abs(hitT.get() - closestT) <= MODEL_HIT_TIE_EPSILON
                            && preferBodyPart(
                                    candidate.bodyPart(),
                                    closestHit != null ? closestHit.bodyPart() : null,
                                    hitPosition,
                                    centerDistance,
                                    closestCenterDistance)) {
                closestT = hitT.get();
                closestCenterDistance = centerDistance;
                closestHit = new ModelHit(hitPosition, modelDirection, candidate.bodyPart());
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
            double modelScale,
            Vec3 modelStart,
            Vec3 modelEnd) {
        Vec3 hitLocation = resolveImpactLocation(target, arrow, hitResult, motion);
        Vec3 modelDirection = normalizedOrDefault(modelEnd.subtract(modelStart));
        return snapToHumanoidSurface(
                toModelSpace(target, boundingBox, axes, hitLocation, modelScale),
                modelDirection);
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

    private static ModelHit snapToHumanoidSurface(Vec3 modelPosition, Vec3 modelDirection) {
        BodyPartBox closest = HUMANOID_MODEL_BOXES[0];
        double closestDistance = closest.box().distanceToSqr(modelPosition);
        double closestCenterDistance = closest.box().centerDistanceToSqr(modelPosition);
        for (int index = 1; index < HUMANOID_MODEL_BOXES.length; index++) {
            BodyPartBox candidate = HUMANOID_MODEL_BOXES[index];
            ModelBox box = candidate.box();
            double distance = box.distanceToSqr(modelPosition);
            double centerDistance = box.centerDistanceToSqr(modelPosition);
            if (distance < closestDistance - MODEL_HIT_TIE_EPSILON
                    || Math.abs(distance - closestDistance) <= MODEL_HIT_TIE_EPSILON
                            && preferBodyPart(
                                    candidate.bodyPart(),
                                    closest.bodyPart(),
                                    modelPosition,
                                    centerDistance,
                                    closestCenterDistance)) {
                closest = candidate;
                closestDistance = distance;
                closestCenterDistance = centerDistance;
            }
        }

        return new ModelHit(
                closest.box().clampToSurface(modelPosition, modelDirection),
                modelDirection,
                closest.bodyPart());
    }

    private static boolean preferBodyPart(
            LodgedArrowBodyPart candidate,
            LodgedArrowBodyPart current,
            Vec3 position,
            double candidateCenterDistance,
            double currentCenterDistance) {
        LodgedArrowBodyPart expected = classifyBodyPart((float) position.x, (float) position.y);
        if (candidate == expected && current != expected) {
            return true;
        }
        if (current == expected && candidate != expected) {
            return false;
        }
        return candidateCenterDistance < currentCenterDistance;
    }

    private record BodyAxes(Vec3 right, Vec3 forward) {
        static BodyAxes of(float bodyYaw) {
            float yawRadians = bodyYaw * Mth.DEG_TO_RAD;
            Vec3 right = new Vec3(Math.cos(yawRadians), 0.0D, Math.sin(yawRadians));
            Vec3 forward = new Vec3(-Math.sin(yawRadians), 0.0D, Math.cos(yawRadians));
            return new BodyAxes(right, forward);
        }
    }

    private record ModelBox(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
        ModelBox inflate(double amount) {
            if (amount <= 0.0D) {
                return this;
            }

            return new ModelBox(minX - amount, minY - amount, minZ - amount, maxX + amount, maxY + amount, maxZ + amount);
        }

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

        double centerDistanceToSqr(Vec3 point) {
            double dx = point.x - centerX();
            double dy = point.y - centerY();
            double dz = point.z - centerZ();
            return dx * dx + dy * dy + dz * dz;
        }

        Vec3 clampToSurface(Vec3 point, Vec3 direction) {
            double x = Mth.clamp(point.x, minX, maxX);
            double y = Mth.clamp(point.y, minY, maxY);
            double z = Mth.clamp(point.z, minZ, maxZ);
            Surface surface = surface(point, direction);
            Axis axis = surface.axis();
            return switch (axis) {
                case X -> new Vec3(surface.coordinate(), y, z);
                case Y -> new Vec3(x, surface.coordinate(), z);
                case Z -> new Vec3(x, y, surface.coordinate());
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

        private Surface surface(Vec3 point, Vec3 direction) {
            double outsideX = outsideDistance(point.x, minX, maxX);
            double outsideY = outsideDistance(point.y, minY, maxY);
            double outsideZ = outsideDistance(point.z, minZ, maxZ);
            if (outsideX > 0.0D || outsideY > 0.0D || outsideZ > 0.0D) {
                if (outsideX >= outsideY && outsideX >= outsideZ) {
                    return new Surface(Axis.X, point.x < centerX() ? minX : maxX);
                }
                if (outsideY >= outsideZ) {
                    return new Surface(Axis.Y, point.y < centerY() ? minY : maxY);
                }
                return new Surface(Axis.Z, point.z < centerZ() ? minZ : maxZ);
            }

            double xMovement = Math.abs(direction.x);
            double yMovement = Math.abs(direction.y);
            double zMovement = Math.abs(direction.z);
            if (xMovement >= yMovement && xMovement >= zMovement) {
                return new Surface(Axis.X, direction.x >= 0.0D ? minX : maxX);
            }
            if (yMovement >= zMovement) {
                return new Surface(Axis.Y, direction.y >= 0.0D ? minY : maxY);
            }

            return new Surface(Axis.Z, direction.z >= 0.0D ? minZ : maxZ);
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

    private record BodyPartBox(LodgedArrowBodyPart bodyPart, ModelBox box) {
    }

    private record ModelHit(Vec3 position, Vec3 direction, LodgedArrowBodyPart bodyPart) {
    }

    private record AxisIntersection(boolean intersects, double tMin, double tMax) {
    }

    private record Surface(Axis axis, double coordinate) {
    }

    private enum Axis {
        X,
        Y,
        Z
    }
}
