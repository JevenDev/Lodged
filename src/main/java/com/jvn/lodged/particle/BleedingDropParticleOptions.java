package com.jvn.lodged.particle;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public record BleedingDropParticleOptions(int color) implements ParticleOptions {
    public static final MapCodec<BleedingDropParticleOptions> CODEC =
            Codec.INT.fieldOf("color").xmap(BleedingDropParticleOptions::new, BleedingDropParticleOptions::color);
    public static final StreamCodec<? super RegistryFriendlyByteBuf, BleedingDropParticleOptions> STREAM_CODEC =
            ByteBufCodecs.INT.map(BleedingDropParticleOptions::new, BleedingDropParticleOptions::color);

    @Override
    public ParticleType<?> getType() {
        return LodgedParticles.BLEEDING_DROP.get();
    }
}
