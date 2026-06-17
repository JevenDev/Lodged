package com.jvn.lodged.network;

import com.jvn.lodged.network.payload.SyncPlayerArrowsPayload;
import com.jvn.lodged.network.payload.SyncEntityArrowsPayload;
import com.jvn.lodged.world.LodgedArrowVisual;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public final class ClientArrowState {
    private static List<LodgedArrowVisual> removableArrows = List.of();
    private static final Map<Integer, EntityArrows> ENTITY_ARROWS = new HashMap<>();
    private static int syncedArrowCount;
    private static boolean synced;

    private ClientArrowState() {
    }

    public static int removableArrowCount() {
        return removableArrows.size();
    }

    public static int syncedArrowCount() {
        return syncedArrowCount;
    }

    public static boolean hasSynced() {
        return synced;
    }

    public static List<LodgedArrowVisual> removableArrows() {
        return removableArrows;
    }

    public static EntityArrows entityArrows(LivingEntity entity) {
        return ENTITY_ARROWS.getOrDefault(entity.getId(), EntityArrows.EMPTY);
    }

    static void handleSync(SyncPlayerArrowsPayload payload, IPayloadContext context) {
        removableArrows = payload.arrows();
        syncedArrowCount = payload.arrowCount();
        synced = true;
    }

    static void handleSync(SyncEntityArrowsPayload payload, IPayloadContext context) {
        if (payload.arrowCount() <= 0 || payload.arrows().isEmpty()) {
            ENTITY_ARROWS.remove(payload.entityId());
            return;
        }

        ENTITY_ARROWS.put(payload.entityId(), new EntityArrows(payload.arrows(), payload.arrowCount()));
    }

    public record EntityArrows(List<LodgedArrowVisual> arrows, int arrowCount) {
        private static final EntityArrows EMPTY = new EntityArrows(List.of(), 0);
    }
}
