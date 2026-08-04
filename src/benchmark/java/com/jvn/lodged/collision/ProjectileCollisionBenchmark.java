package com.jvn.lodged.collision;

import java.util.Locale;

public final class ProjectileCollisionBenchmark {
    private static final int PROJECTILES = 250_000;
    private static final int PARTS_PER_ENTITY = 48;
    private static volatile double blackhole;

    private ProjectileCollisionBenchmark() {
    }

    public static void main(String[] args) {
        Locale.setDefault(Locale.ROOT);
        for (int warmup = 0; warmup < 3; warmup++) {
            run("warmup", Mode.MIXED, false);
        }
        run("dense_hits", Mode.HIT, true);
        run("dense_misses", Mode.MISS, true);
        run("mixed_projectiles", Mode.MIXED, true);
    }

    private static void run(String name, Mode mode, boolean print) {
        long startNanos = System.nanoTime();
        double checksum = 0.0D;
        for (int projectile = 0; projectile < PROJECTILES; projectile++) {
            double startY = switch (mode) {
                case HIT -> ((projectile & 31) - 16) * 0.01D;
                case MISS -> 3.0D + (projectile & 7) * 0.1D;
                case MIXED -> (projectile & 3) == 0 ? 2.0D : ((projectile & 31) - 16) * 0.01D;
            };
            double nearest = SweptCuboid.MISS;
            for (int part = 0; part < PARTS_PER_ENTITY; part++) {
                double centerX = 1.0D + part * 0.18D;
                double time = SweptCuboid.intersect(
                        -1.0D, startY, 0.0D, 6.0D, 0.02D, 0.01D,
                        centerX - 0.08D, -0.2D, -0.15D,
                        centerX + 0.08D, 0.2D, 0.15D);
                if (time < nearest) {
                    nearest = time;
                }
            }
            checksum += Double.isFinite(nearest) ? nearest : 1.0D;
        }
        long elapsed = System.nanoTime() - startNanos;
        blackhole = checksum;
        if (print) {
            double seconds = elapsed / 1_000_000_000.0D;
            double rayPartTests = (double) PROJECTILES * PARTS_PER_ENTITY;
            System.out.printf(
                    "%-20s %,12.0f projectiles/s  %,12.0f part-tests/s  %.2f ms  checksum=%.3f%n",
                    name, PROJECTILES / seconds, rayPartTests / seconds,
                    elapsed / 1_000_000.0D, blackhole);
        }
    }

    private enum Mode {
        HIT,
        MISS,
        MIXED
    }
}
