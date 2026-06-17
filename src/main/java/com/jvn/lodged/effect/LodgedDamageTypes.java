package com.jvn.lodged.effect;

import com.jvn.lodged.Lodged;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageType;

public final class LodgedDamageTypes {
    public static final ResourceKey<DamageType> BLEEDING = ResourceKey.create(
            Registries.DAMAGE_TYPE,
            ResourceLocation.fromNamespaceAndPath(Lodged.MOD_ID, "bleeding"));

    private LodgedDamageTypes() {
    }
}
