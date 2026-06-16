package com.jvn.lodged.world;

import com.jvn.lodged.Lodged;
import com.jvn.lodged.config.LodgedConfig;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.LivingEntity;

final class LodgedArrowStorage {
    private static final String STORAGE_KEY = Lodged.MOD_ID + ":lodged_arrows";

    private LodgedArrowStorage() {
    }

    static void add(LivingEntity entity, LodgedArrowData arrowData) {
        ListTag arrows = getOrCreateArrowList(entity);
        int maxTrackedArrows = LodgedConfig.maxTrackedArrowsPerEntity();

        while (arrows.size() >= maxTrackedArrows) {
            arrows.remove(0);
        }

        arrows.add(arrowData.save(entity));
        entity.getPersistentData().put(STORAGE_KEY, arrows);
    }

    static List<LodgedArrowData> removeAll(LivingEntity entity) {
        List<LodgedArrowData> arrows = readAll(entity);
        entity.getPersistentData().remove(STORAGE_KEY);
        return arrows;
    }

    private static List<LodgedArrowData> readAll(LivingEntity entity) {
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

    private static ListTag getOrCreateArrowList(LivingEntity entity) {
        CompoundTag persistentData = entity.getPersistentData();
        if (persistentData.contains(STORAGE_KEY, Tag.TAG_LIST)) {
            return persistentData.getList(STORAGE_KEY, Tag.TAG_COMPOUND);
        }
        return new ListTag();
    }
}
