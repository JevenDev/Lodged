package com.jvn.lodged.config;

import com.jvn.lodged.world.LodgedArrowBodyPart;
import com.jvn.lodged.world.LodgedArrowDepth;
import java.util.List;

public final class LodgedConfig {
    private static final LodgedConfigWrapper CONFIG = LodgedConfigWrapper.createAndLoad();

    private LodgedConfig() {
    }

    public static void load() {
        CONFIG.getClass();
    }

    public static boolean enableArrowRecovery() {
        return arrowRecovery().enableArrowRecovery();
    }

    public static double recoveryChance() {
        return arrowRecovery().recoveryChance();
    }

    public static int maxTrackedArrowsPerEntity() {
        return arrowRecovery().maxTrackedArrowsPerEntity();
    }

    public static boolean recoverPlayerArrowsOnly() {
        return arrowRecovery().recoverPlayerArrowsOnly();
    }

    public static boolean recoverMobArrows() {
        return arrowRecovery().recoverMobArrows() || !recoverPlayerArrowsOnly();
    }

    public static boolean recoverInfinityArrows() {
        return arrowRecovery().recoverInfinityArrows();
    }

    public static boolean recoverCreativeArrows() {
        return arrowRecovery().recoverCreativeArrows();
    }

    public static boolean enableArrowBreakOnEntityHit() {
        return arrowBreakage().enableArrowBreakOnEntityHit();
    }

    public static boolean enableArrowBreakOnBlockHit() {
        return arrowBreakage().enableArrowBreakOnBlockHit();
    }

    public static boolean enableMobArrowBreak() {
        return arrowBreakage().enableMobArrowBreak();
    }

    public static double regularArrowImpactBreakChance() {
        return arrowBreakage().regularArrowImpactBreakChance();
    }

    public static double mobArrowImpactBreakChance() {
        return arrowBreakage().mobArrowImpactBreakChance();
    }

    public static double infinityArrowImpactBreakChance() {
        return arrowBreakage().infinityArrowImpactBreakChance();
    }

    public static boolean preserveArrowItemStack() {
        return arrowRecovery().preserveArrowItemStack();
    }

    public static boolean preventPlayerArrowDespawn() {
        return arrowRecovery().preventPlayerArrowDespawn();
    }

    public static boolean preventNonPlayerArrowDespawn() {
        return arrowRecovery().preventNonPlayerArrowDespawn();
    }

    public static boolean enablePlayerArrowRemoval() {
        return playerArrowRemoval().enablePlayerArrowRemoval();
    }

    public static boolean showInventoryTurnHint() {
        return playerArrowRemoval().showInventoryTurnHint();
    }

    public static boolean showInventoryTurnHintTooltip() {
        return playerArrowRemoval().showInventoryTurnHintTooltip();
    }

    public static boolean enableArrowRemovalAnimation() {
        return playerArrowRemoval().enableArrowRemovalAnimation();
    }

    public static double arrowRemovalAnimationSpeedMultiplier() {
        return Math.max(0.1D, playerArrowRemoval().arrowRemovalAnimationSpeedMultiplier());
    }

    public static double playerArrowRemovalSuccessChance(LodgedArrowBodyPart bodyPart) {
        return switch (bodyPart) {
            case HEAD -> playerArrowRemoval().playerArrowRemovalHeadSuccessChance();
            case CHEST -> playerArrowRemoval().playerArrowRemovalChestSuccessChance();
            case ARM -> playerArrowRemoval().playerArrowRemovalArmSuccessChance();
            case LEG -> playerArrowRemoval().playerArrowRemovalLegSuccessChance();
        };
    }

    public static double playerArrowRemovalInfinitySuccessMultiplier() {
        return playerArrowRemoval().playerArrowRemovalInfinitySuccessMultiplier();
    }

    public static float playerArrowRemovalBreakDamage() {
        return (float) playerArrowRemoval().playerArrowRemovalBreakDamage();
    }

    public static boolean arrowRemovalAppliesKnockback() {
        return playerArrowRemoval().arrowRemovalAppliesKnockback();
    }

    public static boolean allowArrowRemovalInCreative() {
        return playerArrowRemoval().allowArrowRemovalInCreative();
    }

    public static boolean requireInventoryScreenForRemoval() {
        return playerArrowRemoval().requireInventoryScreenForRemoval();
    }

