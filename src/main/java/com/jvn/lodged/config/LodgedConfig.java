package com.jvn.lodged.config;

import java.util.List;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.common.ModConfigSpec;

public final class LodgedConfig {
    public static final ModConfigSpec SPEC;

    private static final ModConfigSpec.BooleanValue ENABLE_ARROW_RECOVERY;
    private static final ModConfigSpec.DoubleValue RECOVERY_CHANCE;
    private static final ModConfigSpec.IntValue MAX_TRACKED_ARROWS_PER_ENTITY;
    private static final ModConfigSpec.BooleanValue RECOVER_PLAYER_ARROWS_ONLY;
    private static final ModConfigSpec.BooleanValue RECOVER_INFINITY_ARROWS;
    private static final ModConfigSpec.BooleanValue RECOVER_CREATIVE_ARROWS;
    private static final ModConfigSpec.BooleanValue PRESERVE_ARROW_ITEM_STACK;
    private static final ModConfigSpec.ConfigValue<List<? extends String>> ENTITY_DENYLIST;

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

        RECOVER_INFINITY_ARROWS = builder
                .comment("If true, survival Infinity-generated arrows can be recovered.")
                .translation("lodged.configuration.recoverInfinityArrows")
                .define("recoverInfinityArrows", false);

        RECOVER_CREATIVE_ARROWS = builder
                .comment("If true, arrows fired by creative-mode players can be recovered.")
                .translation("lodged.configuration.recoverCreativeArrows")
                .define("recoverCreativeArrows", false);

        PRESERVE_ARROW_ITEM_STACK = builder
                .comment("If true, store the original arrow ItemStack so spectral and tipped arrow data can be recovered.")
                .translation("lodged.configuration.preserveArrowItemStack")
                .define("preserveArrowItemStack", true);

        ENTITY_DENYLIST = builder
                .comment("Entity type ids that should never track or drop lodged arrows.")
                .translation("lodged.configuration.entityDenylist")
                .defineListAllowEmpty("entityDenylist",
                        List.of("minecraft:slime", "minecraft:magma_cube", "minecraft:armor_stand"),
                        () -> "minecraft:zombie",
                        LodgedConfig::isValidEntityId);

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

    public static boolean recoverInfinityArrows() {
        return RECOVER_INFINITY_ARROWS.get();
    }

    public static boolean recoverCreativeArrows() {
        return RECOVER_CREATIVE_ARROWS.get();
    }

    public static boolean preserveArrowItemStack() {
        return PRESERVE_ARROW_ITEM_STACK.get();
    }

    public static List<? extends String> entityDenylist() {
        return ENTITY_DENYLIST.get();
    }

    private static boolean isValidEntityId(Object value) {
        return value instanceof String id && ResourceLocation.tryParse(id) != null;
    }
}
