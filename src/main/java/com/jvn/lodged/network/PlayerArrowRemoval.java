package com.jvn.lodged.network;

import com.jvn.lodged.network.payload.ArmorArrowRemovalActionPayload;
import com.jvn.lodged.network.payload.ArrowRemovalResultPayload.Result;
import com.jvn.lodged.network.payload.RemoveHorseArmorArrowPayload;
import com.jvn.lodged.network.payload.RemovePlayerArrowPayload.Target;
import com.jvn.lodged.network.payload.RemovePlayerArrowPayload;
import com.jvn.lodged.network.payload.ShieldArrowRemovalActionPayload.Action;
import com.jvn.lodged.network.payload.ShieldArrowRemovalActionPayload;
import com.jvn.lodged.world.LodgedArrowVisual;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.inventory.HorseInventoryMenu;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public final class PlayerArrowRemoval {
    private PlayerArrowRemoval() {
    }

    static void handleRequest(RemovePlayerArrowPayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) {
            return;
        }

        if (!ArrowRemovalValidation.canAttemptRemoval(player, payload)) {
            ArrowRemovalOutcomes.sendRemovalResult(player, Result.CANT_REMOVE_NOW, payload.target(), payload.hand(), LodgedArrowVisual.DEFAULT);
            LodgedNetwork.syncPlayerArrows(player);
            return;
        }

        if (!ArrowRemovalValidation.acceptRequest(player)) {
            ArrowRemovalOutcomes.sendRemovalResult(player, Result.CANT_REMOVE_NOW, payload.target(), payload.hand(), LodgedArrowVisual.DEFAULT);
            return;
        }

        if (payload.target() == Target.SHIELD) {
            ArrowRemovalOutcomes.removeShieldArrow(player, payload.hand(), payload.arrowIndex(), false);
            return;
        }

        if (payload.target() == Target.ARMOR) {
            ArrowRemovalOutcomes.removeArmorArrow(player, payload.armorSlot(), payload.arrowIndex(), false);
            return;
        }

        ArrowRemovalOutcomes.removeBodyArrow(player, payload.arrowIndex());
    }

    static void handleHorseArmorRequest(RemoveHorseArmorArrowPayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) {
            return;
        }

        AbstractHorse horse = ArrowRemovalValidation.openHorse(player);
        if (!ArrowRemovalValidation.canAttemptHorseArmorRemoval(player, horse, payload)) {
            ArrowRemovalOutcomes.sendRemovalResult(
                    player,
                    Result.CANT_REMOVE_NOW,
                    Target.ARMOR,
                    InteractionHand.MAIN_HAND,
                    LodgedArrowVisual.DEFAULT);
            if (player.containerMenu instanceof HorseInventoryMenu) {
                player.containerMenu.broadcastChanges();
            }
            return;
        }

        if (!ArrowRemovalValidation.acceptRequest(player)) {
            ArrowRemovalOutcomes.sendRemovalResult(
                    player,
                    Result.CANT_REMOVE_NOW,
                    Target.ARMOR,
                    InteractionHand.MAIN_HAND,
                    LodgedArrowVisual.DEFAULT);
            return;
        }

        if (payload.target() == Target.BODY) {
            ArrowRemovalOutcomes.removeMobBodyArrow(player, horse, payload.arrowIndex());
        } else {
            ArrowRemovalOutcomes.removeHorseArmorArrow(player, horse, payload.arrowIndex(), false);
        }
    }

    static void handleShieldAction(ShieldArrowRemovalActionPayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) {
            return;
        }

        if (payload.action() == Action.CANCEL) {
            ArrowRemovalAttempts.stopShieldArrowRemoval(player);
            return;
        }

        if (!ArrowRemovalAttempts.startShieldArrowRemoval(player, payload.hand())) {
            ArrowRemovalAttempts.rejectShieldArrowRemoval(player, payload.hand());
        }
    }

    static void handleArmorAction(ArmorArrowRemovalActionPayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) {
            return;
        }

        if (payload.action() == ArmorArrowRemovalActionPayload.Action.CANCEL) {
            ArrowRemovalAttempts.stopArmorArrowRemoval(player);
            return;
        }

        if (!ArrowRemovalAttempts.startInWorldArrowRemoval(player, payload.targetEntityId())) {
            ArrowRemovalAttempts.rejectArmorArrowRemoval(player);
        }
    }

    public static void onEntityTick(EntityTickEvent.Post event) {
        Entity entity = event.getEntity();
        if (entity.level().isClientSide() || !(entity instanceof ServerPlayer player)) {
            return;
        }

        ArrowRemovalAttempts.tickShieldArrowRemoval(player);
        ArrowRemovalAttempts.tickArmorArrowRemoval(player);
    }

    public static void onServerStopped(ServerStoppedEvent event) {
        ArrowRemovalValidation.clear();
        ArrowRemovalAttempts.clear();
    }

    static void clearCooldown(ServerPlayer player) {
        ArrowRemovalValidation.clearCooldown(player);
        ArrowRemovalAttempts.stopShieldArrowRemoval(player);
        ArrowRemovalAttempts.stopArmorArrowRemoval(player);
    }

    public static boolean isRemovingShieldArrow(LivingEntity entity) {
        return ArrowRemovalAttempts.isRemovingShieldArrow(entity);
    }

    public static void syncShieldArrowRemovalToPlayer(ServerPlayer viewer, LivingEntity entity) {
        ArrowRemovalAttempts.syncShieldArrowRemovalToPlayer(viewer, entity);
    }
}
