package com.jvn.lodged.world;

import com.jvn.lodged.Lodged;
import com.jvn.lodged.config.LodgedConfig;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

public final class LodgedShieldArrowStorage {
    private static final String STORAGE_KEY = Lodged.MOD_ID + ":shield_arrows";
    private static final String STACK_KEY = "stack";
    private static final String RECOVERABLE_KEY = "recoverable";
    private static final String FROM_PLAYER_KEY = "from_player";
    private static final String INFINITY_GENERATED_KEY = "infinity_generated";
    private static final String CREATIVE_GENERATED_KEY = "creative_generated";

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

        arrows.add(arrowData.withSingleStack());
        writeAll(shield, arrows, registries);
        return true;
    }

    public static List<LodgedArrowVisual> readAll(ItemStack shield) {
        if (shield.isEmpty()) {
            return List.of();
        }

        CompoundTag customData = shield.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (!customData.contains(STORAGE_KEY, Tag.TAG_LIST)) {
            return List.of();
        }

        ListTag storedArrows = customData.getList(STORAGE_KEY, Tag.TAG_COMPOUND);
        List<LodgedArrowVisual> arrows = new ArrayList<>(storedArrows.size());
        for (int index = 0; index < storedArrows.size(); index++) {
            arrows.add(LodgedArrowVisual.load(storedArrows.getCompound(index)));
        }
        return arrows;
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
        if (shield.isEmpty()) {
            return List.of();
        }

        CompoundTag customData = shield.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (!customData.contains(STORAGE_KEY, Tag.TAG_LIST)) {
            return List.of();
        }

        ListTag storedArrows = customData.getList(STORAGE_KEY, Tag.TAG_COMPOUND);
        List<LodgedShieldArrowData> arrows = new ArrayList<>(storedArrows.size());
        for (int index = 0; index < storedArrows.size(); index++) {
            CompoundTag arrowTag = storedArrows.getCompound(index);
            ItemStack stack = ItemStack.parse(registries, arrowTag.get(STACK_KEY))
                    .orElse(ItemStack.EMPTY)
                    .copyWithCount(1);
            if (!stack.isEmpty()) {
                arrows.add(new LodgedShieldArrowData(
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

    private static void writeAll(ItemStack shield, List<LodgedShieldArrowData> arrows, HolderLookup.Provider registries) {
        CustomData.update(DataComponents.CUSTOM_DATA, shield, tag -> {
            if (arrows.isEmpty()) {
                tag.remove(STORAGE_KEY);
                return;
            }

            ListTag storedArrows = new ListTag();
            for (LodgedShieldArrowData arrow : arrows) {
                CompoundTag arrowTag = new CompoundTag();
                arrowTag.put(STACK_KEY, arrow.stack().copyWithCount(1).save(registries));
                arrowTag.putBoolean(RECOVERABLE_KEY, arrow.recoverable());
                arrowTag.putBoolean(FROM_PLAYER_KEY, arrow.fromPlayer());
                arrowTag.putBoolean(INFINITY_GENERATED_KEY, arrow.infinityGenerated());
                arrowTag.putBoolean(CREATIVE_GENERATED_KEY, arrow.creativeGenerated());
                arrow.visual().save(arrowTag);
                storedArrows.add(arrowTag);
            }
            tag.put(STORAGE_KEY, storedArrows);
        });
    }

    public record LodgedShieldArrowData(
            ItemStack stack,
            boolean recoverable,
            boolean fromPlayer,
            boolean infinityGenerated,
            boolean creativeGenerated,
            LodgedArrowVisual visual) {
        public LodgedShieldArrowData {
            stack = stack.copyWithCount(1);
            visual = visual.withStack(stack);
        }

        LodgedShieldArrowData withSingleStack() {
            return new LodgedShieldArrowData(
                    stack.copyWithCount(1),
                    recoverable,
                    fromPlayer,
                    infinityGenerated,
                    creativeGenerated,
                    visual);
        }
    }
}
