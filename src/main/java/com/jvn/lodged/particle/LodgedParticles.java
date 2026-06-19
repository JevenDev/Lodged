package com.jvn.lodged.particle;

import com.jvn.lodged.Lodged;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class LodgedParticles {
    public static final DeferredRegister<ParticleType<?>> PARTICLES =
            DeferredRegister.create(Registries.PARTICLE_TYPE, Lodged.MOD_ID);

    public static final DeferredHolder<ParticleType<?>, ParticleType<BleedingDropParticleOptions>> BLEEDING_DROP =
            PARTICLES.register("bleeding_drop", () -> new ParticleType<BleedingDropParticleOptions>(false) {
                @Override
                public com.mojang.serialization.MapCodec<BleedingDropParticleOptions> codec() {
                    return BleedingDropParticleOptions.CODEC;
                }

                @Override
                public net.minecraft.network.codec.StreamCodec<? super net.minecraft.network.RegistryFriendlyByteBuf, BleedingDropParticleOptions> streamCodec() {
                    return BleedingDropParticleOptions.STREAM_CODEC;
                }
            });

    private LodgedParticles() {
    }
}
