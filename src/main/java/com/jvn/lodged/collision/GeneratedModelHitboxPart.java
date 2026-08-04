package com.jvn.lodged.collision;

import com.jvn.lodged.world.LodgedArrowBodyPart;

record GeneratedModelHitboxPart(
        String serializedName,
        String modelPartPath,
        LodgedArrowBodyPart bodyPart,
        double pivotX,
        double pivotY,
        double pivotZ,
        double minX,
        double minY,
        double minZ,
        double maxX,
        double maxY,
        double maxZ) implements ModelColliderPart {
}
