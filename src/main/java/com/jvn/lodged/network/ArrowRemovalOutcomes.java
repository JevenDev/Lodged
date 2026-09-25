package com.jvn.lodged.network;

import com.jvn.lodged.config.LodgedConfig;
import com.jvn.lodged.effect.BleedingEvents;
import com.jvn.lodged.effect.LodgedDamageTypes;
import com.jvn.lodged.network.payload.ArrowRemovalResultPayload.Result;
import com.jvn.lodged.network.payload.ArrowRemovalResultPayload;
import com.jvn.lodged.network.payload.RemovePlayerArrowPayload.Target;
import com.jvn.lodged.world.LodgedArmorArrowStorage.LodgedArmorArrowData;
import com.jvn.lodged.world.LodgedArmorArrowStorage;
import com.jvn.lodged.world.LodgedArrowData;
import com.jvn.lodged.world.LodgedArrowStacks;
import com.jvn.lodged.world.LodgedArrowStorage;
import com.jvn.lodged.world.LodgedArrowVisual;
import com.jvn.lodged.world.LodgedShieldArrowStorage.LodgedShieldArrowData;
import com.jvn.lodged.world.LodgedShieldArrowStorage;
import com.jvn.toucanlib.neoforge.network.ToucanNetwork;
import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

final class ArrowRemovalOutcomes {
    private static final int NO_INVENTORY_SLOT = -1;

    private ArrowRemovalOutcomes() {
    }

    static void removeBodyArrow(ServerPlayer player, int arrowIndex) {
        List<LodgedArrowData> arrows = LodgedArrowStorage.readAll(player);
        if (arrowIndex >= arrows.size()) {
            sendRemovalResult(player, Result.CANT_REMOVE_NOW, Target.BODY, InteractionHand.MAIN_HAND, LodgedArrowVisual.DEFAULT);
            LodgedNetwork.syncPlayerArrows(player);
            return;
        }

        LodgedArrowData arrow = arrows.get(arrowIndex);
        double successChance = ArrowRemovalRules.removalSuccessChance(arrow);
        if (successChance <= 0.0D) {
            sendRemovalResult(player, Result.TOO_RISKY, Target.BODY, InteractionHand.MAIN_HAND, arrow.visual());
            return;
        }

        LodgedArrowData removedArrow = LodgedArrowStorage.removeAt(player, arrowIndex);
        if (removedArrow == null) {
            sendRemovalResult(player, Result.CANT_REMOVE_NOW, Target.BODY, InteractionHand.MAIN_HAND, arrow.visual());
            LodgedNetwork.syncPlayerArrows(player);
            return;
        }

        boolean safelyRemoved = succeeds(player, successChance);
        if (safelyRemoved) {
            sendSuccessfulRemoval(player, Target.BODY, InteractionHand.MAIN_HAND, removedArrow.visual(),
                    ArrowRemovalRules.canRecoverArrow(removedArrow) ? removedArrow.stack() : ItemStack.EMPTY);
            ArrowRemovalEffects.playSound(player, SoundEvents.ITEM_PICKUP, 0.2F, 2.0F);
        } else {
            damageForBrokenArrow(player);
            sendRemovalResult(player, Result.FAILED, Target.BODY, InteractionHand.MAIN_HAND, removedArrow.visual());
            ArrowRemovalEffects.playSound(player, SoundEvents.ITEM_BREAK, 0.8F, 1.0F);
        }
        if (removedArrow.causesBleeding()) {
            BleedingEvents.tryApplyFromArrowRemoval(player, removedArrow.visual(), !safelyRemoved);
        }

        LodgedNetwork.syncPlayerArrows(player);
        LodgedNetwork.syncEntityArrows(player);
    }

