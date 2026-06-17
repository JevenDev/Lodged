package com.jvn.lodged.config;

import com.jvn.lodged.world.LodgedArrowBodyPart;
import java.util.List;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.common.ModConfigSpec;

public final class LodgedConfig {
    public static final ModConfigSpec SPEC;

    private static final ModConfigSpec.BooleanValue ENABLE_ARROW_RECOVERY;
    private static final ModConfigSpec.DoubleValue RECOVERY_CHANCE;
    private static final ModConfigSpec.IntValue MAX_TRACKED_ARROWS_PER_ENTITY;
    private static final ModConfigSpec.BooleanValue RECOVER_PLAYER_ARROWS_ONLY;
    private static final ModConfigSpec.BooleanValue RECOVER_MOB_ARROWS;
    private static final ModConfigSpec.BooleanValue RECOVER_INFINITY_ARROWS;
    private static final ModConfigSpec.BooleanValue RECOVER_CREATIVE_ARROWS;
    private static final ModConfigSpec.BooleanValue ENABLE_ARROW_BREAK_ON_ENTITY_HIT;
    private static final ModConfigSpec.BooleanValue ENABLE_ARROW_BREAK_ON_BLOCK_HIT;
    private static final ModConfigSpec.BooleanValue ENABLE_MOB_ARROW_BREAK;
    private static final ModConfigSpec.DoubleValue REGULAR_ARROW_IMPACT_BREAK_CHANCE;
    private static final ModConfigSpec.DoubleValue MOB_ARROW_IMPACT_BREAK_CHANCE;
    private static final ModConfigSpec.DoubleValue INFINITY_ARROW_IMPACT_BREAK_CHANCE;
    private static final ModConfigSpec.BooleanValue PRESERVE_ARROW_ITEM_STACK;
    private static final ModConfigSpec.BooleanValue PREVENT_PLAYER_ARROW_DESPAWN;
    private static final ModConfigSpec.BooleanValue PREVENT_NON_PLAYER_ARROW_DESPAWN;
    private static final ModConfigSpec.BooleanValue ENABLE_PLAYER_ARROW_REMOVAL;
    private static final ModConfigSpec.DoubleValue PLAYER_ARROW_REMOVAL_HEAD_SUCCESS_CHANCE;
    private static final ModConfigSpec.DoubleValue PLAYER_ARROW_REMOVAL_CHEST_SUCCESS_CHANCE;
    private static final ModConfigSpec.DoubleValue PLAYER_ARROW_REMOVAL_ARM_SUCCESS_CHANCE;
    private static final ModConfigSpec.DoubleValue PLAYER_ARROW_REMOVAL_LEG_SUCCESS_CHANCE;
    private static final ModConfigSpec.DoubleValue PLAYER_ARROW_REMOVAL_INFINITY_SUCCESS_MULTIPLIER;
    private static final ModConfigSpec.DoubleValue PLAYER_ARROW_REMOVAL_BREAK_DAMAGE;
    private static final ModConfigSpec.BooleanValue ALLOW_ARROW_REMOVAL_IN_CREATIVE;
    private static final ModConfigSpec.BooleanValue REQUIRE_INVENTORY_SCREEN_FOR_REMOVAL;
    private static final ModConfigSpec.IntValue MAX_REMOVABLE_PLAYER_ARROWS;
    private static final ModConfigSpec.ConfigValue<List<? extends String>> ENTITY_DENYLIST;
    private static final ModConfigSpec.BooleanValue ENABLE_BLEEDING;
    private static final ModConfigSpec.BooleanValue ARROW_REMOVAL_CAUSES_BLEEDING;
    private static final ModConfigSpec.BooleanValue ENCHANTS_CAUSE_BLEEDING;
    private static final ModConfigSpec.BooleanValue WEAPONS_CAUSE_BLEEDING;
    private static final ModConfigSpec.BooleanValue SKELETONS_AFFECTED_BY_BLEEDING;
    private static final ModConfigSpec.BooleanValue UNDEAD_AFFECTED_BY_BLEEDING;
    private static final ModConfigSpec.BooleanValue FULL_ARMOR_PREVENTS_BLEEDING;
    private static final ModConfigSpec.BooleanValue PARTIAL_ARMOR_PREVENTS_BLEEDING;
    private static final ModConfigSpec.DoubleValue FULL_ARMOR_BLEEDING_CHANCE;
    private static final ModConfigSpec.DoubleValue PARTIAL_ARMOR_BLEEDING_CHANCE;
    private static final ModConfigSpec.DoubleValue NO_ARMOR_BLEEDING_CHANCE;
    private static final ModConfigSpec.DoubleValue FAILED_ARROW_REMOVAL_FULL_ARMOR_BLEEDING_CHANCE;
    private static final ModConfigSpec.DoubleValue FAILED_ARROW_REMOVAL_PARTIAL_ARMOR_BLEEDING_CHANCE;
    private static final ModConfigSpec.DoubleValue FAILED_ARROW_REMOVAL_NO_ARMOR_BLEEDING_CHANCE;
    private static final ModConfigSpec.IntValue BLEEDING_DAMAGE_DURATION;
    private static final ModConfigSpec.IntValue BLEEDING_ARROW_REMOVAL_DURATION;
    private static final ModConfigSpec.IntValue BLEEDING_MAX_DURATION;
    private static final ModConfigSpec.IntValue BLEEDING_STRONG_DURATION_THRESHOLD;
    private static final ModConfigSpec.IntValue BLEEDING_BASE_TICK_INTERVAL;
    private static final ModConfigSpec.IntValue BLEEDING_STRONG_TICK_INTERVAL;
    private static final ModConfigSpec.DoubleValue BLEEDING_BASE_DAMAGE;
    private static final ModConfigSpec.DoubleValue BLEEDING_STRONG_DAMAGE;
    private static final ModConfigSpec.BooleanValue BLEEDING_DRIP_PARTICLES;
    private static final ModConfigSpec.IntValue BLEEDING_BASE_DRIP_INTERVAL;
    private static final ModConfigSpec.IntValue BLEEDING_STRONG_DRIP_INTERVAL;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

