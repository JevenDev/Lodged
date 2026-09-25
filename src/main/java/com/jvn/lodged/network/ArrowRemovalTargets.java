package com.jvn.lodged.network;

import com.jvn.lodged.config.LodgedConfig;
import com.jvn.lodged.network.payload.RemovePlayerArrowPayload.Target;
import com.jvn.lodged.world.LodgedArmorArrowStorage.LodgedArmorArrowData;
import com.jvn.lodged.world.LodgedArmorArrowStorage;
import com.jvn.lodged.world.LodgedArrowData;
import com.jvn.lodged.world.LodgedArrowRemovalScoring;
import com.jvn.lodged.world.LodgedArrowStorage;
import com.jvn.lodged.world.LodgedArrowVisual;
import com.jvn.lodged.world.LodgedShieldArrowStorage.LodgedShieldArrowData;
import java.util.List;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.item.ItemStack;

final class ArrowRemovalTargets {
    private ArrowRemovalTargets() {
    }

    static InWorldArrowTarget priorityMobArrow(ServerPlayer player, LivingEntity mob) {
        InWorldArrowTarget best = null;
        double bestScore = Double.MAX_VALUE;
        if (mob instanceof AbstractHorse horse) {
            List<LodgedArmorArrowData> armorArrows =
                    LodgedArmorArrowStorage.readData(horse.getItemBySlot(EquipmentSlot.BODY), player.registryAccess());
            for (int index = 0; index < armorArrows.size(); index++) {
                LodgedArrowVisual visual = armorArrows.get(index).visual();
                double score = LodgedArrowRemovalScoring.armorArrowPriorityScore(
                        EquipmentSlot.BODY,
                        visual,
                        player.getMainArm());
                if (score < bestScore) {
                    bestScore = score;
                    best = new InWorldArrowTarget(Target.ARMOR, EquipmentSlot.BODY, mob.getId(), index, visual);
                }
            }
        }
        List<LodgedArrowData> arrows = LodgedArrowStorage.readAll(mob);
        for (int index = 0; index < arrows.size(); index++) {
            LodgedArrowData arrow = arrows.get(index);
            double chance = LodgedConfig.tamedMobArrowRemovalSuccessChance(arrow.visual().bodyPart())
                    * LodgedConfig.arrowDepthRemovalSuccessMultiplier(arrow.visual().depth());
            double score = LodgedArrowRemovalScoring.bodyArrowPriorityScore(arrow.visual(), chance);
            if (score < bestScore) {
                bestScore = score;
                best = new InWorldArrowTarget(Target.BODY, EquipmentSlot.CHEST, mob.getId(), index, arrow.visual());
            }
        }
        return best;
    }

    static int priorityShieldArrowIndex(List<LodgedShieldArrowData> arrows) {
        int bestIndex = 0;
        double bestScore = Double.MAX_VALUE;
        for (int index = 0; index < arrows.size(); index++) {
            LodgedArrowVisual visual = arrows.get(index).visual();
            double score = Math.abs(visual.modelX()) + Math.abs(visual.modelY() * 0.75D);
            if (score < bestScore) {
                bestScore = score;
                bestIndex = index;
            }
        }
        return bestIndex;
    }

    static InWorldArrowTarget priorityArmorArrow(ServerPlayer player) {
        InWorldArrowTarget best = null;
        double bestScore = Double.MAX_VALUE;
        for (EquipmentSlot slot : LodgedArmorArrowStorage.armorSlots()) {
            ItemStack armor = player.getItemBySlot(slot);
            List<LodgedArmorArrowData> arrows = LodgedArmorArrowStorage.readData(armor, player.registryAccess());
            for (int index = 0; index < arrows.size(); index++) {
                LodgedArrowVisual visual = arrows.get(index).visual();
                double score = LodgedArrowRemovalScoring.armorArrowPriorityScore(slot, visual, player.getMainArm());
                if (score < bestScore) {
                    bestScore = score;
                    best = new InWorldArrowTarget(Target.ARMOR, slot, player.getId(), index, visual);
                }
            }
        }
        return best;
    }

    static InWorldArrowTarget priorityBodyArrow(ServerPlayer player) {
        List<LodgedArrowData> arrows = LodgedArrowStorage.readAll(player);
        int arrowCount = Math.min(arrows.size(), LodgedConfig.maxRemovablePlayerArrows());
        InWorldArrowTarget best = null;
        double bestScore = Double.MAX_VALUE;
        for (int index = 0; index < arrowCount; index++) {
            LodgedArrowData arrow = arrows.get(index);
            double score = LodgedArrowRemovalScoring.bodyArrowPriorityScore(
                    arrow.visual(),
                    ArrowRemovalRules.removalSuccessChance(arrow));
            if (score < bestScore) {
                bestScore = score;
                best = new InWorldArrowTarget(Target.BODY, EquipmentSlot.CHEST, player.getId(), index, arrow.visual());
            }
        }
        return best;
    }

    record InWorldArrowTarget(
            Target target,
            EquipmentSlot slot,
            int targetEntityId,
            int arrowIndex,
            LodgedArrowVisual visual) {
    }
}
