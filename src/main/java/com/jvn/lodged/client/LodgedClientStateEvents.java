package com.jvn.lodged.client;

import com.jvn.lodged.network.ClientArrowState;
import com.jvn.lodged.network.ClientBandageState;
import net.minecraft.client.player.LocalPlayer;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;

final class LodgedClientStateEvents {
    private LodgedClientStateEvents() {
    }

    static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        ClientArrowState.reset();
        ClientBandageState.reset();
    }

    static void onEntityLeaveLevel(EntityLeaveLevelEvent event) {
        if (!event.getLevel().isClientSide()) {
            return;
        }

        if (event.getEntity() instanceof LocalPlayer) {
            ClientArrowState.reset();
            ClientBandageState.reset();
        } else {
            ClientArrowState.removeEntity(event.getEntity().getId());
            ClientBandageState.removeEntity(event.getEntity().getId());
        }
    }
}
