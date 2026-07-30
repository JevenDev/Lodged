package com.jvn.lodged.world;

import com.jvn.lodged.Lodged;
import com.jvn.lodged.config.LodgedConfig;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.AnimalArmorItem;
import net.minecraft.world.item.ItemStack;

public final class LodgedArmorArrowStorage {
    private static final String STORAGE_KEY = Lodged.MOD_ID + ":armor_arrows";
    private static final List<EquipmentSlot> ARMOR_SLOTS = List.of(
            EquipmentSlot.HEAD,
            EquipmentSlot.CHEST,
            EquipmentSlot.LEGS,
            EquipmentSlot.FEET);

    private LodgedArmorArrowStorage() {
    }

    public static boolean add(ItemStack armor, LodgedArmorArrowData arrowData, HolderLookup.Provider registries) {
        if (!LodgedConfig.enableArmorArrowLodging()) {
            return false;
        }

        return LodgedItemArrowStorage.add(
                armor,
                STORAGE_KEY,
                arrowData,
                LodgedConfig.maxTrackedArrowsPerArmorPiece(),
                registries,
                LodgedArmorArrowData::new);
    }

    public static List<LodgedArrowVisual> readAll(ItemStack armor) {
        return LodgedItemArrowStorage.readVisuals(armor, STORAGE_KEY);
    }

    public static List<LodgedArrowVisual> readAllEquipped(LivingEntity entity) {
        List<LodgedArrowVisual> arrows = new ArrayList<>();
        int maxTrackedArrows = LodgedConfig.maxTrackedArrowsPerArmorPiece();
        if (maxTrackedArrows <= 0) {
            return List.of();
        }

        for (EquipmentSlot slot : ARMOR_SLOTS) {
            List<LodgedArrowVisual> slotArrows = readAll(entity.getItemBySlot(slot));
            arrows.addAll(slotArrows.subList(0, Math.min(slotArrows.size(), maxTrackedArrows)));
        }

        ItemStack bodyArmor = entity.getItemBySlot(EquipmentSlot.BODY);
        if (isHorseArmor(bodyArmor)) {
            List<LodgedArrowVisual> bodyArmorArrows = readAll(bodyArmor);
            arrows.addAll(bodyArmorArrows.subList(0, Math.min(bodyArmorArrows.size(), maxTrackedArrows)));
        }
        return arrows;
    }

    public static LodgedArmorArrowData removeAt(
            ItemStack armor,
            int arrowIndex,
            HolderLookup.Provider registries) {
        return LodgedItemArrowStorage.removeAt(
                armor, STORAGE_KEY, arrowIndex, registries, LodgedArmorArrowData::new);
    }

    public static List<LodgedArmorArrowData> readData(ItemStack armor, HolderLookup.Provider registries) {
        return LodgedItemArrowStorage.readData(armor, registries, STORAGE_KEY, LodgedArmorArrowData::new);
    }

    public static List<EquipmentSlot> armorSlots() {
        return ARMOR_SLOTS;
    }

    public static boolean isHorseArmor(ItemStack stack) {
        return stack.getItem() instanceof AnimalArmorItem animalArmor
                && animalArmor.getBodyType() == AnimalArmorItem.BodyType.EQUESTRIAN;
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
    }
}
