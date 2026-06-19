package com.jvn.lodged.effect;

import com.jvn.lodged.config.LodgedConfig;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

public final class BleedingMobEffect extends MobEffect {
    public BleedingMobEffect() {
        super(MobEffectCategory.HARMFUL, 0x7A0C0C);
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        int interval = LodgedConfig.bleedingTickInterval(amplifier);
        return interval > 0 && duration % interval == 0;
    }

    @Override
    public boolean applyEffectTick(LivingEntity entity, int amplifier) {
        if (!(entity.level() instanceof ServerLevel level) || !LodgedConfig.enableBleeding()) {
            return true;
        }

        if (!BleedingEvents.canBleed(entity)) {
            return true;
        }

        Holder<DamageType> bleeding = level.registryAccess()
                .registryOrThrow(Registries.DAMAGE_TYPE)
                .getHolderOrThrow(LodgedDamageTypes.BLEEDING);

        float damage = LodgedConfig.bleedingDamage(amplifier);
        if (damage > 0.0F) {
            entity.hurt(new DamageSource(bleeding), damage);
        }

        BleedingEvents.spawnBleedingParticles(entity, 3 + amplifier * 2);
        return true;
    }
}
