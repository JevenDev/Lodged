package com.jvn.lodged.network;

import com.jvn.lodged.compat.PlayerInventoryMenus;
import com.jvn.lodged.config.LodgedConfig;
import com.jvn.lodged.mixin.HorseInventoryMenuAccessor;
import com.jvn.lodged.network.ArrowRemovalAttempts.ArmorArrowRemovalAttempt;
import com.jvn.lodged.network.payload.RemoveHorseArmorArrowPayload;
import com.jvn.lodged.network.payload.RemovePlayerArrowPayload.Target;
import com.jvn.lodged.network.payload.RemovePlayerArrowPayload;
import com.jvn.lodged.world.LodgedArmorArrowStorage.LodgedArmorArrowData;
import com.jvn.lodged.world.LodgedArmorArrowStorage;
import com.jvn.lodged.world.LodgedArrowData;
import com.jvn.lodged.world.LodgedArrowStorage;
import com.jvn.lodged.world.LodgedArrowVisual;
import com.jvn.lodged.world.LodgedShieldArrowStorage.LodgedShieldArrowData;
import com.jvn.lodged.world.LodgedShieldArrowStorage;
import java.util.List;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.inventory.HorseInventoryMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.ItemAbilities;

final class ArrowRemovalValidation {
    private static final ArrowRemovalCooldown REQUEST_COOLDOWN = new ArrowRemovalCooldown(2);

    private ArrowRemovalValidation() {
    }

    static boolean acceptRequest(ServerPlayer player) {
        return REQUEST_COOLDOWN.accept(player.getUUID(), player.level().getGameTime());
    }

    static boolean canAttemptRemoval(ServerPlayer player, RemovePlayerArrowPayload payload) {
        if (!LodgedConfig.enablePlayerArrowRemoval() || !player.isAlive()) {
            return false;
        }

        if (payload.arrowIndex() < 0) {
            return false;
        }

        if (payload.target() == Target.BODY) {
            if (payload.arrowIndex() >= LodgedConfig.maxRemovablePlayerArrows()
                    || payload.arrowIndex() >= LodgedArrowStorage.readAll(player).size()) {
                return false;
            }
        } else if (payload.target() == Target.SHIELD) {
            int maxShieldArrows = LodgedConfig.maxTrackedArrowsPerShield();
            ItemStack shield = player.getItemInHand(payload.hand());
            if (maxShieldArrows <= 0
                    || payload.arrowIndex() >= maxShieldArrows
                    || shield.isEmpty()
                    || !shield.canPerformAction(ItemAbilities.SHIELD_BLOCK)
                    || payload.arrowIndex() >= LodgedShieldArrowStorage.readData(shield, player.registryAccess()).size()) {
                return false;
            }
        } else if (payload.target() == Target.ARMOR) {
            if (!isArmorSlot(payload.armorSlot())
                    || payload.arrowIndex() >= LodgedConfig.maxTrackedArrowsPerArmorPiece()
                    || !canAttemptArmorRemoval(player, payload.armorSlot(), payload.arrowIndex())) {
                return false;
            }
        } else {
            return false;
        }

        if (player.isCreative() && !LodgedConfig.allowArrowRemovalInCreative()) {
            return false;
        }

        if (LodgedConfig.requireInventoryScreenForRemoval()
                && (!PlayerInventoryMenus.supportsArrowRemoval(player.containerMenu)
                        || !player.containerMenu.stillValid(player))) {
            return false;
        }

        return true;
    }

    static AbstractHorse openHorse(ServerPlayer player) {
        if (!(player.containerMenu instanceof HorseInventoryMenu menu) || !menu.stillValid(player)) {
            return null;
        }
        return ((HorseInventoryMenuAccessor) menu).lodged$getHorse();
    }

