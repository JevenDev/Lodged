package com.jvn.lodged.world;

import com.jvn.lodged.config.LodgedConfig;
import com.jvn.lodged.effect.BleedingEvents;
import com.jvn.lodged.effect.LodgedEffects;
import com.jvn.lodged.network.LodgedNetwork;
import com.jvn.lodged.network.PlayerArrowRemoval;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.entity.projectile.SpectralArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.ItemAbilities;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingShieldBlockEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

public final class LodgedArrowEvents {
    private static final long BROKEN_ARROW_IMPACT_EXPIRY_TICKS = 20L;
    private static final long SHIELD_ARROW_IMPACT_EXPIRY_TICKS = 20L;
    private static final long BLOCK_ARROW_BREAK_DELAY_TICKS = 4L;
    private static final long BLOCK_ARROW_BREAK_EXPIRY_TICKS = 40L;
    private static final int MAX_DIZZINESS_AMPLIFIER = 4;
    private static final double SHIELD_ARROW_REMOVAL_DURABILITY_CHANCE = 0.35D;
    private static final Map<UUID, BrokenArrowImpact> BROKEN_ARROW_IMPACTS = new HashMap<>();
    private static final Map<UUID, ShieldArrowImpact> SHIELD_ARROW_IMPACTS = new HashMap<>();
    private static final Map<UUID, Integer> PENDING_ARROW_COUNT_REMOVALS = new HashMap<>();
    private static final Map<UUID, Long> PENDING_BLOCK_ARROW_BREAKS = new HashMap<>();

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

        Entity owner = arrow.getOwner();
        boolean fromPlayer = owner instanceof Player;
        boolean fromMob = owner instanceof LivingEntity && !fromPlayer;
        boolean creativeGenerated = owner instanceof Player player
                && player.getAbilities().instabuild
                && arrow.pickup == AbstractArrow.Pickup.CREATIVE_ONLY;
        boolean infinityGenerated = fromPlayer
                && !creativeGenerated
                && arrow.pickup == AbstractArrow.Pickup.CREATIVE_ONLY;
        long gameTime = arrow.level().getGameTime();
        removeExpiredBrokenArrowImpacts(gameTime);
        removeExpiredShieldArrowImpacts(gameTime);

        HitResult hitResult = event.getRayTraceResult();
        if (hitResult instanceof BlockHitResult
                && LodgedConfig.enableArrowBreakOnBlockHit()
                && breaksOnImpact(arrow, fromPlayer, fromMob, infinityGenerated, creativeGenerated)) {
            rememberPendingBlockArrowBreak(arrow);
            return;
        }

        if (!(hitResult instanceof EntityHitResult entityHitResult)
                || !(entityHitResult.getEntity() instanceof LivingEntity target)) {
            return;
        }

        if (wouldShieldBlock(target, arrow)) {
            if (LodgedConfig.enableShieldArrowLodging()) {
                rememberShieldArrowImpact(arrow, target, LodgedArrowVisual.fromShieldImpact(target, arrow, entityHitResult));
            }
            return;
        }

        LodgedArrowVisual visual = LodgedArrowVisual.fromImpact(target, arrow, entityHitResult);
        applyLegShotSlowness(target, visual);
        if (LodgedConfig.enableArrowBreakOnEntityHit()
                && breaksOnImpact(arrow, fromPlayer, fromMob, infinityGenerated, creativeGenerated)) {
            rememberBrokenArrowImpact(arrow, target, visual);
            return;
        }

        if (!canTrack(target)) {
            return;
        }

