package com.jvn.lodged.world;

import com.jvn.lodged.config.LodgedConfig;
import com.jvn.lodged.effect.BleedingEvents;
import com.jvn.lodged.network.LodgedNetwork;
import com.jvn.lodged.network.PlayerArrowRemoval;
import com.jvn.lodged.world.ArrowImpactRules.ProjectileStack;
import com.jvn.lodged.world.PendingArrowImpacts.ArmorArrowImpact;
import com.jvn.lodged.world.PendingArrowImpacts.BodyArrowImpact;
import com.jvn.lodged.world.PendingArrowImpacts.BrokenArrowImpact;
import com.jvn.lodged.world.PendingArrowImpacts.ShieldArrowImpact;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.EntityHitResult;
import net.neoforged.neoforge.common.ItemAbilities;
import net.neoforged.neoforge.event.entity.living.ArmorHurtEvent;
import net.neoforged.neoforge.event.entity.living.LivingShieldBlockEvent;

final class ArrowImpactResolution {
    private ArrowImpactResolution() {
    }

    static void onResolvedEntityHit(
            AbstractArrow arrow,
            LivingEntity target,
            boolean damageAccepted) {
        if (arrow.level().isClientSide()) {
            return;
        }

        UUID arrowUuid = arrow.getUUID();
        UUID targetUuid = target.getUUID();
        ArmorArrowImpact armorImpact = PendingArrowImpacts.consumeArmorImpact(arrowUuid, targetUuid, damageAccepted);
        BrokenArrowImpact brokenImpact = PendingArrowImpacts.consumeBrokenImpact(arrowUuid, targetUuid, damageAccepted);
        BodyArrowImpact bodyImpact = PendingArrowImpacts.consumeBodyImpact(arrowUuid, targetUuid, damageAccepted);

        if (armorImpact != null) {
            processArmorArrowImpact(target, arrow, armorImpact);
            return;
        }

        if (brokenImpact != null) {
            processBrokenArrowImpact(target, arrow, brokenImpact);
            return;
        }

        if (bodyImpact == null
                || !ArrowImpactConfirmation.hasVanillaBodyArrow(
                        arrow.isRemoved(),
                        arrow.getPierceLevel())) {
            return;
        }

        applyLegShotSlowness(target, bodyImpact.arrowData().visual());
        LodgedArrowStorage.add(target, bodyImpact.arrowData());
        syncTrackedArrows(target);
    }

    static void onShieldBlock(LivingShieldBlockEvent event) {
        LivingEntity blocker = event.getEntity();
        if (PlayerArrowRemoval.isRemovingShieldArrow(blocker)) {
            event.setBlocked(false);
            event.setBlockedDamage(0.0F);
            event.setShieldDamage(0.0F);
            return;
        }

        if (event.isCanceled()
                || blocker.level().isClientSide()
                || !event.getBlocked()
                || event.getDamageSource().is(DamageTypeTags.BYPASSES_SHIELD)
                || !(event.getDamageSource().getDirectEntity() instanceof AbstractArrow arrow)
                || !ArrowImpactRules.isSupportedArrow(arrow)) {
            return;
        }

        ItemStack shield = blocker.getUseItem();
        if (shield.isEmpty() || !shield.canPerformAction(ItemAbilities.SHIELD_BLOCK)) {
            return;
        }

        Entity owner = arrow.getOwner();
        boolean fromPlayer = owner instanceof Player;
        boolean fromMob = owner instanceof LivingEntity && !fromPlayer;
        boolean creativeGenerated = owner instanceof Player player
                && player.getAbilities().instabuild
                && arrow.pickup == AbstractArrow.Pickup.CREATIVE_ONLY;
        boolean infinityGenerated = fromPlayer
                && !creativeGenerated
                && arrow.pickup == AbstractArrow.Pickup.CREATIVE_ONLY;

        if (LodgedConfig.enableArrowBreakOnEntityHit()
                && ArrowImpactRules.breaksOnImpact(arrow, fromPlayer, fromMob, infinityGenerated, creativeGenerated)) {
            PendingArrowImpacts.removeShieldImpact(arrow.getUUID());
            playArrowBreakSound(arrow);
            arrow.discard();
            return;
        }

        ShieldArrowImpact rememberedImpact = PendingArrowImpacts.removeShieldImpact(arrow.getUUID());
        if (!LodgedConfig.enableShieldArrowLodging() || !ArrowImpactRules.canLodge(arrow)) {
            return;
        }

        LodgedArrowVisual visual = rememberedImpact != null && blocker.getUUID().equals(rememberedImpact.targetUuid())
                ? rememberedImpact.visual()
                : LodgedArrowVisual.fromShieldImpact(blocker, arrow, new EntityHitResult(blocker, arrow.position()));

        boolean willEvictArrow = willEvictShieldArrow(shield);
        ProjectileStack projectileStack = ArrowImpactRules.projectileStack(arrow);
        if (LodgedShieldArrowStorage.add(
                shield,
                new LodgedShieldArrowStorage.LodgedShieldArrowData(
                        projectileStack.stack(),
                        projectileStack.recoverable(),
                        fromPlayer,
                        infinityGenerated,
                        creativeGenerated,
                        visual),
                blocker.registryAccess())) {
            if (willEvictArrow) {
                maybeDamageShieldFromArrowRemoval(blocker, shield);
            }
            blocker.setItemInHand(blocker.getUsedItemHand(), shield);
            if (blocker instanceof ServerPlayer player) {
                player.containerMenu.broadcastChanges();
            }
            arrow.discard();
        }
    }

