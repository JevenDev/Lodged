package com.jvn.lodged.client;

import java.util.Arrays;
import java.util.Comparator;

final class ScreenPolygon {
    private ScreenPolygon() {
    }

    record ScreenPoint(double x, double y) {
    }

    static ScreenPoint[] convexHull(ScreenPoint[] points) {
        ScreenPoint[] sorted = Arrays.copyOf(points, points.length);
        Arrays.sort(sorted, Comparator.comparingDouble(ScreenPoint::x).thenComparingDouble(ScreenPoint::y));

        ScreenPoint[] hull = new ScreenPoint[sorted.length * 2];
        int size = 0;
        for (ScreenPoint point : sorted) {
            while (size >= 2 && cross(hull[size - 2], hull[size - 1], point) <= 0.0D) {
                size--;
            }
            hull[size++] = point;
        }

        int lowerSize = size;
        for (int index = sorted.length - 2; index >= 0; index--) {
            ScreenPoint point = sorted[index];
            while (size > lowerSize && cross(hull[size - 2], hull[size - 1], point) <= 0.0D) {
                size--;
            }
            hull[size++] = point;
        }

        if (size <= 1) {
            return Arrays.copyOf(hull, size);
        }
        return Arrays.copyOf(hull, size - 1);
    }

    static boolean isInsideOrNearPolygon(ScreenPoint[] polygon, double mouseX, double mouseY, double margin) {
        if (polygon.length < 3) {
            double marginSqr = margin * margin;
            for (ScreenPoint point : polygon) {
                double deltaX = mouseX - point.x();
                double deltaY = mouseY - point.y();
                if ((deltaX * deltaX) + (deltaY * deltaY) <= marginSqr) {
                    return true;
                }
            }
            return false;
        }

        boolean hasPositive = false;
        boolean hasNegative = false;
        ScreenPoint mousePoint = new ScreenPoint(mouseX, mouseY);
        for (int index = 0; index < polygon.length; index++) {
            ScreenPoint start = polygon[index];
            ScreenPoint end = polygon[(index + 1) % polygon.length];
            double side = cross(start, end, mousePoint);
            hasPositive |= side > 0.0D;
            hasNegative |= side < 0.0D;
            if (hasPositive && hasNegative) {
                return isNearPolygonEdge(polygon, mouseX, mouseY, margin);
            }
        }
        return true;
    }

    private static boolean isNearPolygonEdge(ScreenPoint[] polygon, double mouseX, double mouseY, double margin) {
        double marginSqr = margin * margin;
        for (int index = 0; index < polygon.length; index++) {
            ScreenPoint start = polygon[index];
            ScreenPoint end = polygon[(index + 1) % polygon.length];
            if (distanceToSegmentSqr(mouseX, mouseY, start, end) <= marginSqr) {
                return true;
            }
        }
        return false;
    }

    private static double distanceToSegmentSqr(double mouseX, double mouseY, ScreenPoint start, ScreenPoint end) {
        double deltaX = end.x() - start.x();
        double deltaY = end.y() - start.y();
        double lengthSqr = (deltaX * deltaX) + (deltaY * deltaY);
        if (lengthSqr <= 1.0E-7D) {
            double pointDeltaX = mouseX - start.x();
            double pointDeltaY = mouseY - start.y();
            return (pointDeltaX * pointDeltaX) + (pointDeltaY * pointDeltaY);
        }

        double progress = ((mouseX - start.x()) * deltaX + (mouseY - start.y()) * deltaY) / lengthSqr;
        progress = Math.max(0.0D, Math.min(1.0D, progress));
        double closestX = start.x() + (deltaX * progress);
        double closestY = start.y() + (deltaY * progress);
        double closestDeltaX = mouseX - closestX;
        double closestDeltaY = mouseY - closestY;
        return (closestDeltaX * closestDeltaX) + (closestDeltaY * closestDeltaY);
    }

    private static double cross(ScreenPoint start, ScreenPoint end, ScreenPoint point) {
        return ((end.x() - start.x()) * (point.y() - start.y()))
                - ((end.y() - start.y()) * (point.x() - start.x()));
    }
}
