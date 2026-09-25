package com.jvn.lodged.client;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.jvn.lodged.client.ScreenPolygon.ScreenPoint;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ScreenPolygonTest {
    @Test
    void hullDropsInteriorAndDuplicatePointsWithoutReorderingItsInput() {
        ScreenPoint[] points = {
                new ScreenPoint(10, 10), new ScreenPoint(0, 0), new ScreenPoint(5, 5),
                new ScreenPoint(10, 0), new ScreenPoint(0, 10), new ScreenPoint(0, 0)
        };
        ScreenPoint[] original = points.clone();

        ScreenPoint[] hull = ScreenPolygon.convexHull(points);

        assertArrayEquals(original, points);
        assertEquals(4, hull.length);
        assertEquals(Set.of(points[0], points[1], points[3], points[4]), Set.of(hull));
    }

    @Test
    void pickingIncludesTheEdgeMarginButRejectsPointsBeyondIt() {
        ScreenPoint[] square = square();

        assertTrue(ScreenPolygon.isInsideOrNearPolygon(square, 5, 5, 2));
        assertTrue(ScreenPolygon.isInsideOrNearPolygon(square, 0, 5, 2));
        assertTrue(ScreenPolygon.isInsideOrNearPolygon(square, -2, 5, 2));
        assertFalse(ScreenPolygon.isInsideOrNearPolygon(square, -2.01, 5, 2));
        assertFalse(ScreenPolygon.isInsideOrNearPolygon(square, -2, -2, 2));
    }

    @Test
    void pickingWorksWithEitherWinding() {
        ScreenPoint[] clockwise = square();
        ScreenPoint[] counterclockwise = {clockwise[3], clockwise[2], clockwise[1], clockwise[0]};

        for (ScreenPoint[] polygon : new ScreenPoint[][]{clockwise, counterclockwise}) {
            assertTrue(ScreenPolygon.isInsideOrNearPolygon(polygon, 5, 5, 0));
            assertFalse(ScreenPolygon.isInsideOrNearPolygon(polygon, 15, 5, 0));
        }
    }

    @Test
    void emptyAndSinglePointProjectionsStaySafeToPick() {
        assertEquals(0, ScreenPolygon.convexHull(new ScreenPoint[0]).length);
        assertFalse(ScreenPolygon.isInsideOrNearPolygon(new ScreenPoint[0], 0, 0, 3));

        ScreenPoint[] point = ScreenPolygon.convexHull(new ScreenPoint[]{new ScreenPoint(4, 5)});
        assertEquals(1, point.length);
        assertTrue(ScreenPolygon.isInsideOrNearPolygon(point, 7, 5, 3));
        assertFalse(ScreenPolygon.isInsideOrNearPolygon(point, 7.01, 5, 3));
    }

    @Test
    void collinearCornersReduceToTheirEndpoints() {
        ScreenPoint[] hull = ScreenPolygon.convexHull(new ScreenPoint[]{
                new ScreenPoint(10, 0), new ScreenPoint(5, 0), new ScreenPoint(0, 0)
        });

        assertArrayEquals(new ScreenPoint[]{new ScreenPoint(0, 0), new ScreenPoint(10, 0)}, hull);
    }

    private static ScreenPoint[] square() {
        return new ScreenPoint[]{
                new ScreenPoint(0, 0), new ScreenPoint(10, 0),
                new ScreenPoint(10, 10), new ScreenPoint(0, 10)
        };
    }
}
