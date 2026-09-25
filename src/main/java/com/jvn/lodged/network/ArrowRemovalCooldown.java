package com.jvn.lodged.network;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

final class ArrowRemovalCooldown {
    private final int cooldownTicks;
    private final Map<UUID, Long> lastRequestTick = new HashMap<>();

    ArrowRemovalCooldown(int cooldownTicks) {
        this.cooldownTicks = cooldownTicks;
    }

    boolean accept(UUID playerUuid, long gameTime) {
        Long lastTick = lastRequestTick.get(playerUuid);
        if (lastTick != null && gameTime - lastTick < cooldownTicks) {
            return false;
        }

        lastRequestTick.put(playerUuid, gameTime);
        return true;
    }

    void clear(UUID playerUuid) {
        lastRequestTick.remove(playerUuid);
    }

    void clear() {
        lastRequestTick.clear();
    }
}
