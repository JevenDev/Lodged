package com.jvn.lodged.collision;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.Test;

class SweptCuboidTest {
    private static final double EPSILON = 1.0E-12D;

    @Test
    void findsEntryTimeForHighVelocityProjectile() {
        double time = SweptCuboid.intersect(
                -10.0D, 0.0D, 0.0D,
                40.0D, 0.0D, 0.0D,
                -0.5D, -0.5D, -0.5D,
                0.5D, 0.5D, 0.5D);

        assertEquals(0.2375D, time, EPSILON);
    }

    @Test
    void parallelRayOutsideOneAxisMisses() {
        double time = SweptCuboid.intersect(
                -2.0D, 2.0D, 0.0D,
                4.0D, 0.0D, 0.0D,
                -0.5D, -0.5D, -0.5D,
                0.5D, 0.5D, 0.5D);

        assertEquals(SweptCuboid.MISS, time);
    }

    @Test
    void projectileStartingInsideHitsImmediately() {
        assertEquals(0.0D, SweptCuboid.intersect(
                0.0D, 0.0D, 0.0D,
                3.0D, 1.0D, 0.0D,
                -0.5D, -0.5D, -0.5D,
                0.5D, 0.5D, 0.5D), EPSILON);
    }

    @Test
    void halfPixelInflationCatchesEdgeGraze() {
        double inflation = ModelHitboxCache.DEFAULT_INFLATION;
        double withoutInflation = SweptCuboid.intersect(
                -2.0D, 0.53D, 0.0D, 4.0D, 0.0D, 0.0D,
                -0.5D, -0.5D, -0.5D, 0.5D, 0.5D, 0.5D);
        double withInflation = SweptCuboid.intersect(
                -2.0D, 0.53D, 0.0D, 4.0D, 0.0D, 0.0D,
                -0.5D - inflation, -0.5D - inflation, -0.5D - inflation,
                0.5D + inflation, 0.5D + inflation, 0.5D + inflation);

        assertEquals(SweptCuboid.MISS, withoutInflation);
        assertTrue(Double.isFinite(withInflation));
    }

    @Test
    void nearestCuboidWinsIndependentOfIterationOrder() {
        double near = SweptCuboid.intersect(
                0.0D, 0.0D, 0.0D, 10.0D, 0.0D, 0.0D,
                2.0D, -1.0D, -1.0D, 3.0D, 1.0D, 1.0D);
        double far = SweptCuboid.intersect(
                0.0D, 0.0D, 0.0D, 10.0D, 0.0D, 0.0D,
                6.0D, -1.0D, -1.0D, 7.0D, 1.0D, 1.0D);

        assertEquals(0.2D, Math.min(far, near), EPSILON);
    }

    @Test
    void blockClippedSegmentCannotReachCuboidBehindBlock() {
        assertEquals(SweptCuboid.MISS, SweptCuboid.intersect(
                0.0D, 0.0D, 0.0D, 4.0D, 0.0D, 0.0D,
                5.0D, -1.0D, -1.0D, 6.0D, 1.0D, 1.0D));
    }

    @Test
    void modelPartIdentityRoundTripsAndMapsToRendererNames() {
        for (ModelHitboxPart part : ModelHitboxPart.values()) {
            assertSame(part, ModelHitboxPart.byId(part.id()));
            assertSame(part, ModelHitboxPart.bySerializedName(part.serializedName()));
        }

        assertEquals("right_front_leg", ModelHitboxPart.SPIDER_RIGHT_FRONT_LEG.modelPartPath());
        assertEquals("body1", ModelHitboxPart.SPIDER_REAR_BODY.modelPartPath());
    }
}
