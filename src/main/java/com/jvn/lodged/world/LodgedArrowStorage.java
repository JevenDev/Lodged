package com.jvn.lodged.world;

import com.jvn.lodged.Lodged;
import com.jvn.lodged.config.LodgedConfig;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;

public final class LodgedArrowStorage {
    private static final String STORAGE_KEY = Lodged.MOD_ID + ":lodged_arrows";

    private LodgedArrowStorage() {
    }

    public static void add(LivingEntity entity, LodgedArrowData arrowData) {
        ListTag arrows = getOrCreateArrowList(entity);
        int maxTrackedArrows = LodgedConfig.maxTrackedArrowsPerEntity();

        while (arrows.size() >= maxTrackedArrows) {
            arrows.remove(0);
        }

        arrows.add(arrowData.save(entity));
        entity.getPersistentData().put(STORAGE_KEY, arrows);
    }

    public static List<LodgedArrowData> removeAll(LivingEntity entity) {
        List<LodgedArrowData> arrows = readAll(entity);
        entity.getPersistentData().remove(STORAGE_KEY);
        return arrows;
    }

    public static int count(LivingEntity entity) {
        return readAll(entity).size();
    }

    public static List<LodgedArrowData> readAll(LivingEntity entity) {
        CompoundTag persistentData = entity.getPersistentData();
        if (!persistentData.contains(STORAGE_KEY, Tag.TAG_LIST)) {
            return List.of();
        }

        ListTag storedArrows = persistentData.getList(STORAGE_KEY, Tag.TAG_COMPOUND);
        List<LodgedArrowData> arrows = new ArrayList<>(storedArrows.size());
        for (int index = 0; index < storedArrows.size(); index++) {
            LodgedArrowData arrowData = LodgedArrowData.load(entity, storedArrows.getCompound(index));
            if (!arrowData.stack().isEmpty()) {
                arrows.add(arrowData);
            }
        }
        return arrows;
    }

    @Nullable
    public static LodgedArrowData removeAt(LivingEntity entity, int arrowIndex) {
        List<LodgedArrowData> arrows = new ArrayList<>(readAll(entity));
        if (arrowIndex < 0 || arrowIndex >= arrows.size()) {
            return null;
        }

        LodgedArrowData removedArrow = arrows.remove(arrowIndex);
        writeAll(entity, arrows);
        return removedArrow;
    }

    private static void writeAll(LivingEntity entity, List<LodgedArrowData> arrows) {
        if (arrows.isEmpty()) {
            entity.getPersistentData().remove(STORAGE_KEY);
            return;
        }

        ListTag storedArrows = new ListTag();
        for (LodgedArrowData arrowData : arrows) {
            if (!arrowData.stack().isEmpty()) {
                storedArrows.add(arrowData.save(entity));
            }
        }

        if (storedArrows.isEmpty()) {
            entity.getPersistentData().remove(STORAGE_KEY);
        } else {
            entity.getPersistentData().put(STORAGE_KEY, storedArrows);
        }
    }

    private static ListTag getOrCreateArrowList(LivingEntity entity) {
        CompoundTag persistentData = entity.getPersistentData();
        if (persistentData.contains(STORAGE_KEY, Tag.TAG_LIST)) {
            return persistentData.getList(STORAGE_KEY, Tag.TAG_COMPOUND);
        }
        return new ListTag();
    }
}
