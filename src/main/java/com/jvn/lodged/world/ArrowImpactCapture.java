package com.jvn.lodged.world;

import com.jvn.lodged.config.LodgedConfig;
import com.jvn.lodged.network.PlayerArrowRemoval;
import com.jvn.lodged.world.ArrowImpactRules.ProjectileStack;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.ItemAbilities;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;

final class ArrowImpactCapture {
    private ArrowImpactCapture() {
    }

    static void onProjectileImpact(ProjectileImpactEvent event) {
        if (event.isCanceled() || event.getProjectile().level().isClientSide()) {
            return;
        }

        if (!(event.getProjectile() instanceof AbstractArrow arrow) || !ArrowImpactRules.isSupportedArrow(arrow)) {
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
        HitResult hitResult = event.getRayTraceResult();
        if (hitResult instanceof BlockHitResult
                && LodgedConfig.enableArrowBreakOnBlockHit()
                && ArrowImpactRules.breaksOnImpact(arrow, fromPlayer, fromMob, infinityGenerated, creativeGenerated)) {
            PendingArrowImpacts.rememberPendingBlockArrowBreak(arrow);
            return;
        }

        if (!(hitResult instanceof EntityHitResult entityHitResult)
                || !(entityHitResult.getEntity() instanceof LivingEntity target)) {
            return;
        }

        PendingArrowImpacts.clearPendingEntityImpacts(arrow.getUUID());

        boolean canLodge = ArrowImpactRules.canLodge(arrow);
        if (!canLodge) {
            if (wouldShieldBlock(target, arrow)) {
                return;
            }

            if (LodgedConfig.enableArrowBreakOnEntityHit()
                    && ArrowImpactRules.breaksOnImpact(arrow, fromPlayer, fromMob, infinityGenerated, creativeGenerated)) {
                PendingArrowImpacts.rememberBrokenArrowImpact(arrow, target, LodgedArrowVisual.fromImpact(target, arrow, entityHitResult));
            }
            return;
        }

        if (wouldShieldBlock(target, arrow)) {
            if (LodgedConfig.enableShieldArrowLodging()) {
                PendingArrowImpacts.rememberShieldArrowImpact(arrow, target, LodgedArrowVisual.fromShieldImpact(target, arrow, entityHitResult));
            }
            return;
        }

        LodgedArrowVisual visual = LodgedArrowVisual.fromImpact(target, arrow, entityHitResult);
        ProjectileStack projectileStack = ArrowImpactRules.projectileStack(arrow);
        if (tryLodgeArrowInArmor(
                target,
                arrow,
                visual,
                projectileStack,
                fromPlayer,
                fromMob,
                infinityGenerated,
                creativeGenerated)) {
            return;
        }

        visual = visual.withDepth(ArrowDepthCalculator.arrowDepth(target, arrow, visual));
        if (LodgedConfig.enableArrowBreakOnEntityHit()
                && ArrowImpactRules.breaksOnImpact(arrow, fromPlayer, fromMob, infinityGenerated, creativeGenerated)) {
            PendingArrowImpacts.rememberBrokenArrowImpact(arrow, target, visual);
            return;
        }

        if (arrow.getPierceLevel() > 0) {
            return;
        }

        if (!ArrowImpactRules.canTrack(target)) {
            return;
        }

        boolean trackForVisuals = !(target instanceof Player);
        boolean trackForDeathRecovery = projectileStack.recoverable()
                && ArrowImpactRules.canRecoverOnDeath(fromPlayer, infinityGenerated, creativeGenerated);
        boolean trackForPlayerRemoval = target instanceof Player && LodgedConfig.enablePlayerArrowRemoval();
        if (!trackForVisuals && !trackForDeathRecovery && !trackForPlayerRemoval) {
            return;
        }

        if (projectileStack.stack().isEmpty()) {
            return;
        }

        PendingArrowImpacts.rememberBodyArrowImpact(
                arrow,
                target,
                new LodgedArrowData(
                        projectileStack.stack(),
                        projectileStack.recoverable(),
                        ArrowImpactRules.causesBleeding(arrow),
                        fromPlayer,
                        infinityGenerated,
                        creativeGenerated,
                        visual));
    }

    private static boolean tryLodgeArrowInArmor(
            LivingEntity target,
            AbstractArrow arrow,
            LodgedArrowVisual visual,
            ProjectileStack projectileStack,
            boolean fromPlayer,
            boolean fromMob,
            boolean infinityGenerated,
            boolean creativeGenerated) {
        if (!LodgedConfig.enableArmorArrowLodging()
                || LodgedConfig.maxTrackedArrowsPerArmorPiece() <= 0
                || projectileStack.stack().isEmpty()) {
            return false;
        }

        EquipmentSlot slot = LodgedArmorArrowStorage.isHorseArmor(target.getItemBySlot(EquipmentSlot.BODY))
                ? EquipmentSlot.BODY
                : visual.armorSlot();
        ItemStack armor = target.getItemBySlot(slot);
        if (!(armor.getItem() instanceof ArmorItem armorItem) || armorItem.getEquipmentSlot() != slot) {
            return false;
        }

        double penetrationChance = armorPenetrationChance(armorItem);
        if (penetrationChance >= 1.0D || target.getRandom().nextDouble() < penetrationChance) {
            return false;
        }

        boolean breaks = LodgedConfig.enableArrowBreakOnEntityHit()
                && ArrowImpactRules.breaksOnImpact(arrow, fromPlayer, fromMob, infinityGenerated, creativeGenerated);
        PendingArrowImpacts.rememberArmorArrowImpact(
                arrow,
                target,
                slot,
                visual,
                projectileStack,
                fromPlayer,
                infinityGenerated,
                creativeGenerated,
                breaks,
                arrow.getPierceLevel());
        return true;
    }

    private static double armorPenetrationChance(ArmorItem armorItem) {
        double chance = LodgedConfig.armorArrowBasePenetrationChance()
                - (armorItem.getDefense() * LodgedConfig.armorArrowDefensePenaltyPerPoint());
        return Mth.clamp(
                chance,
                LodgedConfig.armorArrowMinPenetrationChance(),
                LodgedConfig.armorArrowMaxPenetrationChance());
    }

    private static boolean wouldShieldBlock(LivingEntity target, AbstractArrow arrow) {
        if (arrow.getPierceLevel() > 0 || !target.isBlocking() || PlayerArrowRemoval.isRemovingShieldArrow(target)) {
            return false;
        }

        ItemStack shield = target.getUseItem();
        if (shield.isEmpty() || !shield.canPerformAction(ItemAbilities.SHIELD_BLOCK)) {
            return false;
        }

        Vec3 viewVector = target.calculateViewVector(0.0F, target.getYHeadRot());
        Vec3 sourceToTarget = arrow.position().vectorTo(target.position());
        Vec3 horizontalDirection = new Vec3(sourceToTarget.x, 0.0D, sourceToTarget.z);
        if (horizontalDirection.lengthSqr() < 1.0E-7D) {
            return false;
        }

        return horizontalDirection.normalize().dot(viewVector) < 0.0D;
    }
}
