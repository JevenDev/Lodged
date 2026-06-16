package com.jvn.lodged.network;

import com.jvn.lodged.Lodged;
import com.jvn.lodged.config.LodgedConfig;
import com.jvn.lodged.network.payload.RemovePlayerArrowPayload;
import com.jvn.lodged.network.payload.SyncPlayerArrowsPayload;
import com.jvn.lodged.world.LodgedArrowStorage;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class LodgedNetwork {
    private static final String NETWORK_VERSION = "1";

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
    }

    public static void syncPlayerArrows(ServerPlayer player) {
        int arrowCount = Math.min(LodgedArrowStorage.count(player), LodgedConfig.maxRemovablePlayerArrows());
        PacketDistributor.sendToPlayer(player, new SyncPlayerArrowsPayload(arrowCount));
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(Lodged.MOD_ID, path);
    }
}