    static boolean canAttemptHorseArmorRemoval(
            ServerPlayer player,
            AbstractHorse horse,
            RemoveHorseArmorArrowPayload payload) {
        if (!LodgedConfig.enablePlayerArrowRemoval()
                || !LodgedConfig.enableArmorArrowLodging()
                || LodgedConfig.maxTrackedArrowsPerArmorPiece() <= 0
                || !player.isAlive()
                || horse == null
                || horse.getId() != payload.horseEntityId()
                || !horse.isAlive()
                || payload.arrowIndex() < 0
                || payload.arrowIndex() >= LodgedConfig.maxTrackedArrowsPerArmorPiece()) {
            return false;
        }

        if (player.isCreative() && !LodgedConfig.allowArrowRemovalInCreative()) {
            return false;
        }

        if (payload.target() == Target.BODY) {
            return LodgedConfig.enableTamedMobArrowRemoval()
                    && canAttemptTamedMobRemoval(player, horse, false)
                    && payload.arrowIndex() < LodgedArrowStorage.readAll(horse).size();
        }
        if (payload.target() != Target.ARMOR) {
            return false;
        }

        ItemStack armor = horse.getItemBySlot(EquipmentSlot.BODY);
        return LodgedArmorArrowStorage.isHorseArmor(armor)
                && payload.arrowIndex()
                        < LodgedArmorArrowStorage.readData(armor, player.registryAccess()).size();
    }

    static boolean canAttemptHeldShieldRemoval(ServerPlayer player, ItemStack shield) {
        return canAttemptHeldShieldRemoval(player, shield, 0, null);
    }

    static boolean canAttemptHeldShieldRemoval(
            ServerPlayer player,
            ItemStack shield,
            int arrowIndex,
            LodgedArrowVisual expectedVisual) {
        if (!LodgedConfig.enablePlayerArrowRemoval()
                || !player.isAlive()
                || shield.isEmpty()
                || !shield.canPerformAction(ItemAbilities.SHIELD_BLOCK)
                || LodgedConfig.maxTrackedArrowsPerShield() <= 0) {
            return false;
        }

        if (player.isCreative() && !LodgedConfig.allowArrowRemovalInCreative()) {
            return false;
        }

        List<LodgedShieldArrowData> arrows = LodgedShieldArrowStorage.readData(shield, player.registryAccess());
        return arrowIndex >= 0
                && arrowIndex < arrows.size()
                && (expectedVisual == null || arrows.get(arrowIndex).visual().matches(expectedVisual));
    }

    private static boolean canAttemptArmorRemoval(ServerPlayer player, EquipmentSlot slot, int arrowIndex) {
        return canAttemptArmorRemoval(player, slot, arrowIndex, null);
    }

    private static boolean canAttemptArmorRemoval(
            ServerPlayer player,
            EquipmentSlot slot,
            int arrowIndex,
            LodgedArrowVisual expectedVisual) {
        if (!LodgedConfig.enablePlayerArrowRemoval()
                || !LodgedConfig.enableArmorArrowLodging()
                || !player.isAlive()
                || LodgedConfig.maxTrackedArrowsPerArmorPiece() <= 0) {
            return false;
        }

        if (player.isCreative() && !LodgedConfig.allowArrowRemovalInCreative()) {
            return false;
        }

        ItemStack armor = player.getItemBySlot(slot);
        List<LodgedArmorArrowData> arrows = LodgedArmorArrowStorage.readData(armor, player.registryAccess());
        return arrowIndex >= 0
                && arrowIndex < arrows.size()
                && (expectedVisual == null || arrows.get(arrowIndex).visual().matches(expectedVisual));
    }

    private static boolean isArmorSlot(EquipmentSlot slot) {
        return slot == EquipmentSlot.HEAD
                || slot == EquipmentSlot.CHEST
                || slot == EquipmentSlot.LEGS
                || slot == EquipmentSlot.FEET;
    }

