package com.jvn.lodged.network;

import com.jvn.lodged.config.LodgedConfig;
import com.jvn.lodged.effect.BleedingEvents;
import com.jvn.lodged.network.payload.RemovePlayerArrowPayload;
import com.jvn.lodged.world.LodgedArrowData;
import com.jvn.lodged.world.LodgedArrowStorage;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

final class PlayerArrowRemoval {
    private static final int REQUEST_COOLDOWN_TICKS = 2;
    private static final Map<UUID, Long> LAST_REQUEST_TICK = new HashMap<>();

    private PlayerArrowRemoval() {
    }

    static void handleRequest(RemovePlayerArrowPayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) {
            return;
        }

        if (!canAttemptRemoval(player, payload.arrowIndex())) {
            LodgedNetwork.syncPlayerArrows(player);
            return;
        }

        long gameTime = player.level().getGameTime();
        Long lastRequestTick = LAST_REQUEST_TICK.get(player.getUUID());
        if (lastRequestTick != null && gameTime - lastRequestTick < REQUEST_COOLDOWN_TICKS) {
            return;
        }
        LAST_REQUEST_TICK.put(player.getUUID(), gameTime);

        LodgedArrowData removedArrow = LodgedArrowStorage.removeAt(player, payload.arrowIndex());
        if (removedArrow == null) {
            LodgedNetwork.syncPlayerArrows(player);
            return;
        }

        double successChance = removalSuccessChance(removedArrow);
        boolean recovered = successChance >= 1.0D || (successChance > 0.0D && player.getRandom().nextDouble() < successChance);
        if (recovered) {
            recoverArrow(player, removedArrow.stack());
            playSound(player, SoundEvents.ITEM_PICKUP, 0.2F, 2.0F);
        } else {
            damageForBrokenArrow(player);
            playSound(player, SoundEvents.ITEM_BREAK, 0.8F, 1.0F);
        }
        BleedingEvents.tryApplyFromArrowRemoval(player, removedArrow.visual(), !recovered);

        LodgedNetwork.syncPlayerArrows(player);
        LodgedNetwork.syncEntityArrows(player);
    }

    static void clearCooldown(ServerPlayer player) {
        LAST_REQUEST_TICK.remove(player.getUUID());
    }

    private static boolean canAttemptRemoval(ServerPlayer player, int arrowIndex) {
        if (!LodgedConfig.enablePlayerArrowRemoval() || !player.isAlive()) {
            return false;
        }

        if (arrowIndex < 0 || arrowIndex >= LodgedConfig.maxRemovablePlayerArrows()) {
            return false;
        }

        if (arrowIndex >= LodgedArrowStorage.readAll(player).size()) {
            return false;
        }

        if (player.isCreative() && !LodgedConfig.allowArrowRemovalInCreative()) {
            return false;
        }

        if (LodgedConfig.requireInventoryScreenForRemoval() && !(player.containerMenu instanceof InventoryMenu)) {
            return false;
        }

        return true;
    }

    private static void recoverArrow(Player player, ItemStack storedStack) {
        ItemStack recoveredStack = storedStack.copyWithCount(1);
        if (recoveredStack.isEmpty()) {
            return;
        }

        if (!player.addItem(recoveredStack)) {
            player.drop(recoveredStack, false);
        }
    }

    private static double removalSuccessChance(LodgedArrowData arrow) {
        if (arrow.infinityGenerated() && !LodgedConfig.recoverInfinityArrows()) {
            return 0.0D;
        }

        double successChance = LodgedConfig.playerArrowRemovalSuccessChance(arrow.visual().bodyPart());
        if (arrow.infinityGenerated()) {
            successChance *= LodgedConfig.playerArrowRemovalInfinitySuccessMultiplier();
        }
        return successChance;
    }

    private static void damageForBrokenArrow(ServerPlayer player) {
        float damage = LodgedConfig.playerArrowRemovalBreakDamage();
        if (damage > 0.0F) {
            player.hurt(player.damageSources().generic(), damage);
        }
    }

    private static void playSound(ServerPlayer player, net.minecraft.sounds.SoundEvent sound, float volume, float pitch) {
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), sound, SoundSource.PLAYERS, volume, pitch);
    }
}
