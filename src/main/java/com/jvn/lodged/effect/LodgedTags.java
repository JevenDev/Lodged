package com.jvn.lodged.effect;

import com.jvn.lodged.Lodged;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.enchantment.Enchantment;

public final class LodgedTags {
    public static final TagKey<Enchantment> BLEEDING_LIVING_ENCHANTMENTS = enchantment("bleeding_living");
    public static final TagKey<Enchantment> BLEEDING_UNDEAD_ENCHANTMENTS = enchantment("bleeding_undead");

    public static final TagKey<Item> BLEEDING_WEAPONS = item("bleeding_weapons");

    public static final TagKey<EntityType<?>> BLEEDING_UNDEAD = entityType("bleeding_undead");
    public static final TagKey<EntityType<?>> BLEEDING_SKELETONS = entityType("bleeding_skeletons");
    public static final TagKey<EntityType<?>> BLEEDING_IMMUNE = entityType("bleeding_immune");
    public static final TagKey<EntityType<?>> TRACKABLE_PROJECTILES = entityType("trackable_projectiles");
    public static final TagKey<EntityType<?>> RECOVERABLE_PROJECTILES = entityType("recoverable_projectiles");
    public static final TagKey<EntityType<?>> BLEEDING_PROJECTILES = entityType("bleeding_projectiles");
    public static final TagKey<EntityType<?>> NON_LODGING_PROJECTILES = entityType("non_lodging_projectiles");

    private LodgedTags() {
    }

    private static TagKey<Enchantment> enchantment(String path) {
        return TagKey.create(Registries.ENCHANTMENT, id(path));
    }

    private static TagKey<Item> item(String path) {
        return TagKey.create(Registries.ITEM, id(path));
    }

    private static TagKey<EntityType<?>> entityType(String path) {
        return TagKey.create(Registries.ENTITY_TYPE, id(path));
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(Lodged.MOD_ID, path);
    }
}
