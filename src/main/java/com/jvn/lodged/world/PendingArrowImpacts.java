package com.jvn.lodged.world;

import com.jvn.lodged.world.ArrowImpactRules.ProjectileStack;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

final class PendingArrowImpacts {
    private static final long BROKEN_ARROW_IMPACT_EXPIRY_TICKS = 20L;
    private static final long SHIELD_ARROW_IMPACT_EXPIRY_TICKS = 20L;
    private static final long ARMOR_ARROW_IMPACT_EXPIRY_TICKS = 20L;
    private static final long BODY_ARROW_IMPACT_EXPIRY_TICKS = 20L;
    private static final long BLOCK_ARROW_BREAK_DELAY_TICKS = 4L;
    private static final long BLOCK_ARROW_BREAK_EXPIRY_TICKS = 40L;
    private static final Map<UUID, BrokenArrowImpact> BROKEN_ARROW_IMPACTS = new HashMap<>();
    private static final Map<UUID, ShieldArrowImpact> SHIELD_ARROW_IMPACTS = new HashMap<>();
    private static final Map<UUID, ArmorArrowImpact> ARMOR_ARROW_IMPACTS = new HashMap<>();
    private static final Map<UUID, BodyArrowImpact> BODY_ARROW_IMPACTS = new HashMap<>();
    private static final Map<UUID, PendingBlockArrowBreak> PENDING_BLOCK_ARROW_BREAKS = new HashMap<>();

    private PendingArrowImpacts() {
    }

    static void rememberBrokenArrowImpact(AbstractArrow arrow, LivingEntity target, LodgedArrowVisual visual) {
        BROKEN_ARROW_IMPACTS.put(
                arrow.getUUID(),
                new BrokenArrowImpact(
                        target.getUUID(),
                        visual,
                        ArrowImpactRules.causesBleeding(arrow),
                        arrow.level().dimension(),
                        arrow.level().getGameTime()));
    }

    static void rememberShieldArrowImpact(AbstractArrow arrow, LivingEntity target, LodgedArrowVisual visual) {
        SHIELD_ARROW_IMPACTS.put(
                arrow.getUUID(),
                new ShieldArrowImpact(
                        target.getUUID(),
                        visual,
                        arrow.level().dimension(),
                        arrow.level().getGameTime()));
    }

    static void rememberArmorArrowImpact(
            AbstractArrow arrow,
            LivingEntity target,
            EquipmentSlot slot,
            LodgedArrowVisual visual,
            ProjectileStack projectileStack,
            boolean fromPlayer,
            boolean infinityGenerated,
            boolean creativeGenerated,
            boolean breaks,
            int pierceLevel) {
        ARMOR_ARROW_IMPACTS.put(
                arrow.getUUID(),
                new ArmorArrowImpact(
                        target.getUUID(),
                        slot,
                        projectileStack.stack(),
                        projectileStack.recoverable(),
                        fromPlayer,
                        infinityGenerated,
                        creativeGenerated,
                        visual,
                        breaks,
                        pierceLevel,
                        arrow.level().dimension(),
                        arrow.level().getGameTime()));
    }

    static void rememberBodyArrowImpact(
            AbstractArrow arrow,
            LivingEntity target,
            LodgedArrowData arrowData) {
        BODY_ARROW_IMPACTS.put(
                arrow.getUUID(),
                new BodyArrowImpact(
                        target.getUUID(),
                        arrowData,
                        arrow.level().dimension(),
                        arrow.level().getGameTime()));
    }

    static void clearPendingEntityImpacts(UUID arrowUuid) {
        BROKEN_ARROW_IMPACTS.remove(arrowUuid);
        SHIELD_ARROW_IMPACTS.remove(arrowUuid);
        ARMOR_ARROW_IMPACTS.remove(arrowUuid);
        BODY_ARROW_IMPACTS.remove(arrowUuid);
    }

    static void rememberPendingBlockArrowBreak(AbstractArrow arrow) {
        arrow.pickup = AbstractArrow.Pickup.DISALLOWED;
        PENDING_BLOCK_ARROW_BREAKS.put(
                arrow.getUUID(),
                new PendingBlockArrowBreak(
                        arrow.level().dimension(),
                        arrow.level().getGameTime() + BLOCK_ARROW_BREAK_DELAY_TICKS));
    }

