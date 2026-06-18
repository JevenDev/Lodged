package com.jvn.lodged.effect;

import com.jvn.lodged.Lodged;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffect;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class LodgedEffects {
    public static final DeferredRegister<MobEffect> EFFECTS =
            DeferredRegister.create(Registries.MOB_EFFECT, Lodged.MOD_ID);

    public static final DeferredHolder<MobEffect, MobEffect> BLEEDING =
            EFFECTS.register("bleeding", BleedingMobEffect::new);

    public static final DeferredHolder<MobEffect, MobEffect> DIZZINESS =
            EFFECTS.register("dizziness", DizzinessMobEffect::new);

    private LodgedEffects() {
    }
}
