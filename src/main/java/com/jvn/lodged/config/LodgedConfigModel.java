package com.jvn.lodged.config;

import io.wispforest.owo.config.annotation.Config;
import io.wispforest.owo.config.annotation.Expanded;
import io.wispforest.owo.config.annotation.Modmenu;
import io.wispforest.owo.config.annotation.Nest;
import io.wispforest.owo.config.annotation.PredicateConstraint;
import io.wispforest.owo.config.annotation.RangeConstraint;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.resources.ResourceLocation;

@Config(name = "lodged", wrapperName = "LodgedConfigWrapper")
@Modmenu(modId = "lodged")
public class LodgedConfigModel {
    @Nest
    @Expanded
    public ArrowRecovery arrowRecovery = new ArrowRecovery();

    @Nest
    @Expanded
    public ArrowBreakage arrowBreakage = new ArrowBreakage();

    @Nest
    @Expanded
    public PlayerArrowRemoval playerArrowRemoval = new PlayerArrowRemoval();

    @Nest
    @Expanded
    public ArrowDepth arrowDepth = new ArrowDepth();

    @Nest
    @Expanded
    public ShieldArrows shieldArrows = new ShieldArrows();

    @Nest
    @Expanded
    public ArmorArrows armorArrows = new ArmorArrows();

    @Nest
    @Expanded
    public LegShotSlowness legShotSlowness = new LegShotSlowness();

    @Nest
    @Expanded
    public BleedingRules bleedingRules = new BleedingRules();

    @Nest
    @Expanded
    public BleedingEffect bleedingEffect = new BleedingEffect();

    public static class ArrowRecovery {
        public boolean enableArrowRecovery = true;

        @RangeConstraint(min = 0.0D, max = 1.0D)
        public double recoveryChance = 0.35D;

        @RangeConstraint(min = 0, max = 64)
        public int maxTrackedArrowsPerEntity = 8;

        public boolean recoverPlayerArrowsOnly = true;
        public boolean recoverMobArrows = true;
        public boolean recoverInfinityArrows = false;
        public boolean recoverCreativeArrows = false;
        public boolean preserveArrowItemStack = true;
        public boolean preventPlayerArrowDespawn = true;
        public boolean preventNonPlayerArrowDespawn = false;

        @PredicateConstraint("validEntityDenylist")
        public List<String> entityDenylist = new ArrayList<>(List.of(
                "minecraft:slime",
                "minecraft:magma_cube",
                "minecraft:armor_stand"));

        public static boolean validEntityDenylist(List<String> ids) {
            return ids.stream().allMatch(id -> ResourceLocation.tryParse(id) != null);
        }
    }

    public static class ArrowBreakage {
        public boolean enableArrowBreakOnEntityHit = true;
        public boolean enableArrowBreakOnBlockHit = true;
        public boolean enableMobArrowBreak = true;

        @RangeConstraint(min = 0.0D, max = 1.0D)
        public double regularArrowImpactBreakChance = 0.02D;

        @RangeConstraint(min = 0.0D, max = 1.0D)
        public double mobArrowImpactBreakChance = 0.0D;

        @RangeConstraint(min = 0.0D, max = 1.0D)
        public double infinityArrowImpactBreakChance = 0.0D;
    }

    public static class PlayerArrowRemoval {
        public boolean enablePlayerArrowRemoval = true;
        public boolean showInventoryTurnHint = true;
        public boolean showInventoryTurnHintTooltip = true;
        public boolean enableArrowRemovalAnimation = true;

        @RangeConstraint(min = 0.1D, max = 10.0D)
        public double arrowRemovalAnimationSpeedMultiplier = 1.0D;

        @RangeConstraint(min = 0.0D, max = 1.0D)
        public double playerArrowRemovalHeadSuccessChance = 0.35D;

        @RangeConstraint(min = 0.0D, max = 1.0D)
        public double playerArrowRemovalChestSuccessChance = 0.65D;

        @RangeConstraint(min = 0.0D, max = 1.0D)
        public double playerArrowRemovalArmSuccessChance = 0.85D;

        @RangeConstraint(min = 0.0D, max = 1.0D)
        public double playerArrowRemovalLegSuccessChance = 0.85D;

        @RangeConstraint(min = 0.0D, max = 1.0D)
        public double playerArrowRemovalInfinitySuccessMultiplier = 0.5D;

