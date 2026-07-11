package com.jvn.lodged.network;

import com.jvn.lodged.network.payload.ArrowRemovalResultPayload;
import com.jvn.lodged.network.payload.RemovePlayerArrowPayload.Target;
import com.jvn.lodged.network.payload.SyncArmorArrowRemovalPayload;
import com.jvn.lodged.network.payload.SyncPlayerArrowsPayload;
import com.jvn.lodged.network.payload.SyncEntityArrowsPayload;
import com.jvn.lodged.network.payload.SyncShieldArrowRemovalPayload;
import com.jvn.lodged.world.LodgedArrowVisual;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public final class ClientArrowState {
    private static List<LodgedArrowVisual> removableArrows = List.of();
    private static final Map<Integer, EntityArrows> ENTITY_ARROWS = new HashMap<>();
    private static final Map<Integer, ShieldArrowRemovalState> SHIELD_ARROW_REMOVALS = new HashMap<>();
    private static final Map<Integer, ArmorArrowRemovalState> ARMOR_ARROW_REMOVALS = new HashMap<>();
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

    public static ArmorArrowRemovalState armorArrowRemoval(LivingEntity entity) {
        return ARMOR_ARROW_REMOVALS.getOrDefault(entity.getId(), ArmorArrowRemovalState.INACTIVE);
    }

    public static void tickShieldArrowRemovals() {
        SHIELD_ARROW_REMOVALS.replaceAll((entityId, state) -> state.tick());
        ARMOR_ARROW_REMOVALS.replaceAll((entityId, state) -> state.tick());
    }

    public static void setLocalShieldArrowRemoval(int entityId, InteractionHand shieldHand, boolean active) {
        setShieldArrowRemoval(entityId, active, shieldHand);
    }

    public static void setLocalArmorArrowRemoval(
            int entityId,
            boolean active,
            Target target,
            EquipmentSlot slot,
            LodgedArrowVisual arrow) {
        setArmorArrowRemoval(entityId, active, target, slot, arrow);
    }

    public static void removeEntity(int entityId) {
        ENTITY_ARROWS.remove(entityId);
        SHIELD_ARROW_REMOVALS.remove(entityId);
        ARMOR_ARROW_REMOVALS.remove(entityId);
    }

    public static void reset() {
        removableArrows = List.of();
        ENTITY_ARROWS.clear();
        SHIELD_ARROW_REMOVALS.clear();
        ARMOR_ARROW_REMOVALS.clear();
        syncedArrowCount = 0;
        synced = false;
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

    public static void handleSync(SyncArmorArrowRemovalPayload payload, IPayloadContext context) {
        setArmorArrowRemoval(payload.entityId(), payload.active(), payload.target(), payload.slot(), payload.arrow());
    }

    public static void handleRemovalResult(ArrowRemovalResultPayload payload, IPayloadContext context) {
        com.jvn.lodged.client.LodgedInventoryArrowUi.handleRemovalResult(payload);
    }

    private static void setShieldArrowRemoval(int entityId, boolean active, InteractionHand shieldHand) {
        if (!active) {
            SHIELD_ARROW_REMOVALS.remove(entityId);
            return;
        }

        SHIELD_ARROW_REMOVALS.put(entityId, new ShieldArrowRemovalState(shieldHand, 0));
    }

    private static void setArmorArrowRemoval(
            int entityId,
            boolean active,
            Target target,
            EquipmentSlot slot,
            LodgedArrowVisual arrow) {
        if (!active) {
            ARMOR_ARROW_REMOVALS.remove(entityId);
            return;
        }

        ARMOR_ARROW_REMOVALS.put(entityId, new ArmorArrowRemovalState(target, slot, arrow, 0));
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

    public record ArmorArrowRemovalState(Target target, EquipmentSlot slot, LodgedArrowVisual arrow, int ticks) {
        private static final ArmorArrowRemovalState INACTIVE =
                new ArmorArrowRemovalState(Target.ARMOR, EquipmentSlot.CHEST, LodgedArrowVisual.DEFAULT, -1);

        public boolean active() {
            return ticks >= 0;
        }

        private ArmorArrowRemovalState tick() {
            return new ArmorArrowRemovalState(target, slot, arrow, ticks + 1);
        }
    }
}
