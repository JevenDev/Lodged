package com.jvn.lodged.config;

import com.jvn.lodged.Lodged;
import com.jvn.lodged.world.LodgedArrowBodyPart;
import com.jvn.lodged.world.LodgedArrowDepth;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleSupplier;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;
import net.minecraft.util.Mth;

public final class LodgedConfig {
    private static final int CURRENT_CONFIG_VERSION = 8;
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
        int loadedVersion = config.configVersion();
        boolean legacyUnversioned = isLegacyUnversionedConfig(config);

        if (legacyUnversioned) {
            if (!config.arrowRecovery.recoverMobArrows()) {
                config.arrowRecovery.recoverMobArrows(true);
                Lodged.LOGGER.info("Updated legacy Lodged config: enabled mob arrow recovery to match current defaults");
            }
            save = true;
        }

        if (legacyUnversioned || loadedVersion < 2) {
            migrateBalancedDefaults(config);
            Lodged.LOGGER.info("Updated Lodged's unchanged defaults to the balanced v2 values");
            save = true;
        }

        if (loadedVersion < 4 && config.tamedMobArrowRemoval.requireMobOwnership()) {
            config.tamedMobArrowRemoval.requireMobOwnership(false);
            Lodged.LOGGER.info("Updated tamed mob arrow removal to allow any tamed mob by default");
            save = true;
        }

        if (loadedVersion < 5
                && Double.compare(config.tamedMobArrowRemoval.tamedMobArrowRemovalRange(), 4.5D) == 0) {
            config.tamedMobArrowRemoval.tamedMobArrowRemovalRange(1.0D);
            Lodged.LOGGER.info("Updated the default tamed mob arrow removal range to one block");
            save = true;
        }

        if (loadedVersion < 7 && !config.projectileCollision.enableModelAccurateProjectileCollision()) {
            config.projectileCollision.enableModelAccurateProjectileCollision(true);
            Lodged.LOGGER.info("Enabled model-accurate projectile collision to match the new default");
            save = true;
        }

        if (loadedVersion < CURRENT_CONFIG_VERSION) {
            save = true;
        }

        if (config.configVersion() != CURRENT_CONFIG_VERSION) {
            config.configVersion(CURRENT_CONFIG_VERSION);
        }

