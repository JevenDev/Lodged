package com.jvn.lodged.effect;

import com.jvn.lodged.config.LodgedConfig;
import com.jvn.lodged.particle.LodgedParticles;
import com.jvn.lodged.world.LodgedArrowVisual;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

public final class BleedingEvents {
    private static final String WOUND_KEY = "lodged:bleeding_wound";
    private static final String WOUNDS_KEY = "lodged:bleeding_wounds";
    private static final String WOUND_RIGHT_KEY = "right";
    private static final String WOUND_HEIGHT_KEY = "height";
    private static final String WOUND_FORWARD_KEY = "forward";
    private static final int MAX_STORED_WOUNDS = 8;
    private static final EquipmentSlot[] ARMOR_SLOTS = {
            EquipmentSlot.HEAD,
            EquipmentSlot.CHEST,
            EquipmentSlot.LEGS,
            EquipmentSlot.FEET
    };

    private BleedingEvents() {
    }

    @SubscribeEvent
    public static void onLivingDamage(LivingDamageEvent.Post event) {
        LivingEntity target = event.getEntity();
        if (target.level().isClientSide() || event.getNewDamage() <= 0.0F) {
            return;
        }

        DamageSource source = event.getSource();
        if (source.is(LodgedDamageTypes.BLEEDING) || !(source.getEntity() instanceof LivingEntity attacker)) {
            return;
        }

        if (!source.isDirect()) {
            return;
        }

        ItemStack weapon = attacker.getMainHandItem();
        if (weapon.isEmpty() || !shouldWeaponApplyBleeding(weapon, target)) {
            return;
        }

        tryApplyBleeding(
                target,
                LodgedConfig.bleedingDamageDuration(),
                woundFromDamage(target, attacker),
                normalArmorBleedingChance(target));
    }