        @RangeConstraint(min = 0.0D, max = 20.0D)
        public double playerArrowRemovalBreakDamage = 2.0D;

        public boolean arrowRemovalAppliesKnockback = false;
        public boolean allowArrowRemovalInCreative = true;
        public boolean requireInventoryScreenForRemoval = true;

        @RangeConstraint(min = 0, max = 64)
        public int maxRemovablePlayerArrows = 0;

        public boolean enableLodgedArrowDizziness = true;

        @RangeConstraint(min = 0, max = 64)
        public int lodgedArrowDizzinessMinArrows = 4;

        @RangeConstraint(min = 0.0D, max = 1.0D)
        public double lodgedArrowDizzinessPercentOfMax = 0.5D;

        @RangeConstraint(min = 0, max = 1200)
        public int lodgedArrowDizzinessDuration = 80;

        @RangeConstraint(min = 1, max = 1200)
        public int lodgedArrowDizzinessRefreshInterval = 20;
    }

    public static class ArrowDepth {
        public boolean enableArrowDepthTiers = true;

        @RangeConstraint(min = 0.0D, max = 1.0D)
        public double deepLodgedHeadChance = 0.06D;

        @RangeConstraint(min = 0.0D, max = 1.0D)
        public double deepLodgedChestChance = 0.10D;

        @RangeConstraint(min = 0.0D, max = 1.0D)
        public double deepLodgedArmChance = 0.02D;

        @RangeConstraint(min = 0.0D, max = 1.0D)
        public double deepLodgedLegChance = 0.025D;

        @RangeConstraint(min = 0.0D, max = 1.0D)
        public double shallowHeadChance = 0.35D;

        @RangeConstraint(min = 0.0D, max = 1.0D)
        public double shallowChestChance = 0.45D;

        @RangeConstraint(min = 0.0D, max = 1.0D)
        public double shallowArmChance = 0.78D;

        @RangeConstraint(min = 0.0D, max = 1.0D)
        public double shallowLegChance = 0.80D;

        @RangeConstraint(min = 0.0D, max = 10.0D)
        public double velocityBonusStart = 2.2D;

        @RangeConstraint(min = 0.01D, max = 10.0D)
        public double velocityBonusRange = 1.4D;

        @RangeConstraint(min = 0.0D, max = 1.0D)
        public double velocityDeepLodgedMaxBonus = 0.12D;

        @RangeConstraint(min = 0.0D, max = 1.0D)
        public double velocityShallowMaxPenalty = 0.18D;

        @RangeConstraint(min = 0.0D, max = 1.0D)
        public double criticalDeepLodgedBonus = 0.08D;

        @RangeConstraint(min = 0.0D, max = 1.0D)
        public double criticalShallowPenalty = 0.08D;

        @RangeConstraint(min = 0.0D, max = 1.0D)
        public double weaponDamageDeepLodgedBonusPerDamage = 0.08D;

        @RangeConstraint(min = 0.0D, max = 1.0D)
        public double weaponDamageDeepLodgedMaxBonus = 0.30D;

        @RangeConstraint(min = 0.0D, max = 1.0D)
        public double powerDeepLodgedBonusPerLevel = 0.06D;

        @RangeConstraint(min = 0.0D, max = 1.0D)
        public double crossbowDeepLodgedBonus = 0.03D;

        @RangeConstraint(min = 0.0D, max = 1.0D)
        public double armoredDeepLodgedMultiplier = 0.35D;

        @RangeConstraint(min = 0.0D, max = 1.0D)
        public double unarmoredHeadChestDeepLodgedBonus = 0.08D;

        @RangeConstraint(min = 0.0D, max = 1.0D)
        public double unarmoredLimbDeepLodgedBonus = 0.04D;

        @RangeConstraint(min = 0, max = 10)
        public int strongHeadChestPowerLevelThreshold = 4;

        @RangeConstraint(min = 0.0D, max = 50.0D)
        public double strongHeadChestDamageThreshold = 4.0D;

        @RangeConstraint(min = 0.0D, max = 1.0D)
        public double strongHeadChestDeepLodgedChance = 0.80D;

        @RangeConstraint(min = 0.0D, max = 1.0D)
        public double powerFiveHeadChestDeepLodgedChance = 0.95D;

