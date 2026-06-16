package com.jvn.lodged.network;

import com.jvn.lodged.network.payload.SyncPlayerArrowsPayload;
import com.jvn.lodged.world.LodgedArrowVisual;
import java.util.List;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public final class ClientArrowState {
    private static List<LodgedArrowVisual> removableArrows = List.of();

    private ClientArrowState() {
    }

    public static int removableArrowCount() {
        return removableArrows.size();
    }

    public static List<LodgedArrowVisual> removableArrows() {
        return removableArrows;
    }

    static void handleSync(SyncPlayerArrowsPayload payload, IPayloadContext context) {
        removableArrows = payload.arrows();
    }
}
