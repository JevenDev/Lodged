package com.jvn.lodged.world;

import com.jvn.lodged.config.LodgedConfig;
import com.jvn.lodged.effect.LodgedEffects;
import com.jvn.lodged.network.LodgedNetwork;
import com.jvn.lodged.network.PlayerArrowRemoval;
import java.util.List;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.damagesource.DamageContainer;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;
import net.neoforged.neoforge.event.entity.living.ArmorHurtEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingShieldBlockEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

public final class LodgedArrowEvents {
    private static final int MAX_DIZZINESS_AMPLIFIER = 4;
    private static final int ARROWS_PER_DIZZINESS_LEVEL = 2;

    private LodgedArrowEvents() {
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onProjectileImpact(ProjectileImpactEvent event) {
        ArrowImpactCapture.onProjectileImpact(event);
    }

    @SubscribeEvent
    public static void onLivingDamage(LivingDamageEvent.Post event) {
        if (event.getEntity().level().isClientSide()
                || !(event.getSource().getDirectEntity() instanceof AbstractArrow arrow)
                || !ArrowImpactRules.isSupportedArrow(arrow)
                || !(arrow instanceof ArrowDamageAccess access)) {
            return;
        }

        ArrowDamageConfirmation confirmation = access.lodged$damageConfirmation();
        if (confirmation != null) {
            confirmation.recordDamage(
                    event.getEntity().getUUID(),
                    event.getNewDamage(),
                    event.getReduction(DamageContainer.Reduction.ABSORPTION));
        }
    }

    public static void onResolvedEntityHit(AbstractArrow arrow, LivingEntity target, boolean damageAccepted) {
        ArrowImpactResolution.onResolvedEntityHit(arrow, target, damageAccepted);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onShieldBlock(LivingShieldBlockEvent event) {
        ArrowImpactResolution.onShieldBlock(event);
    }

    @SubscribeEvent
    public static void onArmorHurt(ArmorHurtEvent event) {
        ArrowImpactResolution.onArmorHurt(event);
    }

    @SubscribeEvent
    public static void onEntityTick(EntityTickEvent.Post event) {
        Entity entity = event.getEntity();
        if (entity.level().isClientSide()) {
            return;
        }

        if (entity instanceof AbstractArrow arrow) {
            ArrowImpactResolution.tickBlockBreak(arrow);
            return;
        }

        if (entity instanceof ServerPlayer player) {
            tickPlayerDizziness(player);
        }
    }

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (event.getLevel() instanceof ServerLevel level) {
            PendingArrowImpacts.removeExpiredImpacts(level);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onLivingDeath(LivingDeathEvent event) {
        if (event.isCanceled() || event.getEntity().level().isClientSide()) {
            return;
        }

        LivingEntity entity = event.getEntity();
        List<LodgedArrowData> lodgedArrows = LodgedArrowStorage.removeAll(entity);
        LodgedNetwork.syncEntityArrows(entity);
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
            if (!ArrowImpactRules.canRecoverOnDeath(lodgedArrow)) {
                continue;
            }

            ItemStack stack = lodgedArrow.stack();
            if (stack.isEmpty()) {
                continue;
            }

            if (recoveryChance >= 1.0D || entity.getRandom().nextDouble() < recoveryChance) {
                entity.spawnAtLocation(LodgedArrowStacks.copyForRecovery(stack));
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
        clearPlayerTransientState(event.getEntity());
        syncPlayer(event.getEntity());
    }

    @SubscribeEvent
    public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        clearPlayerTransientState(event.getEntity());
        syncPlayer(event.getEntity());
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        PendingArrowImpacts.clear();
    }

    @SubscribeEvent
    public static void onPlayerStartTracking(PlayerEvent.StartTracking event) {
        if (event.getEntity() instanceof ServerPlayer player && event.getTarget() instanceof LivingEntity target) {
            LodgedNetwork.syncEntityArrowsToPlayer(player, target);
            PlayerArrowRemoval.syncShieldArrowRemovalToPlayer(player, target);
        }
    }

    private static void tickPlayerDizziness(ServerPlayer player) {
        int interval = LodgedConfig.lodgedArrowDizzinessRefreshInterval();
        if (!LodgedConfig.enableLodgedArrowDizziness()
                || interval <= 0
                || (player.level().getGameTime() + player.getId()) % interval != 0L) {
            return;
        }

        int duration = LodgedConfig.lodgedArrowDizzinessDuration();
        if (duration <= 0) {
            return;
        }

        int amplifier = playerDizzinessAmplifier(player);
        if (amplifier < 0) {
            return;
        }

        MobEffectInstance existing = player.getEffect(LodgedEffects.DIZZINESS);
        if (existing != null && existing.getDuration() > duration && existing.getAmplifier() == amplifier) {
            return;
        }

        player.addEffect(new MobEffectInstance(LodgedEffects.DIZZINESS, duration, amplifier, false, false, true));
    }

    private static int playerDizzinessAmplifier(ServerPlayer player) {
        int maxTrackedArrows = LodgedConfig.maxTrackedBodyArrowsPerEntity();
        if (maxTrackedArrows <= 0) {
            return -1;
        }

        int arrowCount = LodgedArrowStorage.readAll(player).size();
        int threshold = playerDizzinessThreshold(maxTrackedArrows);
        if (threshold <= 0 || arrowCount < threshold) {
            return -1;
        }

        return Math.min(MAX_DIZZINESS_AMPLIFIER, (arrowCount - threshold) / ARROWS_PER_DIZZINESS_LEVEL);
    }

    private static int playerDizzinessThreshold(int maxTrackedArrows) {
        int threshold = Integer.MAX_VALUE;
        int minArrows = LodgedConfig.lodgedArrowDizzinessMinArrows();
        if (minArrows > 0) {
            threshold = Math.min(threshold, minArrows);
        }

        double percentOfMax = LodgedConfig.lodgedArrowDizzinessPercentOfMax();
        if (percentOfMax > 0.0D) {
            threshold = Math.min(threshold, Math.max(1, (int) Math.ceil(maxTrackedArrows * percentOfMax)));
        }

        return threshold == Integer.MAX_VALUE ? -1 : threshold;
    }

    private static void syncPlayer(Player player) {
        if (player instanceof ServerPlayer serverPlayer) {
            LodgedNetwork.syncPlayerArrows(serverPlayer);
        }
    }

    private static void clearPlayerTransientState(Player player) {
        if (player instanceof ServerPlayer serverPlayer) {
            LodgedNetwork.clearPlayerArrowRemovalCooldown(serverPlayer);
        }
    }

    public static boolean isSupportedArrow(AbstractArrow arrow) {
        return ArrowImpactRules.isSupportedArrow(arrow);
    }
}
