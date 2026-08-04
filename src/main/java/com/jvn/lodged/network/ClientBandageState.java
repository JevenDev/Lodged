package com.jvn.lodged.network;

import com.jvn.lodged.item.BandageItem;
import com.jvn.lodged.network.payload.SyncBandageUsePayload;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public final class ClientBandageState {
    private static final Map<Integer, BandageUseState> BANDAGE_USES = new HashMap<>();

    private ClientBandageState() {
    }

    public static boolean isActive(LivingEntity entity) {
        return BANDAGE_USES.containsKey(entity.getId());
    }

    public static InteractionHand hand(LivingEntity entity) {
        BandageUseState state = BANDAGE_USES.get(entity.getId());
        return state == null ? InteractionHand.MAIN_HAND : state.hand();
    }

    public static int ticks(LivingEntity entity) {
        BandageUseState state = BANDAGE_USES.get(entity.getId());
        return state == null ? 0 : state.ticks();
    }

    public static void tick() {
        BANDAGE_USES.replaceAll((entityId, state) -> state.tick());
    }

    public static void removeEntity(int entityId) {
        BANDAGE_USES.remove(entityId);
    }

    public static void reset() {
        BANDAGE_USES.clear();
    }

    public static void handleSync(SyncBandageUsePayload payload, IPayloadContext context) {
        if (!payload.active()) {
            BANDAGE_USES.remove(payload.entityId());
            return;
        }
        BANDAGE_USES.put(payload.entityId(), new BandageUseState(payload.hand(), 0));
    }

    private record BandageUseState(InteractionHand hand, int ticks) {
        private BandageUseState tick() {
            return new BandageUseState(hand, Math.min(ticks + 1, BandageItem.USE_DURATION));
        }
    }
}
