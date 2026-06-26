package com.jvn.lodged.network;

import com.jvn.lodged.config.LodgedConfig;
import com.jvn.lodged.effect.BleedingEvents;
import com.jvn.lodged.effect.LodgedDamageTypes;
import com.jvn.lodged.network.payload.RemovePlayerArrowPayload;
import com.jvn.lodged.network.payload.RemovePlayerArrowPayload.Target;
import com.jvn.lodged.world.LodgedArrowBodyPart;
import com.jvn.lodged.world.LodgedArrowData;
import com.jvn.lodged.world.LodgedArrowStorage;
import com.jvn.lodged.world.LodgedShieldArrowStorage;
import com.jvn.lodged.world.LodgedShieldArrowStorage.LodgedShieldArrowData;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.ItemAbilities;
import net.neoforged.neoforge.network.handling.IPayloadContext;

final class PlayerArrowRemoval {
    private static final int REQUEST_COOLDOWN_TICKS = 2;
    private static final double SHIELD_ARROW_REMOVAL_DURABILITY_CHANCE = 0.35D;
    private static final Map<UUID, Long> LAST_REQUEST_TICK = new HashMap<>();

    private PlayerArrowRemoval() {
    }

    static void handleRequest(RemovePlayerArrowPayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) {
            return;
        }

        if (!canAttemptRemoval(player, payload)) {
            LodgedNetwork.syncPlayerArrows(player);
            return;
        }

        long gameTime = player.level().getGameTime();
        Long lastRequestTick = LAST_REQUEST_TICK.get(player.getUUID());
        if (lastRequestTick != null && gameTime - lastRequestTick < REQUEST_COOLDOWN_TICKS) {
            return;
        }
        LAST_REQUEST_TICK.put(player.getUUID(), gameTime);

        if (payload.target() == Target.SHIELD) {
            removeShieldArrow(player, payload);
            return;
        }

        removeBodyArrow(player, payload.arrowIndex());
    }

    static void clearCooldown(ServerPlayer player) {
        LAST_REQUEST_TICK.remove(player.getUUID());
    }

    private static void removeBodyArrow(ServerPlayer player, int arrowIndex) {
        LodgedArrowData removedArrow = LodgedArrowStorage.removeAt(player, arrowIndex);
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

    private static void removeShieldArrow(ServerPlayer player, RemovePlayerArrowPayload payload) {
        ItemStack shield = player.getItemInHand(payload.hand());
        LodgedShieldArrowData removedArrow = LodgedShieldArrowStorage.removeAt(shield, payload.arrowIndex(), player.registryAccess());
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
            playSound(player, SoundEvents.ITEM_BREAK, 0.8F, 1.0F);
        }

        maybeDamageShieldFromArrowRemoval(player, shield, LivingEntity.getSlotForHand(payload.hand()));
        player.setItemInHand(payload.hand(), shield);
        player.containerMenu.broadcastChanges();
    }

    private static boolean canAttemptRemoval(ServerPlayer player, RemovePlayerArrowPayload payload) {
        if (!LodgedConfig.enablePlayerArrowRemoval() || !player.isAlive()) {
            return false;
        }

        if (payload.arrowIndex() < 0) {
            return false;
        }

        if (payload.target() == Target.BODY) {
            if (payload.arrowIndex() >= LodgedConfig.maxRemovablePlayerArrows()
                    || payload.arrowIndex() >= LodgedArrowStorage.readAll(player).size()) {
                return false;
            }
        } else {
            int maxShieldArrows = LodgedConfig.maxTrackedArrowsPerShield();
            ItemStack shield = player.getItemInHand(payload.hand());
            if (maxShieldArrows <= 0
                    || payload.arrowIndex() >= maxShieldArrows
                    || shield.isEmpty()
                    || !shield.canPerformAction(ItemAbilities.SHIELD_BLOCK)
                    || payload.arrowIndex() >= LodgedShieldArrowStorage.readData(shield, player.registryAccess()).size()) {
                return false;
            }
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
        return removalSuccessChance(
                arrow.fromPlayer(),
                arrow.infinityGenerated(),
                arrow.creativeGenerated(),
                LodgedConfig.playerArrowRemovalSuccessChance(arrow.visual().bodyPart()));
    }

    private static double removalSuccessChance(LodgedShieldArrowData arrow) {
        if (!arrow.fromPlayer() && !LodgedConfig.recoverMobShieldArrows()) {
            return 0.0D;
        }

        if (arrow.infinityGenerated() && !LodgedConfig.recoverInfinityArrows()) {
            return 0.0D;
        }

        if (arrow.creativeGenerated() && !LodgedConfig.recoverCreativeArrows()) {
            return 0.0D;
        }

        double successChance = LodgedConfig.playerArrowRemovalSuccessChance(LodgedArrowBodyPart.ARM);
        if (arrow.infinityGenerated()) {
            successChance *= LodgedConfig.playerArrowRemovalInfinitySuccessMultiplier();
        }
        return successChance;
    }

    private static double removalSuccessChance(
            boolean fromPlayer,
            boolean infinityGenerated,
            boolean creativeGenerated,
            double baseSuccessChance) {
        if (!fromPlayer && !LodgedConfig.recoverMobArrows()) {
            return 0.0D;
        }

        if (infinityGenerated && !LodgedConfig.recoverInfinityArrows()) {
            return 0.0D;
        }

        if (creativeGenerated && !LodgedConfig.recoverCreativeArrows()) {
            return 0.0D;
        }

        double successChance = baseSuccessChance;
        if (infinityGenerated) {
            successChance *= LodgedConfig.playerArrowRemovalInfinitySuccessMultiplier();
        }
        return successChance;
    }

    private static void damageForBrokenArrow(ServerPlayer player) {
        float damage = LodgedConfig.playerArrowRemovalBreakDamage();
        if (damage > 0.0F) {
            Holder<DamageType> arrowRemoval = player.level().registryAccess()
                    .registryOrThrow(Registries.DAMAGE_TYPE)
                    .getHolderOrThrow(LodgedDamageTypes.ARROW_REMOVAL);
            player.hurt(new DamageSource(arrowRemoval), damage);
        }
    }

    private static void maybeDamageShieldFromArrowRemoval(ServerPlayer player, ItemStack shield, EquipmentSlot slot) {
        if (shield.isEmpty()
                || player.getAbilities().instabuild
                || player.getRandom().nextDouble() >= SHIELD_ARROW_REMOVAL_DURABILITY_CHANCE) {
            return;
        }

        shield.hurtAndBreak(1, player, slot);
    }

    private static void playSound(ServerPlayer player, net.minecraft.sounds.SoundEvent sound, float volume, float pitch) {
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), sound, SoundSource.PLAYERS, volume, pitch);
    }
}