    static void onArmorHurt(ArmorHurtEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide()
                || !LodgedConfig.enableArmorArrowDurabilityPenalty()
                || LodgedConfig.armorArrowExtraDurabilityLossAmount() <= 0) {
            return;
        }

        for (EquipmentSlot slot : LodgedArmorArrowStorage.armorSlots()) {
            ItemStack armor = event.getArmorItemStack(slot);
            int arrowCount = LodgedArmorArrowStorage.count(armor);
            if (arrowCount <= 0 || event.getNewDamage(slot) <= 0.0F) {
                continue;
            }

            double chance = armorArrowExtraDurabilityLossChance(arrowCount);
            if (chance >= 1.0D || entity.getRandom().nextDouble() < chance) {
                event.setNewDamage(
                        slot,
                        event.getNewDamage(slot) + LodgedConfig.armorArrowExtraDurabilityLossAmount());
            }
        }
    }

    private static void applyLegShotSlowness(LivingEntity target, LodgedArrowVisual visual) {
        int duration = LodgedConfig.legShotSlownessDuration();
        if (!LodgedConfig.enableLegShotSlowness()
                || duration <= 0
                || !visual.bodyPart().isLeg()
                || !(target instanceof ServerPlayer player)) {
            return;
        }

        player.addEffect(new MobEffectInstance(
                MobEffects.MOVEMENT_SLOWDOWN,
                duration,
                LodgedConfig.legShotSlownessAmplifier(),
                false,
                false,
                true));
    }

    private static void processArmorArrowImpact(LivingEntity target, AbstractArrow arrow, ArmorArrowImpact impact) {
        ItemStack armor = target.getItemBySlot(impact.slot());
        if (!(armor.getItem() instanceof ArmorItem armorItem) || armorItem.getEquipmentSlot() != impact.slot()) {
            return;
        }

        if (impact.breaks()) {
            playArrowBreakSound(target, arrow);
            maybeDamageArmor(target, armor, impact.slot(), LodgedConfig.armorArrowBreakDurabilityDamageChance());
            target.setItemSlot(impact.slot(), armor);
            LodgedArrowStorage.removeVanillaBodyArrow(target, arrow, impact.pierceLevel());
            discardArrow(arrow);
            syncTrackedArrows(target);
            return;
        }

        boolean willEvictArrow = willEvictArmorArrow(armor);
        if (LodgedArmorArrowStorage.add(
                armor,
                new LodgedArmorArrowStorage.LodgedArmorArrowData(
                        impact.stack(),
                        impact.recoverable(),
                        impact.fromPlayer(),
                        impact.infinityGenerated(),
                        impact.creativeGenerated(),
                        impact.visual()),
                target.registryAccess())) {
            if (willEvictArrow) {
                maybeDamageArmor(target, armor, impact.slot(), LodgedConfig.armorArrowRemovalDurabilityDamageChance());
            }
            target.setItemSlot(impact.slot(), armor);
            LodgedArrowStorage.removeVanillaBodyArrow(target, arrow, impact.pierceLevel());
            discardArrow(arrow);
            if (target instanceof ServerPlayer player) {
                player.containerMenu.broadcastChanges();
            }
            syncTrackedArrows(target);
        }
    }

    private static void processBrokenArrowImpact(
            LivingEntity target,
            AbstractArrow arrow,
            BrokenArrowImpact impact) {
        if (impact.causesBleeding()) {
            BleedingEvents.tryApplyFromBrokenArrowImpact(target, impact.visual());
        }
        playArrowBreakSound(arrow);
        LodgedArrowStorage.removeVanillaBodyArrow(target, arrow, arrow.getPierceLevel());
        discardArrow(arrow);
        syncTrackedArrows(target);
    }

    private static void discardArrow(AbstractArrow arrow) {
        arrow.discard();
    }

    private static void syncTrackedArrows(LivingEntity target) {
        LodgedNetwork.syncEntityArrows(target);
        if (target instanceof ServerPlayer player) {
            LodgedNetwork.syncPlayerArrows(player);
        }
    }

    private static double armorArrowExtraDurabilityLossChance(int arrowCount) {
        double chance = LodgedConfig.armorArrowExtraDurabilityLossChance()
                + Math.max(0, arrowCount - 1) * LodgedConfig.armorArrowExtraDurabilityLossChancePerAdditionalArrow();
        return Mth.clamp(chance, 0.0D, LodgedConfig.armorArrowExtraDurabilityLossMaxChance());
    }

    private static void playArrowBreakSound(AbstractArrow arrow) {
        arrow.level().playSound(
                null,
                arrow.getX(),
                arrow.getY(),
                arrow.getZ(),
                SoundEvents.ITEM_BREAK,
                SoundSource.NEUTRAL,
                0.8F,
                1.0F);
    }

    private static void playArrowBreakSound(LivingEntity target, AbstractArrow arrow) {
        if (arrow != null) {
            playArrowBreakSound(arrow);
            return;
        }

        target.level().playSound(
                null,
                target.getX(),
                target.getY(),
                target.getZ(),
                SoundEvents.ITEM_BREAK,
                SoundSource.NEUTRAL,
                0.8F,
                1.0F);
    }

    private static boolean willEvictShieldArrow(ItemStack shield) {
        int maxTrackedArrows = LodgedConfig.maxTrackedArrowsPerShield();
        return maxTrackedArrows > 0 && LodgedShieldArrowStorage.count(shield) >= maxTrackedArrows;
    }

    private static boolean willEvictArmorArrow(ItemStack armor) {
        int maxTrackedArrows = LodgedConfig.maxTrackedArrowsPerArmorPiece();
        return maxTrackedArrows > 0 && LodgedArmorArrowStorage.count(armor) >= maxTrackedArrows;
    }

    private static void maybeDamageShieldFromArrowRemoval(LivingEntity holder, ItemStack shield) {
        maybeDamageShieldFromArrowRemoval(holder, shield, LivingEntity.getSlotForHand(holder.getUsedItemHand()));
    }

    private static void maybeDamageShieldFromArrowRemoval(LivingEntity holder, ItemStack shield, EquipmentSlot slot) {
        if (shield.isEmpty()
                || holder.level().random.nextDouble() >= LodgedConfig.shieldArrowDurabilityDamageChance()
                || holder instanceof Player player && player.getAbilities().instabuild) {
            return;
        }

        shield.hurtAndBreak(1, holder, slot);
    }

    private static void maybeDamageArmor(LivingEntity holder, ItemStack armor, EquipmentSlot slot, double chance) {
        if (armor.isEmpty()
                || chance <= 0.0D
                || holder.level().random.nextDouble() >= chance
                || holder instanceof Player player && player.getAbilities().instabuild) {
            return;
        }

        armor.hurtAndBreak(1, holder, slot);
    }

    static void tickBlockBreak(AbstractArrow arrow) {
        if (PendingArrowImpacts.removeDueBlockBreak(arrow)) {
            playArrowBreakSound(arrow);
            arrow.discard();
        }
    }
}
