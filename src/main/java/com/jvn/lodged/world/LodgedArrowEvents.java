package com.jvn.lodged.world;

import com.jvn.lodged.config.LodgedConfig;
import com.jvn.lodged.network.LodgedNetwork;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
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
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

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
        boolean creativeGenerated = owner instanceof Player player
                && player.getAbilities().instabuild
                && arrow.pickup == AbstractArrow.Pickup.CREATIVE_ONLY;
        boolean infinityGenerated = fromPlayer
                && !creativeGenerated
                && arrow.pickup == AbstractArrow.Pickup.CREATIVE_ONLY;

        boolean trackForDeathRecovery = canRecoverOnDeath(fromPlayer, infinityGenerated, creativeGenerated);
        boolean trackForPlayerRemoval = target instanceof Player && LodgedConfig.enablePlayerArrowRemoval();
        if (!trackForDeathRecovery && !trackForPlayerRemoval) {
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
                target.level().getGameTime(),
                LodgedArrowVisual.fromImpact(target, arrow, hitResult)));

        if (target instanceof ServerPlayer player) {
            LodgedNetwork.syncPlayerArrows(player);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onLivingDeath(LivingDeathEvent event) {
        if (event.isCanceled() || event.getEntity().level().isClientSide()) {
            return;
        }

        LivingEntity entity = event.getEntity();
        List<LodgedArrowData> lodgedArrows = LodgedArrowStorage.removeAll(entity);
        if (entity instanceof ServerPlayer player) {
            LodgedNetwork.syncPlayerArrows(player);
        }

        if (lodgedArrows.isEmpty() || !LodgedConfig.enableArrowRecovery()) {
            return;
        }

        double recoveryChance = LodgedConfig.recoveryChance();
        if (recoveryChance <= 0.0D) {
            return;
        }

        for (LodgedArrowData lodgedArrow : lodgedArrows) {
            if (!canRecoverOnDeath(lodgedArrow)) {
                continue;
            }

            ItemStack stack = lodgedArrow.stack();
            if (stack.isEmpty()) {
                continue;
            }

            if (recoveryChance >= 1.0D || entity.getRandom().nextDouble() < recoveryChance) {
                entity.spawnAtLocation(stack.copyWithCount(1));
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        syncPlayer(event.getEntity());
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            LodgedNetwork.clearPlayerArrowRemovalCooldown(serverPlayer);
        }
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        syncPlayer(event.getEntity());
    }

    @SubscribeEvent
    public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        syncPlayer(event.getEntity());
    }

    private static boolean canTrack(LivingEntity target) {
        return LodgedConfig.maxTrackedArrowsPerEntity() > 0
                && !isDeniedEntity(target);
    }

    private static boolean canRecoverOnDeath(LodgedArrowData lodgedArrow) {
        return canRecoverOnDeath(lodgedArrow.fromPlayer(), lodgedArrow.infinityGenerated(), lodgedArrow.creativeGenerated());
    }

    private static boolean canRecoverOnDeath(boolean fromPlayer, boolean infinityGenerated, boolean creativeGenerated) {
        if (!LodgedConfig.enableArrowRecovery()) {
            return false;
        }

        if (LodgedConfig.recoverPlayerArrowsOnly() && !fromPlayer) {
            return false;
        }

        if (infinityGenerated && !LodgedConfig.recoverInfinityArrows()) {
            return false;
        }

        return !creativeGenerated || LodgedConfig.recoverCreativeArrows();
    }

    private static void syncPlayer(Player player) {
        if (player instanceof ServerPlayer serverPlayer) {
            LodgedNetwork.syncPlayerArrows(serverPlayer);
        }
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