        builder.push("lodged");

        ENABLE_ARROW_RECOVERY = builder
                .comment("If true, tracked lodged arrows can drop from living entities when they die.")
                .translation("lodged.configuration.enableArrowRecovery")
                .define("enableArrowRecovery", true);

        RECOVERY_CHANCE = builder
                .comment("Chance for each tracked lodged arrow to drop on death. Values are clamped from 0.0 to 1.0.")
                .translation("lodged.configuration.recoveryChance")
                .defineInRange("recoveryChance", 0.35D, 0.0D, 1.0D);

        MAX_TRACKED_ARROWS_PER_ENTITY = builder
                .comment("Maximum lodged arrows stored on a single entity. Set to 0 to disable tracking.")
                .translation("lodged.configuration.maxTrackedArrowsPerEntity")
                .defineInRange("maxTrackedArrowsPerEntity", 8, 0, 64);

        RECOVER_PLAYER_ARROWS_ONLY = builder
                .comment("If true, only arrows fired by players can be recovered.")
                .translation("lodged.configuration.recoverPlayerArrowsOnly")
                .define("recoverPlayerArrowsOnly", true);

        RECOVER_MOB_ARROWS = builder
                .comment("If true, arrows fired by mobs can be recovered. This also allows recovery when recoverPlayerArrowsOnly is true.")
                .translation("lodged.configuration.recoverMobArrows")
                .define("recoverMobArrows", false);

        RECOVER_INFINITY_ARROWS = builder
                .comment("If true, survival Infinity-generated arrows can be recovered.")
                .translation("lodged.configuration.recoverInfinityArrows")
                .define("recoverInfinityArrows", false);

        RECOVER_CREATIVE_ARROWS = builder
                .comment("If true, arrows fired by creative-mode players can be recovered.")
                .translation("lodged.configuration.recoverCreativeArrows")
                .define("recoverCreativeArrows", false);

        ENABLE_ARROW_BREAK_ON_ENTITY_HIT = builder
                .comment("If true, arrows can break when they hit living entities. The source-specific break chance still controls whether each arrow breaks.")
                .translation("lodged.configuration.enableArrowBreakOnEntityHit")
                .define("enableArrowBreakOnEntityHit", true);

        ENABLE_ARROW_BREAK_ON_BLOCK_HIT = builder
                .comment("If true, arrows can break when they hit blocks. The source-specific break chance still controls whether each arrow breaks.")
                .translation("lodged.configuration.enableArrowBreakOnBlockHit")
                .define("enableArrowBreakOnBlockHit", true);