    static void removeMobBodyArrow(ServerPlayer player, LivingEntity mob, int arrowIndex) {
        List<LodgedArrowData> arrows = LodgedArrowStorage.readAll(mob);
        if (arrowIndex < 0 || arrowIndex >= arrows.size()) {
            sendRemovalResult(player, Result.CANT_REMOVE_NOW, Target.BODY, InteractionHand.MAIN_HAND, LodgedArrowVisual.DEFAULT);
            LodgedNetwork.syncEntityArrows(mob);
            return;
        }

        LodgedArrowData arrow = arrows.get(arrowIndex);
        double baseChance = LodgedConfig.tamedMobArrowRemovalSuccessChance(arrow.visual().bodyPart())
                * LodgedConfig.arrowDepthRemovalSuccessMultiplier(arrow.visual().depth());
        double successChance = ArrowRemovalRules.removalSuccessChance(arrow.infinityGenerated(), baseChance);
        if (successChance <= 0.0D) {
            sendRemovalResult(player, Result.TOO_RISKY, Target.BODY, InteractionHand.MAIN_HAND, arrow.visual());
            return;
        }

        LodgedArrowData removedArrow = LodgedArrowStorage.removeAt(mob, arrowIndex);
        if (removedArrow == null) {
            sendRemovalResult(player, Result.CANT_REMOVE_NOW, Target.BODY, InteractionHand.MAIN_HAND, arrow.visual());
            LodgedNetwork.syncEntityArrows(mob);
            return;
        }

        boolean safelyRemoved = succeeds(player, successChance);
        if (safelyRemoved) {
            sendSuccessfulRemoval(
                    player,
                    Target.BODY,
                    InteractionHand.MAIN_HAND,
                    removedArrow.visual(),
                    ArrowRemovalRules.canRecoverArrow(removedArrow) ? removedArrow.stack() : ItemStack.EMPTY);
            ArrowRemovalEffects.playSound(mob, SoundEvents.ITEM_PICKUP, 0.2F, 2.0F);
        } else {
            sendRemovalResult(player, Result.FAILED, Target.BODY, InteractionHand.MAIN_HAND, removedArrow.visual());
            ArrowRemovalEffects.playSound(mob, SoundEvents.ITEM_BREAK, 0.8F, 1.0F);
        }

        if (LodgedConfig.tamedMobArrowRemovalCausesBleeding() && removedArrow.causesBleeding()) {
            BleedingEvents.tryApplyFromArrowRemoval(mob, removedArrow.visual(), !safelyRemoved);
        }
        if (LodgedConfig.tamedMobArrowRemovalParticles() && mob.level() instanceof ServerLevel level) {
            Vec3 position = ArrowRemovalEffects.bodyArrowWorldPosition(mob, removedArrow.visual());
            level.sendParticles(ParticleTypes.CRIT, position.x, position.y, position.z, 6, 0.04D, 0.04D, 0.04D, 0.02D);
        }

        LodgedNetwork.syncEntityArrows(mob);
    }