        @RangeConstraint(min = 0.0D, max = 10.0D)
        public double fastCriticalVelocityThreshold = 2.75D;

        @RangeConstraint(min = 0.0D, max = 1.0D)
        public double fastCriticalHeadChestDeepLodgedChance = 0.65D;

        @RangeConstraint(min = 0.0D, max = 1.0D)
        public double powerFiveAnyBodyPartDeepLodgedChance = 0.55D;

        @RangeConstraint(min = 0.0D, max = 1.0D)
        public double deepLodgedMaxChance = 0.85D;

        @RangeConstraint(min = 0.0D, max = 1.0D)
        public double powerFiveDeepLodgedMaxChance = 0.98D;

        @RangeConstraint(min = 0.0D, max = 1.0D)
        public double shallowMinChance = 0.10D;

        @RangeConstraint(min = 0.0D, max = 1.0D)
        public double shallowMaxChance = 0.90D;

        @RangeConstraint(min = 0.0D, max = 10.0D)
        public double shallowRemovalSuccessMultiplier = 1.25D;

        @RangeConstraint(min = 0.0D, max = 10.0D)
        public double lodgedRemovalSuccessMultiplier = 1.0D;

        @RangeConstraint(min = 0.0D, max = 10.0D)
        public double deepLodgedRemovalSuccessMultiplier = 0.55D;

        @RangeConstraint(min = 0.0D, max = 10.0D)
        public double shallowBleedingChanceMultiplier = 0.55D;

        @RangeConstraint(min = 0.0D, max = 10.0D)
        public double lodgedBleedingChanceMultiplier = 1.0D;

        @RangeConstraint(min = 0.0D, max = 10.0D)
        public double deepLodgedBleedingChanceMultiplier = 1.75D;

        @RangeConstraint(min = 0.0D, max = 10.0D)
        public double shallowBleedingDurationMultiplier = 0.75D;

        @RangeConstraint(min = 0.0D, max = 10.0D)
        public double lodgedBleedingDurationMultiplier = 1.0D;

        @RangeConstraint(min = 0.0D, max = 10.0D)
        public double deepLodgedBleedingDurationMultiplier = 1.5D;

        @RangeConstraint(min = 1, max = 5000)
        public int shallowRemovalAnimationMs = 480;

        @RangeConstraint(min = 1, max = 5000)
        public int lodgedRemovalAnimationMs = 620;

        @RangeConstraint(min = 1, max = 5000)
        public int deepLodgedRemovalAnimationMs = 1000;
    }

    public static class ShieldArrows {
        public boolean enableShieldArrowLodging = true;
        public boolean recoverMobShieldArrows = true;

        @RangeConstraint(min = 0, max = 64)
        public int maxTrackedArrowsPerShield = 0;

        public boolean renderOwnShieldArrowsInFirstPerson = true;
    }

    public static class ArmorArrows {
        public boolean enableArmorArrowLodging = true;
        public boolean recoverMobArmorArrows = true;
        public boolean renderArmorArrows = true;
        public boolean enableArmorArrowDurabilityPenalty = true;

        @RangeConstraint(min = 0, max = 64)
        public int maxTrackedArrowsPerArmorPiece = 4;

        @RangeConstraint(min = 0.0D, max = 1.0D)
        public double armorArrowBasePenetrationChance = 0.72D;

        @RangeConstraint(min = 0.0D, max = 1.0D)
        public double armorArrowDefensePenaltyPerPoint = 0.055D;

        @RangeConstraint(min = 0.0D, max = 1.0D)
        public double armorArrowMinPenetrationChance = 0.05D;

        @RangeConstraint(min = 0.0D, max = 1.0D)
        public double armorArrowMaxPenetrationChance = 0.95D;

        @RangeConstraint(min = 0.0D, max = 1.0D)
        public double armorArrowRemovalSuccessChance = 0.92D;

        @RangeConstraint(min = 1, max = 1200)
        public int armorArrowRemovalTicks = 45;

        @RangeConstraint(min = 0.0D, max = 1.0D)
        public double armorArrowRemovalDurabilityDamageChance = 0.35D;

        @RangeConstraint(min = 0.0D, max = 1.0D)
        public double armorArrowRemovalBreakChance = 0.05D;

