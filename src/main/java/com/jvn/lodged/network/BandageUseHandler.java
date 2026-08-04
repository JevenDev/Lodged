package com.jvn.lodged.network;

import com.jvn.lodged.effect.LodgedEffects;
import com.jvn.lodged.item.BandageItem;
import com.jvn.lodged.item.LodgedItems;
import com.jvn.lodged.network.payload.SyncBandageUsePayload;
import com.jvn.lodged.network.payload.UseBandagePayload;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public final class BandageUseHandler {
    private static final Map<UUID, InventoryBandageUse> INVENTORY_USES = new HashMap<>();
    private static final Map<UUID, InteractionHand> KEYBOUND_HAND_USES = new HashMap<>();

    private BandageUseHandler() {
    }

    static void handleRequest(UseBandagePayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) {
            return;
        }

        if (payload.action() == UseBandagePayload.Action.CANCEL) {
            stopKeyboundUse(player);
            return;
        }

        startKeyboundUse(player);
    }

    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        tickHandUse(player);
        tickInventoryUse(player);
    }

    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            INVENTORY_USES.remove(player.getUUID());
            KEYBOUND_HAND_USES.remove(player.getUUID());
        }
    }

    public static void onPlayerStartTracking(PlayerEvent.StartTracking event) {
        if (!(event.getEntity() instanceof ServerPlayer viewer)
                || !(event.getTarget() instanceof ServerPlayer target)) {
            return;
        }

        InventoryBandageUse use = INVENTORY_USES.get(target.getUUID());
        if (use != null) {
            PacketDistributor.sendToPlayer(
                    viewer,
                    new SyncBandageUsePayload(target.getId(), true, use.hand()));
        }
    }

    public static void onServerStopped(ServerStoppedEvent event) {
        INVENTORY_USES.clear();
        KEYBOUND_HAND_USES.clear();
    }

    private static void startKeyboundUse(ServerPlayer player) {
        if (!canStart(player)) {
            return;
        }

        Inventory inventory = player.getInventory();
        if (player.getMainHandItem().is(LodgedItems.BANDAGE.get())) {
            startHandUse(player, InteractionHand.MAIN_HAND);
            return;
        }

        for (int slot = 0; slot < Inventory.getSelectionSize(); slot++) {
            if (inventory.getItem(slot).is(LodgedItems.BANDAGE.get())) {
                startInventoryUse(player, slot);
                return;
            }
        }

        if (player.getOffhandItem().is(LodgedItems.BANDAGE.get())) {
            startHandUse(player, InteractionHand.OFF_HAND);
            return;
        }

        for (int slot = Inventory.getSelectionSize(); slot < Inventory.INVENTORY_SIZE; slot++) {
            if (inventory.getItem(slot).is(LodgedItems.BANDAGE.get())) {
                startInventoryUse(player, slot);
                return;
            }
        }
    }

    private static boolean canStart(ServerPlayer player) {
        return player.isAlive()
                && !player.isSpectator()
                && !player.isUsingItem()
                && player.hasEffect(LodgedEffects.BLEEDING)
                && !INVENTORY_USES.containsKey(player.getUUID())
                && !KEYBOUND_HAND_USES.containsKey(player.getUUID());
    }

    private static void startHandUse(ServerPlayer player, InteractionHand hand) {
        KEYBOUND_HAND_USES.put(player.getUUID(), hand);
        player.startUsingItem(hand);
    }

    private static void startInventoryUse(ServerPlayer player, int sourceSlot) {
        InventoryBandageUse use =
                new InventoryBandageUse(sourceSlot, InteractionHand.MAIN_HAND, 0);
        INVENTORY_USES.put(player.getUUID(), use);
        syncInventoryUse(player, true, use.hand());
    }

    private static void tickHandUse(ServerPlayer player) {
        InteractionHand hand = KEYBOUND_HAND_USES.get(player.getUUID());
        if (hand == null) {
            return;
        }

        if (!player.isUsingItem()
                || player.getUsedItemHand() != hand
                || !player.getUseItem().is(LodgedItems.BANDAGE.get())) {
            KEYBOUND_HAND_USES.remove(player.getUUID());
        }
    }

    private static void tickInventoryUse(ServerPlayer player) {
        InventoryBandageUse use = INVENTORY_USES.get(player.getUUID());
        if (use == null) {
            return;
        }

        ItemStack sourceStack = player.getInventory().getItem(use.sourceSlot());
        if (!player.isAlive()
                || player.isSpectator()
                || player.isUsingItem()
                || !player.hasEffect(LodgedEffects.BLEEDING)
                || !sourceStack.is(LodgedItems.BANDAGE.get())) {
            stopInventoryUse(player);
            return;
        }

        int ticks = use.ticks() + 1;
        if (ticks < BandageItem.USE_DURATION) {
            INVENTORY_USES.put(
                    player.getUUID(),
                    new InventoryBandageUse(use.sourceSlot(), use.hand(), ticks));
            return;
        }

        ItemStack result = sourceStack.finishUsingItem(player.level(), player);
        player.getInventory().setItem(use.sourceSlot(), result);
        broadcastInventoryChanges(player);
        stopInventoryUse(player);
    }

    private static void stopKeyboundUse(ServerPlayer player) {
        stopInventoryUse(player);

        InteractionHand hand = KEYBOUND_HAND_USES.remove(player.getUUID());
        if (hand != null
                && player.isUsingItem()
                && player.getUsedItemHand() == hand
                && player.getUseItem().is(LodgedItems.BANDAGE.get())) {
            player.releaseUsingItem();
        }
    }

    private static void stopInventoryUse(ServerPlayer player) {
        InventoryBandageUse use = INVENTORY_USES.remove(player.getUUID());
        if (use != null) {
            syncInventoryUse(player, false, use.hand());
        }
    }

    private static void syncInventoryUse(
            ServerPlayer player,
            boolean active,
            InteractionHand hand) {
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(
                player,
                new SyncBandageUsePayload(player.getId(), active, hand));
    }

    private static void broadcastInventoryChanges(ServerPlayer player) {
        player.inventoryMenu.broadcastChanges();
        if (player.containerMenu != player.inventoryMenu) {
            player.containerMenu.broadcastChanges();
        }
    }

    private record InventoryBandageUse(
            int sourceSlot,
            InteractionHand hand,
            int ticks) {
    }
}
