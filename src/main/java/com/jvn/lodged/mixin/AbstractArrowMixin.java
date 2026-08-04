package com.jvn.lodged.mixin;

import com.jvn.lodged.collision.ModelAccurateProjectileCollision;
import com.jvn.lodged.collision.ModelColliderPart;
import com.jvn.lodged.collision.ModelHitboxProjectileAccess;
import com.jvn.lodged.world.LodgedArrowBodyPart;
import java.util.function.Predicate;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractArrow.class)
public abstract class AbstractArrowMixin implements ModelHitboxProjectileAccess {
    @Unique
    private int lodged$modelHitEntityId = Integer.MIN_VALUE;
    @Unique
    private LodgedArrowBodyPart lodged$modelHitBodyPart;
    @Unique
    private String lodged$modelHitPartPath;
    @Unique
    private float lodged$modelHitX;
    @Unique
    private float lodged$modelHitY;
    @Unique
    private float lodged$modelHitZ;
    @Unique
    private float lodged$modelHitDirectionX;
    @Unique
    private float lodged$modelHitDirectionY;
    @Unique
    private float lodged$modelHitDirectionZ = 1.0F;

    @Shadow
    protected abstract boolean canHitEntity(Entity target);

    @Inject(method = "findHitEntity", at = @At("HEAD"), cancellable = true, require = 0)
    private void lodged$findModelHit(
            Vec3 start,
            Vec3 end,
            CallbackInfoReturnable<EntityHitResult> callbackInfo) {
        AbstractArrow arrow = (AbstractArrow) (Object) this;
        if (!ModelAccurateProjectileCollision.shouldReplaceVanilla(arrow)) {
            return;
        }

        try {
            Predicate<Entity> filter = this::canHitEntity;
            callbackInfo.setReturnValue(ModelAccurateProjectileCollision.findHit(arrow, start, end, filter));
        } catch (Throwable throwable) {
            lodged$clearModelHit();
            ModelAccurateProjectileCollision.disableAfterFailure(throwable);
        }
    }

    @Override
    public void lodged$clearModelHit() {
        lodged$modelHitEntityId = Integer.MIN_VALUE;
        lodged$modelHitBodyPart = null;
        lodged$modelHitPartPath = null;
    }

    @Override
    public void lodged$setModelHit(
            int entityId,
            LodgedArrowBodyPart bodyPart,
            ModelColliderPart modelPart,
            float modelX,
            float modelY,
            float modelZ,
            float directionX,
            float directionY,
            float directionZ) {
        lodged$modelHitEntityId = entityId;
        lodged$modelHitBodyPart = bodyPart;
        lodged$modelHitPartPath = modelPart.modelPartPath();
        lodged$modelHitX = modelX;
        lodged$modelHitY = modelY;
        lodged$modelHitZ = modelZ;
        lodged$modelHitDirectionX = directionX;
        lodged$modelHitDirectionY = directionY;
        lodged$modelHitDirectionZ = directionZ;
    }

    @Override public int lodged$modelHitEntityId() { return lodged$modelHitEntityId; }
    @Override public LodgedArrowBodyPart lodged$modelHitBodyPart() { return lodged$modelHitBodyPart; }
    @Override public String lodged$modelHitPartPath() { return lodged$modelHitPartPath; }
    @Override public float lodged$modelHitX() { return lodged$modelHitX; }
    @Override public float lodged$modelHitY() { return lodged$modelHitY; }
    @Override public float lodged$modelHitZ() { return lodged$modelHitZ; }
    @Override public float lodged$modelHitDirectionX() { return lodged$modelHitDirectionX; }
    @Override public float lodged$modelHitDirectionY() { return lodged$modelHitDirectionY; }
    @Override public float lodged$modelHitDirectionZ() { return lodged$modelHitDirectionZ; }
}
