package com.jvn.lodged.world;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

final class LodgedItemArrowStorage {
    private static final String STACK_KEY = "stack";
    private static final String RECOVERABLE_KEY = "recoverable";
    private static final String FROM_PLAYER_KEY = "from_player";
    private static final String INFINITY_GENERATED_KEY = "infinity_generated";
    private static final String CREATIVE_GENERATED_KEY = "creative_generated";

    private LodgedItemArrowStorage() {
    }

    static List<LodgedArrowVisual> readVisuals(ItemStack item, String storageKey) {
        ListTag storedArrows = storedArrows(item, storageKey);
        if (storedArrows == null) {
            return List.of();
        }

        List<LodgedArrowVisual> arrows = new ArrayList<>(storedArrows.size());
        for (int index = 0; index < storedArrows.size(); index++) {
            arrows.add(LodgedArrowVisual.load(storedArrows.getCompound(index)));
        }
        return arrows;
    }

    static <T extends ItemArrowData> List<T> readData(
            ItemStack item,
            HolderLookup.Provider registries,
            String storageKey,
            ArrowDataFactory<T> factory) {
        ListTag storedArrows = storedArrows(item, storageKey);
        if (storedArrows == null) {
            return List.of();
        }

        List<T> arrows = new ArrayList<>(storedArrows.size());
        for (int index = 0; index < storedArrows.size(); index++) {
            CompoundTag arrowTag = storedArrows.getCompound(index);
            ItemStack stack = ItemStack.parse(registries, arrowTag.get(STACK_KEY))
                    .orElse(ItemStack.EMPTY)
                    .copyWithCount(1);
            if (!stack.isEmpty()) {
                arrows.add(factory.create(
                        stack,
                        !arrowTag.contains(RECOVERABLE_KEY) || arrowTag.getBoolean(RECOVERABLE_KEY),
                        arrowTag.getBoolean(FROM_PLAYER_KEY),
                        arrowTag.getBoolean(INFINITY_GENERATED_KEY),
                        arrowTag.getBoolean(CREATIVE_GENERATED_KEY),
                        LodgedArrowVisual.load(arrowTag)));
            }
        }
        return arrows;
    }

    static void writeAll(
            ItemStack item,
            String storageKey,
            List<? extends ItemArrowData> arrows,
            HolderLookup.Provider registries) {
        CustomData.update(DataComponents.CUSTOM_DATA, item, tag -> {
            if (arrows.isEmpty()) {
                tag.remove(storageKey);
                return;
            }

            ListTag storedArrows = new ListTag();
            for (ItemArrowData arrow : arrows) {
                CompoundTag arrowTag = new CompoundTag();
                arrowTag.put(STACK_KEY, arrow.stack().copyWithCount(1).save(registries));
                arrowTag.putBoolean(RECOVERABLE_KEY, arrow.recoverable());
                arrowTag.putBoolean(FROM_PLAYER_KEY, arrow.fromPlayer());
                arrowTag.putBoolean(INFINITY_GENERATED_KEY, arrow.infinityGenerated());
                arrowTag.putBoolean(CREATIVE_GENERATED_KEY, arrow.creativeGenerated());
                arrow.visual().save(arrowTag);
                storedArrows.add(arrowTag);
            }
            tag.put(storageKey, storedArrows);
        });
    }

    private static ListTag storedArrows(ItemStack item, String storageKey) {
        if (item.isEmpty()) {
            return null;
        }

        CompoundTag customData = item.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (!customData.contains(storageKey, Tag.TAG_LIST)) {
            return null;
        }
        return customData.getList(storageKey, Tag.TAG_COMPOUND);
    }

    interface ItemArrowData {
        ItemStack stack();

        boolean recoverable();

        boolean fromPlayer();

        boolean infinityGenerated();

        boolean creativeGenerated();

        LodgedArrowVisual visual();
    }

    @FunctionalInterface
    interface ArrowDataFactory<T extends ItemArrowData> {
        T create(
                ItemStack stack,
                boolean recoverable,
                boolean fromPlayer,
                boolean infinityGenerated,
                boolean creativeGenerated,
                LodgedArrowVisual visual);
    }
}
