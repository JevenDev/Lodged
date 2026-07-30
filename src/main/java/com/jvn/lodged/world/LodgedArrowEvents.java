package com.jvn.lodged.world;

import com.jvn.lodged.config.LodgedConfig;
import com.jvn.lodged.effect.BleedingEvents;
import com.jvn.lodged.effect.LodgedEffects;
import com.jvn.lodged.effect.LodgedTags;
import com.jvn.lodged.network.LodgedNetwork;
import com.jvn.lodged.network.PlayerArrowRemoval;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
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
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.ItemAbilities;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;
import net.neoforged.neoforge.event.entity.living.ArmorHurtEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingShieldBlockEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;

public final class LodgedArrowEvents {
    private static final long BROKEN_ARROW_IMPACT_EXPIRY_TICKS = 20L;
    private static final long SHIELD_ARROW_IMPACT_EXPIRY_TICKS = 20L;
    private static final long ARMOR_ARROW_IMPACT_EXPIRY_TICKS = 20L;
    private static final long BLOCK_ARROW_BREAK_DELAY_TICKS = 4L;
    private static final long BLOCK_ARROW_BREAK_EXPIRY_TICKS = 40L;
    private static final int MAX_DIZZINESS_AMPLIFIER = 4;
    private static final int ARROWS_PER_DIZZINESS_LEVEL = 2;
    private static final Map<UUID, BrokenArrowImpact> BROKEN_ARROW_IMPACTS = new HashMap<>();
    private static final Map<UUID, ShieldArrowImpact> SHIELD_ARROW_IMPACTS = new HashMap<>();
    private static final Map<UUID, ArmorArrowImpact> ARMOR_ARROW_IMPACTS = new HashMap<>();
    private static final Map<UUID, Integer> PENDING_ARROW_COUNT_REMOVALS = new HashMap<>();
    private static final Map<UUID, PendingBlockArrowBreak> PENDING_BLOCK_ARROW_BREAKS = new HashMap<>();

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

        boolean canLodge = canLodge(arrow);
        if (!canLodge) {
            if (wouldShieldBlock(target, arrow)) {
                return;
            }

            if (LodgedConfig.enableArrowBreakOnEntityHit()
                    && breaksOnImpact(arrow, fromPlayer, fromMob, infinityGenerated, creativeGenerated)) {
                rememberBrokenArrowImpact(arrow, target, LodgedArrowVisual.fromImpact(target, arrow, entityHitResult));
            }
            return;
        }

        if (wouldShieldBlock(target, arrow)) {
            if (LodgedConfig.enableShieldArrowLodging()) {
                rememberShieldArrowImpact(arrow, target, LodgedArrowVisual.fromShieldImpact(target, arrow, entityHitResult));
            }
            return;
        }

        LodgedArrowVisual visual = LodgedArrowVisual.fromImpact(target, arrow, entityHitResult);
        ProjectileStack projectileStack = projectileStack(arrow);
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

        visual = visual.withDepth(arrowDepth(target, arrow, visual));
        applyLegShotSlowness(target, visual);
        if (LodgedConfig.enableArrowBreakOnEntityHit()
                && breaksOnImpact(arrow, fromPlayer, fromMob, infinityGenerated, creativeGenerated)) {
            rememberBrokenArrowImpact(arrow, target, visual);
            return;
        }

        if (arrow.getPierceLevel() > 0) {
            return;
        }

        if (!canTrack(target)) {
            return;
        }

        boolean trackForVisuals = !(target instanceof Player);
        boolean trackForDeathRecovery = projectileStack.recoverable()
                && canRecoverOnDeath(fromPlayer, infinityGenerated, creativeGenerated);
        boolean trackForPlayerRemoval = target instanceof Player && LodgedConfig.enablePlayerArrowRemoval();
        if (!trackForVisuals && !trackForDeathRecovery && !trackForPlayerRemoval) {
            return;
        }

        if (projectileStack.stack().isEmpty()) {
            return;
        }