    static void removeShieldArrow(ServerPlayer player, InteractionHand hand, int arrowIndex, boolean spawnBreakParticles) {
        ItemStack shield = player.getItemInHand(hand);
        List<LodgedShieldArrowData> arrows = LodgedShieldArrowStorage.readData(shield, player.registryAccess());
        if (arrowIndex >= arrows.size()) {
            sendRemovalResult(player, Result.CANT_REMOVE_NOW, Target.SHIELD, hand, LodgedArrowVisual.DEFAULT);
            LodgedNetwork.syncPlayerArrows(player);
            return;
        }

        LodgedShieldArrowData arrow = arrows.get(arrowIndex);
        double successChance = ArrowRemovalRules.removalSuccessChance(arrow);
        if (successChance <= 0.0D) {
            sendRemovalResult(player, Result.TOO_RISKY, Target.SHIELD, hand, arrow.visual());
            return;
        }

        LodgedShieldArrowData removedArrow = LodgedShieldArrowStorage.removeAt(shield, arrowIndex, player.registryAccess());
        if (removedArrow == null) {
            sendRemovalResult(player, Result.CANT_REMOVE_NOW, Target.SHIELD, hand, arrow.visual());
            LodgedNetwork.syncPlayerArrows(player);
            return;
        }

        boolean safelyRemoved = succeeds(player, successChance);
        if (safelyRemoved) {
            sendSuccessfulRemoval(player, Target.SHIELD, hand, removedArrow.visual(),
                    ArrowRemovalRules.canRecoverArrow(removedArrow) ? removedArrow.stack() : ItemStack.EMPTY);
            ArrowRemovalEffects.playSound(player, SoundEvents.ITEM_PICKUP, 0.2F, 2.0F);
        } else {
            sendRemovalResult(player, Result.FAILED, Target.SHIELD, hand, removedArrow.visual());
            ArrowRemovalEffects.playSound(player, SoundEvents.ITEM_BREAK, 0.8F, 1.0F);
        }

        if (spawnBreakParticles) {
            ArrowRemovalEffects.spawnShieldArrowBreakParticles(player, hand, removedArrow.visual());
        }

        maybeDamageShieldFromArrowRemoval(player, shield, LivingEntity.getSlotForHand(hand));
        player.setItemInHand(hand, shield);
        player.containerMenu.broadcastChanges();
    }

    static void removeArmorArrow(
            ServerPlayer player,
            EquipmentSlot slot,
            int arrowIndex,
            boolean spawnBreakParticles) {
        ItemStack armor = player.getItemBySlot(slot);
        List<LodgedArmorArrowData> arrows = LodgedArmorArrowStorage.readData(armor, player.registryAccess());
        if (arrowIndex >= arrows.size()) {
            sendRemovalResult(player, Result.CANT_REMOVE_NOW, Target.ARMOR, InteractionHand.MAIN_HAND, LodgedArrowVisual.DEFAULT);
            return;
        }

        LodgedArmorArrowData arrow = arrows.get(arrowIndex);
        double successChance = ArrowRemovalRules.removalSuccessChance(arrow);
        if (successChance <= 0.0D) {
            sendRemovalResult(player, Result.TOO_RISKY, Target.ARMOR, InteractionHand.MAIN_HAND, arrow.visual());
            return;
        }

        LodgedArmorArrowData removedArrow = LodgedArmorArrowStorage.removeAt(armor, arrowIndex, player.registryAccess());
        if (removedArrow == null) {
            sendRemovalResult(player, Result.CANT_REMOVE_NOW, Target.ARMOR, InteractionHand.MAIN_HAND, arrow.visual());
            return;
        }

        boolean broke = player.getRandom().nextDouble() < LodgedConfig.armorArrowRemovalBreakChance();
        boolean safelyRemoved = !broke && succeeds(player, successChance);
        if (safelyRemoved) {
            sendSuccessfulRemoval(player, Target.ARMOR, InteractionHand.MAIN_HAND, removedArrow.visual(),
                    ArrowRemovalRules.canRecoverArrow(removedArrow) ? removedArrow.stack() : ItemStack.EMPTY);
            ArrowRemovalEffects.playSound(player, SoundEvents.ITEM_PICKUP, 0.2F, 2.0F);
            maybeDamageArmorFromArrowRemoval(player, armor, slot, LodgedConfig.armorArrowRemovalDurabilityDamageChance());
        } else {
            sendRemovalResult(player, Result.FAILED, Target.ARMOR, InteractionHand.MAIN_HAND, removedArrow.visual());
            ArrowRemovalEffects.playSound(player, SoundEvents.ITEM_BREAK, 0.8F, 1.0F);
            maybeDamageArmorFromArrowRemoval(
                    player,
                    armor,
                    slot,
                    broke
                            ? LodgedConfig.armorArrowBreakDurabilityDamageChance()
                            : LodgedConfig.armorArrowRemovalDurabilityDamageChance());
        }

        if (spawnBreakParticles && !safelyRemoved) {
            ArrowRemovalEffects.spawnArmorArrowBreakParticles(player, removedArrow.visual());
        }

        player.setItemSlot(slot, armor);
        player.containerMenu.broadcastChanges();
    }