        ENABLE_MOB_ARROW_BREAK = builder
                .comment("If true, arrows fired by non-player entities can break on impact.")
                .translation("lodged.configuration.enableMobArrowBreak")
                .define("enableMobArrowBreak", true);

        REGULAR_ARROW_IMPACT_BREAK_CHANCE = builder
                .comment("Chance for each regular arrow to break immediately on impact. Values are clamped from 0.0 to 1.0.")
                .translation("lodged.configuration.regularArrowImpactBreakChance")
                .defineInRange("regularArrowImpactBreakChance", 0.02D, 0.0D, 1.0D);

        MOB_ARROW_IMPACT_BREAK_CHANCE = builder
                .comment("Chance for each mob-fired arrow to break immediately on impact. Values are clamped from 0.0 to 1.0.")
                .translation("lodged.configuration.mobArrowImpactBreakChance")
                .defineInRange("mobArrowImpactBreakChance", 0.0D, 0.0D, 1.0D);

        INFINITY_ARROW_IMPACT_BREAK_CHANCE = builder
                .comment("Chance for each survival Infinity-generated arrow to break immediately on impact. Values are clamped from 0.0 to 1.0.")
                .translation("lodged.configuration.infinityArrowImpactBreakChance")
                .defineInRange("infinityArrowImpactBreakChance", 0.0D, 0.0D, 1.0D);

        PRESERVE_ARROW_ITEM_STACK = builder
                .comment("If true, store the original arrow ItemStack so spectral and tipped arrow data can be recovered.")
                .translation("lodged.configuration.preserveArrowItemStack")
                .define("preserveArrowItemStack", true);

        PREVENT_PLAYER_ARROW_DESPAWN = builder
                .comment("If true, arrows stuck in players will not naturally despawn from the vanilla stuck-arrow renderer.")
                .translation("lodged.configuration.preventPlayerArrowDespawn")
                .define("preventPlayerArrowDespawn", true);

        PREVENT_NON_PLAYER_ARROW_DESPAWN = builder
                .comment("If true, arrows stuck in non-player living entities will not naturally despawn from the vanilla stuck-arrow renderer.")
                .translation("lodged.configuration.preventNonPlayerArrowDespawn")
                .define("preventNonPlayerArrowDespawn", false);

        ENABLE_PLAYER_ARROW_REMOVAL = builder
                .comment("If true, players can remove arrows lodged in their own body from the inventory player preview.")
                .translation("lodged.configuration.enablePlayerArrowRemoval")
                .define("enablePlayerArrowRemoval", true);

        PLAYER_ARROW_REMOVAL_HEAD_SUCCESS_CHANCE = builder
                .comment("Chance that removing a player arrow from the head succeeds and recovers the arrow item. Values are clamped from 0.0 to 1.0.")
                .translation("lodged.configuration.playerArrowRemovalHeadSuccessChance")
                .defineInRange("playerArrowRemovalHeadSuccessChance", 0.35D, 0.0D, 1.0D);

        PLAYER_ARROW_REMOVAL_CHEST_SUCCESS_CHANCE = builder
                .comment("Chance that removing a player arrow from the chest succeeds and recovers the arrow item. Values are clamped from 0.0 to 1.0.")
                .translation("lodged.configuration.playerArrowRemovalChestSuccessChance")
                .defineInRange("playerArrowRemovalChestSuccessChance", 0.65D, 0.0D, 1.0D);

        PLAYER_ARROW_REMOVAL_ARM_SUCCESS_CHANCE = builder
                .comment("Chance that removing a player arrow from an arm succeeds and recovers the arrow item. Values are clamped from 0.0 to 1.0.")
                .translation("lodged.configuration.playerArrowRemovalArmSuccessChance")
                .defineInRange("playerArrowRemovalArmSuccessChance", 0.85D, 0.0D, 1.0D);

        PLAYER_ARROW_REMOVAL_LEG_SUCCESS_CHANCE = builder
                .comment("Chance that removing a player arrow from a leg succeeds and recovers the arrow item. Values are clamped from 0.0 to 1.0.")
                .translation("lodged.configuration.playerArrowRemovalLegSuccessChance")
                .defineInRange("playerArrowRemovalLegSuccessChance", 0.85D, 0.0D, 1.0D);

