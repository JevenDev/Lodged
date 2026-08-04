package com.jvn.lodged.collision;

import com.jvn.lodged.world.LodgedArrowBodyPart;

public interface ModelHitboxProjectileAccess {
    void lodged$clearModelHit();

    void lodged$setModelHit(
            int entityId,
            LodgedArrowBodyPart bodyPart,
            ModelColliderPart modelPart,
            float modelX,
            float modelY,
            float modelZ,
            float directionX,
            float directionY,
            float directionZ);

    int lodged$modelHitEntityId();
    LodgedArrowBodyPart lodged$modelHitBodyPart();
    String lodged$modelHitPartPath();
    float lodged$modelHitX();
    float lodged$modelHitY();
    float lodged$modelHitZ();
    float lodged$modelHitDirectionX();
    float lodged$modelHitDirectionY();
    float lodged$modelHitDirectionZ();
}