    public static int maxRemovablePlayerArrows() {
        int configuredMax = playerArrowRemoval().maxRemovablePlayerArrows();
        return configuredMax > 0 ? configuredMax : maxTrackedArrowsPerEntity();
    }

    public static boolean enableArrowDepthTiers() {
        return arrowDepth().enableArrowDepthTiers();
    }

    public static double deepLodgedBaseChance(LodgedArrowBodyPart bodyPart) {
        return switch (bodyPart) {
            case HEAD -> arrowDepth().deepLodgedHeadChance();
            case CHEST -> arrowDepth().deepLodgedChestChance();
            case ARM -> arrowDepth().deepLodgedArmChance();
            case LEG -> arrowDepth().deepLodgedLegChance();
        };
    }

    public static double shallowBaseChance(LodgedArrowBodyPart bodyPart) {
        return switch (bodyPart) {
            case HEAD -> arrowDepth().shallowHeadChance();
            case CHEST -> arrowDepth().shallowChestChance();
            case ARM -> arrowDepth().shallowArmChance();
            case LEG -> arrowDepth().shallowLegChance();
        };
    }

    public static double arrowDepthVelocityBonusStart() {
        return arrowDepth().velocityBonusStart();
    }

    public static double arrowDepthVelocityBonusRange() {
        return Math.max(0.01D, arrowDepth().velocityBonusRange());
    }

    public static double velocityDeepLodgedMaxBonus() {
        return arrowDepth().velocityDeepLodgedMaxBonus();
    }

    public static double velocityShallowMaxPenalty() {
        return arrowDepth().velocityShallowMaxPenalty();
    }

    public static double criticalDeepLodgedBonus() {
        return arrowDepth().criticalDeepLodgedBonus();
    }

    public static double criticalShallowPenalty() {
        return arrowDepth().criticalShallowPenalty();
    }

    public static double weaponDamageDeepLodgedBonusPerDamage() {
        return arrowDepth().weaponDamageDeepLodgedBonusPerDamage();
    }

    public static double weaponDamageDeepLodgedMaxBonus() {
        return arrowDepth().weaponDamageDeepLodgedMaxBonus();
    }

    public static double powerDeepLodgedBonusPerLevel() {
        return arrowDepth().powerDeepLodgedBonusPerLevel();
    }

    public static double crossbowDeepLodgedBonus() {
        return arrowDepth().crossbowDeepLodgedBonus();
    }

    public static double armoredDeepLodgedMultiplier() {
        return arrowDepth().armoredDeepLodgedMultiplier();
    }

    public static double unarmoredDeepLodgedBonus(LodgedArrowBodyPart bodyPart) {
        return switch (bodyPart) {
            case HEAD, CHEST -> arrowDepth().unarmoredHeadChestDeepLodgedBonus();
            case ARM, LEG -> arrowDepth().unarmoredLimbDeepLodgedBonus();
        };
    }

    public static int strongHeadChestPowerLevelThreshold() {
        return arrowDepth().strongHeadChestPowerLevelThreshold();
    }

    public static double strongHeadChestDamageThreshold() {
        return arrowDepth().strongHeadChestDamageThreshold();
    }

    public static double strongHeadChestDeepLodgedChance() {
        return arrowDepth().strongHeadChestDeepLodgedChance();
    }

    public static double powerFiveHeadChestDeepLodgedChance() {
        return arrowDepth().powerFiveHeadChestDeepLodgedChance();
    }

    public static double fastCriticalVelocityThreshold() {
        return arrowDepth().fastCriticalVelocityThreshold();
    }

    public static double fastCriticalHeadChestDeepLodgedChance() {
        return arrowDepth().fastCriticalHeadChestDeepLodgedChance();
    }

    public static double powerFiveAnyBodyPartDeepLodgedChance() {
        return arrowDepth().powerFiveAnyBodyPartDeepLodgedChance();
    }

    public static double deepLodgedMaxChance(int powerLevel) {
        return powerLevel >= 5
                ? arrowDepth().powerFiveDeepLodgedMaxChance()
                : arrowDepth().deepLodgedMaxChance();
    }

    public static double shallowMinChance() {
        return arrowDepth().shallowMinChance();
    }

    public static double shallowMaxChance() {
        return Math.max(shallowMinChance(), arrowDepth().shallowMaxChance());
    }