    static void removeExpiredImpacts(ServerLevel level) {
        long gameTime = level.getGameTime();
        BROKEN_ARROW_IMPACTS.entrySet().removeIf(entry ->
                entry.getValue().dimension().equals(level.dimension())
                        && gameTime - entry.getValue().gameTime() > BROKEN_ARROW_IMPACT_EXPIRY_TICKS);
        SHIELD_ARROW_IMPACTS.entrySet().removeIf(entry ->
                entry.getValue().dimension().equals(level.dimension())
                        && gameTime - entry.getValue().gameTime() > SHIELD_ARROW_IMPACT_EXPIRY_TICKS);
        ARMOR_ARROW_IMPACTS.entrySet().removeIf(entry ->
                entry.getValue().dimension().equals(level.dimension())
                        && gameTime - entry.getValue().gameTime() > ARMOR_ARROW_IMPACT_EXPIRY_TICKS);
        BODY_ARROW_IMPACTS.entrySet().removeIf(entry ->
                entry.getValue().dimension().equals(level.dimension())
                        && gameTime - entry.getValue().gameTime() > BODY_ARROW_IMPACT_EXPIRY_TICKS);
        PENDING_BLOCK_ARROW_BREAKS.entrySet().removeIf(entry ->
                entry.getValue().dimension().equals(level.dimension())
                        && gameTime - entry.getValue().breakTime() > BLOCK_ARROW_BREAK_EXPIRY_TICKS);
    }

    static ArmorArrowImpact consumeArmorImpact(UUID arrowUuid, UUID targetUuid, boolean damageAccepted) {
        return ArrowImpactConfirmation.consume(
                ARMOR_ARROW_IMPACTS, arrowUuid, targetUuid, damageAccepted, ArmorArrowImpact::targetUuid);
    }

    static BrokenArrowImpact consumeBrokenImpact(UUID arrowUuid, UUID targetUuid, boolean damageAccepted) {
        return ArrowImpactConfirmation.consume(
                BROKEN_ARROW_IMPACTS, arrowUuid, targetUuid, damageAccepted, BrokenArrowImpact::targetUuid);
    }

    static BodyArrowImpact consumeBodyImpact(UUID arrowUuid, UUID targetUuid, boolean damageAccepted) {
        return ArrowImpactConfirmation.consume(
                BODY_ARROW_IMPACTS, arrowUuid, targetUuid, damageAccepted, BodyArrowImpact::targetUuid);
    }

    static ShieldArrowImpact removeShieldImpact(UUID arrowUuid) {
        return SHIELD_ARROW_IMPACTS.remove(arrowUuid);
    }

    static boolean removeDueBlockBreak(AbstractArrow arrow) {
        PendingBlockArrowBreak pendingBreak = PENDING_BLOCK_ARROW_BREAKS.get(arrow.getUUID());
        if (pendingBreak == null
                || !pendingBreak.dimension().equals(arrow.level().dimension())
                || arrow.level().getGameTime() < pendingBreak.breakTime()) {
            return false;
        }

        PENDING_BLOCK_ARROW_BREAKS.remove(arrow.getUUID());
        return true;
    }

    static void clear() {
        BROKEN_ARROW_IMPACTS.clear();
        SHIELD_ARROW_IMPACTS.clear();
        ARMOR_ARROW_IMPACTS.clear();
        BODY_ARROW_IMPACTS.clear();
        PENDING_BLOCK_ARROW_BREAKS.clear();
    }

    record BrokenArrowImpact(
            UUID targetUuid,
            LodgedArrowVisual visual,
            boolean causesBleeding,
            ResourceKey<Level> dimension,
            long gameTime) {
    }

    record ShieldArrowImpact(
            UUID targetUuid,
            LodgedArrowVisual visual,
            ResourceKey<Level> dimension,
            long gameTime) {
    }

    record ArmorArrowImpact(
            UUID targetUuid,
            EquipmentSlot slot,
            ItemStack stack,
            boolean recoverable,
            boolean fromPlayer,
            boolean infinityGenerated,
            boolean creativeGenerated,
            LodgedArrowVisual visual,
            boolean breaks,
            int pierceLevel,
            ResourceKey<Level> dimension,
            long gameTime) {
        ArmorArrowImpact {
            stack = stack.copyWithCount(1);
            visual = visual.withStack(stack);
        }
    }

    record BodyArrowImpact(
            UUID targetUuid,
            LodgedArrowData arrowData,
            ResourceKey<Level> dimension,
            long gameTime) {
    }

    private record PendingBlockArrowBreak(
            ResourceKey<Level> dimension,
            long breakTime) {
    }
}
