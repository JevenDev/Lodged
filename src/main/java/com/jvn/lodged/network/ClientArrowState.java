package com.jvn.lodged.network;

import com.jvn.lodged.network.payload.SyncPlayerArrowsPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public final class ClientArrowState {
    private static int removableArrowCount;

    private ClientArrowState() {
    }

    public static int removableArrowCount() {
        return removableArrowCount;
    }

    static void handleSync(SyncPlayerArrowsPayload payload, IPayloadContext context) {
        removableArrowCount = Math.max(0, payload.arrowCount());
    }
}