        PLAYER_ARROW_REMOVAL_INFINITY_SUCCESS_MULTIPLIER = builder
                .comment("Multiplier applied to player arrow removal success chance for Infinity-generated arrows. This only applies when recoverInfinityArrows is enabled.")
                .translation("lodged.configuration.playerArrowRemovalInfinitySuccessMultiplier")
                .defineInRange("playerArrowRemovalInfinitySuccessMultiplier", 0.5D, 0.0D, 1.0D);

        PLAYER_ARROW_REMOVAL_BREAK_DAMAGE = builder
                .comment("Damage dealt when player arrow removal fails and breaks the arrow. Damage is in half-hearts, so 2.0 equals one heart.")
                .translation("lodged.configuration.playerArrowRemovalBreakDamage")
                .defineInRange("playerArrowRemovalBreakDamage", 2.0D, 0.0D, 20.0D);

        ALLOW_ARROW_REMOVAL_IN_CREATIVE = builder
                .comment("If true, creative-mode players may remove arrows lodged in their own body.")
                .translation("lodged.configuration.allowArrowRemovalInCreative")
                .define("allowArrowRemovalInCreative", true);

        REQUIRE_INVENTORY_SCREEN_FOR_REMOVAL = builder
                .comment("If true, removal requests are only accepted while the player is in their own inventory menu.")
                .translation("lodged.configuration.requireInventoryScreenForRemoval")
                .define("requireInventoryScreenForRemoval", true);

        MAX_REMOVABLE_PLAYER_ARROWS = builder
                .comment("Maximum player lodged arrows exposed to the inventory removal UI. Set to 0 to follow maxTrackedArrowsPerEntity.")
                .translation("lodged.configuration.maxRemovablePlayerArrows")
                .defineInRange("maxRemovablePlayerArrows", 0, 0, 64);

        ENTITY_DENYLIST = builder
                .comment("Entity type ids that should never track or drop lodged arrows.")
                .translation("lodged.configuration.entityDenylist")
                .defineListAllowEmpty("entityDenylist",
                        List.of("minecraft:slime", "minecraft:magma_cube", "minecraft:armor_stand"),
                        () -> "minecraft:zombie",
                        LodgedConfig::isValidEntityId);

        ENABLE_BLEEDING = builder
                .comment("If true, Lodged can apply its bleeding mob effect.")
                .translation("lodged.configuration.enableBleeding")
                .define("enableBleeding", true);

        ARROW_REMOVAL_CAUSES_BLEEDING = builder
                .comment("If true, pulling lodged arrows out of your body can apply bleeding.")
                .translation("lodged.configuration.arrowRemovalCausesBleeding")
                .define("arrowRemovalCausesBleeding", true);

        ENCHANTS_CAUSE_BLEEDING = builder
                .comment("If true, weapons with enchantments in Lodged's bleeding enchantment tags can apply bleeding.")
                .translation("lodged.configuration.enchantsCauseBleeding")
                .define("enchantsCauseBleeding", true);

        WEAPONS_CAUSE_BLEEDING = builder
                .comment("If true, unenchanted weapons in the lodged:bleeding_weapons item tag can apply bleeding.")
                .translation("lodged.configuration.weaponsCauseBleeding")
                .define("weaponsCauseBleeding", false);

        SKELETONS_AFFECTED_BY_BLEEDING = builder
                .comment("If true, entity types in lodged:bleeding_skeletons can bleed.")
                .translation("lodged.configuration.skeletonsAffectedByBleeding")
                .define("skeletonsAffectedByBleeding", false);

        UNDEAD_AFFECTED_BY_BLEEDING = builder
                .comment("If true, undead entity types in lodged:bleeding_undead can bleed. Zombies are included by default.")
                .translation("lodged.configuration.undeadAffectedByBleeding")
                .define("undeadAffectedByBleeding", true);

        FULL_ARMOR_PREVENTS_BLEEDING = builder
                .comment("If true, wearing armor in all four normal armor slots prevents new bleeding.")
                .translation("lodged.configuration.fullArmorPreventsBleeding")
                .define("fullArmorPreventsBleeding", true);

        PARTIAL_ARMOR_PREVENTS_BLEEDING = builder
                .comment("If true, wearing any normal armor piece prevents new bleeding.")
                .translation("lodged.configuration.partialArmorPreventsBleeding")
                .define("partialArmorPreventsBleeding", false);