    public static double arrowDepthRemovalSuccessMultiplier(LodgedArrowDepth depth) {
        return switch (depth) {
            case SHALLOW -> arrowDepth().shallowRemovalSuccessMultiplier();
            case LODGED -> arrowDepth().lodgedRemovalSuccessMultiplier();
            case DEEP_LODGED -> arrowDepth().deepLodgedRemovalSuccessMultiplier();
        };
    }

    public static double arrowDepthBleedingChanceMultiplier(LodgedArrowDepth depth) {
        return switch (depth) {
            case SHALLOW -> arrowDepth().shallowBleedingChanceMultiplier();
            case LODGED -> arrowDepth().lodgedBleedingChanceMultiplier();
            case DEEP_LODGED -> arrowDepth().deepLodgedBleedingChanceMultiplier();
        };
    }

    public static double arrowDepthBleedingDurationMultiplier(LodgedArrowDepth depth) {
        return switch (depth) {
            case SHALLOW -> arrowDepth().shallowBleedingDurationMultiplier();
            case LODGED -> arrowDepth().lodgedBleedingDurationMultiplier();
            case DEEP_LODGED -> arrowDepth().deepLodgedBleedingDurationMultiplier();
        };
    }

    public static int arrowDepthRemovalAnimationMs(LodgedArrowDepth depth) {
        return switch (depth) {
            case SHALLOW -> arrowDepth().shallowRemovalAnimationMs();
            case LODGED -> arrowDepth().lodgedRemovalAnimationMs();
            case DEEP_LODGED -> arrowDepth().deepLodgedRemovalAnimationMs();
        };
    }

    public static boolean enableShieldArrowLodging() {
        return shieldArrows().enableShieldArrowLodging();
    }

    public static boolean recoverMobShieldArrows() {
        return shieldArrows().recoverMobShieldArrows();
    }

    public static int maxTrackedArrowsPerShield() {
        int configuredMax = shieldArrows().maxTrackedArrowsPerShield();
        return configuredMax > 0 ? configuredMax : maxTrackedArrowsPerEntity();
    }

    public static boolean renderOwnShieldArrowsInFirstPerson() {
        return shieldArrows().renderOwnShieldArrowsInFirstPerson();
    }

    public static boolean enableLodgedArrowDizziness() {
        return playerArrowRemoval().enableLodgedArrowDizziness();
    }

    public static int lodgedArrowDizzinessMinArrows() {
        return playerArrowRemoval().lodgedArrowDizzinessMinArrows();
    }

    public static double lodgedArrowDizzinessPercentOfMax() {
        return playerArrowRemoval().lodgedArrowDizzinessPercentOfMax();
    }

    public static int lodgedArrowDizzinessDuration() {
        return playerArrowRemoval().lodgedArrowDizzinessDuration();
    }

    public static int lodgedArrowDizzinessRefreshInterval() {
        return playerArrowRemoval().lodgedArrowDizzinessRefreshInterval();
    }

    public static boolean enableLegShotSlowness() {
        return legShotSlowness().enableLegShotSlowness();
    }

    public static int legShotSlownessDuration() {
        return legShotSlowness().legShotSlownessDuration();
    }

    public static int legShotSlownessAmplifier() {
        return Math.max(0, legShotSlowness().legShotSlownessLevel() - 1);
    }

    public static List<? extends String> entityDenylist() {
        return arrowRecovery().entityDenylist();
    }

    public static boolean enableBleeding() {
        return bleedingRules().enableBleeding();
    }

    public static boolean arrowRemovalCausesBleeding() {
        return enableBleeding() && bleedingRules().arrowRemovalCausesBleeding();
    }

    public static boolean enchantsCauseBleeding() {
        return enableBleeding() && bleedingRules().enchantsCauseBleeding();
    }

    public static boolean weaponsCauseBleeding() {
        return enableBleeding() && bleedingRules().weaponsCauseBleeding();
    }

    public static boolean skeletonsAffectedByBleeding() {
        return bleedingRules().skeletonsAffectedByBleeding();
    }

    public static boolean undeadAffectedByBleeding() {
        return bleedingRules().undeadAffectedByBleeding();
    }

    public static boolean fullArmorPreventsBleeding() {
        return bleedingRules().fullArmorPreventsBleeding();
    }

    public static boolean partialArmorPreventsBleeding() {
        return bleedingRules().partialArmorPreventsBleeding();
    }

    public static double fullArmorBleedingChance() {
        return bleedingRules().fullArmorBleedingChance();
    }

