package com.jvn.lodged.collision;

import com.jvn.lodged.Lodged;
import com.jvn.lodged.config.LodgedConfig;
import com.jvn.lodged.effect.LodgedTags;
import com.jvn.lodged.network.PlayerArrowRemoval;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.ItemAbilities;

public final class ModelAccurateProjectileCollision {
    private static final double VANILLA_ENTITY_INFLATION = 0.3D;
    private static final ThreadLocal<ModelHitboxCache> FALLBACK_CACHE =
            ThreadLocal.withInitial(ModelHitboxCache::new);
    private static final AtomicBoolean RUNTIME_DISABLED = new AtomicBoolean();

    private ModelAccurateProjectileCollision() {
    }

    public static boolean shouldReplaceVanilla(AbstractArrow arrow) {
        return !RUNTIME_DISABLED.get()
                && LodgedConfig.enableModelAccurateProjectileCollision()
                && arrow.getType().is(LodgedTags.TRACKABLE_PROJECTILES);
    }

    public static void disableAfterFailure(Throwable throwable) {
        if (RUNTIME_DISABLED.compareAndSet(false, true)) {
            Lodged.LOGGER.error(
                    "Disabled model-accurate projectile collision for this session after an unexpected compatibility failure",
                    throwable);
        }
    }

    @Nullable
    public static EntityHitResult findHit(
            AbstractArrow arrow,
            Vec3 start,
            Vec3 end,
            Predicate<Entity> filter) {
        ModelHitboxProjectileAccess projectileAccess = (ModelHitboxProjectileAccess) arrow;
        projectileAccess.lodged$clearModelHit();

        double deltaX = end.x - start.x;
        double deltaY = end.y - start.y;
        double deltaZ = end.z - start.z;
        AABB broadPhase = arrow.getBoundingBox()
                .expandTowards(deltaX, deltaY, deltaZ)
                .inflate(1.0D);
        List<Entity> candidates = arrow.level().getEntities(arrow, broadPhase, filter);

        Entity nearestEntity = null;
        double nearestTime = SweptCuboid.MISS;
        ModelColliderPart nearestPart = null;
        double nearestModelX = 0.0D;
        double nearestModelY = 0.0D;
        double nearestModelZ = 0.0D;
        double nearestDirectionX = 0.0D;
        double nearestDirectionY = 0.0D;
        double nearestDirectionZ = 1.0D;
        double inflation = LodgedConfig.modelHitboxInflation();

        for (int candidateIndex = 0, candidateCount = candidates.size();
                candidateIndex < candidateCount;
                candidateIndex++) {
            Entity candidate = candidates.get(candidateIndex);
            if (!(candidate instanceof LivingEntity living) || !ModelHitboxCache.supports(living)) {
                AABB box = candidate.getBoundingBox().inflate(VANILLA_ENTITY_INFLATION);
                double time = SweptCuboid.intersect(
                        start.x, start.y, start.z,
                        deltaX, deltaY, deltaZ,
                        box.minX, box.minY, box.minZ,
                        box.maxX, box.maxY, box.maxZ);
                if (time < nearestTime) {
                    nearestTime = time;
                    nearestEntity = candidate;
                    nearestPart = null;
                }
                continue;
            }

            ModelHitboxCache cache = living instanceof ModelHitboxCacheHolder holder
                    ? holder.lodged$getModelHitboxCache()
                    : FALLBACK_CACHE.get();
            if (!cache.prepare(living, 1.0F)) {
                continue;
            }

            double entityStartX = start.x - living.getX();
            double entityStartY = start.y - living.getY();
            double entityStartZ = start.z - living.getZ();
            double partScale = cache.scale();
            int partCount = cache.partCount();
            for (int partIndex = 0; partIndex < partCount; partIndex++) {
                double relativeX = entityStartX - cache.originX(partIndex);
                double relativeY = entityStartY - cache.originY(partIndex);
                double relativeZ = entityStartZ - cache.originZ(partIndex);
                double localStartX = relativeX * cache.axisXX(partIndex)
                        + relativeY * cache.axisXY(partIndex)
                        + relativeZ * cache.axisXZ(partIndex);
                double localStartY = relativeX * cache.axisYX(partIndex)
                        + relativeY * cache.axisYY(partIndex)
                        + relativeZ * cache.axisYZ(partIndex);
                double localStartZ = relativeX * cache.axisZX(partIndex)
                        + relativeY * cache.axisZY(partIndex)
                        + relativeZ * cache.axisZZ(partIndex);
                double localDeltaX = deltaX * cache.axisXX(partIndex)
                        + deltaY * cache.axisXY(partIndex)
                        + deltaZ * cache.axisXZ(partIndex);
                double localDeltaY = deltaX * cache.axisYX(partIndex)
                        + deltaY * cache.axisYY(partIndex)
                        + deltaZ * cache.axisYZ(partIndex);
                double localDeltaZ = deltaX * cache.axisZX(partIndex)
                        + deltaY * cache.axisZY(partIndex)
                        + deltaZ * cache.axisZZ(partIndex);

                ModelColliderPart part = cache.part(partIndex);
                double time = SweptCuboid.intersect(
                        localStartX, localStartY, localStartZ,
                        localDeltaX, localDeltaY, localDeltaZ,
                        part.minX() * partScale - inflation,
                        part.minY() * partScale - inflation,
                        part.minZ() * partScale - inflation,
                        part.maxX() * partScale + inflation,
                        part.maxY() * partScale + inflation,
                        part.maxZ() * partScale + inflation);
                if (time >= nearestTime) {
                    continue;
                }

                nearestTime = time;
                nearestEntity = living;
                nearestPart = part;
                nearestModelX = part.pivotX() + (localStartX + localDeltaX * time) / partScale;
                nearestModelY = part.pivotY() + (localStartY + localDeltaY * time) / partScale;
                nearestModelZ = part.pivotZ() + (localStartZ + localDeltaZ * time) / partScale;
                double directionLength = Math.sqrt(
                        localDeltaX * localDeltaX + localDeltaY * localDeltaY + localDeltaZ * localDeltaZ);
                if (directionLength > 1.0E-9D) {
                    nearestDirectionX = localDeltaX / directionLength;
                    nearestDirectionY = localDeltaY / directionLength;
                    nearestDirectionZ = localDeltaZ / directionLength;
                }
            }

            if (preservesVanillaShieldHit(arrow, living)) {
                AABB shieldFallback = living.getBoundingBox().inflate(VANILLA_ENTITY_INFLATION);
                double time = SweptCuboid.intersect(
                        start.x, start.y, start.z,
                        deltaX, deltaY, deltaZ,
                        shieldFallback.minX, shieldFallback.minY, shieldFallback.minZ,
                        shieldFallback.maxX, shieldFallback.maxY, shieldFallback.maxZ);
                if (time < nearestTime) {
                    nearestTime = time;
                    nearestEntity = living;
                    nearestPart = null;
                }
            }
        }

        if (nearestEntity == null) {
            return null;
        }

        Vec3 location = new Vec3(
                start.x + deltaX * nearestTime,
                start.y + deltaY * nearestTime,
                start.z + deltaZ * nearestTime);
        if (nearestPart != null) {
            projectileAccess.lodged$setModelHit(
                    nearestEntity.getId(),
                    nearestPart.bodyPart(),
                    nearestPart,
                    (float) nearestModelX,
                    (float) nearestModelY,
                    (float) nearestModelZ,
                    (float) nearestDirectionX,
                    (float) nearestDirectionY,
                    (float) nearestDirectionZ);
        }
        return new EntityHitResult(nearestEntity, location);
    }

    private static boolean preservesVanillaShieldHit(AbstractArrow arrow, LivingEntity target) {
        if (arrow.getPierceLevel() > 0
                || !target.isBlocking()
                || PlayerArrowRemoval.isRemovingShieldArrow(target)) {
            return false;
        }
        ItemStack shield = target.getUseItem();
        if (shield.isEmpty() || !shield.canPerformAction(ItemAbilities.SHIELD_BLOCK)) {
            return false;
        }

        double directionX = target.getX() - arrow.getX();
        double directionZ = target.getZ() - arrow.getZ();
        if (directionX * directionX + directionZ * directionZ < 1.0E-7D) {
            return false;
        }
        double yaw = target.getYHeadRot() * Math.PI / 180.0D;
        return directionX * -Math.sin(yaw) + directionZ * Math.cos(yaw) < 0.0D;
    }
}
