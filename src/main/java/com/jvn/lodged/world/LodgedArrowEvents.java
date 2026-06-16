package com.jvn.lodged.world;

import com.jvn.lodged.config.LodgedConfig;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.entity.projectile.SpectralArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.EntityHitResult;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;

public final class LodgedArrowEvents {
    private LodgedArrowEvents() {
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onProjectileImpact(ProjectileImpactEvent event) {
        if (event.isCanceled() || event.getProjectile().level().isClientSide()) {
            return;
        }

        if (!(event.getProjectile() instanceof AbstractArrow arrow) || !isSupportedArrow(arrow)) {
            return;
        }

        if (!(event.getRayTraceResult() instanceof EntityHitResult hitResult)
                || !(hitResult.getEntity() instanceof LivingEntity target)) {
            return;
        }

        if (!canTrack(target)) {
            return;
        }

        Entity owner = arrow.getOwner();
        boolean fromPlayer = owner instanceof Player;
        if (LodgedConfig.recoverPlayerArrowsOnly() && !fromPlayer) {
            return;
        }

        boolean creativeGenerated = owner instanceof Player player
                && player.getAbilities().instabuild
                && arrow.pickup == AbstractArrow.Pickup.CREATIVE_ONLY;
        boolean infinityGenerated = fromPlayer
                && !creativeGenerated
                && arrow.pickup == AbstractArrow.Pickup.CREATIVE_ONLY;

        if (infinityGenerated && !LodgedConfig.recoverInfinityArrows()) {
            return;
        }

        if (creativeGenerated && !LodgedConfig.recoverCreativeArrows()) {
            return;
        }

        ItemStack recoveredStack = getRecoverableStack(arrow);
        if (recoveredStack.isEmpty()) {
            return;
        }

        LodgedArrowStorage.add(target, new LodgedArrowData(
                recoveredStack,
                fromPlayer,
                infinityGenerated,
                creativeGenerated,
                target.level().getGameTime()));
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onLivingDeath(LivingDeathEvent event) {
        if (event.isCanceled() || event.getEntity().level().isClientSide()) {
            return;
        }

        LivingEntity entity = event.getEntity();
        List<LodgedArrowData> lodgedArrows = LodgedArrowStorage.removeAll(entity);
        if (lodgedArrows.isEmpty() || !LodgedConfig.enableArrowRecovery()) {
            return;
        }

        double recoveryChance = LodgedConfig.recoveryChance();
        if (recoveryChance <= 0.0D) {
            return;
        }

        for (LodgedArrowData lodgedArrow : lodgedArrows) {
            ItemStack stack = lodgedArrow.stack();
            if (stack.isEmpty()) {
                continue;
            }

            if (recoveryChance >= 1.0D || entity.getRandom().nextDouble() < recoveryChance) {
                entity.spawnAtLocation(stack.copyWithCount(1));
            }
        }
    }

    private static boolean canTrack(LivingEntity target) {
        return LodgedConfig.enableArrowRecovery()
                && LodgedConfig.maxTrackedArrowsPerEntity() > 0
                && !isDeniedEntity(target);
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

    private static boolean isSupportedArrow(AbstractArrow arrow) {
        return arrow instanceof Arrow || arrow instanceof SpectralArrow;
    }

    private static ItemStack getRecoverableStack(AbstractArrow arrow) {
        if (LodgedConfig.preserveArrowItemStack()) {
            return arrow.getPickupItemStackOrigin().copyWithCount(1);
        }

        if (arrow instanceof SpectralArrow) {
            return new ItemStack(Items.SPECTRAL_ARROW);
        }

        return new ItemStack(Items.ARROW);
    }
}