        LodgedArrowStorage.add(target, new LodgedArrowData(
                projectileStack.stack(),
                projectileStack.recoverable(),
                causesBleeding(arrow),
                fromPlayer,
                infinityGenerated,
                creativeGenerated,
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
        if (!LodgedConfig.enableShieldArrowLodging() || !canLodge(arrow)) {
            return;
        }

        LodgedArrowVisual visual = rememberedImpact != null && blocker.getUUID().equals(rememberedImpact.targetUuid())
                ? rememberedImpact.visual()
                : LodgedArrowVisual.fromShieldImpact(blocker, arrow, new EntityHitResult(blocker, arrow.position()));

        boolean willEvictArrow = willEvictShieldArrow(shield);
        ProjectileStack projectileStack = projectileStack(arrow);
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

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onLivingDamage(LivingDamageEvent.Post event) {
        LivingEntity target = event.getEntity();
        if (target.level().isClientSide()
                || event.getNewDamage() <= 0.0F
                || !(event.getSource().getDirectEntity() instanceof AbstractArrow arrow)) {
            return;
        }

        ArmorArrowImpact armorImpact = ARMOR_ARROW_IMPACTS.remove(arrow.getUUID());
        if (armorImpact != null && target.getUUID().equals(armorImpact.targetUuid())) {
            processArmorArrowImpact(target, arrow, armorImpact);
            return;
        }

        BrokenArrowImpact brokenImpact = BROKEN_ARROW_IMPACTS.remove(arrow.getUUID());
        if (brokenImpact == null || !target.getUUID().equals(brokenImpact.targetUuid())) {
            return;
        }

        if (brokenImpact.causesBleeding()) {
            BleedingEvents.tryApplyFromBrokenArrowImpact(target, brokenImpact.visual());
        }
        playArrowBreakSound(arrow);
        discardArrow(arrow);
        if (arrow.getPierceLevel() <= 0) {
            PENDING_ARROW_COUNT_REMOVALS.merge(target.getUUID(), 1, Integer::sum);
        }
    }

    @SubscribeEvent
    public static void onArmorHurt(ArmorHurtEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide()
                || !LodgedConfig.enableArmorArrowDurabilityPenalty()
                || LodgedConfig.armorArrowExtraDurabilityLossAmount() <= 0) {
            return;
        }

        for (EquipmentSlot slot : LodgedArmorArrowStorage.armorSlots()) {
            ItemStack armor = event.getArmorItemStack(slot);
            int arrowCount = LodgedArmorArrowStorage.readAll(armor).size();
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

    @SubscribeEvent
    public static void onEntityTick(EntityTickEvent.Post event) {
        Entity entity = event.getEntity();
        if (entity.level().isClientSide()) {
            return;
        }

        if (entity instanceof AbstractArrow arrow) {
            processPendingBlockArrowBreak(arrow);
            return;
        }

        if (!(entity instanceof LivingEntity target)) {
            return;
        }

        if (target instanceof ServerPlayer player) {
            processPendingArrowCountRemovals(player);
            tickPlayerDizziness(player);
        } else {
            processPendingArrowCountRemovals(target);
        }
    }

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (event.getLevel() instanceof ServerLevel level) {
            processPendingArmorArrowImpacts(level);
            removeExpiredImpacts(level);
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
        BROKEN_ARROW_IMPACTS.clear();
        SHIELD_ARROW_IMPACTS.clear();
        ARMOR_ARROW_IMPACTS.clear();
        PENDING_ARROW_COUNT_REMOVALS.clear();
        PENDING_BLOCK_ARROW_BREAKS.clear();
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

    private static boolean canTrack(LivingEntity target) {
        return LodgedConfig.maxTrackedArrowsPerEntity() > 0
                && !isDeniedEntity(target);
    }

    private static boolean canRecoverOnDeath(LodgedArrowData lodgedArrow) {
        return lodgedArrow.recoverable()
                && canRecoverOnDeath(lodgedArrow.fromPlayer(), lodgedArrow.infinityGenerated(), lodgedArrow.creativeGenerated());
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

    private static LodgedArrowDepth arrowDepth(LivingEntity target, AbstractArrow arrow, LodgedArrowVisual visual) {
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
                && breaksOnImpact(arrow, fromPlayer, fromMob, infinityGenerated, creativeGenerated);
        rememberArmorArrowImpact(
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

    private static void processArmorArrowImpact(LivingEntity target, AbstractArrow arrow, ArmorArrowImpact impact) {
        ItemStack armor = target.getItemBySlot(impact.slot());
        if (!(armor.getItem() instanceof ArmorItem armorItem) || armorItem.getEquipmentSlot() != impact.slot()) {
            return;
        }

        if (impact.breaks()) {
            playArrowBreakSound(target, arrow);
            maybeDamageArmor(target, armor, impact.slot(), LodgedConfig.armorArrowBreakDurabilityDamageChance());
            target.setItemSlot(impact.slot(), armor);
            removeVanillaBodyArrowForArmorImpact(target, impact);
            discardArrow(arrow);
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
            if (target instanceof ServerPlayer player) {
                player.containerMenu.broadcastChanges();
            }
            LodgedNetwork.syncEntityArrows(target);
            if (target instanceof ServerPlayer player) {
                LodgedNetwork.syncPlayerArrows(player);
            }
            removeVanillaBodyArrowForArmorImpact(target, impact);
            discardArrow(arrow);
        }
    }

    private static void processPendingArmorArrowImpacts(ServerLevel level) {
        Iterator<Map.Entry<UUID, ArmorArrowImpact>> iterator = ARMOR_ARROW_IMPACTS.entrySet().iterator();
        long gameTime = level.getGameTime();
        while (iterator.hasNext()) {
            Map.Entry<UUID, ArmorArrowImpact> entry = iterator.next();
            ArmorArrowImpact impact = entry.getValue();
            if (!impact.dimension().equals(level.dimension()) || gameTime <= impact.gameTime()) {
                continue;
            }

            iterator.remove();
            Entity entity = level.getEntity(impact.targetUuid());
            if (entity instanceof LivingEntity target) {
                processArmorArrowImpact(target, null, impact);
            }
        }
    }

    private static void removeVanillaBodyArrowForArmorImpact(LivingEntity target, ArmorArrowImpact impact) {
        if (impact.pierceLevel() <= 0) {
            PENDING_ARROW_COUNT_REMOVALS.merge(target.getUUID(), 1, Integer::sum);
        }
    }

    private static void discardArrow(AbstractArrow arrow) {
        if (arrow != null) {
            arrow.discard();
        }
    }

    private static double armorPenetrationChance(ArmorItem armorItem) {
        double chance = LodgedConfig.armorArrowBasePenetrationChance()
                - (armorItem.getDefense() * LodgedConfig.armorArrowDefensePenaltyPerPoint());
        return Mth.clamp(
                chance,
                LodgedConfig.armorArrowMinPenetrationChance(),
                LodgedConfig.armorArrowMaxPenetrationChance());
    }

    private static double armorArrowExtraDurabilityLossChance(int arrowCount) {
        double chance = LodgedConfig.armorArrowExtraDurabilityLossChance()
                + Math.max(0, arrowCount - 1) * LodgedConfig.armorArrowExtraDurabilityLossChancePerAdditionalArrow();
        return Mth.clamp(chance, 0.0D, LodgedConfig.armorArrowExtraDurabilityLossMaxChance());
    }

    private static void rememberBrokenArrowImpact(AbstractArrow arrow, LivingEntity target, LodgedArrowVisual visual) {
        BROKEN_ARROW_IMPACTS.put(
                arrow.getUUID(),
                new BrokenArrowImpact(
                        target.getUUID(),
                        visual,
                        causesBleeding(arrow),
                        arrow.level().dimension(),
                        arrow.level().getGameTime()));
    }

    private static void rememberShieldArrowImpact(AbstractArrow arrow, LivingEntity target, LodgedArrowVisual visual) {
        SHIELD_ARROW_IMPACTS.put(
                arrow.getUUID(),
                new ShieldArrowImpact(
                        target.getUUID(),
                        visual,
                        arrow.level().dimension(),
                        arrow.level().getGameTime()));
    }

    private static void rememberArmorArrowImpact(
            AbstractArrow arrow,
            LivingEntity target,
            EquipmentSlot slot,
            LodgedArrowVisual visual,
            ProjectileStack projectileStack,
            boolean fromPlayer,
            boolean infinityGenerated,
            boolean creativeGenerated,
            boolean breaks,
            int pierceLevel) {
        ARMOR_ARROW_IMPACTS.put(
                arrow.getUUID(),
                new ArmorArrowImpact(
                        target.getUUID(),
                        slot,
                        projectileStack.stack(),
                        projectileStack.recoverable(),
                        fromPlayer,
                        infinityGenerated,
                        creativeGenerated,
                        visual,
                        breaks,
                        pierceLevel,
                        arrow.level().dimension(),
                        arrow.level().getGameTime()));
    }

    private static void rememberPendingBlockArrowBreak(AbstractArrow arrow) {
        arrow.pickup = AbstractArrow.Pickup.DISALLOWED;
        PENDING_BLOCK_ARROW_BREAKS.put(
                arrow.getUUID(),
                new PendingBlockArrowBreak(
                        arrow.level().dimension(),
                        arrow.level().getGameTime() + BLOCK_ARROW_BREAK_DELAY_TICKS));
    }

    private static void processPendingBlockArrowBreak(AbstractArrow arrow) {
        PendingBlockArrowBreak pendingBreak = PENDING_BLOCK_ARROW_BREAKS.get(arrow.getUUID());
        if (pendingBreak == null
                || !pendingBreak.dimension().equals(arrow.level().dimension())
                || arrow.level().getGameTime() < pendingBreak.breakTime()) {
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

    private static void removeExpiredImpacts(ServerLevel level) {
        long gameTime = level.getGameTime();
        BROKEN_ARROW_IMPACTS.entrySet().removeIf(entry ->
                entry.getValue().dimension().equals(level.dimension())
                        && gameTime - entry.getValue().gameTime() > BROKEN_ARROW_IMPACT_EXPIRY_TICKS);
        SHIELD_ARROW_IMPACTS.entrySet().removeIf(entry ->
                entry.getValue().dimension().equals(level.dimension())
                        && gameTime - entry.getValue().gameTime() > SHIELD_ARROW_IMPACT_EXPIRY_TICKS);
        ARMOR_ARROW_IMPACTS.entrySet().removeIf(entry ->
                entry.getValue().dimension().equals(level.dimension())
                        && gameTime - entry.getValue().gameTime() > ARMOR_ARROW_IMPACT_EXPIRY_TICKS);
        PENDING_BLOCK_ARROW_BREAKS.entrySet().removeIf(entry ->
                entry.getValue().dimension().equals(level.dimension())
                        && gameTime - entry.getValue().breakTime() > BLOCK_ARROW_BREAK_EXPIRY_TICKS);
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
        return arrow.getType().is(LodgedTags.TRACKABLE_PROJECTILES);
    }

    private static boolean canLodge(AbstractArrow arrow) {
        return !arrow.getType().is(LodgedTags.NON_LODGING_PROJECTILES);
    }

    private static boolean causesBleeding(AbstractArrow arrow) {
        return arrow.getType().is(LodgedTags.BLEEDING_PROJECTILES);
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

    private static boolean willEvictArmorArrow(ItemStack armor) {
        int maxTrackedArrows = LodgedConfig.maxTrackedArrowsPerArmorPiece();
        return maxTrackedArrows > 0 && LodgedArmorArrowStorage.readAll(armor).size() >= maxTrackedArrows;
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

    private static ProjectileStack projectileStack(AbstractArrow arrow) {
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
                return originalStack.copyWithCount(1);
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

    private record ProjectileStack(ItemStack stack, boolean recoverable) {
    }

    private record BrokenArrowImpact(
            UUID targetUuid,
            LodgedArrowVisual visual,
            boolean causesBleeding,
            ResourceKey<Level> dimension,
            long gameTime) {
    }

    private record ShieldArrowImpact(
            UUID targetUuid,
            LodgedArrowVisual visual,
            ResourceKey<Level> dimension,
            long gameTime) {
    }

    private record ArmorArrowImpact(
            UUID targetUuid,
            EquipmentSlot slot,
            ItemStack stack,
            boolean recoverable,
            boolean fromPlayer,
            boolean infinityGenerated,
            boolean creativeGenerated,
            LodgedArrowVisual visual,
            boolean breaks,
            int pierceLevel,
            ResourceKey<Level> dimension,
            long gameTime) {
        private ArmorArrowImpact {
            stack = stack.copyWithCount(1);
            visual = visual.withStack(stack);
        }
    }

    private record PendingBlockArrowBreak(
            ResourceKey<Level> dimension,
            long breakTime) {
    }
}
