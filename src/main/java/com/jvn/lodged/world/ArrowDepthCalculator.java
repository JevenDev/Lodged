package com.jvn.lodged.world;

import com.jvn.lodged.config.LodgedConfig;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;

final class ArrowDepthCalculator {
    private ArrowDepthCalculator() {
    }

    static LodgedArrowDepth arrowDepth(LivingEntity target, AbstractArrow arrow, LodgedArrowVisual visual) {
        if (!LodgedConfig.enableArrowDepthTiers()) {
            return LodgedArrowDepth.LODGED;
        }

        LodgedArrowBodyPart bodyPart = visual.bodyPart();
        double deepChance = deepLodgedChance(target, arrow, bodyPart);
        if (deepChance >= 1.0D || (deepChance > 0.0D && target.getRandom().nextDouble() < deepChance)) {
            return LodgedArrowDepth.DEEP_LODGED;
        }

        double shallowChance = shallowLodgedChance(bodyPart, arrow);
        return target.getRandom().nextDouble() < shallowChance
                ? LodgedArrowDepth.SHALLOW
                : LodgedArrowDepth.LODGED;
    }

    private static double deepLodgedChance(LivingEntity target, AbstractArrow arrow, LodgedArrowBodyPart bodyPart) {
        double chance = LodgedConfig.deepLodgedBaseChance(bodyPart);

        double velocity = arrow.getDeltaMovement().length();
        double velocityProgress = Mth.clamp(
                (velocity - LodgedConfig.arrowDepthVelocityBonusStart())
                        / LodgedConfig.arrowDepthVelocityBonusRange(),
                0.0D,
                1.0D);
        chance += velocityProgress * LodgedConfig.velocityDeepLodgedMaxBonus();

        if (arrow.isCritArrow()) {
            chance += LodgedConfig.criticalDeepLodgedBonus();
        }

        ItemStack weapon = arrow.getWeaponItem();
        int powerLevel = powerLevel(weapon);
        double modifiedDamage = modifiedArrowDamage(target, arrow, weapon);
        double weaponDamageBoost = Math.max(0.0D, modifiedDamage - arrow.getBaseDamage());
        chance += Mth.clamp(
                weaponDamageBoost * LodgedConfig.weaponDamageDeepLodgedBonusPerDamage(),
                0.0D,
                LodgedConfig.weaponDamageDeepLodgedMaxBonus());
        chance += powerLevel * LodgedConfig.powerDeepLodgedBonusPerLevel();

        if (arrow.shotFromCrossbow()) {
            chance += LodgedConfig.crossbowDeepLodgedBonus();
        }

        boolean hasBodyPartArmor = hasArmorOnBodyPart(target, bodyPart);
        if (hasBodyPartArmor) {
            chance *= LodgedConfig.armoredDeepLodgedMultiplier();
        } else {
            chance += LodgedConfig.unarmoredDeepLodgedBonus(bodyPart);
        }

        if (!hasBodyPartArmor
                && (bodyPart == LodgedArrowBodyPart.HEAD || bodyPart == LodgedArrowBodyPart.CHEST)
                && arrow.isCritArrow()
                && (powerLevel >= LodgedConfig.strongHeadChestPowerLevelThreshold()
                || modifiedDamage >= LodgedConfig.strongHeadChestDamageThreshold())) {
            chance = Math.max(chance, LodgedConfig.powerHeadChestDeepLodgedChance(powerLevel));
        }

        if (!hasBodyPartArmor
                && (bodyPart == LodgedArrowBodyPart.HEAD || bodyPart == LodgedArrowBodyPart.CHEST)
                && arrow.isCritArrow()
                && arrow.getDeltaMovement().length() >= LodgedConfig.fastCriticalVelocityThreshold()) {
            chance = Math.max(chance, LodgedConfig.fastCriticalHeadChestDeepLodgedChance());
        }

        if (!hasBodyPartArmor && arrow.isCritArrow() && powerLevel > 0) {
            chance = Math.max(chance, LodgedConfig.powerAnyBodyPartDeepLodgedChance(powerLevel));
        }

        return Mth.clamp(chance, 0.0D, LodgedConfig.deepLodgedMaxChance(powerLevel));
    }

    private static double modifiedArrowDamage(LivingEntity target, AbstractArrow arrow, ItemStack weapon) {
        if (weapon == null || weapon.isEmpty() || !(arrow.level() instanceof ServerLevel level)) {
            return arrow.getBaseDamage();
        }

        Entity owner = arrow.getOwner();
        DamageSource damageSource = arrow.damageSources().arrow(arrow, owner != null ? owner : arrow);
        return EnchantmentHelper.modifyDamage(level, weapon, target, damageSource, (float) arrow.getBaseDamage());
    }

    private static int powerLevel(ItemStack weapon) {
        if (weapon == null || weapon.isEmpty()) {
            return 0;
        }

        for (var entry : EnchantmentHelper.getEnchantmentsForCrafting(weapon).entrySet()) {
            if (entry.getKey().is(Enchantments.POWER)) {
                return entry.getIntValue();
            }
        }
        return 0;
    }

    private static double shallowLodgedChance(LodgedArrowBodyPart bodyPart, AbstractArrow arrow) {
        double chance = LodgedConfig.shallowBaseChance(bodyPart);

        double velocity = arrow.getDeltaMovement().length();
        double velocityProgress = Mth.clamp(
                (velocity - LodgedConfig.arrowDepthVelocityBonusStart())
                        / LodgedConfig.arrowDepthVelocityBonusRange(),
                0.0D,
                1.0D);
        chance -= velocityProgress * LodgedConfig.velocityShallowMaxPenalty();
        if (arrow.isCritArrow()) {
            chance -= LodgedConfig.criticalShallowPenalty();
        }
        return Mth.clamp(chance, LodgedConfig.shallowMinChance(), LodgedConfig.shallowMaxChance());
    }

    private static boolean hasArmorOnBodyPart(LivingEntity target, LodgedArrowBodyPart bodyPart) {
        if (LodgedArmorArrowStorage.isHorseArmor(target.getItemBySlot(EquipmentSlot.BODY))) {
            return true;
        }

        return switch (bodyPart) {
            case HEAD -> isArmorInSlot(target, EquipmentSlot.HEAD);
            case CHEST, LEFT_ARM, RIGHT_ARM -> isArmorInSlot(target, EquipmentSlot.CHEST);
            case LEFT_LEG, RIGHT_LEG -> isArmorInSlot(target, EquipmentSlot.LEGS) || isArmorInSlot(target, EquipmentSlot.FEET);
        };
    }

    private static boolean isArmorInSlot(LivingEntity target, EquipmentSlot slot) {
        ItemStack stack = target.getItemBySlot(slot);
        return stack.getItem() instanceof ArmorItem armorItem && armorItem.getEquipmentSlot() == slot;
    }
}
