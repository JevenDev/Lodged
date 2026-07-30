package com.jvn.lodged.world;

import com.jvn.lodged.Lodged;
import com.jvn.lodged.config.LodgedConfig;
import java.util.List;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.ItemStack;

public final class LodgedShieldArrowStorage {
    private static final String STORAGE_KEY = Lodged.MOD_ID + ":shield_arrows";

    private LodgedShieldArrowStorage() {
    }

    public static boolean add(ItemStack shield, LodgedShieldArrowData arrowData, HolderLookup.Provider registries) {
        if (!LodgedConfig.enableShieldArrowLodging()) {
            return false;
        }

        return LodgedItemArrowStorage.add(
                shield,
                STORAGE_KEY,
                arrowData,
                LodgedConfig.maxTrackedArrowsPerShield(),
                registries,
                LodgedShieldArrowData::new);
    }

    public static List<LodgedArrowVisual> readAll(ItemStack shield) {
        return LodgedItemArrowStorage.readVisuals(shield, STORAGE_KEY);
    }

    public static int count(ItemStack shield) {
        return LodgedItemArrowStorage.countVisuals(shield, STORAGE_KEY);
    }

    public static LodgedShieldArrowData removeAt(ItemStack shield, int arrowIndex, HolderLookup.Provider registries) {
        return LodgedItemArrowStorage.removeAt(
                shield, STORAGE_KEY, arrowIndex, registries, LodgedShieldArrowData::new);
    }

    public static List<LodgedShieldArrowData> readData(ItemStack shield, HolderLookup.Provider registries) {
        return LodgedItemArrowStorage.readData(shield, registries, STORAGE_KEY, LodgedShieldArrowData::new);
    }

    public record LodgedShieldArrowData(
            ItemStack stack,
            boolean recoverable,
            boolean fromPlayer,
            boolean infinityGenerated,
            boolean creativeGenerated,
            LodgedArrowVisual visual) implements LodgedItemArrowStorage.ItemArrowData {
        public LodgedShieldArrowData {
            stack = stack.copyWithCount(1);
            visual = visual.withStack(stack);
        }
    }
}