    static void removeHorseArmorArrow(
            ServerPlayer player,
            AbstractHorse horse,
            int arrowIndex,
            boolean spawnBreakParticles) {
        ItemStack armor = horse.getItemBySlot(EquipmentSlot.BODY);
        List<LodgedArmorArrowData> arrows = LodgedArmorArrowStorage.readData(armor, player.registryAccess());
        if (arrowIndex < 0 || arrowIndex >= arrows.size()) {
            sendRemovalResult(
                    player,
                    Result.CANT_REMOVE_NOW,
                    Target.ARMOR,
                    InteractionHand.MAIN_HAND,
                    LodgedArrowVisual.DEFAULT);
            player.containerMenu.broadcastChanges();
            return;
        }

        LodgedArmorArrowData arrow = arrows.get(arrowIndex);
        double successChance = ArrowRemovalRules.removalSuccessChance(arrow);
        if (successChance <= 0.0D) {
            sendRemovalResult(player, Result.TOO_RISKY, Target.ARMOR, InteractionHand.MAIN_HAND, arrow.visual());
            return;
        }

        LodgedArmorArrowData removedArrow =
                LodgedArmorArrowStorage.removeAt(armor, arrowIndex, player.registryAccess());
        if (removedArrow == null) {
            sendRemovalResult(player, Result.CANT_REMOVE_NOW, Target.ARMOR, InteractionHand.MAIN_HAND, arrow.visual());
            player.containerMenu.broadcastChanges();
            return;
        }

        boolean broke = player.getRandom().nextDouble() < LodgedConfig.armorArrowRemovalBreakChance();
        boolean safelyRemoved = !broke && succeeds(player, successChance);
        if (safelyRemoved) {
            sendSuccessfulRemoval(
                    player,
                    Target.ARMOR,
                    InteractionHand.MAIN_HAND,
                    removedArrow.visual(),
                    ArrowRemovalRules.canRecoverArrow(removedArrow) ? removedArrow.stack() : ItemStack.EMPTY);
            ArrowRemovalEffects.playSound(horse, SoundEvents.ITEM_PICKUP, 0.2F, 2.0F);
            maybeDamageHorseArmor(
                    horse,
                    armor,
                    LodgedConfig.armorArrowRemovalDurabilityDamageChance());
        } else {
            sendRemovalResult(player, Result.FAILED, Target.ARMOR, InteractionHand.MAIN_HAND, removedArrow.visual());
            ArrowRemovalEffects.playSound(horse, SoundEvents.ITEM_BREAK, 0.8F, 1.0F);
            maybeDamageHorseArmor(
                    horse,
                    armor,
                    broke
                            ? LodgedConfig.armorArrowBreakDurabilityDamageChance()
                            : LodgedConfig.armorArrowRemovalDurabilityDamageChance());
        }

        if (spawnBreakParticles && !safelyRemoved) {
            ArrowRemovalEffects.spawnArmorArrowBreakParticles(horse, removedArrow.visual());
        }

        horse.setItemSlot(EquipmentSlot.BODY, armor);
        player.containerMenu.broadcastChanges();
        LodgedNetwork.syncEntityArrows(horse);
    }

