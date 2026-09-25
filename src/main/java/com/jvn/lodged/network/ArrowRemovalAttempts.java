package com.jvn.lodged.network;

import com.jvn.lodged.config.LodgedConfig;
import com.jvn.lodged.network.ArrowRemovalTargets.InWorldArrowTarget;
import com.jvn.lodged.network.payload.RemovePlayerArrowPayload.Target;
import com.jvn.lodged.network.payload.SyncArmorArrowRemovalPayload;
import com.jvn.lodged.network.payload.SyncShieldArrowRemovalPayload;
import com.jvn.lodged.world.LodgedArrowVisual;
import com.jvn.lodged.world.LodgedShieldArrowStorage.LodgedShieldArrowData;
import com.jvn.lodged.world.LodgedShieldArrowStorage;
import com.jvn.toucanlib.neoforge.network.ToucanNetwork;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.item.ItemStack;

final class ArrowRemovalAttempts {
    private static final Map<UUID, ShieldArrowRemovalAttempt> SHIELD_ARROW_REMOVALS = new HashMap<>();
    private static final Map<UUID, ArmorArrowRemovalAttempt> ARMOR_ARROW_REMOVALS = new HashMap<>();

    private ArrowRemovalAttempts() {
    }

    static boolean isRemovingShieldArrow(LivingEntity entity) {
        return SHIELD_ARROW_REMOVALS.containsKey(entity.getUUID());
    }

    static void syncShieldArrowRemovalToPlayer(ServerPlayer viewer, LivingEntity entity) {
        ShieldArrowRemovalAttempt attempt = SHIELD_ARROW_REMOVALS.get(entity.getUUID());
        if (attempt != null) {
            ToucanNetwork.sendToPlayer(
                    viewer,
                    new SyncShieldArrowRemovalPayload(entity.getId(), true, attempt.hand()));
        }
        ArmorArrowRemovalAttempt armorAttempt = ARMOR_ARROW_REMOVALS.get(entity.getUUID());
        if (armorAttempt != null) {
            ToucanNetwork.sendToPlayer(
                    viewer,
                    new SyncArmorArrowRemovalPayload(
                            entity.getId(),
                            true,
                            armorAttempt.target(),
                            armorAttempt.slot(),
                            armorAttempt.targetEntityId(),
                            armorAttempt.visual()));
        }
    }

    static boolean startShieldArrowRemoval(ServerPlayer player, InteractionHand hand) {
        UUID playerUuid = player.getUUID();
        ShieldArrowRemovalAttempt activeAttempt = SHIELD_ARROW_REMOVALS.get(playerUuid);
        if (activeAttempt != null) {
            syncShieldArrowRemoval(player, true, activeAttempt.hand());
            return true;
        }
        if (ARMOR_ARROW_REMOVALS.containsKey(playerUuid)) {
            return false;
        }

        ItemStack shield = player.getItemInHand(hand);
        if (!ArrowRemovalValidation.canAttemptHeldShieldRemoval(player, shield)) {
            return false;
        }

        List<LodgedShieldArrowData> arrows = LodgedShieldArrowStorage.readData(shield, player.registryAccess());
        if (arrows.isEmpty()) {
            return false;
        }

        int arrowIndex = ArrowRemovalTargets.priorityShieldArrowIndex(arrows);
        long gameTime = player.level().getGameTime();
        long completeTick = gameTime + shieldArrowRemovalTicks(player);
        SHIELD_ARROW_REMOVALS.put(
                playerUuid,
                new ShieldArrowRemovalAttempt(hand, arrowIndex, completeTick, arrows.get(arrowIndex).visual()));
        syncShieldArrowRemoval(player, true, hand);
        return true;
    }

