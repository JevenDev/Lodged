package com.jvn.lodged.effect;

import com.jvn.lodged.Lodged;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.damagesource.DamageType;

public final class LodgedDamageTypes {
    public static final ResourceKey<DamageType> BLEEDING = ResourceKey.create(
            Registries.DAMAGE_TYPE,
            Lodged.IDS.id("bleeding"));
    public static final ResourceKey<DamageType> ARROW_REMOVAL = ResourceKey.create(
            Registries.DAMAGE_TYPE,
            Lodged.IDS.id("arrow_removal"));

    private LodgedDamageTypes() {
    }
}