    private static ArrowRecoveryResult recoverArrow(Player player, ItemStack storedStack) {
        ItemStack recoveredStack = LodgedArrowStacks.copyForRecovery(storedStack);
        if (recoveredStack.isEmpty()) {
            return new ArrowRecoveryResult(false, NO_INVENTORY_SLOT);
        }

        int inventorySlot = player.getInventory().getSlotWithRemainingSpace(recoveredStack);
        if (inventorySlot == -1) {
            inventorySlot = player.getInventory().getFreeSlot();
        }

        if (inventorySlot >= 0 && player.getInventory().add(inventorySlot, recoveredStack)) {
            return new ArrowRecoveryResult(false, inventorySlot);
        }

        if (!recoveredStack.isEmpty()) {
            player.drop(recoveredStack, false);
            return new ArrowRecoveryResult(true, NO_INVENTORY_SLOT);
        }
        return new ArrowRecoveryResult(false, inventorySlot);
    }

    private static boolean succeeds(ServerPlayer player, double chance) {
        return chance >= 1.0D || chance > 0.0D && player.getRandom().nextDouble() < chance;
    }

    private static void sendSuccessfulRemoval(
            ServerPlayer player,
            Target target,
            InteractionHand hand,
            LodgedArrowVisual arrow,
            ItemStack recoverableStack) {
        if (recoverableStack.isEmpty()) {
            sendRemovalResult(player, Result.REMOVED, target, hand, arrow);
            return;
        }

        ArrowRecoveryResult recoveryResult = recoverArrow(player, recoverableStack);
        sendRemovalResult(
                player,
                recoveryResult.inventoryFull() ? Result.INVENTORY_FULL : Result.SUCCESS,
                target,
                hand,
                arrow,
                recoveryResult.inventorySlot());
    }

    private static void damageForBrokenArrow(ServerPlayer player) {
        float damage = LodgedConfig.playerArrowRemovalBreakDamage();
        if (damage > 0.0F) {
            Holder<DamageType> arrowRemoval = player.level().registryAccess()
                    .registryOrThrow(Registries.DAMAGE_TYPE)
                    .getHolderOrThrow(LodgedDamageTypes.ARROW_REMOVAL);
            Vec3 previousMovement = player.getDeltaMovement();
            player.hurt(new DamageSource(arrowRemoval), damage);
            if (!LodgedConfig.arrowRemovalAppliesKnockback()) {
                player.setDeltaMovement(previousMovement);
            }
        }
    }

    private static void maybeDamageShieldFromArrowRemoval(ServerPlayer player, ItemStack shield, EquipmentSlot slot) {
        if (shield.isEmpty()
                || player.getAbilities().instabuild
                || player.getRandom().nextDouble() >= LodgedConfig.shieldArrowDurabilityDamageChance()) {
            return;
        }

        shield.hurtAndBreak(1, player, slot);
    }

    private static void maybeDamageArmorFromArrowRemoval(
            ServerPlayer player,
            ItemStack armor,
            EquipmentSlot slot,
            double chance) {
        if (armor.isEmpty()
                || chance <= 0.0D
                || player.getAbilities().instabuild
                || player.getRandom().nextDouble() >= chance) {
            return;
        }

        armor.hurtAndBreak(1, player, slot);
    }

    private static void maybeDamageHorseArmor(AbstractHorse horse, ItemStack armor, double chance) {
        if (armor.isEmpty()
                || chance <= 0.0D
                || horse.getRandom().nextDouble() >= chance) {
            return;
        }

        armor.hurtAndBreak(1, horse, EquipmentSlot.BODY);
    }

    static void sendRemovalResult(
            ServerPlayer player,
            Result result,
            Target target,
            InteractionHand hand,
            LodgedArrowVisual arrow) {
        sendRemovalResult(player, result, target, hand, arrow, NO_INVENTORY_SLOT);
    }

    static void sendRemovalResult(
            ServerPlayer player,
            Result result,
            Target target,
            InteractionHand hand,
            LodgedArrowVisual arrow,
            int inventorySlot) {
        ToucanNetwork.sendToPlayer(player, new ArrowRemovalResultPayload(result, target, hand, arrow, inventorySlot));
    }

    private record ArrowRecoveryResult(boolean inventoryFull, int inventorySlot) {
    }
}