    static boolean startInWorldArrowRemoval(ServerPlayer player, int targetEntityId) {
        UUID playerUuid = player.getUUID();
        ArmorArrowRemovalAttempt activeAttempt = ARMOR_ARROW_REMOVALS.get(playerUuid);
        if (activeAttempt != null) {
            syncArmorArrowRemoval(
                    player,
                    true,
                    activeAttempt.target(),
                    activeAttempt.slot(),
                    activeAttempt.targetEntityId(),
                    activeAttempt.visual());
            return true;
        }
        if (SHIELD_ARROW_REMOVALS.containsKey(playerUuid) || player.isUsingItem()) {
            return false;
        }

        InWorldArrowTarget target;
        if (targetEntityId == player.getId()) {
            target = ArrowRemovalTargets.priorityArmorArrow(player);
            if (target == null) {
                target = ArrowRemovalTargets.priorityBodyArrow(player);
            }
        } else {
            if (ArrowRemovalTargets.priorityArmorArrow(player) != null || ArrowRemovalTargets.priorityBodyArrow(player) != null) {
                return false;
            }
            Entity entity = player.level().getEntity(targetEntityId);
            if (!(entity instanceof LivingEntity mob)
                    || !ArrowRemovalValidation.canAttemptTamedMobRemoval(player, mob, true)) {
                return false;
            }
            target = ArrowRemovalTargets.priorityMobArrow(player, mob);
        }
        if (target == null) {
            return false;
        }

        long gameTime = player.level().getGameTime();
        long completeTick = gameTime + inWorldRemovalTicks(player, target);
        ARMOR_ARROW_REMOVALS.put(
                playerUuid,
                new ArmorArrowRemovalAttempt(
                        target.target(),
                        target.slot(),
                        target.targetEntityId(),
                        target.arrowIndex(),
                        completeTick,
                        target.visual()));
        syncArmorArrowRemoval(player, true, target.target(), target.slot(), target.targetEntityId(), target.visual());
        return true;
    }

    static void tickShieldArrowRemoval(ServerPlayer player) {
        ShieldArrowRemovalAttempt attempt = SHIELD_ARROW_REMOVALS.get(player.getUUID());
        if (attempt == null) {
            return;
        }

        ItemStack shield = player.getItemInHand(attempt.hand());
        if (!ArrowRemovalValidation.canAttemptHeldShieldRemoval(player, shield, attempt.arrowIndex(), attempt.visual())
                || !player.isUsingItem()
                || player.getUsedItemHand() != attempt.hand()) {
            stopShieldArrowRemoval(player);
            return;
        }

        if (player.level().getGameTime() < attempt.completeTick()) {
            return;
        }

        stopShieldArrowRemoval(player);
        ArrowRemovalOutcomes.removeShieldArrow(player, attempt.hand(), attempt.arrowIndex(), true);
    }

    static void tickArmorArrowRemoval(ServerPlayer player) {
        ArmorArrowRemovalAttempt attempt = ARMOR_ARROW_REMOVALS.get(player.getUUID());
        if (attempt == null) {
            return;
        }

        if (!ArrowRemovalValidation.canAttemptInWorldRemoval(player, attempt) || player.isUsingItem()) {
            stopArmorArrowRemoval(player);
            return;
        }

        if (player.level().getGameTime() < attempt.completeTick()) {
            return;
        }

        stopArmorArrowRemoval(player);
        if (attempt.targetEntityId() != player.getId()) {
            Entity target = player.level().getEntity(attempt.targetEntityId());
            if (target instanceof LivingEntity mob) {
                if (attempt.target() == Target.ARMOR && mob instanceof AbstractHorse horse) {
                    ArrowRemovalOutcomes.removeHorseArmorArrow(player, horse, attempt.arrowIndex(), LodgedConfig.tamedMobArrowRemovalParticles());
                } else {
                    ArrowRemovalOutcomes.removeMobBodyArrow(player, mob, attempt.arrowIndex());
                }
            }
            return;
        }
        if (attempt.target() == Target.BODY) {
            ArrowRemovalOutcomes.removeBodyArrow(player, attempt.arrowIndex());
        } else {
            ArrowRemovalOutcomes.removeArmorArrow(player, attempt.slot(), attempt.arrowIndex(), true);
        }
    }

