package com.jvn.lodged.network;

import com.jvn.lodged.Lodged;
import com.jvn.lodged.config.LodgedConfig;
import com.jvn.lodged.network.payload.RemovePlayerArrowPayload;
import com.jvn.lodged.network.payload.SyncEntityArrowsPayload;
import com.jvn.lodged.network.payload.SyncPlayerArrowsPayload;
import com.jvn.lodged.world.LodgedArrowData;
import com.jvn.lodged.world.LodgedArrowStorage;
import com.jvn.lodged.world.LodgedArrowVisual;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class LodgedNetwork {
    private static final String NETWORK_VERSION = "3";

    private LodgedNetwork() {
    }

    public static void registerPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(NETWORK_VERSION);
        registrar.playToServer(
                RemovePlayerArrowPayload.TYPE,
                RemovePlayerArrowPayload.STREAM_CODEC,
                PlayerArrowRemoval::handleRequest);
        registrar.playToClient(
                SyncPlayerArrowsPayload.TYPE,
                SyncPlayerArrowsPayload.STREAM_CODEC,
                ClientArrowState::handleSync);
        registrar.playToClient(
                SyncEntityArrowsPayload.TYPE,
                SyncEntityArrowsPayload.STREAM_CODEC,
                ClientArrowState::handleSync);
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
        PacketDistributor.sendToPlayer(player, new SyncPlayerArrowsPayload(arrows, storedArrowCount));
    }

    public static void syncEntityArrows(LivingEntity entity) {
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(entity, entityArrowsPayload(entity));
    }

    public static void syncEntityArrowsToPlayer(ServerPlayer player, LivingEntity entity) {
        PacketDistributor.sendToPlayer(player, entityArrowsPayload(entity));
    }

    public static void clearPlayerArrowRemovalCooldown(ServerPlayer player) {
        PlayerArrowRemoval.clearCooldown(player);
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(Lodged.MOD_ID, path);
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
