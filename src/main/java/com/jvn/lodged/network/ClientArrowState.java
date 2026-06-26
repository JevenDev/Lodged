package com.jvn.lodged.network;

import com.jvn.lodged.network.payload.SyncPlayerArrowsPayload;
import com.jvn.lodged.network.payload.SyncEntityArrowsPayload;
import com.jvn.lodged.network.payload.SyncShieldArrowRemovalPayload;
import com.jvn.lodged.world.LodgedArrowVisual;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public final class ClientArrowState {
    private static List<LodgedArrowVisual> removableArrows = List.of();
    private static final Map<Integer, EntityArrows> ENTITY_ARROWS = new HashMap<>();
    private static final Map<Integer, ShieldArrowRemovalState> SHIELD_ARROW_REMOVALS = new HashMap<>();
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

    public static ShieldArrowRemovalState shieldArrowRemoval(LivingEntity entity) {
        return SHIELD_ARROW_REMOVALS.getOrDefault(entity.getId(), ShieldArrowRemovalState.INACTIVE);
    }

    public static void tickShieldArrowRemovals() {
        SHIELD_ARROW_REMOVALS.replaceAll((entityId, state) -> state.tick());
    }

    public static void setLocalShieldArrowRemoval(int entityId, InteractionHand shieldHand, boolean active) {
        setShieldArrowRemoval(entityId, active, shieldHand);
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

    public static void handleSync(SyncShieldArrowRemovalPayload payload, IPayloadContext context) {
        setShieldArrowRemoval(payload.entityId(), payload.active(), payload.shieldHand());
    }

    private static void setShieldArrowRemoval(int entityId, boolean active, InteractionHand shieldHand) {
        if (!active) {
            SHIELD_ARROW_REMOVALS.remove(entityId);
            return;
        }

        SHIELD_ARROW_REMOVALS.put(entityId, new ShieldArrowRemovalState(shieldHand, 0));
    }

    public record EntityArrows(List<LodgedArrowVisual> arrows, int arrowCount) {
        private static final EntityArrows EMPTY = new EntityArrows(List.of(), 0);
    }

    public record ShieldArrowRemovalState(InteractionHand shieldHand, int ticks) {
        private static final ShieldArrowRemovalState INACTIVE = new ShieldArrowRemovalState(InteractionHand.MAIN_HAND, -1);

        public boolean active() {
            return ticks >= 0;
        }

        private ShieldArrowRemovalState tick() {
            return new ShieldArrowRemovalState(shieldHand, ticks + 1);
        }
    }
}
