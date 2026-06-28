package com.jvn.lodged.config;

import com.jvn.lodged.Lodged;
import com.jvn.lodged.world.LodgedArrowBodyPart;
import com.jvn.lodged.world.LodgedArrowDepth;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import net.minecraft.util.Mth;

public final class LodgedConfig {
    private static final int CURRENT_CONFIG_VERSION = 1;
    private static final int MAX_SYNCED_BODY_ARROW_VISUALS = 64;
    private static final String CONFIG_VERSION_KEY = "configVersion";
    private static final LodgedConfigWrapper CONFIG = loadConfig();

    private LodgedConfig() {
    }

    public static void load() {
        CONFIG.getClass();
    }

    private static LodgedConfigWrapper loadConfig() {
        LodgedConfigWrapper config = LodgedConfigWrapper.createAndLoad();
        migrateConfig(config);
        return config;
    }

    private static void migrateConfig(LodgedConfigWrapper config) {
        boolean save = false;

        if (isLegacyUnversionedConfig(config)) {
            if (!config.arrowRecovery.recoverMobArrows()) {
                config.arrowRecovery.recoverMobArrows(true);
                Lodged.LOGGER.info("Updated legacy Lodged config: enabled mob arrow recovery to match current defaults");
            }
            save = true;
        }

        if (config.configVersion() < CURRENT_CONFIG_VERSION) {
            save = true;
        }

        if (config.configVersion() != CURRENT_CONFIG_VERSION) {
            config.configVersion(CURRENT_CONFIG_VERSION);
        }

        if (save) {
            config.save();
        }
    }