        boolean trackForVisuals = !(target instanceof Player);
        boolean trackForDeathRecovery = canRecoverOnDeath(fromPlayer, infinityGenerated, creativeGenerated);
        boolean trackForPlayerRemoval = target instanceof Player && LodgedConfig.enablePlayerArrowRemoval();
        if (!trackForVisuals && !trackForDeathRecovery && !trackForPlayerRemoval) {
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
                visual));

        LodgedNetwork.syncEntityArrows(target);
        if (target instanceof ServerPlayer player) {
            LodgedNetwork.syncPlayerArrows(player);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onShieldBlock(LivingShieldBlockEvent event) {
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
                || !isSupportedArrow(arrow)) {
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
                && breaksOnImpact(arrow, fromPlayer, fromMob, infinityGenerated, creativeGenerated)) {
            SHIELD_ARROW_IMPACTS.remove(arrow.getUUID());
            playArrowBreakSound(arrow);
            arrow.discard();
            return;
        }

        ShieldArrowImpact rememberedImpact = SHIELD_ARROW_IMPACTS.remove(arrow.getUUID());
        if (!LodgedConfig.enableShieldArrowLodging()) {
            return;
        }

        LodgedArrowVisual visual = rememberedImpact != null && blocker.getUUID().equals(rememberedImpact.targetUuid())
                ? rememberedImpact.visual()
                : LodgedArrowVisual.fromShieldImpact(blocker, arrow, new EntityHitResult(blocker, arrow.position()));

        boolean willEvictArrow = willEvictShieldArrow(shield);
        ItemStack recoveredStack = getRecoverableStack(arrow);
        if (LodgedShieldArrowStorage.add(
                shield,
                new LodgedShieldArrowStorage.LodgedShieldArrowData(
                        recoveredStack,
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

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onLivingDamage(LivingDamageEvent.Post event) {
        LivingEntity target = event.getEntity();
        if (target.level().isClientSide()
                || event.getNewDamage() <= 0.0F
                || !(event.getSource().getDirectEntity() instanceof AbstractArrow arrow)) {
            return;
        }

        BrokenArrowImpact brokenImpact = BROKEN_ARROW_IMPACTS.remove(arrow.getUUID());
        if (brokenImpact == null || !target.getUUID().equals(brokenImpact.targetUuid())) {
            return;
        }

        BleedingEvents.tryApplyFromBrokenArrowImpact(target, brokenImpact.visual());
        if (arrow.getPierceLevel() <= 0) {
            PENDING_ARROW_COUNT_REMOVALS.merge(target.getUUID(), 1, Integer::sum);
        }
    }

    @SubscribeEvent
    public static void onEntityTick(EntityTickEvent.Post event) {
        Entity entity = event.getEntity();
        if (entity.level().isClientSide()) {
            return;
        }

        removeExpiredBlockArrowBreaks(entity.level().getGameTime());
        if (entity instanceof AbstractArrow arrow) {
            processPendingBlockArrowBreak(arrow);
            return;
        }

        if (!(entity instanceof LivingEntity target)) {
            return;
        }

        removeExpiredBrokenArrowImpacts(target.level().getGameTime());
        removeExpiredShieldArrowImpacts(target.level().getGameTime());

        if (target instanceof ServerPlayer player) {
            processPendingArrowCountRemovals(player);
            tickPlayerDizziness(player);
        } else {
            processPendingArrowCountRemovals(target);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onLivingDeath(LivingDeathEvent event) {
        if (event.isCanceled() || event.getEntity().level().isClientSide()) {
            return;
        }

        LivingEntity entity = event.getEntity();
        PENDING_ARROW_COUNT_REMOVALS.remove(entity.getUUID());
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

    @SubscribeEvent
    public static void onPlayerStartTracking(PlayerEvent.StartTracking event) {
        if (event.getEntity() instanceof ServerPlayer player && event.getTarget() instanceof LivingEntity target) {
            LodgedNetwork.syncEntityArrowsToPlayer(player, target);
            PlayerArrowRemoval.syncShieldArrowRemovalToPlayer(player, target);
        }
    }

    private static void applyLegShotSlowness(LivingEntity target, LodgedArrowVisual visual) {
        int duration = LodgedConfig.legShotSlownessDuration();
        if (!LodgedConfig.enableLegShotSlowness()
                || duration <= 0
                || visual.bodyPart() != LodgedArrowBodyPart.LEG
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

        if (!fromPlayer) {
            return LodgedConfig.recoverMobArrows();
        }

        if (infinityGenerated && !LodgedConfig.recoverInfinityArrows()) {
            return false;
        }

        return !creativeGenerated || LodgedConfig.recoverCreativeArrows();
    }

    private static boolean breaksOnImpact(
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

    private static void rememberBrokenArrowImpact(AbstractArrow arrow, LivingEntity target, LodgedArrowVisual visual) {
        BROKEN_ARROW_IMPACTS.put(
                arrow.getUUID(),
                new BrokenArrowImpact(target.getUUID(), visual, arrow.level().getGameTime()));
    }

    private static void rememberShieldArrowImpact(AbstractArrow arrow, LivingEntity target, LodgedArrowVisual visual) {
        SHIELD_ARROW_IMPACTS.put(
                arrow.getUUID(),
                new ShieldArrowImpact(target.getUUID(), visual, arrow.level().getGameTime()));
    }

    private static void rememberPendingBlockArrowBreak(AbstractArrow arrow) {
        arrow.pickup = AbstractArrow.Pickup.DISALLOWED;
        PENDING_BLOCK_ARROW_BREAKS.put(arrow.getUUID(), arrow.level().getGameTime() + BLOCK_ARROW_BREAK_DELAY_TICKS);
    }

    private static void processPendingBlockArrowBreak(AbstractArrow arrow) {
        Long breakTime = PENDING_BLOCK_ARROW_BREAKS.get(arrow.getUUID());
        if (breakTime == null || arrow.level().getGameTime() < breakTime) {
            return;
        }

        PENDING_BLOCK_ARROW_BREAKS.remove(arrow.getUUID());
        arrow.level().playSound(
                null,
                arrow.getX(),
                arrow.getY(),
                arrow.getZ(),
                SoundEvents.ITEM_BREAK,
                SoundSource.NEUTRAL,
                0.8F,
                1.0F);
        arrow.discard();
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

    private static void removeExpiredBrokenArrowImpacts(long gameTime) {
        BROKEN_ARROW_IMPACTS.entrySet().removeIf(entry ->
                gameTime - entry.getValue().gameTime() > BROKEN_ARROW_IMPACT_EXPIRY_TICKS);
    }

    private static void removeExpiredShieldArrowImpacts(long gameTime) {
        SHIELD_ARROW_IMPACTS.entrySet().removeIf(entry ->
                gameTime - entry.getValue().gameTime() > SHIELD_ARROW_IMPACT_EXPIRY_TICKS);
    }

    private static void removeExpiredBlockArrowBreaks(long gameTime) {
        PENDING_BLOCK_ARROW_BREAKS.entrySet().removeIf(entry ->
                gameTime - entry.getValue() > BLOCK_ARROW_BREAK_EXPIRY_TICKS);
    }

    private static void processPendingArrowCountRemovals(LivingEntity target) {
        Integer removals = PENDING_ARROW_COUNT_REMOVALS.remove(target.getUUID());
        if (removals == null || removals <= 0) {
            return;
        }

        target.setArrowCount(Math.max(0, target.getArrowCount() - removals));
        LodgedNetwork.syncEntityArrows(target);
        if (target instanceof ServerPlayer player) {
            LodgedNetwork.syncPlayerArrows(player);
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
        int maxTrackedArrows = LodgedConfig.maxTrackedArrowsPerEntity();
        if (maxTrackedArrows <= 0) {
            return -1;
        }

        int arrowCount = LodgedArrowStorage.readAll(player).size();
        int threshold = playerDizzinessThreshold(maxTrackedArrows);
        if (threshold <= 0 || arrowCount < threshold) {
            return -1;
        }

        return Math.min(MAX_DIZZINESS_AMPLIFIER, arrowCount - threshold);
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

    private static boolean willEvictShieldArrow(ItemStack shield) {
        int maxTrackedArrows = LodgedConfig.maxTrackedArrowsPerShield();
        return maxTrackedArrows > 0 && LodgedShieldArrowStorage.readAll(shield).size() >= maxTrackedArrows;
    }

    private static void maybeDamageShieldFromArrowRemoval(LivingEntity holder, ItemStack shield) {
        maybeDamageShieldFromArrowRemoval(holder, shield, LivingEntity.getSlotForHand(holder.getUsedItemHand()));
    }

    private static void maybeDamageShieldFromArrowRemoval(LivingEntity holder, ItemStack shield, EquipmentSlot slot) {
        if (shield.isEmpty()
                || holder.level().random.nextDouble() >= SHIELD_ARROW_REMOVAL_DURABILITY_CHANCE
                || holder instanceof Player player && player.getAbilities().instabuild) {
            return;
        }

        shield.hurtAndBreak(1, holder, slot);
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

    private record BrokenArrowImpact(UUID targetUuid, LodgedArrowVisual visual, long gameTime) {
    }

    private record ShieldArrowImpact(UUID targetUuid, LodgedArrowVisual visual, long gameTime) {
    }
}
