package com.jvn.lodged.network;

import com.jvn.lodged.Lodged;
import com.jvn.lodged.config.LodgedConfig;
import com.jvn.lodged.network.payload.RemovePlayerArrowPayload;
import com.jvn.lodged.network.payload.SyncPlayerArrowsPayload;
import com.jvn.lodged.world.LodgedArrowVisual;
import com.jvn.lodged.world.LodgedArrowStorage;
import java.util.List;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class LodgedNetwork {
    private static final String NETWORK_VERSION = "2";

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
        int maxArrows = LodgedConfig.maxRemovablePlayerArrows();
        List<LodgedArrowVisual> arrows = LodgedArrowStorage.readAll(player)
                .stream()
                .limit(maxArrows)
                .map(arrow -> arrow.visual())
                .toList();
        PacketDistributor.sendToPlayer(player, new SyncPlayerArrowsPayload(arrows));
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(Lodged.MOD_ID, path);
    }
}
