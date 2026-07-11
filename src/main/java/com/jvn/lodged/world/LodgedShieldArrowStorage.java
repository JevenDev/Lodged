package com.jvn.lodged.world;

import com.jvn.lodged.Lodged;
import com.jvn.lodged.config.LodgedConfig;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.ItemStack;

public final class LodgedShieldArrowStorage {
    private static final String STORAGE_KEY = Lodged.MOD_ID + ":shield_arrows";

    private LodgedShieldArrowStorage() {
    }

    public static boolean add(ItemStack shield, LodgedShieldArrowData arrowData, HolderLookup.Provider registries) {
        int maxTrackedArrows = LodgedConfig.maxTrackedArrowsPerShield();
        if (!LodgedConfig.enableShieldArrowLodging()
                || shield.isEmpty()
                || arrowData.stack().isEmpty()
                || maxTrackedArrows <= 0) {
            return false;
        }

        List<LodgedShieldArrowData> arrows = new ArrayList<>(readData(shield, registries));
        while (arrows.size() >= maxTrackedArrows) {
            arrows.remove(0);
        }

        arrows.add(arrowData);
        writeAll(shield, arrows, registries);
        return true;
    }

    public static List<LodgedArrowVisual> readAll(ItemStack shield) {
        return LodgedItemArrowStorage.readVisuals(shield, STORAGE_KEY);
    }

    public static LodgedShieldArrowData removeAt(ItemStack shield, int arrowIndex, HolderLookup.Provider registries) {
        List<LodgedShieldArrowData> arrows = new ArrayList<>(readData(shield, registries));
        if (arrowIndex < 0 || arrowIndex >= arrows.size()) {
            return null;
        }

        LodgedShieldArrowData removed = arrows.remove(arrowIndex);
        writeAll(shield, arrows, registries);
        return removed;
    }

    public static List<LodgedShieldArrowData> readData(ItemStack shield, HolderLookup.Provider registries) {
        return LodgedItemArrowStorage.readData(shield, registries, STORAGE_KEY, LodgedShieldArrowData::new);
    }

    private static void writeAll(ItemStack shield, List<LodgedShieldArrowData> arrows, HolderLookup.Provider registries) {
        LodgedItemArrowStorage.writeAll(shield, STORAGE_KEY, arrows, registries);
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
