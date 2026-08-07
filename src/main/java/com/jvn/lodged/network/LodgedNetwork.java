package com.jvn.lodged.network;

import com.jvn.lodged.Lodged;
import com.jvn.lodged.config.LodgedConfig;
import com.jvn.lodged.network.payload.ArmorArrowRemovalActionPayload;
import com.jvn.lodged.network.payload.ArrowRemovalResultPayload;
import com.jvn.lodged.network.payload.RemoveHorseArmorArrowPayload;
import com.jvn.lodged.network.payload.RemovePlayerArrowPayload;
import com.jvn.lodged.network.payload.ShieldArrowRemovalActionPayload;
import com.jvn.lodged.network.payload.SyncArmorArrowRemovalPayload;
import com.jvn.lodged.network.payload.SyncBandageUsePayload;
import com.jvn.lodged.network.payload.SyncEntityArrowsPayload;
import com.jvn.lodged.network.payload.SyncPlayerArrowsPayload;
import com.jvn.lodged.network.payload.SyncShieldArrowRemovalPayload;
import com.jvn.lodged.network.payload.UseBandagePayload;
import com.jvn.lodged.world.LodgedArrowData;
import com.jvn.lodged.world.LodgedArrowStorage;
import com.jvn.lodged.world.LodgedArrowVisual;
import com.jvn.toucanlib.neoforge.network.ToucanNetwork;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

public final class LodgedNetwork {
    private static final String NETWORK_VERSION = "19";

    private LodgedNetwork() {
    }

    public static void registerPayloads(RegisterPayloadHandlersEvent event) {
        ToucanNetwork network = ToucanNetwork.create(Lodged.MOD_ID, NETWORK_VERSION, event);
        network.playToServer(
                RemovePlayerArrowPayload.TYPE,
                RemovePlayerArrowPayload.STREAM_CODEC,
                PlayerArrowRemoval::handleRequest);
        network.playToServer(
                RemoveHorseArmorArrowPayload.TYPE,
                RemoveHorseArmorArrowPayload.STREAM_CODEC,
                PlayerArrowRemoval::handleHorseArmorRequest);
        network.playToServer(
                ShieldArrowRemovalActionPayload.TYPE,
                ShieldArrowRemovalActionPayload.STREAM_CODEC,
                PlayerArrowRemoval::handleShieldAction);
        network.playToServer(
                ArmorArrowRemovalActionPayload.TYPE,
                ArmorArrowRemovalActionPayload.STREAM_CODEC,
                PlayerArrowRemoval::handleArmorAction);
        network.playToServer(
                UseBandagePayload.TYPE,
                UseBandagePayload.STREAM_CODEC,
                BandageUseHandler::handleRequest);
        network.safePlayToClient(
                SyncBandageUsePayload.TYPE,
                SyncBandageUsePayload.STREAM_CODEC,
                "com.jvn.lodged.network.ClientBandageState",
                "handleSync");
        network.safePlayToClient(
                SyncPlayerArrowsPayload.TYPE,
                SyncPlayerArrowsPayload.STREAM_CODEC,
                "com.jvn.lodged.network.ClientArrowState",
                "handleSync");
        network.safePlayToClient(
                SyncEntityArrowsPayload.TYPE,
                SyncEntityArrowsPayload.STREAM_CODEC,
                "com.jvn.lodged.network.ClientArrowState",
                "handleSync");
        network.safePlayToClient(
                SyncShieldArrowRemovalPayload.TYPE,
                SyncShieldArrowRemovalPayload.STREAM_CODEC,
                "com.jvn.lodged.network.ClientArrowState",
                "handleSync");
        network.safePlayToClient(
                SyncArmorArrowRemovalPayload.TYPE,
                SyncArmorArrowRemovalPayload.STREAM_CODEC,
                "com.jvn.lodged.network.ClientArrowState",
                "handleSync");
        network.safePlayToClient(
                ArrowRemovalResultPayload.TYPE,
                ArrowRemovalResultPayload.STREAM_CODEC,
                "com.jvn.lodged.network.ClientArrowState",
                "handleRemovalResult");
    }

    public static void syncPlayerArrows(ServerPlayer player) {
        List<LodgedArrowData> storedArrows = LodgedArrowStorage.readAll(player);
        int storedArrowCount = storedArrows.size();
        if (LodgedConfig.enablePlayerArrowRemoval()) {
            player.setArrowCount(storedArrowCount);
        } else {
            LodgedArrowStorage.restoreArrowCount(player, storedArrowCount);
        }
        int maxArrows = LodgedConfig.maxRemovablePlayerArrows();
        int arrowCount = Math.min(storedArrows.size(), maxArrows);
        List<LodgedArrowVisual> arrows = new ArrayList<>(arrowCount);
        for (int index = 0; index < arrowCount; index++) {
            arrows.add(storedArrows.get(index).visual());
        }
        ToucanNetwork.sendToPlayer(player, new SyncPlayerArrowsPayload(arrows, storedArrowCount));
    }

    public static void syncEntityArrows(LivingEntity entity) {
        ToucanNetwork.sendToPlayersTrackingEntityAndSelf(entity, entityArrowsPayload(entity));
    }

    public static void syncEntityArrowsToPlayer(ServerPlayer player, LivingEntity entity) {
        ToucanNetwork.sendToPlayer(player, entityArrowsPayload(entity));
    }

    public static void clearPlayerArrowRemovalCooldown(ServerPlayer player) {
        PlayerArrowRemoval.clearCooldown(player);
    }

    private static SyncEntityArrowsPayload entityArrowsPayload(LivingEntity entity) {
        List<LodgedArrowData> storedArrows = LodgedArrowStorage.readAll(entity);
        int storedArrowCount = storedArrows.size();
        LodgedArrowStorage.restoreArrowCount(entity, storedArrowCount);
        int arrowCount = Math.min(storedArrows.size(), 64);
        List<LodgedArrowVisual> arrows = new ArrayList<>(arrowCount);
        for (int index = 0; index < arrowCount; index++) {
            arrows.add(storedArrows.get(index).visual());
        }
        return new SyncEntityArrowsPayload(entity.getId(), arrows, storedArrowCount);
    }
}