    static boolean canAttemptInWorldRemoval(ServerPlayer player, ArmorArrowRemovalAttempt attempt) {
        if (attempt.targetEntityId() != player.getId()) {
            Entity entity = player.level().getEntity(attempt.targetEntityId());
            if (!(entity instanceof LivingEntity mob) || !canAttemptTamedMobRemoval(player, mob, false)) {
                return false;
            }
            if (attempt.target() == Target.ARMOR) {
                return mob instanceof AbstractHorse horse
                        && hasExpectedMobArmorArrow(horse, attempt.arrowIndex(), attempt.visual());
            }
            return hasExpectedMobBodyArrow(mob, attempt.arrowIndex(), attempt.visual());
        }
        if (attempt.target() == Target.BODY) {
            return canAttemptBodyRemoval(player, attempt.arrowIndex(), attempt.visual());
        }

        return canAttemptArmorRemoval(player, attempt.slot(), attempt.arrowIndex(), attempt.visual());
    }

    private static boolean canAttemptBodyRemoval(
            ServerPlayer player,
            int arrowIndex,
            LodgedArrowVisual expectedVisual) {
        if (!LodgedConfig.enablePlayerArrowRemoval() || !player.isAlive() || arrowIndex < 0) {
            return false;
        }

        if (player.isCreative() && !LodgedConfig.allowArrowRemovalInCreative()) {
            return false;
        }

        List<LodgedArrowData> arrows = LodgedArrowStorage.readAll(player);
        return arrowIndex < LodgedConfig.maxRemovablePlayerArrows()
                && arrowIndex < arrows.size()
                && (expectedVisual == null || arrows.get(arrowIndex).visual().matches(expectedVisual));
    }

    static boolean canAttemptTamedMobRemoval(ServerPlayer player, LivingEntity mob, boolean requireInitialAim) {
        if (!LodgedConfig.enableTamedMobArrowRemoval()
                || !player.isAlive()
                || !mob.isAlive()
                || player.getVehicle() != mob
                && distanceToBounds(player, mob) > LodgedConfig.tamedMobArrowRemovalRange()
                || !player.hasLineOfSight(mob)) {
            return false;
        }

        boolean tamed;
        boolean owned;
        if (mob instanceof TamableAnimal tamable) {
            tamed = tamable.isTame();
            owned = tamable.isOwnedBy(player);
        } else if (mob instanceof AbstractHorse horse) {
            tamed = horse.isTamed();
            owned = player.getUUID().equals(horse.getOwnerUUID());
        } else {
            return false;
        }
        if (!tamed || LodgedConfig.requireMobOwnership() && !owned) {
            return false;
        }
        if (player.getVehicle() == mob) {
            return true;
        }
        if (!requireInitialAim) {
            return true;
        }
        Vec3 eye = player.getEyePosition();
        double aimDistance = LodgedConfig.tamedMobArrowRemovalRange() + player.getEyeHeight();
        Vec3 end = eye.add(player.getViewVector(1.0F).scale(aimDistance));
        return mob.getBoundingBox().inflate(0.3D).clip(eye, end).isPresent();
    }

    private static double distanceToBounds(Entity source, Entity target) {
        return Math.sqrt(target.getBoundingBox().distanceToSqr(source.position()));
    }

    private static boolean hasExpectedMobBodyArrow(LivingEntity mob, int arrowIndex, LodgedArrowVisual expectedVisual) {
        List<LodgedArrowData> arrows = LodgedArrowStorage.readAll(mob);
        return arrowIndex >= 0
                && arrowIndex < arrows.size()
                && arrows.get(arrowIndex).visual().matches(expectedVisual);
    }

    private static boolean hasExpectedMobArmorArrow(
            AbstractHorse horse,
            int arrowIndex,
            LodgedArrowVisual expectedVisual) {
        List<LodgedArmorArrowData> arrows =
                LodgedArmorArrowStorage.readData(horse.getItemBySlot(EquipmentSlot.BODY), horse.registryAccess());
        return arrowIndex >= 0
                && arrowIndex < arrows.size()
                && arrows.get(arrowIndex).visual().matches(expectedVisual);
    }

    static void clearCooldown(ServerPlayer player) {
        REQUEST_COOLDOWN.clear(player.getUUID());
    }

    static void clear() {
        REQUEST_COOLDOWN.clear();
    }
}
