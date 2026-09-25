package com.jvn.lodged.world;

import com.jvn.lodged.config.LodgedConfig;
import com.jvn.lodged.effect.LodgedTags;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.entity.projectile.SpectralArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

final class ArrowImpactRules {
    private ArrowImpactRules() {
    }

    static boolean canTrack(LivingEntity target) {
        return LodgedConfig.maxTrackedArrowsPerEntity() > 0
                && !isDeniedEntity(target);
    }

    static boolean canRecoverOnDeath(LodgedArrowData lodgedArrow) {
        return lodgedArrow.recoverable()
                && canRecoverOnDeath(lodgedArrow.fromPlayer(), lodgedArrow.infinityGenerated(), lodgedArrow.creativeGenerated());
    }

    static boolean canRecoverOnDeath(boolean fromPlayer, boolean infinityGenerated, boolean creativeGenerated) {
        if (!LodgedConfig.enableArrowRecovery()) {
            return false;
        }

        if (!fromPlayer) {
            return LodgedConfig.recoverMobArrows();
        }

        if (infinityGenerated && !LodgedConfig.recoverInfinityArrows()) {
            return false;
        }

        return !creativeGenerated || LodgedConfig.recoverCreativeArrows();
    }

    static boolean breaksOnImpact(
            AbstractArrow arrow,
            boolean fromPlayer,
            boolean fromMob,
            boolean infinityGenerated,
            boolean creativeGenerated) {
        double breakChance = impactBreakChance(fromPlayer, fromMob, infinityGenerated, creativeGenerated);
        return breakChance >= 1.0D || (breakChance > 0.0D && arrow.getRandom().nextDouble() < breakChance);
    }

    private static double impactBreakChance(
            boolean fromPlayer,
            boolean fromMob,
            boolean infinityGenerated,
            boolean creativeGenerated) {
        if (infinityGenerated) {
            return LodgedConfig.infinityArrowImpactBreakChance();
        }

        if (fromMob && !LodgedConfig.enableMobArrowBreak()) {
            return 0.0D;
        }

        if (!fromPlayer) {
            return LodgedConfig.mobArrowImpactBreakChance();
        }

        if (creativeGenerated) {
            return 0.0D;
        }

        return LodgedConfig.regularArrowImpactBreakChance();
    }

    private static boolean isDeniedEntity(LivingEntity entity) {
        ResourceLocation entityId = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
        if (entityId == null) {
            return false;
        }

        String id = entityId.toString();
        for (String deniedId : LodgedConfig.entityDenylist()) {
            if (id.equals(deniedId)) {
                return true;
            }
        }
        return false;
    }

    static boolean isSupportedArrow(AbstractArrow arrow) {
        return arrow.getType().is(LodgedTags.TRACKABLE_PROJECTILES);
    }

    static boolean canLodge(AbstractArrow arrow) {
        return !arrow.getType().is(LodgedTags.NON_LODGING_PROJECTILES);
    }

    static boolean causesBleeding(AbstractArrow arrow) {
        return arrow.getType().is(LodgedTags.BLEEDING_PROJECTILES);
    }

    static ProjectileStack projectileStack(AbstractArrow arrow) {
        ItemStack recoverableStack = arrow.getType().is(LodgedTags.RECOVERABLE_PROJECTILES)
                ? getRecoverableStack(arrow)
                : ItemStack.EMPTY;
        if (!recoverableStack.isEmpty()) {
            return new ProjectileStack(recoverableStack, true);
        }

        ItemStack renderStack = getRenderStack(arrow);
        return new ProjectileStack(renderStack, false);
    }

    private static ItemStack getRecoverableStack(AbstractArrow arrow) {
        if (LodgedConfig.preserveArrowItemStack()) {
            ItemStack originalStack = arrow.getPickupItemStackOrigin();
            if (!originalStack.isEmpty()) {
                return LodgedArrowStacks.copyForRecovery(originalStack);
            }
        }

        if (arrow instanceof SpectralArrow) {
            return new ItemStack(Items.SPECTRAL_ARROW);
        }

        return arrow instanceof Arrow ? new ItemStack(Items.ARROW) : ItemStack.EMPTY;
    }

    private static ItemStack getRenderStack(AbstractArrow arrow) {
        if (arrow instanceof SpectralArrow) {
            return new ItemStack(Items.SPECTRAL_ARROW);
        }

        return new ItemStack(Items.ARROW);
    }

    record ProjectileStack(ItemStack stack, boolean recoverable) {
    }
}
