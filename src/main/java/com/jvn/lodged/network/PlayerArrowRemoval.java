package com.jvn.lodged.network;

import com.jvn.lodged.config.LodgedConfig;
import com.jvn.lodged.effect.BleedingEvents;
import com.jvn.lodged.effect.LodgedDamageTypes;
import com.jvn.lodged.network.payload.RemovePlayerArrowPayload;
import com.jvn.lodged.network.payload.RemovePlayerArrowPayload.Target;
import com.jvn.lodged.network.payload.ShieldArrowRemovalActionPayload;
import com.jvn.lodged.network.payload.ShieldArrowRemovalActionPayload.Action;
import com.jvn.lodged.network.payload.SyncShieldArrowRemovalPayload;
import com.jvn.lodged.world.LodgedArrowBodyPart;
import com.jvn.lodged.world.LodgedArrowData;
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
    private static final int SHIELD_ARROW_REMOVAL_MIN_TICKS = 40;
    private static final int SHIELD_ARROW_REMOVAL_RANDOM_TICKS = 21;
    private static final int SHIELD_ARROW_BREAK_PARTICLES = 12;
    private static final double SHIELD_CENTER_HEIGHT_RATIO = 0.55D;
    private static final double SHIELD_CENTER_FORWARD_OFFSET = 0.35D;
    private static final double SHIELD_HAND_SIDE_OFFSET = 0.28D;
    private static final double SHIELD_ARROW_REMOVAL_DURABILITY_CHANCE = 0.35D;
    private static final Map<UUID, Long> LAST_REQUEST_TICK = new HashMap<>();
    private static final Map<UUID, ShieldArrowRemovalAttempt> SHIELD_ARROW_REMOVALS = new HashMap<>();

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
            removeShieldArrow(player, payload.hand(), payload.arrowIndex(), false);
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

    public static void onEntityTick(EntityTickEvent.Post event) {
        Entity entity = event.getEntity();
        if (entity.level().isClientSide() || !(entity instanceof ServerPlayer player)) {
            return;
        }

        tickShieldArrowRemoval(player);
    }

    static void clearCooldown(ServerPlayer player) {
        LAST_REQUEST_TICK.remove(player.getUUID());
        stopShieldArrowRemoval(player);
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

    private static void removeShieldArrow(ServerPlayer player, InteractionHand hand, int arrowIndex, boolean spawnBreakParticles) {
        ItemStack shield = player.getItemInHand(hand);
        LodgedShieldArrowData removedArrow = LodgedShieldArrowStorage.removeAt(shield, arrowIndex, player.registryAccess());
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

        if (spawnBreakParticles) {
            spawnShieldArrowBreakParticles(player, hand, removedArrow.visual());
        }

        maybeDamageShieldFromArrowRemoval(player, shield, LivingEntity.getSlotForHand(hand));
        player.setItemInHand(hand, shield);
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

    private static void startShieldArrowRemoval(ServerPlayer player, InteractionHand hand) {
        if (SHIELD_ARROW_REMOVALS.containsKey(player.getUUID())) {
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

        int arrowIndex = player.getRandom().nextInt(arrows.size());
        long gameTime = player.level().getGameTime();
        long completeTick = gameTime + SHIELD_ARROW_REMOVAL_MIN_TICKS + player.getRandom().nextInt(SHIELD_ARROW_REMOVAL_RANDOM_TICKS);
        SHIELD_ARROW_REMOVALS.put(player.getUUID(), new ShieldArrowRemovalAttempt(hand, arrowIndex, completeTick));
        syncShieldArrowRemoval(player, true, hand);
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

    private static void stopShieldArrowRemoval(ServerPlayer player) {
        ShieldArrowRemovalAttempt removed = SHIELD_ARROW_REMOVALS.remove(player.getUUID());
        if (removed != null) {
            syncShieldArrowRemoval(player, false, removed.hand());
        }
    }

    private static void syncShieldArrowRemoval(ServerPlayer player, boolean active, InteractionHand hand) {
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(
                player,
                new SyncShieldArrowRemovalPayload(player.getId(), active, hand));
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

    private static void playSound(ServerPlayer player, net.minecraft.sounds.SoundEvent sound, float volume, float pitch) {
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), sound, SoundSource.PLAYERS, volume, pitch);
    }

    private record ShieldArrowRemovalAttempt(InteractionHand hand, int arrowIndex, long completeTick) {
    }
}
