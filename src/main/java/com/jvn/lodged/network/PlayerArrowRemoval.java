package com.jvn.lodged.network;

import com.jvn.lodged.config.LodgedConfig;
import com.jvn.lodged.effect.BleedingEvents;
import com.jvn.lodged.effect.LodgedDamageTypes;
import com.jvn.lodged.network.payload.ArmorArrowRemovalActionPayload;
import com.jvn.lodged.network.payload.ArrowRemovalResultPayload;
import com.jvn.lodged.network.payload.ArrowRemovalResultPayload.Result;
import com.jvn.lodged.network.payload.RemovePlayerArrowPayload;
import com.jvn.lodged.network.payload.RemovePlayerArrowPayload.Target;
import com.jvn.lodged.network.payload.ShieldArrowRemovalActionPayload;
import com.jvn.lodged.network.payload.ShieldArrowRemovalActionPayload.Action;
import com.jvn.lodged.network.payload.SyncArmorArrowRemovalPayload;
import com.jvn.lodged.network.payload.SyncShieldArrowRemovalPayload;
import com.jvn.lodged.world.LodgedArmorArrowStorage;
import com.jvn.lodged.world.LodgedArmorArrowStorage.LodgedArmorArrowData;
import com.jvn.lodged.world.LodgedArrowData;
import com.jvn.lodged.world.LodgedArrowRemovalScoring;
import com.jvn.lodged.world.LodgedArrowVisual;
import com.jvn.lodged.world.LodgedArrowStorage;
import com.jvn.lodged.world.LodgedShieldArrowStorage;
import com.jvn.lodged.world.LodgedShieldArrowStorage.LodgedShieldArrowData;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.ItemAbilities;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public final class PlayerArrowRemoval {
    private static final int REQUEST_COOLDOWN_TICKS = 2;
    private static final int SHIELD_ARROW_BREAK_PARTICLES = 12;
    private static final int ARMOR_ARROW_BREAK_PARTICLES = 10;
    private static final int NO_INVENTORY_SLOT = -1;
    private static final double SHIELD_CENTER_HEIGHT_RATIO = 0.55D;
    private static final double SHIELD_CENTER_FORWARD_OFFSET = 0.35D;
    private static final double SHIELD_HAND_SIDE_OFFSET = 0.28D;
    private static final Map<UUID, Long> LAST_REQUEST_TICK = new HashMap<>();
    private static final Map<UUID, ShieldArrowRemovalAttempt> SHIELD_ARROW_REMOVALS = new HashMap<>();
    private static final Map<UUID, ArmorArrowRemovalAttempt> ARMOR_ARROW_REMOVALS = new HashMap<>();

    private PlayerArrowRemoval() {
    }

    static void handleRequest(RemovePlayerArrowPayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) {
            return;
        }

        if (!canAttemptRemoval(player, payload)) {
            sendRemovalResult(player, Result.CANT_REMOVE_NOW, payload.target(), payload.hand(), LodgedArrowVisual.DEFAULT);
            LodgedNetwork.syncPlayerArrows(player);
            return;
        }

        long gameTime = player.level().getGameTime();
        Long lastRequestTick = LAST_REQUEST_TICK.get(player.getUUID());
        if (lastRequestTick != null && gameTime - lastRequestTick < REQUEST_COOLDOWN_TICKS) {
            sendRemovalResult(player, Result.CANT_REMOVE_NOW, payload.target(), payload.hand(), LodgedArrowVisual.DEFAULT);
            return;
        }
        LAST_REQUEST_TICK.put(player.getUUID(), gameTime);

        if (payload.target() == Target.SHIELD) {
            removeShieldArrow(player, payload.hand(), payload.arrowIndex(), false);
            return;
        }

        if (payload.target() == Target.ARMOR) {
            removeArmorArrow(player, payload.armorSlot(), payload.arrowIndex(), false);
            return;
        }

        removeBodyArrow(player, payload.arrowIndex());
    }

    static void handleShieldAction(ShieldArrowRemovalActionPayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) {
            return;
        }

        if (payload.action() == Action.CANCEL) {
            stopShieldArrowRemoval(player);
            return;
        }

        startShieldArrowRemoval(player, payload.hand());
    }

    static void handleArmorAction(ArmorArrowRemovalActionPayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) {
            return;
        }

        if (payload.action() == ArmorArrowRemovalActionPayload.Action.CANCEL) {
            stopArmorArrowRemoval(player);
            return;
        }

        startInWorldArrowRemoval(player);
    }

    public static void onEntityTick(EntityTickEvent.Post event) {
        Entity entity = event.getEntity();
        if (entity.level().isClientSide() || !(entity instanceof ServerPlayer player)) {
            return;
        }

        tickShieldArrowRemoval(player);
        tickArmorArrowRemoval(player);
    }

    static void clearCooldown(ServerPlayer player) {
        LAST_REQUEST_TICK.remove(player.getUUID());
        stopShieldArrowRemoval(player);
        stopArmorArrowRemoval(player);
    }

    public static boolean isRemovingShieldArrow(LivingEntity entity) {
        return SHIELD_ARROW_REMOVALS.containsKey(entity.getUUID());
    }

    public static void syncShieldArrowRemovalToPlayer(ServerPlayer viewer, LivingEntity entity) {
        ShieldArrowRemovalAttempt attempt = SHIELD_ARROW_REMOVALS.get(entity.getUUID());
        if (attempt != null) {
            PacketDistributor.sendToPlayer(
                    viewer,
                    new SyncShieldArrowRemovalPayload(entity.getId(), true, attempt.hand()));
        }
        ArmorArrowRemovalAttempt armorAttempt = ARMOR_ARROW_REMOVALS.get(entity.getUUID());
        if (armorAttempt != null) {
            PacketDistributor.sendToPlayer(
                    viewer,
                    new SyncArmorArrowRemovalPayload(
                            entity.getId(),
                            true,
                            armorAttempt.target(),
                            armorAttempt.slot(),
                            armorAttempt.visual()));
        }
    }

    private static void removeBodyArrow(ServerPlayer player, int arrowIndex) {
        List<LodgedArrowData> arrows = LodgedArrowStorage.readAll(player);
        if (arrowIndex >= arrows.size()) {
            sendRemovalResult(player, Result.CANT_REMOVE_NOW, Target.BODY, InteractionHand.MAIN_HAND, LodgedArrowVisual.DEFAULT);
            LodgedNetwork.syncPlayerArrows(player);
            return;
        }

        LodgedArrowData arrow = arrows.get(arrowIndex);
        double successChance = removalSuccessChance(arrow);
        if (successChance <= 0.0D) {
            sendRemovalResult(player, Result.TOO_RISKY, Target.BODY, InteractionHand.MAIN_HAND, arrow.visual());
            return;
        }

        LodgedArrowData removedArrow = LodgedArrowStorage.removeAt(player, arrowIndex);
        if (removedArrow == null) {
            sendRemovalResult(player, Result.CANT_REMOVE_NOW, Target.BODY, InteractionHand.MAIN_HAND, arrow.visual());
            LodgedNetwork.syncPlayerArrows(player);
            return;
        }

        boolean recovered = canRecoverArrow(removedArrow)
                && (successChance >= 1.0D || (successChance > 0.0D && player.getRandom().nextDouble() < successChance));
        if (recovered) {
            ArrowRecoveryResult recoveryResult = recoverArrow(player, removedArrow.stack());
            sendRemovalResult(
                    player,
                    recoveryResult.inventoryFull() ? Result.INVENTORY_FULL : Result.SUCCESS,
                    Target.BODY,
                    InteractionHand.MAIN_HAND,
                    removedArrow.visual(),
                    recoveryResult.inventorySlot());
            playSound(player, SoundEvents.ITEM_PICKUP, 0.2F, 2.0F);
        } else {
            damageForBrokenArrow(player);
            sendRemovalResult(player, Result.FAILED, Target.BODY, InteractionHand.MAIN_HAND, removedArrow.visual());
            playSound(player, SoundEvents.ITEM_BREAK, 0.8F, 1.0F);
        }
        if (removedArrow.causesBleeding()) {
            BleedingEvents.tryApplyFromArrowRemoval(player, removedArrow.visual(), !recovered);
        }

        LodgedNetwork.syncPlayerArrows(player);
        LodgedNetwork.syncEntityArrows(player);
    }

    private static void removeShieldArrow(ServerPlayer player, InteractionHand hand, int arrowIndex, boolean spawnBreakParticles) {
        ItemStack shield = player.getItemInHand(hand);
        List<LodgedShieldArrowData> arrows = LodgedShieldArrowStorage.readData(shield, player.registryAccess());
        if (arrowIndex >= arrows.size()) {
            sendRemovalResult(player, Result.CANT_REMOVE_NOW, Target.SHIELD, hand, LodgedArrowVisual.DEFAULT);
            LodgedNetwork.syncPlayerArrows(player);
            return;
        }

        LodgedShieldArrowData arrow = arrows.get(arrowIndex);
        double successChance = removalSuccessChance(arrow);
        if (successChance <= 0.0D) {
            sendRemovalResult(player, Result.TOO_RISKY, Target.SHIELD, hand, arrow.visual());
            return;
        }

        LodgedShieldArrowData removedArrow = LodgedShieldArrowStorage.removeAt(shield, arrowIndex, player.registryAccess());
        if (removedArrow == null) {
            sendRemovalResult(player, Result.CANT_REMOVE_NOW, Target.SHIELD, hand, arrow.visual());
            LodgedNetwork.syncPlayerArrows(player);
            return;
        }

        boolean recovered = canRecoverArrow(removedArrow)
                && (successChance >= 1.0D || (successChance > 0.0D && player.getRandom().nextDouble() < successChance));
        if (recovered) {
            ArrowRecoveryResult recoveryResult = recoverArrow(player, removedArrow.stack());
            sendRemovalResult(
                    player,
                    recoveryResult.inventoryFull() ? Result.INVENTORY_FULL : Result.SUCCESS,
                    Target.SHIELD,
                    hand,
                    removedArrow.visual(),
                    recoveryResult.inventorySlot());
            playSound(player, SoundEvents.ITEM_PICKUP, 0.2F, 2.0F);
        } else {
            sendRemovalResult(player, Result.FAILED, Target.SHIELD, hand, removedArrow.visual());
            playSound(player, SoundEvents.ITEM_BREAK, 0.8F, 1.0F);
        }

        if (spawnBreakParticles) {
            spawnShieldArrowBreakParticles(player, hand, removedArrow.visual());
        }

        maybeDamageShieldFromArrowRemoval(player, shield, LivingEntity.getSlotForHand(hand));
        player.setItemInHand(hand, shield);
        player.containerMenu.broadcastChanges();
    }

    private static void removeArmorArrow(
            ServerPlayer player,
            EquipmentSlot slot,
            int arrowIndex,
            boolean spawnBreakParticles) {
        ItemStack armor = player.getItemBySlot(slot);
        List<LodgedArmorArrowData> arrows = LodgedArmorArrowStorage.readData(armor, player.registryAccess());
        if (arrowIndex >= arrows.size()) {
            sendRemovalResult(player, Result.CANT_REMOVE_NOW, Target.ARMOR, InteractionHand.MAIN_HAND, LodgedArrowVisual.DEFAULT);
            return;
        }

        LodgedArmorArrowData arrow = arrows.get(arrowIndex);
        double successChance = removalSuccessChance(arrow);
        if (successChance <= 0.0D) {
            sendRemovalResult(player, Result.TOO_RISKY, Target.ARMOR, InteractionHand.MAIN_HAND, arrow.visual());
            return;
        }

        LodgedArmorArrowData removedArrow = LodgedArmorArrowStorage.removeAt(armor, arrowIndex, player.registryAccess());
        if (removedArrow == null) {
            sendRemovalResult(player, Result.CANT_REMOVE_NOW, Target.ARMOR, InteractionHand.MAIN_HAND, arrow.visual());
            return;
        }

        boolean broke = player.getRandom().nextDouble() < LodgedConfig.armorArrowRemovalBreakChance();
        boolean recovered = !broke
                && canRecoverArrow(removedArrow)
                && (successChance >= 1.0D || (successChance > 0.0D && player.getRandom().nextDouble() < successChance));
        if (recovered) {
            ArrowRecoveryResult recoveryResult = recoverArrow(player, removedArrow.stack());
            sendRemovalResult(
                    player,
                    recoveryResult.inventoryFull() ? Result.INVENTORY_FULL : Result.SUCCESS,
                    Target.ARMOR,
                    InteractionHand.MAIN_HAND,
                    removedArrow.visual(),
                    recoveryResult.inventorySlot());
            playSound(player, SoundEvents.ITEM_PICKUP, 0.2F, 2.0F);
            maybeDamageArmorFromArrowRemoval(player, armor, slot, LodgedConfig.armorArrowRemovalDurabilityDamageChance());
        } else {
            sendRemovalResult(player, Result.FAILED, Target.ARMOR, InteractionHand.MAIN_HAND, removedArrow.visual());
            playSound(player, SoundEvents.ITEM_BREAK, 0.8F, 1.0F);
            maybeDamageArmorFromArrowRemoval(
                    player,
                    armor,
                    slot,
                    broke
                            ? LodgedConfig.armorArrowBreakDurabilityDamageChance()
                            : LodgedConfig.armorArrowRemovalDurabilityDamageChance());
        }

        if (spawnBreakParticles && !recovered) {
            spawnArmorArrowBreakParticles(player, removedArrow.visual());
        }

        player.setItemSlot(slot, armor);
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
        } else if (payload.target() == Target.SHIELD) {
            int maxShieldArrows = LodgedConfig.maxTrackedArrowsPerShield();
            ItemStack shield = player.getItemInHand(payload.hand());
            if (maxShieldArrows <= 0
                    || payload.arrowIndex() >= maxShieldArrows
                    || shield.isEmpty()
                    || !shield.canPerformAction(ItemAbilities.SHIELD_BLOCK)
                    || payload.arrowIndex() >= LodgedShieldArrowStorage.readData(shield, player.registryAccess()).size()) {
                return false;
            }
        } else if (payload.target() == Target.ARMOR) {
            if (!isArmorSlot(payload.armorSlot())
                    || payload.arrowIndex() >= LodgedConfig.maxTrackedArrowsPerArmorPiece()
                    || !canAttemptArmorRemoval(player, payload.armorSlot(), payload.arrowIndex())) {
                return false;
            }
        } else {
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

    private static void startShieldArrowRemoval(ServerPlayer player, InteractionHand hand) {
        if (SHIELD_ARROW_REMOVALS.containsKey(player.getUUID())
                || ARMOR_ARROW_REMOVALS.containsKey(player.getUUID())) {
            return;
        }

        ItemStack shield = player.getItemInHand(hand);
        if (!canAttemptHeldShieldRemoval(player, shield)) {
            return;
        }

        List<LodgedShieldArrowData> arrows = LodgedShieldArrowStorage.readData(shield, player.registryAccess());
        if (arrows.isEmpty()) {
            return;
        }

        int arrowIndex = priorityShieldArrowIndex(arrows);
        long gameTime = player.level().getGameTime();
        long completeTick = gameTime + shieldArrowRemovalTicks(player);
        SHIELD_ARROW_REMOVALS.put(player.getUUID(), new ShieldArrowRemovalAttempt(hand, arrowIndex, completeTick));
        syncShieldArrowRemoval(player, true, hand);
    }

    private static void startInWorldArrowRemoval(ServerPlayer player) {
        if (SHIELD_ARROW_REMOVALS.containsKey(player.getUUID())
                || ARMOR_ARROW_REMOVALS.containsKey(player.getUUID())
                || player.isUsingItem()) {
            return;
        }

        InWorldArrowTarget target = priorityArmorArrow(player);
        if (target == null) {
            target = priorityBodyArrow(player);
        }
        if (target == null) {
            return;
        }

        long gameTime = player.level().getGameTime();
        long completeTick = gameTime + inWorldRemovalTicks(player, target.target());
        ARMOR_ARROW_REMOVALS.put(
                player.getUUID(),
                new ArmorArrowRemovalAttempt(
                        target.target(),
                        target.slot(),
                        target.arrowIndex(),
                        completeTick,
                        target.visual()));
        syncArmorArrowRemoval(player, true, target.target(), target.slot(), target.visual());
    }

    private static void tickShieldArrowRemoval(ServerPlayer player) {
        ShieldArrowRemovalAttempt attempt = SHIELD_ARROW_REMOVALS.get(player.getUUID());
        if (attempt == null) {
            return;
        }

        ItemStack shield = player.getItemInHand(attempt.hand());
        if (!canAttemptHeldShieldRemoval(player, shield, attempt.arrowIndex())
                || !player.isUsingItem()
                || player.getUsedItemHand() != attempt.hand()) {
            stopShieldArrowRemoval(player);
            return;
        }

        if (player.level().getGameTime() < attempt.completeTick()) {
            return;
        }

        stopShieldArrowRemoval(player);
        removeShieldArrow(player, attempt.hand(), attempt.arrowIndex(), true);
    }

    private static void tickArmorArrowRemoval(ServerPlayer player) {
        ArmorArrowRemovalAttempt attempt = ARMOR_ARROW_REMOVALS.get(player.getUUID());
        if (attempt == null) {
            return;
        }

        if (!canAttemptInWorldRemoval(player, attempt) || player.isUsingItem()) {
            stopArmorArrowRemoval(player);
            return;
        }

        if (player.level().getGameTime() < attempt.completeTick()) {
            return;
        }

        stopArmorArrowRemoval(player);
        if (attempt.target() == Target.BODY) {
            removeBodyArrow(player, attempt.arrowIndex());
        } else {
            removeArmorArrow(player, attempt.slot(), attempt.arrowIndex(), true);
        }
    }

    private static int inWorldRemovalTicks(ServerPlayer player, Target target) {
        if (target == Target.ARMOR) {
            return LodgedConfig.armorArrowRemovalTicks();
        }

        return inWorldBodyArrowRemovalTicks(player);
    }

    private static int inWorldBodyArrowRemovalTicks(ServerPlayer player) {
        int minTicks = LodgedConfig.inWorldBodyArrowRemovalMinTicks();
        int maxTicks = LodgedConfig.inWorldBodyArrowRemovalMaxTicks();
        return minTicks + player.getRandom().nextInt(maxTicks - minTicks + 1);
    }

    private static int shieldArrowRemovalTicks(ServerPlayer player) {
        int minTicks = LodgedConfig.shieldArrowRemovalMinTicks();
        int maxTicks = LodgedConfig.shieldArrowRemovalMaxTicks();
        return minTicks + player.getRandom().nextInt(maxTicks - minTicks + 1);
    }

    private static void stopShieldArrowRemoval(ServerPlayer player) {
        ShieldArrowRemovalAttempt removed = SHIELD_ARROW_REMOVALS.remove(player.getUUID());
        if (removed != null) {
            syncShieldArrowRemoval(player, false, removed.hand());
        }
    }

    private static void stopArmorArrowRemoval(ServerPlayer player) {
        ArmorArrowRemovalAttempt removed = ARMOR_ARROW_REMOVALS.remove(player.getUUID());
        if (removed != null) {
            syncArmorArrowRemoval(player, false, removed.target(), removed.slot(), removed.visual());
        }
    }

    private static void syncShieldArrowRemoval(ServerPlayer player, boolean active, InteractionHand hand) {
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(
                player,
                new SyncShieldArrowRemovalPayload(player.getId(), active, hand));
    }

    private static void syncArmorArrowRemoval(
            ServerPlayer player,
            boolean active,
            Target target,
            EquipmentSlot slot,
            LodgedArrowVisual arrow) {
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(
                player,
                new SyncArmorArrowRemovalPayload(player.getId(), active, target, slot, arrow));
    }

    private static boolean canAttemptHeldShieldRemoval(ServerPlayer player, ItemStack shield) {
        return canAttemptHeldShieldRemoval(player, shield, 0);
    }

    private static boolean canAttemptHeldShieldRemoval(ServerPlayer player, ItemStack shield, int arrowIndex) {
        if (!LodgedConfig.enablePlayerArrowRemoval()
                || !player.isAlive()
                || shield.isEmpty()
                || !shield.canPerformAction(ItemAbilities.SHIELD_BLOCK)
                || LodgedConfig.maxTrackedArrowsPerShield() <= 0) {
            return false;
        }

        if (player.isCreative() && !LodgedConfig.allowArrowRemovalInCreative()) {
            return false;
        }

        return LodgedShieldArrowStorage.readData(shield, player.registryAccess()).size() > arrowIndex;
    }

    private static boolean canAttemptArmorRemoval(ServerPlayer player, EquipmentSlot slot, int arrowIndex) {
        if (!LodgedConfig.enablePlayerArrowRemoval()
                || !LodgedConfig.enableArmorArrowLodging()
                || !player.isAlive()
                || LodgedConfig.maxTrackedArrowsPerArmorPiece() <= 0) {
            return false;
        }

        if (player.isCreative() && !LodgedConfig.allowArrowRemovalInCreative()) {
            return false;
        }

        ItemStack armor = player.getItemBySlot(slot);
        return LodgedArmorArrowStorage.readData(armor, player.registryAccess()).size() > arrowIndex;
    }

    private static boolean isArmorSlot(EquipmentSlot slot) {
        return slot == EquipmentSlot.HEAD
                || slot == EquipmentSlot.CHEST
                || slot == EquipmentSlot.LEGS
                || slot == EquipmentSlot.FEET;
    }

    private static boolean canAttemptInWorldRemoval(ServerPlayer player, ArmorArrowRemovalAttempt attempt) {
        if (attempt.target() == Target.BODY) {
            return canAttemptBodyRemoval(player, attempt.arrowIndex());
        }

        return canAttemptArmorRemoval(player, attempt.slot(), attempt.arrowIndex());
    }

    private static boolean canAttemptBodyRemoval(ServerPlayer player, int arrowIndex) {
        if (!LodgedConfig.enablePlayerArrowRemoval() || !player.isAlive() || arrowIndex < 0) {
            return false;
        }

        if (player.isCreative() && !LodgedConfig.allowArrowRemovalInCreative()) {
            return false;
        }

        return arrowIndex < LodgedConfig.maxRemovablePlayerArrows()
                && arrowIndex < LodgedArrowStorage.readAll(player).size();
    }

    private static int priorityShieldArrowIndex(List<LodgedShieldArrowData> arrows) {
        int bestIndex = 0;
        double bestScore = Double.MAX_VALUE;
        for (int index = 0; index < arrows.size(); index++) {
            LodgedArrowVisual visual = arrows.get(index).visual();
            double score = Math.abs(visual.modelX()) + Math.abs(visual.modelY() * 0.75D);
            if (score < bestScore) {
                bestScore = score;
                bestIndex = index;
            }
        }
        return bestIndex;
    }

    private static InWorldArrowTarget priorityArmorArrow(ServerPlayer player) {
        InWorldArrowTarget best = null;
        double bestScore = Double.MAX_VALUE;
        for (EquipmentSlot slot : LodgedArmorArrowStorage.armorSlots()) {
            ItemStack armor = player.getItemBySlot(slot);
            List<LodgedArmorArrowData> arrows = LodgedArmorArrowStorage.readData(armor, player.registryAccess());
            for (int index = 0; index < arrows.size(); index++) {
                LodgedArrowVisual visual = arrows.get(index).visual();
                double score = LodgedArrowRemovalScoring.armorArrowPriorityScore(slot, visual, player.getMainArm());
                if (score < bestScore) {
                    bestScore = score;
                    best = new InWorldArrowTarget(Target.ARMOR, slot, index, visual);
                }
            }
        }
        return best;
    }

    private static InWorldArrowTarget priorityBodyArrow(ServerPlayer player) {
        List<LodgedArrowData> arrows = LodgedArrowStorage.readAll(player);
        int arrowCount = Math.min(arrows.size(), LodgedConfig.maxRemovablePlayerArrows());
        InWorldArrowTarget best = null;
        double bestScore = Double.MAX_VALUE;
        for (int index = 0; index < arrowCount; index++) {
            LodgedArrowData arrow = arrows.get(index);
            double score = LodgedArrowRemovalScoring.bodyArrowPriorityScore(
                    arrow.visual(),
                    removalSuccessChance(arrow));
            if (score < bestScore) {
                bestScore = score;
                best = new InWorldArrowTarget(Target.BODY, EquipmentSlot.CHEST, index, arrow.visual());
            }
        }
        return best;
    }

    private static ArrowRecoveryResult recoverArrow(Player player, ItemStack storedStack) {
        ItemStack recoveredStack = storedStack.copyWithCount(1);
        if (recoveredStack.isEmpty()) {
            return new ArrowRecoveryResult(false, NO_INVENTORY_SLOT);
        }

        int inventorySlot = player.getInventory().getSlotWithRemainingSpace(recoveredStack);
        if (inventorySlot == -1) {
            inventorySlot = player.getInventory().getFreeSlot();
        }

        if (inventorySlot >= 0 && player.getInventory().add(inventorySlot, recoveredStack)) {
            return new ArrowRecoveryResult(false, inventorySlot);
        }

        if (!recoveredStack.isEmpty()) {
            player.drop(recoveredStack, false);
            return new ArrowRecoveryResult(true, NO_INVENTORY_SLOT);
        }
        return new ArrowRecoveryResult(false, inventorySlot);
    }

    private static double removalSuccessChance(LodgedArrowData arrow) {
        double baseSuccessChance = LodgedConfig.playerArrowRemovalSuccessChance(arrow.visual().bodyPart())
                * LodgedConfig.arrowDepthRemovalSuccessMultiplier(arrow.visual().depth());
        return removalSuccessChance(arrow.infinityGenerated(), baseSuccessChance);
    }

    private static double removalSuccessChance(LodgedShieldArrowData arrow) {
        double successChance = LodgedConfig.shieldArrowRemovalSuccessChance();
        return removalSuccessChance(arrow.infinityGenerated(), successChance);
    }

    private static double removalSuccessChance(LodgedArmorArrowData arrow) {
        double successChance = LodgedConfig.armorArrowRemovalSuccessChance();
        return removalSuccessChance(arrow.infinityGenerated(), successChance);
    }

    private static double removalSuccessChance(boolean infinityGenerated, double baseSuccessChance) {
        if (infinityGenerated && LodgedConfig.recoverInfinityArrows()) {
            return baseSuccessChance * LodgedConfig.playerArrowRemovalInfinitySuccessMultiplier();
        }
        return baseSuccessChance;
    }

    private static boolean canRecoverArrow(LodgedArrowData arrow) {
        return arrow.recoverable()
                && canRecoverArrow(
                        arrow.fromPlayer(),
                        arrow.infinityGenerated(),
                        arrow.creativeGenerated(),
                        LodgedConfig.recoverMobArrows());
    }

    private static boolean canRecoverArrow(LodgedShieldArrowData arrow) {
        return arrow.recoverable()
                && canRecoverArrow(
                        arrow.fromPlayer(),
                        arrow.infinityGenerated(),
                        arrow.creativeGenerated(),
                        LodgedConfig.recoverMobShieldArrows());
    }

    private static boolean canRecoverArrow(LodgedArmorArrowData arrow) {
        return arrow.recoverable()
                && canRecoverArrow(
                        arrow.fromPlayer(),
                        arrow.infinityGenerated(),
                        arrow.creativeGenerated(),
                        LodgedConfig.recoverMobArmorArrows());
    }

    private static boolean canRecoverArrow(
            boolean fromPlayer,
            boolean infinityGenerated,
            boolean creativeGenerated,
            boolean recoverMobArrows) {
        if (!fromPlayer && !recoverMobArrows) {
            return false;
        }

        if (infinityGenerated && !LodgedConfig.recoverInfinityArrows()) {
            return false;
        }

        return !creativeGenerated || LodgedConfig.recoverCreativeArrows();
    }

    private static void damageForBrokenArrow(ServerPlayer player) {
        float damage = LodgedConfig.playerArrowRemovalBreakDamage();
        if (damage > 0.0F) {
            Holder<DamageType> arrowRemoval = player.level().registryAccess()
                    .registryOrThrow(Registries.DAMAGE_TYPE)
                    .getHolderOrThrow(LodgedDamageTypes.ARROW_REMOVAL);
            Vec3 previousMovement = player.getDeltaMovement();
            player.hurt(new DamageSource(arrowRemoval), damage);
            if (!LodgedConfig.arrowRemovalAppliesKnockback()) {
                player.setDeltaMovement(previousMovement);
            }
        }
    }

    private static void maybeDamageShieldFromArrowRemoval(ServerPlayer player, ItemStack shield, EquipmentSlot slot) {
        if (shield.isEmpty()
                || player.getAbilities().instabuild
                || player.getRandom().nextDouble() >= LodgedConfig.shieldArrowDurabilityDamageChance()) {
            return;
        }

        shield.hurtAndBreak(1, player, slot);
    }

    private static void maybeDamageArmorFromArrowRemoval(
            ServerPlayer player,
            ItemStack armor,
            EquipmentSlot slot,
            double chance) {
        if (armor.isEmpty()
                || chance <= 0.0D
                || player.getAbilities().instabuild
                || player.getRandom().nextDouble() >= chance) {
            return;
        }

        armor.hurtAndBreak(1, player, slot);
    }

    private static void spawnShieldArrowBreakParticles(ServerPlayer player, InteractionHand hand, LodgedArrowVisual arrow) {
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }

        Vec3 location = shieldArrowWorldPosition(player, hand, arrow);
        level.sendParticles(
                new BlockParticleOption(ParticleTypes.BLOCK, Blocks.OAK_PLANKS.defaultBlockState()),
                location.x,
                location.y,
                location.z,
                SHIELD_ARROW_BREAK_PARTICLES,
                0.04D,
                0.04D,
                0.04D,
                0.04D);
    }

    private static void spawnArmorArrowBreakParticles(ServerPlayer player, LodgedArrowVisual arrow) {
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }

        Vec3 location = armorArrowWorldPosition(player, arrow);
        level.sendParticles(
                new BlockParticleOption(ParticleTypes.BLOCK, Blocks.IRON_BLOCK.defaultBlockState()),
                location.x,
                location.y,
                location.z,
                ARMOR_ARROW_BREAK_PARTICLES,
                0.035D,
                0.035D,
                0.035D,
                0.035D);
    }

    private static Vec3 shieldArrowWorldPosition(ServerPlayer player, InteractionHand hand, LodgedArrowVisual arrow) {
        float yawRadians = player.getYHeadRot() * ((float) Math.PI / 180.0F);
        Vec3 right = new Vec3(Math.cos(yawRadians), 0.0D, Math.sin(yawRadians));
        Vec3 forward = new Vec3(-Math.sin(yawRadians), 0.0D, Math.cos(yawRadians));
        HumanoidArm shieldArm = hand == InteractionHand.MAIN_HAND
                ? player.getMainArm()
                : player.getMainArm().getOpposite();
        double side = shieldArm == HumanoidArm.RIGHT ? -1.0D : 1.0D;
        Vec3 shieldCenter = new Vec3(
                player.getX(),
                player.getY() + player.getBbHeight() * SHIELD_CENTER_HEIGHT_RATIO,
                player.getZ())
                .add(forward.scale(SHIELD_CENTER_FORWARD_OFFSET))
                .add(right.scale(side * SHIELD_HAND_SIDE_OFFSET));
        return shieldCenter
                .add(right.scale(arrow.modelX()))
                .add(0.0D, -arrow.modelY(), 0.0D)
                .add(forward.scale(-arrow.modelZ()));
    }

    private static Vec3 armorArrowWorldPosition(ServerPlayer player, LodgedArrowVisual arrow) {
        float yawRadians = player.yBodyRot * ((float) Math.PI / 180.0F);
        Vec3 right = new Vec3(Math.cos(yawRadians), 0.0D, Math.sin(yawRadians));
        Vec3 forward = new Vec3(-Math.sin(yawRadians), 0.0D, Math.cos(yawRadians));
        Vec3 local = arrow.toEntityLocalPosition(player);
        return new Vec3(player.getX(), player.getY(), player.getZ())
                .add(right.scale(local.x))
                .add(0.0D, local.y, 0.0D)
                .add(forward.scale(local.z));
    }

    private static void playSound(ServerPlayer player, net.minecraft.sounds.SoundEvent sound, float volume, float pitch) {
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), sound, SoundSource.PLAYERS, volume, pitch);
    }

    private static void sendRemovalResult(
            ServerPlayer player,
            Result result,
            Target target,
            InteractionHand hand,
            LodgedArrowVisual arrow) {
        sendRemovalResult(player, result, target, hand, arrow, NO_INVENTORY_SLOT);
    }

    private static void sendRemovalResult(
            ServerPlayer player,
            Result result,
            Target target,
            InteractionHand hand,
            LodgedArrowVisual arrow,
            int inventorySlot) {
        PacketDistributor.sendToPlayer(player, new ArrowRemovalResultPayload(result, target, hand, arrow, inventorySlot));
    }

    private record ShieldArrowRemovalAttempt(InteractionHand hand, int arrowIndex, long completeTick) {
    }

    private record ArmorArrowRemovalAttempt(
            Target target,
            EquipmentSlot slot,
            int arrowIndex,
            long completeTick,
            LodgedArrowVisual visual) {
    }

    private record InWorldArrowTarget(
            Target target,
            EquipmentSlot slot,
            int arrowIndex,
            LodgedArrowVisual visual) {
    }

    private record ArrowRecoveryResult(boolean inventoryFull, int inventorySlot) {
    }
}
