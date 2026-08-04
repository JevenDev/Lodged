package com.jvn.lodged.collision;

/** Allocation-free segment/AABB intersection used after transforming a ray into part-local space. */
public final class SweptCuboid {
    public static final double MISS = Double.POSITIVE_INFINITY;
    private static final double PARALLEL_EPSILON = 1.0E-12D;

    private SweptCuboid() {
    }

    public static double intersect(
            double startX,
            double startY,
            double startZ,
            double deltaX,
            double deltaY,
            double deltaZ,
            double minX,
            double minY,
            double minZ,
            double maxX,
            double maxY,
            double maxZ) {
        if (startX >= minX && startX <= maxX
                && startY >= minY && startY <= maxY
                && startZ >= minZ && startZ <= maxZ) {
            return 0.0D;
        }

        double near = 0.0D;
        double far = 1.0D;

        if (Math.abs(deltaX) < PARALLEL_EPSILON) {
            if (startX < minX || startX > maxX) {
                return MISS;
            }
        } else {
            double inverse = 1.0D / deltaX;
            double axisNear = (minX - startX) * inverse;
            double axisFar = (maxX - startX) * inverse;
            if (axisNear > axisFar) {
                double swap = axisNear;
                axisNear = axisFar;
                axisFar = swap;
            }
            near = Math.max(near, axisNear);
            far = Math.min(far, axisFar);
            if (near > far) {
                return MISS;
            }
        }

        if (Math.abs(deltaY) < PARALLEL_EPSILON) {
            if (startY < minY || startY > maxY) {
                return MISS;
            }
        } else {
            double inverse = 1.0D / deltaY;
            double axisNear = (minY - startY) * inverse;
            double axisFar = (maxY - startY) * inverse;
            if (axisNear > axisFar) {
                double swap = axisNear;
                axisNear = axisFar;
                axisFar = swap;
            }
            near = Math.max(near, axisNear);
            far = Math.min(far, axisFar);
            if (near > far) {
                return MISS;
            }
        }

        if (Math.abs(deltaZ) < PARALLEL_EPSILON) {
            if (startZ < minZ || startZ > maxZ) {
                return MISS;
            }
        } else {
            double inverse = 1.0D / deltaZ;
            double axisNear = (minZ - startZ) * inverse;
            double axisFar = (maxZ - startZ) * inverse;
            if (axisNear > axisFar) {
                double swap = axisNear;
                axisNear = axisFar;
                axisFar = swap;
            }
            near = Math.max(near, axisNear);
            far = Math.min(far, axisFar);
            if (near > far) {
                return MISS;
            }
        }

        return near >= 0.0D && near <= 1.0D ? near : MISS;
    }
}