        if (save) {
            config.save();
        }
    }

    private static void migrateBalancedDefaults(LodgedConfigWrapper config) {
        migrateDefault(config.arrowRecovery::recoveryChance, config.arrowRecovery::recoveryChance, 0.35D, 0.50D);

        migrateDefault(config.playerArrowRemoval::playerArrowRemovalHeadSuccessChance,
                config.playerArrowRemoval::playerArrowRemovalHeadSuccessChance, 0.35D, 0.50D);
        migrateDefault(config.playerArrowRemoval::playerArrowRemovalChestSuccessChance,
                config.playerArrowRemoval::playerArrowRemovalChestSuccessChance, 0.65D, 0.72D);
        migrateDefault(config.playerArrowRemoval::playerArrowRemovalArmSuccessChance,
                config.playerArrowRemoval::playerArrowRemovalArmSuccessChance, 0.85D, 0.90D);
        migrateDefault(config.playerArrowRemoval::playerArrowRemovalLegSuccessChance,
                config.playerArrowRemoval::playerArrowRemovalLegSuccessChance, 0.85D, 0.90D);
        migrateDefault(config.playerArrowRemoval::playerArrowRemovalBreakDamage,
                config.playerArrowRemoval::playerArrowRemovalBreakDamage, 2.0D, 1.0D);

        migrateDefault(config.arrowDepth::weaponDamageDeepLodgedBonusPerDamage,
                config.arrowDepth::weaponDamageDeepLodgedBonusPerDamage, 0.08D, 0.06D);
        migrateDefault(config.arrowDepth::weaponDamageDeepLodgedMaxBonus,
                config.arrowDepth::weaponDamageDeepLodgedMaxBonus, 0.30D, 0.20D);
        migrateDefault(config.arrowDepth::powerDeepLodgedBonusPerLevel,
                config.arrowDepth::powerDeepLodgedBonusPerLevel, 0.06D, 0.04D);
        migrateDefault(config.arrowDepth::unarmoredHeadChestDeepLodgedBonus,
                config.arrowDepth::unarmoredHeadChestDeepLodgedBonus, 0.08D, 0.06D);
        migrateDefault(config.arrowDepth::unarmoredLimbDeepLodgedBonus,
                config.arrowDepth::unarmoredLimbDeepLodgedBonus, 0.04D, 0.03D);
        migrateDefault(config.arrowDepth::strongHeadChestDeepLodgedChance,
                config.arrowDepth::strongHeadChestDeepLodgedChance, 0.80D, 0.65D);
        migrateDefault(config.arrowDepth::powerHeadChestDeepLodgedChanceBonusPerLevel,
                config.arrowDepth::powerHeadChestDeepLodgedChanceBonusPerLevel, 0.03D, 0.025D);
        migrateDefault(config.arrowDepth::fastCriticalHeadChestDeepLodgedChance,
                config.arrowDepth::fastCriticalHeadChestDeepLodgedChance, 0.65D, 0.55D);
        migrateDefault(config.arrowDepth::powerAnyBodyPartDeepLodgedChancePerLevel,
                config.arrowDepth::powerAnyBodyPartDeepLodgedChancePerLevel, 0.11D, 0.08D);
        migrateDefault(config.arrowDepth::deepLodgedMaxChance,
                config.arrowDepth::deepLodgedMaxChance, 0.85D, 0.80D);
        migrateDefault(config.arrowDepth::powerDeepLodgedMaxChanceBonusPerLevel,
                config.arrowDepth::powerDeepLodgedMaxChanceBonusPerLevel, 0.026D, 0.02D);
        migrateDefault(config.arrowDepth::deepLodgedRemovalSuccessMultiplier,
                config.arrowDepth::deepLodgedRemovalSuccessMultiplier, 0.55D, 0.70D);
        migrateDefault(config.arrowDepth::deepLodgedBleedingChanceMultiplier,
                config.arrowDepth::deepLodgedBleedingChanceMultiplier, 1.75D, 1.40D);
        migrateDefault(config.arrowDepth::deepLodgedBleedingDurationMultiplier,
                config.arrowDepth::deepLodgedBleedingDurationMultiplier, 1.50D, 1.25D);

        migrateDefault(config.shieldArrows::shieldArrowRemovalSuccessChance,
                config.shieldArrows::shieldArrowRemovalSuccessChance, 0.85D, 0.90D);
        migrateDefault(config.shieldArrows::shieldArrowDurabilityDamageChance,
                config.shieldArrows::shieldArrowDurabilityDamageChance, 0.35D, 0.25D);

        migrateDefault(config.armorArrows::armorArrowRemovalSuccessChance,
                config.armorArrows::armorArrowRemovalSuccessChance, 0.92D, 0.95D);
        migrateDefault(config.armorArrows::armorArrowRemovalDurabilityDamageChance,
                config.armorArrows::armorArrowRemovalDurabilityDamageChance, 0.35D, 0.25D);
        migrateDefault(config.armorArrows::armorArrowRemovalBreakChance,
                config.armorArrows::armorArrowRemovalBreakChance, 0.05D, 0.03D);
        migrateDefault(config.armorArrows::armorArrowBreakDurabilityDamageChance,
                config.armorArrows::armorArrowBreakDurabilityDamageChance, 1.0D, 0.75D);
        migrateDefault(config.armorArrows::armorArrowExtraDurabilityLossChance,
                config.armorArrows::armorArrowExtraDurabilityLossChance, 0.10D, 0.08D);
        migrateDefault(config.armorArrows::armorArrowExtraDurabilityLossChancePerAdditionalArrow,
                config.armorArrows::armorArrowExtraDurabilityLossChancePerAdditionalArrow, 0.05D, 0.04D);
        migrateDefault(config.armorArrows::armorArrowExtraDurabilityLossMaxChance,
                config.armorArrows::armorArrowExtraDurabilityLossMaxChance, 0.50D, 0.32D);

        migrateDefault(config.bleedingRules::partialArmorBleedingChance,
                config.bleedingRules::partialArmorBleedingChance, 0.65D, 0.50D);
        migrateDefault(config.bleedingRules::noArmorBleedingChance,
                config.bleedingRules::noArmorBleedingChance, 1.0D, 0.85D);
        migrateDefault(config.bleedingRules::failedArrowRemovalFullArmorBleedingChance,
                config.bleedingRules::failedArrowRemovalFullArmorBleedingChance, 0.25D, 0.15D);
        migrateDefault(config.bleedingRules::failedArrowRemovalPartialArmorBleedingChance,
                config.bleedingRules::failedArrowRemovalPartialArmorBleedingChance, 0.75D, 0.60D);
        migrateDefault(config.bleedingRules::failedArrowRemovalNoArmorBleedingChance,
                config.bleedingRules::failedArrowRemovalNoArmorBleedingChance, 1.0D, 0.85D);
        migrateIntDefault(config.bleedingEffect::bleedingArrowRemovalDuration,
                config.bleedingEffect::bleedingArrowRemovalDuration, 200, 160);
    }

    private static void migrateDefault(
            DoubleSupplier getter,
            DoubleConsumer setter,
            double oldDefault,
            double newDefault) {
        if (Double.compare(getter.getAsDouble(), oldDefault) == 0) {
            setter.accept(newDefault);
        }
    }

    private static void migrateIntDefault(
            IntSupplier getter,
            IntConsumer setter,
            int oldDefault,
            int newDefault) {
        if (getter.getAsInt() == oldDefault) {
            setter.accept(newDefault);
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
    public static boolean enableModelAccurateProjectileCollision() {
        return projectileCollision().enableModelAccurateProjectileCollision();
    }

    public static boolean showModelHitboxesInDebug() {
        return projectileCollision().showModelHitboxesInDebug();
    }

    public static double modelHitboxInflation() {
        return Mth.clamp(projectileCollision().modelHitboxInflation(), 0.0D, 0.25D);
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

    public static boolean enableTamedMobArrowRemoval() {
        return tamedMobArrowRemoval().enableTamedMobArrowRemoval();
    }

    public static boolean requireMobOwnership() {
        return tamedMobArrowRemoval().requireMobOwnership();
    }

    public static boolean tamedMobArrowRemovalCausesBleeding() {
        return tamedMobArrowRemoval().tamedMobArrowRemovalCausesBleeding();
    }

    public static boolean tamedMobArrowRemovalParticles() {
        return tamedMobArrowRemoval().tamedMobArrowRemovalParticles();
    }

    public static double tamedMobArrowRemovalRange() {
        return tamedMobArrowRemoval().tamedMobArrowRemovalRange();
    }

    public static double tamedMobArrowRemovalSuccessChance(LodgedArrowBodyPart bodyPart) {
        return switch (bodyPart) {
            case HEAD -> tamedMobArrowRemoval().tamedMobArrowRemovalHeadSuccessChance();
            case CHEST -> tamedMobArrowRemoval().tamedMobArrowRemovalChestSuccessChance();
            case LEFT_ARM, RIGHT_ARM -> tamedMobArrowRemoval().tamedMobArrowRemovalArmSuccessChance();
            case LEFT_LEG, RIGHT_LEG -> tamedMobArrowRemoval().tamedMobArrowRemovalLegSuccessChance();
        };
    }

    public static int tamedMobArrowRemovalMinTicks() {
        return Math.min(tamedMobArrowRemoval().tamedMobArrowRemovalMinTicks(), tamedMobArrowRemovalMaxTicks());
    }

    public static int tamedMobArrowRemovalMaxTicks() {
        return Math.max(
                tamedMobArrowRemoval().tamedMobArrowRemovalMinTicks(),
                tamedMobArrowRemoval().tamedMobArrowRemovalMaxTicks());
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

    public static double bleedingWoundDurationMultiplier(LodgedArrowBodyPart bodyPart) {
        return switch (bodyPart) {
            case HEAD -> bleedingEffect().bleedingHeadWoundDurationMultiplier();
            case CHEST -> bleedingEffect().bleedingChestWoundDurationMultiplier();
            case LEFT_ARM, RIGHT_ARM -> bleedingEffect().bleedingArmWoundDurationMultiplier();
            case LEFT_LEG, RIGHT_LEG -> bleedingEffect().bleedingLegWoundDurationMultiplier();
        };
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

    private static LodgedConfigWrapper.ProjectileCollision projectileCollision() {
        return CONFIG.projectileCollision;
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

    private static LodgedConfigWrapper.TamedMobArrowRemoval tamedMobArrowRemoval() {
        return CONFIG.tamedMobArrowRemoval;
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
