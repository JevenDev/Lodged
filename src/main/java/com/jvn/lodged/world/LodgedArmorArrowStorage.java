package com.jvn.lodged.world;

import com.jvn.lodged.Lodged;
import com.jvn.lodged.config.LodgedConfig;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

public final class LodgedArmorArrowStorage {
    private static final String STORAGE_KEY = Lodged.MOD_ID + ":armor_arrows";

    private LodgedArmorArrowStorage() {
    }

    public static boolean add(ItemStack armor, LodgedArmorArrowData arrowData, HolderLookup.Provider registries) {
        int maxTrackedArrows = LodgedConfig.maxTrackedArrowsPerArmorPiece();
        if (!LodgedConfig.enableArmorArrowLodging()
                || armor.isEmpty()
                || arrowData.stack().isEmpty()
                || maxTrackedArrows <= 0) {
            return false;
        }

        List<LodgedArmorArrowData> arrows = new ArrayList<>(readData(armor, registries));
        while (arrows.size() >= maxTrackedArrows) {
            arrows.remove(0);
        }

        arrows.add(arrowData.withSingleStack());
        writeAll(armor, arrows, registries);
        return true;
    }

    public static List<LodgedArrowVisual> readAll(ItemStack armor) {
        return LodgedItemArrowStorage.readVisuals(armor, STORAGE_KEY);
    }

    public static List<LodgedArrowVisual> readAllEquipped(LivingEntity entity) {
        List<LodgedArrowVisual> arrows = new ArrayList<>();
        for (EquipmentSlot slot : armorSlots()) {
            arrows.addAll(readAll(entity.getItemBySlot(slot)));
        }
        return arrows;
    }

    public static LodgedArmorArrowData removeAt(
            ItemStack armor,
            int arrowIndex,
            HolderLookup.Provider registries) {
        List<LodgedArmorArrowData> arrows = new ArrayList<>(readData(armor, registries));
        if (arrowIndex < 0 || arrowIndex >= arrows.size()) {
            return null;
        }

        LodgedArmorArrowData removed = arrows.remove(arrowIndex);
        writeAll(armor, arrows, registries);
        return removed;
    }

    public static List<LodgedArmorArrowData> readData(ItemStack armor, HolderLookup.Provider registries) {
        return LodgedItemArrowStorage.readData(armor, registries, STORAGE_KEY, LodgedArmorArrowData::new);
    }

    public static EquipmentSlot[] armorSlots() {
        return new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
    }

    private static void writeAll(ItemStack armor, List<LodgedArmorArrowData> arrows, HolderLookup.Provider registries) {
        LodgedItemArrowStorage.writeAll(armor, STORAGE_KEY, arrows, registries);
    }

    public record LodgedArmorArrowData(
            ItemStack stack,
            boolean recoverable,
            boolean fromPlayer,
            boolean infinityGenerated,
            boolean creativeGenerated,
            LodgedArrowVisual visual) implements LodgedItemArrowStorage.ItemArrowData {
        public LodgedArmorArrowData {
            stack = stack.copyWithCount(1);
            visual = visual.withStack(stack);
        }

        LodgedArmorArrowData withSingleStack() {
            return new LodgedArmorArrowData(
                    stack.copyWithCount(1),
                    recoverable,
                    fromPlayer,
                    infinityGenerated,
                    creativeGenerated,
                    visual);
        }
    }
}