    public static double partialArmorBleedingChance() {
        return bleedingRules().partialArmorBleedingChance();
    }

    public static double noArmorBleedingChance() {
        return bleedingRules().noArmorBleedingChance();
    }

    public static double failedArrowRemovalFullArmorBleedingChance() {
        return bleedingRules().failedArrowRemovalFullArmorBleedingChance();
    }

    public static double failedArrowRemovalPartialArmorBleedingChance() {
        return bleedingRules().failedArrowRemovalPartialArmorBleedingChance();
    }

    public static double failedArrowRemovalNoArmorBleedingChance() {
        return bleedingRules().failedArrowRemovalNoArmorBleedingChance();
    }

    public static int bleedingDamageDuration() {
        return bleedingEffect().bleedingDamageDuration();
    }

    public static int bleedingArrowRemovalDuration() {
        return bleedingEffect().bleedingArrowRemovalDuration();
    }

    public static int bleedingMaxDuration() {
        return bleedingEffect().bleedingMaxDuration();
    }

    public static int bleedingStrongDurationThreshold() {
        return Math.min(bleedingEffect().bleedingStrongDurationThreshold(), bleedingMaxDuration());
    }

    public static int bleedingTickInterval(int amplifier) {
        return amplifier > 0
                ? bleedingEffect().bleedingStrongTickInterval()
                : bleedingEffect().bleedingBaseTickInterval();
    }

    public static float bleedingDamage(int amplifier) {
        return (float) (amplifier > 0
                ? bleedingEffect().bleedingStrongDamage()
                : bleedingEffect().bleedingBaseDamage());
    }

    public static boolean bleedingDripParticles() {
        return bleedingEffect().bleedingDripParticles();
    }

    public static boolean simpleBloodCompatParticles() {
        return bleedingDripParticles() && bleedingEffect().simpleBloodCompatParticles();
    }

    public static float simpleBloodGroundDecalChance() {
        return (float) bleedingEffect().simpleBloodGroundDecalChance();
    }

    public static float simpleBloodGroundDecalScaleMultiplier() {
        return (float) bleedingEffect().simpleBloodGroundDecalScaleMultiplier();
    }

    public static float simpleBloodGroundDecalMinScale() {
        return (float) Math.min(
                bleedingEffect().simpleBloodGroundDecalMinScale(),
                bleedingEffect().simpleBloodGroundDecalMaxScale());
    }

    public static float simpleBloodGroundDecalMaxScale() {
        return (float) Math.max(
                bleedingEffect().simpleBloodGroundDecalMinScale(),
                bleedingEffect().simpleBloodGroundDecalMaxScale());
    }

    public static float simpleBloodGroundDecalRandomMinScale() {
        return (float) Math.min(
                bleedingEffect().simpleBloodGroundDecalRandomMinScale(),
                bleedingEffect().simpleBloodGroundDecalRandomMaxScale());
    }

    public static float simpleBloodGroundDecalRandomMaxScale() {
        return (float) Math.max(
                bleedingEffect().simpleBloodGroundDecalRandomMinScale(),
                bleedingEffect().simpleBloodGroundDecalRandomMaxScale());
    }

    public static int bleedingDripInterval(int amplifier) {
        return amplifier > 0
                ? bleedingEffect().bleedingStrongDripInterval()
                : bleedingEffect().bleedingBaseDripInterval();
    }

    private static LodgedConfigWrapper.ArrowRecovery arrowRecovery() {
        return CONFIG.arrowRecovery;
    }

    private static LodgedConfigWrapper.ArrowBreakage arrowBreakage() {
        return CONFIG.arrowBreakage;
    }

    private static LodgedConfigWrapper.PlayerArrowRemoval playerArrowRemoval() {
        return CONFIG.playerArrowRemoval;
    }

    private static LodgedConfigWrapper.ArrowDepth arrowDepth() {
        return CONFIG.arrowDepth;
    }

    private static LodgedConfigWrapper.ShieldArrows shieldArrows() {
        return CONFIG.shieldArrows;
    }

    private static LodgedConfigWrapper.LegShotSlowness legShotSlowness() {
        return CONFIG.legShotSlowness;
    }

    private static LodgedConfigWrapper.BleedingRules bleedingRules() {
        return CONFIG.bleedingRules;
    }

    private static LodgedConfigWrapper.BleedingEffect bleedingEffect() {
        return CONFIG.bleedingEffect;
    }
}
