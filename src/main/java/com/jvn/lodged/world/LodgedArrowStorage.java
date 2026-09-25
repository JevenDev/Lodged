package com.jvn.lodged.world;

import com.jvn.lodged.Lodged;
import com.jvn.lodged.config.LodgedConfig;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import org.jetbrains.annotations.Nullable;

public final class LodgedArrowStorage {
    private static final String STORAGE_KEY = Lodged.MOD_ID + ":lodged_arrows";

    private LodgedArrowStorage() {
    }

    public static void add(LivingEntity entity, LodgedArrowData arrowData) {
        ListTag arrows = getOrCreateArrowList(entity);
        int maxTrackedArrowsPerBodyPart = LodgedConfig.maxTrackedArrowsPerEntity();
        if (maxTrackedArrowsPerBodyPart <= 0) {
            return;
        }

        LodgedArrowBodyPart bodyPart = arrowData.visual().bodyPart();
        int evictedArrows = 0;
        while (arrowCount(arrows, bodyPart) >= maxTrackedArrowsPerBodyPart) {
            removeOldestArrow(arrows, bodyPart);
            evictedArrows++;
        }

        arrows.add(arrowData.save(entity));
        entity.getPersistentData().put(STORAGE_KEY, arrows);
        if (evictedArrows > 0) {
            reconcileRemovedArrows(entity, evictedArrows, arrows.size());
        }
    }

    public static List<LodgedArrowData> removeAll(LivingEntity entity) {
        List<LodgedArrowData> arrows = readAll(entity);
        entity.getPersistentData().remove(STORAGE_KEY);
        reconcileRemovedArrows(entity, arrows.size(), 0);
        return arrows;
    }

    public static int restoreArrowCount(LivingEntity entity) {
        return restoreArrowCount(entity, storedArrowCount(entity));
    }

    public static int restoreArrowCount(LivingEntity entity, int arrowCount) {
        if (arrowCount > entity.getArrowCount()) {
            entity.setArrowCount(arrowCount);
        }
        return arrowCount;
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
        reconcileRemovedArrows(entity, 1, arrows.size());
        return removedArrow;
    }

    static void removeVanillaBodyArrow(
            LivingEntity target,
            AbstractArrow arrow,
            int pierceLevel) {
        if (arrow.isRemoved() && pierceLevel <= 0) {
            target.setArrowCount(Math.max(0, target.getArrowCount() - 1));
        }
    }

    private static void reconcileRemovedArrows(LivingEntity entity, int removedArrows, int storedArrows) {
        entity.setArrowCount(LodgedArrowCount.afterRemoval(entity.getArrowCount(), removedArrows, storedArrows));
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

    private static int arrowCount(ListTag arrows, LodgedArrowBodyPart bodyPart) {
        int count = 0;
        for (int index = 0; index < arrows.size(); index++) {
            if (bodyPart(arrows.getCompound(index)) == bodyPart) {
                count++;
            }
        }
        return count;
    }

    private static void removeOldestArrow(ListTag arrows, LodgedArrowBodyPart bodyPart) {
        for (int index = 0; index < arrows.size(); index++) {
            if (bodyPart(arrows.getCompound(index)) == bodyPart) {
                arrows.remove(index);
                return;
            }
        }
    }

    private static LodgedArrowBodyPart bodyPart(CompoundTag arrow) {
        return LodgedArrowVisual.load(arrow).bodyPart();
    }

    private static int storedArrowCount(LivingEntity entity) {
        return readAll(entity).size();
    }
}