        @RangeConstraint(min = 0.0D, max = 1.0D)
        public double armorArrowBreakDurabilityDamageChance = 1.0D;

        @RangeConstraint(min = 0.0D, max = 1.0D)
        public double armorArrowExtraDurabilityLossChance = 0.10D;

        @RangeConstraint(min = 0.0D, max = 1.0D)
        public double armorArrowExtraDurabilityLossChancePerAdditionalArrow = 0.05D;

        @RangeConstraint(min = 0.0D, max = 1.0D)
        public double armorArrowExtraDurabilityLossMaxChance = 0.50D;

        @RangeConstraint(min = 0, max = 64)
        public int armorArrowExtraDurabilityLossAmount = 1;
    }

    public static class LegShotSlowness {
        public boolean enableLegShotSlowness = true;

        @RangeConstraint(min = 0, max = 1200)
        public int legShotSlownessDuration = 40;

        @RangeConstraint(min = 1, max = 10)
        public int legShotSlownessLevel = 1;
    }

    public static class BleedingRules {
        public boolean enableBleeding = true;
        public boolean arrowRemovalCausesBleeding = true;
        public boolean enchantsCauseBleeding = true;
        public boolean weaponsCauseBleeding = false;
        public boolean skeletonsAffectedByBleeding = false;
        public boolean undeadAffectedByBleeding = true;
        public boolean fullArmorPreventsBleeding = true;
        public boolean partialArmorPreventsBleeding = false;

        @RangeConstraint(min = 0.0D, max = 1.0D)
        public double fullArmorBleedingChance = 0.15D;

        @RangeConstraint(min = 0.0D, max = 1.0D)
        public double partialArmorBleedingChance = 0.65D;

        @RangeConstraint(min = 0.0D, max = 1.0D)
        public double noArmorBleedingChance = 1.0D;

        @RangeConstraint(min = 0.0D, max = 1.0D)
        public double failedArrowRemovalFullArmorBleedingChance = 0.25D;

        @RangeConstraint(min = 0.0D, max = 1.0D)
        public double failedArrowRemovalPartialArmorBleedingChance = 0.75D;

        @RangeConstraint(min = 0.0D, max = 1.0D)
        public double failedArrowRemovalNoArmorBleedingChance = 1.0D;
    }

    public static class BleedingEffect {
        @RangeConstraint(min = 0, max = 12000)
        public int bleedingDamageDuration = 120;

        @RangeConstraint(min = 0, max = 12000)
        public int bleedingArrowRemovalDuration = 200;

        @RangeConstraint(min = 20, max = 12000)
        public int bleedingMaxDuration = 1200;

        @RangeConstraint(min = 20, max = 12000)
        public int bleedingStrongDurationThreshold = 600;

        @RangeConstraint(min = 1, max = 1200)
        public int bleedingBaseTickInterval = 100;

        @RangeConstraint(min = 1, max = 1200)
        public int bleedingStrongTickInterval = 80;

        @RangeConstraint(min = 0.0D, max = 20.0D)
        public double bleedingBaseDamage = 1.0D;

        @RangeConstraint(min = 0.0D, max = 20.0D)
        public double bleedingStrongDamage = 2.0D;

        public boolean bleedingDripParticles = true;

        public boolean simpleBloodCompatParticles = true;

        @RangeConstraint(min = 0.0D, max = 1.0D)
        public double simpleBloodGroundDecalChance = 0.15D;

        @RangeConstraint(min = 0.0D, max = 8.0D)
        public double simpleBloodGroundDecalScaleMultiplier = 1.5D;

        @RangeConstraint(min = 0.0D, max = 8.0D)
        public double simpleBloodGroundDecalMinScale = 0.4D;

        @RangeConstraint(min = 0.0D, max = 8.0D)
        public double simpleBloodGroundDecalMaxScale = 0.8D;

        @RangeConstraint(min = 0.0D, max = 4.0D)
        public double simpleBloodGroundDecalRandomMinScale = 0.82D;

        @RangeConstraint(min = 0.0D, max = 4.0D)
        public double simpleBloodGroundDecalRandomMaxScale = 1.18D;

        @RangeConstraint(min = 1, max = 1200)
        public int bleedingBaseDripInterval = 8;

        @RangeConstraint(min = 1, max = 1200)
        public int bleedingStrongDripInterval = 5;
    }

}