    @SubscribeEvent
    public static void onEntityTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof LivingEntity target) || target.level().isClientSide()) {
            return;
        }

        MobEffectInstance bleeding = target.getEffect(LodgedEffects.BLEEDING);
        if (bleeding == null || !LodgedConfig.bleedingDripParticles() || !canBleed(target)) {
            return;
        }

        int interval = LodgedConfig.bleedingDripInterval(bleeding.getAmplifier());
        if (interval <= 0 || (target.level().getGameTime() + target.getId()) % interval != 0L) {
            return;
        }

        List<Wound> wounds = readWounds(target);
        if (wounds.isEmpty()) {
            Wound wound = randomBodyWound(target);
            rememberWound(target, wound);
            wounds = List.of(wound);
        }

        spawnBleedingParticles(target, wounds, bleeding.getAmplifier() > 0 ? 2 : 1);
    }

    public static boolean tryApplyFromArrowRemoval(LivingEntity target, LodgedArrowVisual arrowVisual, boolean failedRemoval) {
        if (!LodgedConfig.arrowRemovalCausesBleeding()) {
            return false;
        }

        double chance = failedRemoval ? failedArrowRemovalBleedingChance(target) : normalArmorBleedingChance(target);
        return tryApplyBleeding(
                target,
                LodgedConfig.bleedingArrowRemovalDuration(),
                woundFromArrow(target, arrowVisual),
                chance);
    }

    public static boolean tryApplyFromBrokenArrowImpact(LivingEntity target, LodgedArrowVisual arrowVisual) {
        return tryApplyBleeding(
                target,
                LodgedConfig.bleedingDamageDuration(),
                woundFromArrow(target, arrowVisual),
                normalArmorBleedingChance(target));
    }

    public static boolean canBleed(LivingEntity target) {
        if (!LodgedConfig.enableBleeding() || target.getType().is(LodgedTags.BLEEDING_IMMUNE)) {
            return false;
        }

        if (target.getType().is(EntityTypeTags.SKELETONS)) {
            return LodgedConfig.skeletonsAffectedByBleeding()
                    && target.getType().is(LodgedTags.BLEEDING_SKELETONS);
        }

        if (target.getType().is(EntityTypeTags.UNDEAD)) {
            return LodgedConfig.undeadAffectedByBleeding()
                    && target.getType().is(LodgedTags.BLEEDING_UNDEAD);
        }

        return true;
    }

    public static void spawnBleedingParticles(LivingEntity target, int count) {
        List<Wound> wounds = readWounds(target);
        if (wounds.isEmpty()) {
            Wound wound = randomBodyWound(target);
            rememberWound(target, wound);
            wounds = List.of(wound);
        }
        spawnBleedingParticles(target, wounds, count);
    }

    private static void spawnBleedingParticles(LivingEntity target, List<Wound> wounds, int count) {
        if (!(target.level() instanceof ServerLevel level) || count <= 0) {
            return;
        }

        for (Wound wound : wounds) {
            Vec3 position = wound.toWorld(target);
            level.sendParticles(
                    LodgedParticles.BLEEDING_DROP.get(),
                    position.x,
                    position.y,
                    position.z,
                    count,
                    0.018D,
                    0.026D,
                    0.018D,
                    0.012D);
        }
    }

    private static void spawnBleedingParticles(LivingEntity target, Wound wound, int count) {
        spawnBleedingParticles(target, List.of(wound), count);
    }

    private static boolean tryApplyBleeding(LivingEntity target, int addedDuration, Wound wound, double chance) {
        if (target.level().isClientSide() || addedDuration <= 0 || !canBleed(target)) {
            return false;
        }

        if (chance <= 0.0D || (chance < 1.0D && target.getRandom().nextDouble() >= chance)) {
            return false;
        }

        int maxDuration = LodgedConfig.bleedingMaxDuration();
        MobEffectInstance existing = target.getEffect(LodgedEffects.BLEEDING);
        int currentDuration = existing != null ? existing.getDuration() : 0;
        int newDuration = Math.min(maxDuration, currentDuration + addedDuration);
        int amplifier = newDuration >= LodgedConfig.bleedingStrongDurationThreshold() ? 1 : 0;

        rememberWound(target, wound);
        target.addEffect(new MobEffectInstance(LodgedEffects.BLEEDING, newDuration, amplifier, false, false, true));
        spawnBleedingParticles(target, wound, existing == null ? 10 : 5);
        return true;
    }

    private static Wound woundFromArrow(LivingEntity target, LodgedArrowVisual arrowVisual) {
        Vec3 local = arrowVisual.toEntityLocalPosition(target);
        return clampWound(target, new Wound(local.x, local.y, local.z));
    }

    private static Wound woundFromDamage(LivingEntity target, Entity attacker) {
        double height = bodyHitHeight(target);
        double radius = bodyRadius(target);
        Vec3 toAttacker = attacker.position().subtract(target.position());
        BodyAxes axes = BodyAxes.of(target.yBodyRot);
        double right = toAttacker.dot(axes.right());
        double forward = toAttacker.dot(axes.forward());
        double length = Math.sqrt(right * right + forward * forward);
        if (length < 1.0E-4D) {
            return randomBodyWound(target);
        }

        return new Wound(right / length * radius, height, forward / length * radius);
    }

    private static Wound randomBodyWound(LivingEntity target) {
        RandomSource random = target.getRandom();
        double angle = random.nextDouble() * Math.PI * 2.0D;
        double radius = bodyRadius(target);
        return new Wound(Math.cos(angle) * radius, bodyHitHeight(target), Math.sin(angle) * radius);
    }

    private static double bodyHitHeight(LivingEntity target) {
        double height = target.getBbHeight();
        return Mth.clamp(
                height * (0.38D + target.getRandom().nextDouble() * 0.42D),
                Math.min(0.2D, height * 0.25D),
                Math.max(0.3D, height * 0.9D));
    }

    private static double bodyRadius(LivingEntity target) {
        return Math.max(0.055D, target.getBbWidth() * 0.43D);
    }

    private static Wound clampWound(LivingEntity target, Wound wound) {
        double width = Math.max(0.12D, target.getBbWidth());
        double height = Math.max(0.25D, target.getBbHeight());
        double maxSide = width * 0.52D;
        return new Wound(
                Mth.clamp(wound.right(), -maxSide, maxSide),
                Mth.clamp(wound.height(), height * 0.12D, height * 0.94D),
                Mth.clamp(wound.forward(), -maxSide, maxSide));
    }

    private static void rememberWound(LivingEntity target, Wound wound) {
        Wound clampedWound = clampWound(target, wound);
        List<Wound> wounds = new ArrayList<>(readWounds(target));
        wounds.add(clampedWound);
        while (wounds.size() > MAX_STORED_WOUNDS) {
            wounds.remove(0);
        }

        ListTag woundList = new ListTag();
        for (Wound storedWound : wounds) {
            woundList.add(saveWound(storedWound));
        }

        target.getPersistentData().put(WOUNDS_KEY, woundList);
        target.getPersistentData().remove(WOUND_KEY);
    }

    private static CompoundTag saveWound(Wound wound) {
        CompoundTag tag = new CompoundTag();
        tag.putDouble(WOUND_RIGHT_KEY, wound.right());
        tag.putDouble(WOUND_HEIGHT_KEY, wound.height());
        tag.putDouble(WOUND_FORWARD_KEY, wound.forward());
        return tag;
    }

    private static List<Wound> readWounds(LivingEntity target) {
        CompoundTag entityData = target.getPersistentData();
        if (entityData.contains(WOUNDS_KEY, Tag.TAG_LIST)) {
            ListTag woundList = entityData.getList(WOUNDS_KEY, Tag.TAG_COMPOUND);
            List<Wound> wounds = new ArrayList<>(Math.min(woundList.size(), MAX_STORED_WOUNDS));
            int firstIndex = Math.max(0, woundList.size() - MAX_STORED_WOUNDS);
            for (int index = firstIndex; index < woundList.size(); index++) {
                wounds.add(readWound(target, woundList.getCompound(index)));
            }
            return wounds;
        }

        if (!entityData.contains(WOUND_KEY, Tag.TAG_COMPOUND)) {
            return List.of();
        }

        return List.of(readWound(target, entityData.getCompound(WOUND_KEY)));
    }

    private static Wound readWound(LivingEntity target, CompoundTag tag) {
        return clampWound(target, new Wound(
                tag.getDouble(WOUND_RIGHT_KEY),
                tag.getDouble(WOUND_HEIGHT_KEY),
                tag.getDouble(WOUND_FORWARD_KEY)));
    }

    private static boolean shouldWeaponApplyBleeding(ItemStack weapon, LivingEntity target) {
        if (LodgedConfig.weaponsCauseBleeding() && weapon.is(LodgedTags.BLEEDING_WEAPONS)) {
            return true;
        }

        return LodgedConfig.enchantsCauseBleeding() && hasBleedingEnchantment(weapon, target);
    }

    private static boolean hasBleedingEnchantment(ItemStack weapon, LivingEntity target) {
        if (target.getType().is(EntityTypeTags.UNDEAD)) {
            return hasEnchantmentInTag(weapon, LodgedTags.BLEEDING_UNDEAD_ENCHANTMENTS);
        }

        return hasEnchantmentInTag(weapon, LodgedTags.BLEEDING_LIVING_ENCHANTMENTS);
    }

    private static boolean hasEnchantmentInTag(ItemStack weapon, net.minecraft.tags.TagKey<net.minecraft.world.item.enchantment.Enchantment> tag) {
        for (var entry : EnchantmentHelper.getEnchantmentsForCrafting(weapon).entrySet()) {
            if (entry.getIntValue() > 0 && entry.getKey().is(tag)) {
                return true;
            }
        }
        return false;
    }

    private static double normalArmorBleedingChance(LivingEntity target) {
        int armorPieces = armorPieces(target);
        if (armorPieces >= ARMOR_SLOTS.length) {
            return LodgedConfig.fullArmorPreventsBleeding() ? 0.0D : LodgedConfig.fullArmorBleedingChance();
        }

        if (armorPieces > 0) {
            return LodgedConfig.partialArmorPreventsBleeding() ? 0.0D : LodgedConfig.partialArmorBleedingChance();
        }

        return LodgedConfig.noArmorBleedingChance();
    }

    private static double failedArrowRemovalBleedingChance(LivingEntity target) {
        int armorPieces = armorPieces(target);
        if (armorPieces >= ARMOR_SLOTS.length) {
            return LodgedConfig.failedArrowRemovalFullArmorBleedingChance();
        }

        if (armorPieces > 0) {
            return LodgedConfig.failedArrowRemovalPartialArmorBleedingChance();
        }

        return LodgedConfig.failedArrowRemovalNoArmorBleedingChance();
    }

    private static int armorPieces(LivingEntity target) {
        int pieces = 0;
        for (EquipmentSlot slot : ARMOR_SLOTS) {
            if (isArmorInSlot(target.getItemBySlot(slot), slot)) {
                pieces++;
            }
        }
        return pieces;
    }

    private static boolean isArmorInSlot(ItemStack stack, EquipmentSlot slot) {
        return stack.getItem() instanceof ArmorItem armorItem && armorItem.getEquipmentSlot() == slot;
    }

    private record Wound(double right, double height, double forward) {
        Vec3 toWorld(LivingEntity target) {
            BodyAxes axes = BodyAxes.of(target.yBodyRot);
            return new Vec3(target.getX(), target.getBoundingBox().minY + height, target.getZ())
                    .add(axes.right().scale(right))
                    .add(axes.forward().scale(forward));
        }
    }

    private record BodyAxes(Vec3 right, Vec3 forward) {
        static BodyAxes of(float bodyYaw) {
            float yawRadians = bodyYaw * Mth.DEG_TO_RAD;
            Vec3 right = new Vec3(Math.cos(yawRadians), 0.0D, Math.sin(yawRadians));
            Vec3 forward = new Vec3(-Math.sin(yawRadians), 0.0D, Math.cos(yawRadians));
            return new BodyAxes(right, forward);
        }
    }
}