        FULL_ARMOR_BLEEDING_CHANCE = builder
                .comment("Chance for a bleeding trigger to apply when the target has full armor and fullArmorPreventsBleeding is false.")
                .translation("lodged.configuration.fullArmorBleedingChance")
                .defineInRange("fullArmorBleedingChance", 0.15D, 0.0D, 1.0D);

        PARTIAL_ARMOR_BLEEDING_CHANCE = builder
                .comment("Chance for a bleeding trigger to apply when the target has one to three normal armor pieces and partialArmorPreventsBleeding is false.")
                .translation("lodged.configuration.partialArmorBleedingChance")
                .defineInRange("partialArmorBleedingChance", 0.65D, 0.0D, 1.0D);

        NO_ARMOR_BLEEDING_CHANCE = builder
                .comment("Chance for a bleeding trigger to apply when the target has no normal armor pieces.")
                .translation("lodged.configuration.noArmorBleedingChance")
                .defineInRange("noArmorBleedingChance", 1.0D, 0.0D, 1.0D);

        FAILED_ARROW_REMOVAL_FULL_ARMOR_BLEEDING_CHANCE = builder
                .comment("Chance for a failed arrow removal to apply bleeding when the player has full armor. This uses its own armor weighting and is not blocked by fullArmorPreventsBleeding.")
                .translation("lodged.configuration.failedArrowRemovalFullArmorBleedingChance")
                .defineInRange("failedArrowRemovalFullArmorBleedingChance", 0.25D, 0.0D, 1.0D);

        FAILED_ARROW_REMOVAL_PARTIAL_ARMOR_BLEEDING_CHANCE = builder
                .comment("Chance for a failed arrow removal to apply bleeding when the player has one to three normal armor pieces. This uses its own armor weighting and is not blocked by partialArmorPreventsBleeding.")
                .translation("lodged.configuration.failedArrowRemovalPartialArmorBleedingChance")
                .defineInRange("failedArrowRemovalPartialArmorBleedingChance", 0.75D, 0.0D, 1.0D);

        FAILED_ARROW_REMOVAL_NO_ARMOR_BLEEDING_CHANCE = builder
                .comment("Chance for a failed arrow removal to apply bleeding when the player has no normal armor pieces.")
                .translation("lodged.configuration.failedArrowRemovalNoArmorBleedingChance")
                .defineInRange("failedArrowRemovalNoArmorBleedingChance", 1.0D, 0.0D, 1.0D);

        BLEEDING_DAMAGE_DURATION = builder
                .comment("Bleeding duration added by qualifying weapon damage, in ticks.")
                .translation("lodged.configuration.bleedingDamageDuration")
                .defineInRange("bleedingDamageDuration", 120, 0, 20 * 60 * 10);

        BLEEDING_ARROW_REMOVAL_DURATION = builder
                .comment("Bleeding duration added by pulling out a lodged arrow, in ticks.")
                .translation("lodged.configuration.bleedingArrowRemovalDuration")
                .defineInRange("bleedingArrowRemovalDuration", 200, 0, 20 * 60 * 10);

        BLEEDING_MAX_DURATION = builder
                .comment("Maximum total bleeding duration after stacking repeated triggers, in ticks.")
                .translation("lodged.configuration.bleedingMaxDuration")
                .defineInRange("bleedingMaxDuration", 1200, 20, 20 * 60 * 10);

        BLEEDING_STRONG_DURATION_THRESHOLD = builder
                .comment("Bleeding becomes stronger at or above this remaining duration, in ticks.")
                .translation("lodged.configuration.bleedingStrongDurationThreshold")
                .defineInRange("bleedingStrongDurationThreshold", 600, 20, 20 * 60 * 10);

        BLEEDING_BASE_TICK_INTERVAL = builder
                .comment("Ticks between damage pulses for normal bleeding.")
                .translation("lodged.configuration.bleedingBaseTickInterval")
                .defineInRange("bleedingBaseTickInterval", 100, 1, 20 * 60);

        BLEEDING_STRONG_TICK_INTERVAL = builder
                .comment("Ticks between damage pulses for strong bleeding.")
                .translation("lodged.configuration.bleedingStrongTickInterval")
                .defineInRange("bleedingStrongTickInterval", 80, 1, 20 * 60);

        BLEEDING_BASE_DAMAGE = builder
                .comment("Damage dealt by each normal bleeding pulse. Damage is in half-hearts, so 1.0 equals half a heart.")
                .translation("lodged.configuration.bleedingBaseDamage")
                .defineInRange("bleedingBaseDamage", 1.0D, 0.0D, 20.0D);