    private static int inWorldRemovalTicks(ServerPlayer player, InWorldArrowTarget target) {
        if (target.targetEntityId() != player.getId()) {
            int minTicks = LodgedConfig.tamedMobArrowRemovalMinTicks();
            int maxTicks = LodgedConfig.tamedMobArrowRemovalMaxTicks();
            return minTicks + player.getRandom().nextInt(maxTicks - minTicks + 1);
        }
        if (target.target() == Target.ARMOR) {
            return LodgedConfig.armorArrowRemovalTicks();
        }

        return inWorldBodyArrowRemovalTicks(player);
    }

    private static int inWorldBodyArrowRemovalTicks(ServerPlayer player) {
        int minTicks = LodgedConfig.inWorldBodyArrowRemovalMinTicks();
        int maxTicks = LodgedConfig.inWorldBodyArrowRemovalMaxTicks();
        return minTicks + player.getRandom().nextInt(maxTicks - minTicks + 1);
    }

    private static int shieldArrowRemovalTicks(ServerPlayer player) {
        int minTicks = LodgedConfig.shieldArrowRemovalMinTicks();
        int maxTicks = LodgedConfig.shieldArrowRemovalMaxTicks();
        return minTicks + player.getRandom().nextInt(maxTicks - minTicks + 1);
    }

    static void stopShieldArrowRemoval(ServerPlayer player) {
        ShieldArrowRemovalAttempt removed = SHIELD_ARROW_REMOVALS.remove(player.getUUID());
        if (removed != null) {
            syncShieldArrowRemoval(player, false, removed.hand());
        }
    }

    static void stopArmorArrowRemoval(ServerPlayer player) {
        ArmorArrowRemovalAttempt removed = ARMOR_ARROW_REMOVALS.remove(player.getUUID());
        if (removed != null) {
            syncArmorArrowRemoval(player, false, removed.target(), removed.slot(), removed.targetEntityId(), removed.visual());
        }
    }

    private static void syncShieldArrowRemoval(ServerPlayer player, boolean active, InteractionHand hand) {
        ToucanNetwork.sendToPlayersTrackingEntityAndSelf(
                player,
                new SyncShieldArrowRemovalPayload(player.getId(), active, hand));
    }

    private static void syncArmorArrowRemoval(
            ServerPlayer player,
            boolean active,
            Target target,
            EquipmentSlot slot,
            int targetEntityId,
            LodgedArrowVisual arrow) {
        ToucanNetwork.sendToPlayersTrackingEntityAndSelf(
                player,
                new SyncArmorArrowRemovalPayload(player.getId(), active, target, slot, targetEntityId, arrow));
    }

    static void rejectShieldArrowRemoval(ServerPlayer player, InteractionHand hand) {
        ToucanNetwork.sendToPlayer(
                player,
                new SyncShieldArrowRemovalPayload(player.getId(), false, hand));
    }

    static void rejectArmorArrowRemoval(ServerPlayer player) {
        ToucanNetwork.sendToPlayer(
                player,
                new SyncArmorArrowRemovalPayload(
                        player.getId(),
                        false,
                        Target.ARMOR,
                        EquipmentSlot.CHEST,
                        -1,
                        LodgedArrowVisual.DEFAULT));
    }

    static void clear() {
        SHIELD_ARROW_REMOVALS.clear();
        ARMOR_ARROW_REMOVALS.clear();
    }

    private record ShieldArrowRemovalAttempt(
            InteractionHand hand,
            int arrowIndex,
            long completeTick,
            LodgedArrowVisual visual) {
    }

    record ArmorArrowRemovalAttempt(
            Target target,
            EquipmentSlot slot,
            int targetEntityId,
            int arrowIndex,
            long completeTick,
            LodgedArrowVisual visual) {
    }
}