    private static boolean isLegacyUnversionedConfig(LodgedConfigWrapper config) {
        if (!Files.exists(config.fileLocation())) {
            return false;
        }

        try {
            return !Files.readString(config.fileLocation(), StandardCharsets.UTF_8).contains(CONFIG_VERSION_KEY);
        } catch (IOException exception) {
            Lodged.LOGGER.warn("Could not inspect Lodged config for migration", exception);
            return false;
        }
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

    public static int maxTrackedBodyArrowsPerEntity() {
        int maxTrackedArrowsPerBodyPart = maxTrackedArrowsPerEntity();
        return maxTrackedArrowsPerBodyPart > 0
                ? maxTrackedArrowsPerBodyPart * LodgedArrowBodyPart.values().length
                : 0;
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
            case LEFT_ARM, RIGHT_ARM -> playerArrowRemoval().playerArrowRemovalArmSuccessChance();
            case LEFT_LEG, RIGHT_LEG -> playerArrowRemoval().playerArrowRemovalLegSuccessChance();
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
        return configuredMax > 0
                ? configuredMax
                : Math.min(maxTrackedBodyArrowsPerEntity(), MAX_SYNCED_BODY_ARROW_VISUALS);
    }

    public static int inWorldBodyArrowRemovalMinTicks() {
        return Math.min(playerArrowRemoval().inWorldBodyArrowRemovalMinTicks(), inWorldBodyArrowRemovalMaxTicks());
    }

    public static int inWorldBodyArrowRemovalMaxTicks() {
        return Math.max(
                playerArrowRemoval().inWorldBodyArrowRemovalMinTicks(),
                playerArrowRemoval().inWorldBodyArrowRemovalMaxTicks());
    }

    public static boolean enableArrowDepthTiers() {
        return arrowDepth().enableArrowDepthTiers();
    }

    public static double deepLodgedBaseChance(LodgedArrowBodyPart bodyPart) {
        return switch (bodyPart) {
            case HEAD -> arrowDepth().deepLodgedHeadChance();
            case CHEST -> arrowDepth().deepLodgedChestChance();
            case LEFT_ARM, RIGHT_ARM -> arrowDepth().deepLodgedArmChance();
            case LEFT_LEG, RIGHT_LEG -> arrowDepth().deepLodgedLegChance();
        };
    }

    public static double shallowBaseChance(LodgedArrowBodyPart bodyPart) {
        return switch (bodyPart) {
            case HEAD -> arrowDepth().shallowHeadChance();
            case CHEST -> arrowDepth().shallowChestChance();
            case LEFT_ARM, RIGHT_ARM -> arrowDepth().shallowArmChance();
            case LEFT_LEG, RIGHT_LEG -> arrowDepth().shallowLegChance();
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
            case LEFT_ARM, RIGHT_ARM, LEFT_LEG, RIGHT_LEG -> arrowDepth().unarmoredLimbDeepLodgedBonus();
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

    public static double powerHeadChestDeepLodgedChance(int powerLevel) {
        return Mth.clamp(
                strongHeadChestDeepLodgedChance()
                        + positivePowerLevel(powerLevel) * arrowDepth().powerHeadChestDeepLodgedChanceBonusPerLevel(),
                0.0D,
                1.0D);
    }

    public static double fastCriticalVelocityThreshold() {
        return arrowDepth().fastCriticalVelocityThreshold();
    }

    public static double fastCriticalHeadChestDeepLodgedChance() {
        return arrowDepth().fastCriticalHeadChestDeepLodgedChance();
    }

    public static double powerAnyBodyPartDeepLodgedChance(int powerLevel) {
        return Mth.clamp(
                positivePowerLevel(powerLevel) * arrowDepth().powerAnyBodyPartDeepLodgedChancePerLevel(),
                0.0D,
                1.0D);
    }

    public static double deepLodgedMaxChance(int powerLevel) {
        return Mth.clamp(
                arrowDepth().deepLodgedMaxChance()
                        + positivePowerLevel(powerLevel) * arrowDepth().powerDeepLodgedMaxChanceBonusPerLevel(),
                0.0D,
                1.0D);
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

    public static double shieldArrowRemovalSuccessChance() {
        return shieldArrows().shieldArrowRemovalSuccessChance();
    }

    public static int shieldArrowRemovalMinTicks() {
        return Math.min(shieldArrows().shieldArrowRemovalMinTicks(), shieldArrowRemovalMaxTicks());
    }

    public static int shieldArrowRemovalMaxTicks() {
        return Math.max(shieldArrows().shieldArrowRemovalMinTicks(), shieldArrows().shieldArrowRemovalMaxTicks());
    }

    public static double shieldArrowDurabilityDamageChance() {
        return shieldArrows().shieldArrowDurabilityDamageChance();
    }

    public static boolean renderOwnShieldArrowsInFirstPerson() {
        return shieldArrows().renderOwnShieldArrowsInFirstPerson();
    }

    public static boolean enableArmorArrowLodging() {
        return armorArrows().enableArmorArrowLodging();
    }

    public static boolean recoverMobArmorArrows() {
        return armorArrows().recoverMobArmorArrows();
    }

    public static boolean renderArmorArrows() {
        return armorArrows().renderArmorArrows();
    }

    public static boolean enableArmorArrowDurabilityPenalty() {
        return armorArrows().enableArmorArrowDurabilityPenalty();
    }

    public static int maxTrackedArrowsPerArmorPiece() {
        int configuredMax = armorArrows().maxTrackedArrowsPerArmorPiece();
        return configuredMax > 0 ? configuredMax : maxTrackedArrowsPerEntity();
    }

    public static double armorArrowBasePenetrationChance() {
        return armorArrows().armorArrowBasePenetrationChance();
    }

    public static double armorArrowDefensePenaltyPerPoint() {
        return armorArrows().armorArrowDefensePenaltyPerPoint();
    }

    public static double armorArrowMinPenetrationChance() {
        return Math.min(armorArrows().armorArrowMinPenetrationChance(), armorArrowMaxPenetrationChance());
    }

    public static double armorArrowMaxPenetrationChance() {
        return Math.max(armorArrows().armorArrowMinPenetrationChance(), armorArrows().armorArrowMaxPenetrationChance());
    }

    public static double armorArrowRemovalSuccessChance() {
        return armorArrows().armorArrowRemovalSuccessChance();
    }

    public static int armorArrowRemovalTicks() {
        return armorArrows().armorArrowRemovalTicks();
    }

    public static double armorArrowRemovalDurabilityDamageChance() {
        return armorArrows().armorArrowRemovalDurabilityDamageChance();
    }

    public static double armorArrowRemovalBreakChance() {
        return armorArrows().armorArrowRemovalBreakChance();
    }

    public static double armorArrowBreakDurabilityDamageChance() {
        return armorArrows().armorArrowBreakDurabilityDamageChance();
    }

    public static double armorArrowExtraDurabilityLossChance() {
        return armorArrows().armorArrowExtraDurabilityLossChance();
    }

    public static double armorArrowExtraDurabilityLossChancePerAdditionalArrow() {
        return armorArrows().armorArrowExtraDurabilityLossChancePerAdditionalArrow();
    }

    public static double armorArrowExtraDurabilityLossMaxChance() {
        return armorArrows().armorArrowExtraDurabilityLossMaxChance();
    }

    public static int armorArrowExtraDurabilityLossAmount() {
        return armorArrows().armorArrowExtraDurabilityLossAmount();
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

    private static LodgedConfigWrapper.ArmorArrows armorArrows() {
        return CONFIG.armorArrows;
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

    private static int positivePowerLevel(int powerLevel) {
        return Math.max(0, powerLevel);
    }
}