        BLEEDING_STRONG_DAMAGE = builder
                .comment("Damage dealt by each strong bleeding pulse. Damage is in half-hearts, so 2.0 equals one heart.")
                .translation("lodged.configuration.bleedingStrongDamage")
                .defineInRange("bleedingStrongDamage", 2.0D, 0.0D, 20.0D);

        BLEEDING_DRIP_PARTICLES = builder
                .comment("If true, bleeding entities drip blood particles from their wound between damage pulses.")
                .translation("lodged.configuration.bleedingDripParticles")
                .define("bleedingDripParticles", true);

        BLEEDING_BASE_DRIP_INTERVAL = builder
                .comment("Ticks between blood drip particles for normal bleeding.")
                .translation("lodged.configuration.bleedingBaseDripInterval")
                .defineInRange("bleedingBaseDripInterval", 8, 1, 20 * 60);

        BLEEDING_STRONG_DRIP_INTERVAL = builder
                .comment("Ticks between blood drip particles for strong bleeding.")
                .translation("lodged.configuration.bleedingStrongDripInterval")
                .defineInRange("bleedingStrongDripInterval", 5, 1, 20 * 60);

        builder.pop();

        SPEC = builder.build();
    }

    private LodgedConfig() {
    }

    public static boolean enableArrowRecovery() {
        return ENABLE_ARROW_RECOVERY.get();
    }

    public static double recoveryChance() {
        return RECOVERY_CHANCE.get();
    }

    public static int maxTrackedArrowsPerEntity() {
        return MAX_TRACKED_ARROWS_PER_ENTITY.get();
    }

    public static boolean recoverPlayerArrowsOnly() {
        return RECOVER_PLAYER_ARROWS_ONLY.get();
    }

    public static boolean recoverMobArrows() {
        return RECOVER_MOB_ARROWS.get() || !recoverPlayerArrowsOnly();
    }

    public static boolean recoverInfinityArrows() {
        return RECOVER_INFINITY_ARROWS.get();
    }

    public static boolean recoverCreativeArrows() {
        return RECOVER_CREATIVE_ARROWS.get();
    }

    public static boolean enableArrowBreakOnEntityHit() {
        return ENABLE_ARROW_BREAK_ON_ENTITY_HIT.get();
    }

    public static boolean enableArrowBreakOnBlockHit() {
        return ENABLE_ARROW_BREAK_ON_BLOCK_HIT.get();
    }

    public static boolean enableMobArrowBreak() {
        return ENABLE_MOB_ARROW_BREAK.get();
    }

    public static double regularArrowImpactBreakChance() {
        return REGULAR_ARROW_IMPACT_BREAK_CHANCE.get();
    }

    public static double mobArrowImpactBreakChance() {
        return MOB_ARROW_IMPACT_BREAK_CHANCE.get();
    }

    public static double infinityArrowImpactBreakChance() {
        return INFINITY_ARROW_IMPACT_BREAK_CHANCE.get();
    }

    public static boolean preserveArrowItemStack() {
        return PRESERVE_ARROW_ITEM_STACK.get();
    }

    public static boolean preventPlayerArrowDespawn() {
        return PREVENT_PLAYER_ARROW_DESPAWN.get();
    }

    public static boolean preventNonPlayerArrowDespawn() {
        return PREVENT_NON_PLAYER_ARROW_DESPAWN.get();
    }

    public static boolean enablePlayerArrowRemoval() {
        return ENABLE_PLAYER_ARROW_REMOVAL.get();
    }

    public static double playerArrowRemovalSuccessChance(LodgedArrowBodyPart bodyPart) {
        return switch (bodyPart) {
            case HEAD -> PLAYER_ARROW_REMOVAL_HEAD_SUCCESS_CHANCE.get();
            case CHEST -> PLAYER_ARROW_REMOVAL_CHEST_SUCCESS_CHANCE.get();
            case ARM -> PLAYER_ARROW_REMOVAL_ARM_SUCCESS_CHANCE.get();
            case LEG -> PLAYER_ARROW_REMOVAL_LEG_SUCCESS_CHANCE.get();
        };
    }

    public static double playerArrowRemovalInfinitySuccessMultiplier() {
        return PLAYER_ARROW_REMOVAL_INFINITY_SUCCESS_MULTIPLIER.get();
    }

    public static float playerArrowRemovalBreakDamage() {
        return PLAYER_ARROW_REMOVAL_BREAK_DAMAGE.get().floatValue();
    }

    public static boolean allowArrowRemovalInCreative() {
        return ALLOW_ARROW_REMOVAL_IN_CREATIVE.get();
    }

    public static boolean requireInventoryScreenForRemoval() {
        return REQUIRE_INVENTORY_SCREEN_FOR_REMOVAL.get();
    }

    public static int maxRemovablePlayerArrows() {
        int configuredMax = MAX_REMOVABLE_PLAYER_ARROWS.get();
        return configuredMax > 0 ? configuredMax : maxTrackedArrowsPerEntity();
    }

    public static List<? extends String> entityDenylist() {
        return ENTITY_DENYLIST.get();
    }

    public static boolean enableBleeding() {
        return ENABLE_BLEEDING.get();
    }

    public static boolean arrowRemovalCausesBleeding() {
        return enableBleeding() && ARROW_REMOVAL_CAUSES_BLEEDING.get();
    }

    public static boolean enchantsCauseBleeding() {
        return enableBleeding() && ENCHANTS_CAUSE_BLEEDING.get();
    }

    public static boolean weaponsCauseBleeding() {
        return enableBleeding() && WEAPONS_CAUSE_BLEEDING.get();
    }

    public static boolean skeletonsAffectedByBleeding() {
        return SKELETONS_AFFECTED_BY_BLEEDING.get();
    }

    public static boolean undeadAffectedByBleeding() {
        return UNDEAD_AFFECTED_BY_BLEEDING.get();
    }

    public static boolean fullArmorPreventsBleeding() {
        return FULL_ARMOR_PREVENTS_BLEEDING.get();
    }

    public static boolean partialArmorPreventsBleeding() {
        return PARTIAL_ARMOR_PREVENTS_BLEEDING.get();
    }

    public static double fullArmorBleedingChance() {
        return FULL_ARMOR_BLEEDING_CHANCE.get();
    }

    public static double partialArmorBleedingChance() {
        return PARTIAL_ARMOR_BLEEDING_CHANCE.get();
    }

    public static double noArmorBleedingChance() {
        return NO_ARMOR_BLEEDING_CHANCE.get();
    }

    public static double failedArrowRemovalFullArmorBleedingChance() {
        return FAILED_ARROW_REMOVAL_FULL_ARMOR_BLEEDING_CHANCE.get();
    }

    public static double failedArrowRemovalPartialArmorBleedingChance() {
        return FAILED_ARROW_REMOVAL_PARTIAL_ARMOR_BLEEDING_CHANCE.get();
    }

    public static double failedArrowRemovalNoArmorBleedingChance() {
        return FAILED_ARROW_REMOVAL_NO_ARMOR_BLEEDING_CHANCE.get();
    }

    public static int bleedingDamageDuration() {
        return BLEEDING_DAMAGE_DURATION.get();
    }

    public static int bleedingArrowRemovalDuration() {
        return BLEEDING_ARROW_REMOVAL_DURATION.get();
    }

    public static int bleedingMaxDuration() {
        return BLEEDING_MAX_DURATION.get();
    }

    public static int bleedingStrongDurationThreshold() {
        return Math.min(BLEEDING_STRONG_DURATION_THRESHOLD.get(), bleedingMaxDuration());
    }

    public static int bleedingTickInterval(int amplifier) {
        return amplifier > 0 ? BLEEDING_STRONG_TICK_INTERVAL.get() : BLEEDING_BASE_TICK_INTERVAL.get();
    }

    public static float bleedingDamage(int amplifier) {
        return (amplifier > 0 ? BLEEDING_STRONG_DAMAGE.get() : BLEEDING_BASE_DAMAGE.get()).floatValue();
    }

    public static boolean bleedingDripParticles() {
        return BLEEDING_DRIP_PARTICLES.get();
    }

    public static int bleedingDripInterval(int amplifier) {
        return amplifier > 0 ? BLEEDING_STRONG_DRIP_INTERVAL.get() : BLEEDING_BASE_DRIP_INTERVAL.get();
    }

    private static boolean isValidEntityId(Object value) {
        return value instanceof String id && ResourceLocation.tryParse(id) != null;
    }
}
