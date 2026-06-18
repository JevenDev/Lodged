package com.jvn.lodged.client.compat;

import com.jvn.lodged.Lodged;
import java.lang.reflect.Constructor;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.neoforged.fml.ModList;

public final class SimpleBloodCompat {
    private static final String MOD_ID = "simpleblood";
    private static final String GROUND_PARTICLE_OPTIONS_CLASS =
            "io.redspace.simpleblood.client.particles.BloodGroundParticleOptions";
    private static final int DEFAULT_BLOOD_COLOR = 0xFF80000D;
    private static final float GROUND_DECAL_CHANCE = 0.15F;
    private static final float GROUND_DECAL_SCALE_MULTIPLIER = 1.5F;
    private static final float GROUND_DECAL_MIN_SCALE = 0.4F;
    private static final float GROUND_DECAL_MAX_SCALE = 0.8F;

    private static Constructor<?> groundParticleOptionsConstructor;
    private static boolean initialized;
    private static boolean available;

    private SimpleBloodCompat() {
    }

    public static void spawnGroundBlood(ClientLevel level, Vec3 position, float dripScale) {
        if (!isAvailable() || level.random.nextFloat() > GROUND_DECAL_CHANCE) {
            return;
        }

        try {
            float scale = groundDecalScale(level, dripScale);
            Object options = groundParticleOptionsConstructor.newInstance(DEFAULT_BLOOD_COLOR, scale);
            level.addParticle((ParticleOptions) options, true, position.x, position.y, position.z, 0.0D, 0.0D, 0.0D);
        } catch (ReflectiveOperationException | RuntimeException | LinkageError exception) {
            available = false;
            Lodged.LOGGER.warn("Simple Blood compat failed while spawning a ground blood particle", exception);
        }
    }

    private static boolean isAvailable() {
        if (!initialized) {
            initialize();
        }
        return available;
    }

    private static void initialize() {
        initialized = true;
        if (!ModList.get().isLoaded(MOD_ID)) {
            return;
        }

        try {
            Class<?> optionsClass = Class.forName(GROUND_PARTICLE_OPTIONS_CLASS);
            if (!ParticleOptions.class.isAssignableFrom(optionsClass)) {
                Lodged.LOGGER.warn(
                        "Simple Blood compat disabled because {} is not a ParticleOptions type",
                        GROUND_PARTICLE_OPTIONS_CLASS);
                return;
            }

            groundParticleOptionsConstructor = optionsClass.getConstructor(int.class, float.class);
            available = true;
        } catch (ReflectiveOperationException | LinkageError exception) {
            Lodged.LOGGER.warn("Simple Blood compat disabled because its ground blood particle options could not be loaded", exception);
        }
    }

    private static float groundDecalScale(ClientLevel level, float dripScale) {
        float scale = Mth.clamp(
                dripScale * GROUND_DECAL_SCALE_MULTIPLIER,
                GROUND_DECAL_MIN_SCALE,
                GROUND_DECAL_MAX_SCALE);
        return scale * (0.82F + level.random.nextFloat() * 0.36F);
    }
}
