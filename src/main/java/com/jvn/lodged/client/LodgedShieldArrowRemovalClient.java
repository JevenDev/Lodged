package com.jvn.lodged.client;

import com.jvn.lodged.config.LodgedConfig;
import com.jvn.lodged.network.ClientArrowState;
import com.jvn.lodged.network.ClientArrowState.ArmorArrowRemovalState;
import com.jvn.lodged.network.ClientArrowState.ShieldArrowRemovalState;
import com.jvn.lodged.network.payload.ArmorArrowRemovalActionPayload;
import com.jvn.lodged.network.payload.RemovePlayerArrowPayload.Target;
import com.jvn.lodged.network.payload.ShieldArrowRemovalActionPayload;
import com.jvn.lodged.network.payload.ShieldArrowRemovalActionPayload.Action;
import com.jvn.lodged.world.LodgedArmorArrowStorage;
import com.jvn.lodged.world.LodgedArrowRemovalScoring;
import com.jvn.lodged.world.LodgedArrowVisual;
import com.jvn.lodged.world.LodgedShieldArrowStorage;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.util.List;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.extensions.IKeyMappingExtension;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import net.neoforged.neoforge.common.ItemAbilities;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

public final class LodgedShieldArrowRemovalClient {
    private static final int LOCAL_REMOVAL_GRACE_TICKS = 5;
    private static final float FIRST_PERSON_ITEM_DROP_TICKS = 8.0F;
    private static final float FIRST_PERSON_REACH_TICKS = 7.0F;
    private static final float FIRST_PERSON_GRIP_TICKS = 3.0F;
    private static final float FIRST_PERSON_EXTRACTION_TICKS = 22.0F;
    private static final float FIRST_PERSON_SHAKE_SCALE = 0.25F;
    private static final float THIRD_PERSON_EXTRACTION_TICKS = 30.0F;
    private static final float THIRD_PERSON_SHAKE_SCALE = 0.012F;
    private static final float ARM_X_ROT = -1.35F;
    private static final float ARM_Y_ROT = -0.58F;
    private static final float ARM_Z_ROT = 0.18F;
    private static final KeyMapping REMOVE_SHIELD_ARROW_KEY = new KeyMapping(
            "key.lodged.remove_shield_arrow",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_G,
            KeyMapping.CATEGORY_GAMEPLAY);

    private static InteractionHand activeShieldHand;
    private static int activeTicks;
    private static boolean activeArmorRemoval;
    private static int activeArmorTicks;
    private static int activeTargetEntityId = -1;

    private LodgedShieldArrowRemovalClient() {
    }

    public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
        ((IKeyMappingExtension) REMOVE_SHIELD_ARROW_KEY).setKeyConflictContext(KeyConflictContext.IN_GAME);
        event.register(REMOVE_SHIELD_ARROW_KEY);
    }

    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || minecraft.level == null) {
            clearLocalState();
            return;
        }

        ClientArrowState.tickShieldArrowRemovals();
        while (REMOVE_SHIELD_ARROW_KEY.consumeClick()) {
            if (activeShieldHand == null && canStart(player, minecraft)) {
                activeShieldHand = player.getUsedItemHand();
                activeTicks = 0;
                ClientArrowState.setLocalShieldArrowRemoval(player.getId(), activeShieldHand, true);
                PacketDistributor.sendToServer(new ShieldArrowRemovalActionPayload(Action.START, activeShieldHand));
            } else if (!activeArmorRemoval && activeShieldHand == null) {
                InWorldArrowTarget target = priorityInWorldArrow(player, minecraft);
                if (target == null || !canStartInWorldRemoval(player, minecraft)) {
                    continue;
                }
                activeArmorRemoval = true;
                activeArmorTicks = 0;
                activeTargetEntityId = target.targetEntityId();
                ClientArrowState.setLocalArmorArrowRemoval(
                        player.getId(),
                        true,
                        target.target(),
                        target.slot(),
                        target.targetEntityId(),
                        target.arrow());
                PacketDistributor.sendToServer(new ArmorArrowRemovalActionPayload(ArmorArrowRemovalActionPayload.Action.START, target.targetEntityId()));
            }
        }

        if (activeShieldHand != null) {
            activeTicks++;
            if (!ClientArrowState.shieldArrowRemoval(player).active()) {
                clearShieldLocalState();
            } else if (!canContinue(player, minecraft, activeShieldHand) || activeTicks > maxLocalShieldRemovalTicks()) {
                PacketDistributor.sendToServer(new ShieldArrowRemovalActionPayload(Action.CANCEL, activeShieldHand));
                ClientArrowState.setLocalShieldArrowRemoval(player.getId(), activeShieldHand, false);
                clearShieldLocalState();
            }
        }

        if (activeArmorRemoval) {
            activeArmorTicks++;
            if (!ClientArrowState.armorArrowRemoval(player).active()) {
                clearArmorLocalState();
            } else if (!canContinueInWorldRemoval(player, minecraft) || activeArmorTicks > maxLocalInWorldRemovalTicks()) {
                PacketDistributor.sendToServer(new ArmorArrowRemovalActionPayload(ArmorArrowRemovalActionPayload.Action.CANCEL, activeTargetEntityId));
                ClientArrowState.setLocalArmorArrowRemoval(
                        player.getId(),
                        false,
                        Target.ARMOR,
                        EquipmentSlot.CHEST,
                        -1,
                        LodgedArrowVisual.DEFAULT);
                clearArmorLocalState();
            }
        }
    }

    public static boolean renderFirstPersonPullingArm(
            LocalPlayer player,
            InteractionHand hand,
            float partialTick,
            float equippedProgress,
            PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight) {
        if (!shouldRenderFirstPersonPullingHand(player, hand)) {
            return false;
        }

        int ticks = pullingTicks(player);
        boolean heldItemDropping = !player.getItemInHand(hand).isEmpty();
        float age = ticks + partialTick;
        float handAge = heldItemDropping ? age - FIRST_PERSON_ITEM_DROP_TICKS : age;
        if (handAge <= 0.0F) {
            return true;
        }

        HumanoidArm pullingArm = handArm(player, hand);
        float side = pullingArm == HumanoidArm.RIGHT ? 1.0F : -1.0F;
        float reach = firstPersonPullProgress(handAge);
        float rise = heldItemDropping ? firstPersonHandRiseProgress(handAge) : 1.0F;
        float gripAge = Math.max(handAge - FIRST_PERSON_REACH_TICKS, 0.0F);
        float grip = smootherStep(Math.min(gripAge / FIRST_PERSON_GRIP_TICKS, 1.0F));
        float pullAge = Math.max(gripAge - FIRST_PERSON_GRIP_TICKS, 0.0F);
        float pull = smootherStep(Math.min(pullAge / FIRST_PERSON_EXTRACTION_TICKS, 1.0F));
        float effort = firstPersonRemovalEffort(player);
        float tension = Mth.sin(pull * Mth.PI);
        float resistance = tension * effort;
        float lateStrain = smootherStep(Mth.clamp((pull - 0.55F) / 0.45F, 0.0F, 1.0F));
        float shakeAmount = lateStrain * effort * FIRST_PERSON_SHAKE_SCALE;
        float strain = (Mth.sin(age * 2.2F) + (Mth.sin(age * 4.9F) * 0.35F)) * shakeAmount;
        float lateralStrain = Mth.sin((age * 3.15F) + 1.1F) * shakeAmount;

        poseStack.pushPose();
        poseStack.translate(
                side * (0.64000005F - (grip * 0.018F) - (resistance * 0.025F) + (pull * 0.105F) + (lateralStrain * 0.006F)),
                -0.6F + (equippedProgress * -0.6F) - ((1.0F - rise) * 0.82F)
                        + (grip * 0.015F) + (resistance * 0.02F) + (pull * 0.13F) + (strain * 0.005F),
                -0.71999997F - (grip * 0.018F) - (resistance * 0.035F) + (pull * 0.29F) + (lateralStrain * 0.004F));
        poseStack.mulPose(Axis.YP.rotationDegrees(
                side * (Mth.lerp(reach, 45.0F, 128.0F) + (grip * 4.0F) + (resistance * 5.0F) - (pull * 16.0F) + (lateralStrain * 1.2F))));
        poseStack.mulPose(Axis.ZP.rotationDegrees(
                side * ((-28.0F * reach) - (grip * 2.0F) - (resistance * 3.0F) + (pull * 11.0F) + (strain * 1.5F))));
        poseStack.mulPose(Axis.XP.rotationDegrees(
                (-14.0F * reach) - (grip * 3.0F) - (resistance * 4.0F) + (pull * 12.0F) + (lateralStrain * 0.8F)));

        renderPlayerHand(poseStack, buffer, packedLight, player, pullingArm);
        poseStack.popPose();
        return true;
    }

    public static boolean shouldRenderFirstPersonPullingHand(LocalPlayer player, InteractionHand hand) {
        ShieldArrowRemovalState state = ClientArrowState.shieldArrowRemoval(player);
        if (state.active()) {
            return hand == pullingHand(state.shieldHand()) && !player.isInvisible();
        }

        ArmorArrowRemovalState armorState = ClientArrowState.armorArrowRemoval(player);
        return armorState.active()
                && hand == handForArm(player, armorPullingArm(player))
                && !player.isInvisible();
    }

    public static float firstPersonHeldItemDropProgress(LocalPlayer player, InteractionHand hand, float partialTick) {
        ShieldArrowRemovalState state = ClientArrowState.shieldArrowRemoval(player);
        if (state.active()) {
            if (hand != pullingHand(state.shieldHand())) {
                return 1.0F;
            }

            return smoothStep(Math.min((state.ticks() + partialTick) / FIRST_PERSON_ITEM_DROP_TICKS, 1.0F));
        }

        ArmorArrowRemovalState armorState = ClientArrowState.armorArrowRemoval(player);
        if (!armorState.active() || hand != handForArm(player, armorPullingArm(player))) {
            return 1.0F;
        }

        return smoothStep(Math.min((armorState.ticks() + partialTick) / FIRST_PERSON_ITEM_DROP_TICKS, 1.0F));
    }

    public static boolean isPullingShieldArrow(LivingEntity entity) {
        return ClientArrowState.shieldArrowRemoval(entity).active();
    }

    public static boolean isPullingArmorArrow(LivingEntity entity) {
        return ClientArrowState.armorArrowRemoval(entity).active();
    }

    public static HumanoidArm pullingArm(LivingEntity entity) {
        return handArm(entity, pullingHand(ClientArrowState.shieldArrowRemoval(entity).shieldHand()));
    }

    public static HumanoidArm armorPullingArm(LivingEntity entity) {
        ArmorArrowRemovalState state = ClientArrowState.armorArrowRemoval(entity);
        return LodgedArrowRemovalScoring.armorPullingArm(entity.getMainArm(), state.arrow());
    }

    public static ThirdPersonArmPose thirdPersonRemovalArmPose(LivingEntity entity, float ageInTicks) {
        ShieldArrowRemovalState shieldState = ClientArrowState.shieldArrowRemoval(entity);
        if (shieldState.active()) {
            HumanoidArm arm = pullingArm(entity);
            float side = arm == HumanoidArm.RIGHT ? 1.0F : -1.0F;
            return thirdPersonArmPose(
                    arm,
                    shieldRemovalArrow(entity, shieldState.shieldHand()),
                    ageInTicks,
                    shieldState.ticks(),
                    ARM_X_ROT,
                    side * ARM_Y_ROT,
                    side * ARM_Z_ROT);
        }

        ArmorArrowRemovalState armorState = ClientArrowState.armorArrowRemoval(entity);
        if (!armorState.active()) {
            return null;
        }

        HumanoidArm arm = armorPullingArm(entity);
        float side = arm == HumanoidArm.RIGHT ? 1.0F : -1.0F;
        LodgedArrowVisual arrow = armorState.arrow();
        if (armorState.target() == Target.BODY) {
            return thirdPersonArmPose(
                    arm,
                    arrow,
                    ageInTicks,
                    armorState.ticks(),
                    ARM_X_ROT,
                    side * ARM_Y_ROT,
                    side * ARM_Z_ROT);
        }

        float targetX = switch (arrow.bodyPart()) {
            case HEAD -> -2.15F;
            case CHEST, LEFT_ARM, RIGHT_ARM -> -1.35F;
            case LEFT_LEG, RIGHT_LEG -> -0.45F;
        };
        float targetY = side * Mth.clamp(Math.abs(arrow.modelX()) * 1.5F + 0.2F, 0.2F, 0.75F);
        float targetZ = side * (arrow.bodyPart().isLeg() ? -0.35F : 0.25F);
        return thirdPersonArmPose(arm, arrow, ageInTicks, armorState.ticks(), targetX, targetY, targetZ);
    }

    private static ThirdPersonArmPose thirdPersonArmPose(
            HumanoidArm arm,
            LodgedArrowVisual arrow,
            float ageInTicks,
            float ticks,
            float targetX,
            float targetY,
            float targetZ) {
        float reach = smootherStep(Math.min(ticks / FIRST_PERSON_REACH_TICKS, 1.0F));
        float gripAge = Math.max(ticks - FIRST_PERSON_REACH_TICKS, 0.0F);
        float grip = smootherStep(Math.min(gripAge / FIRST_PERSON_GRIP_TICKS, 1.0F));
        float pullAge = Math.max(gripAge - FIRST_PERSON_GRIP_TICKS, 0.0F);
        float pull = smootherStep(Math.min(pullAge / THIRD_PERSON_EXTRACTION_TICKS, 1.0F));
        float effort = removalEffort(arrow);
        float resistance = Mth.sin(pull * Mth.PI) * effort;
        float lateStrain = smootherStep(Mth.clamp((pull - 0.55F) / 0.45F, 0.0F, 1.0F));
        float shakeAmount = lateStrain * effort * THIRD_PERSON_SHAKE_SCALE;
        float strain = (Mth.sin(ageInTicks * 2.2F) + (Mth.sin(ageInTicks * 4.9F) * 0.35F)) * shakeAmount;
        float lateralStrain = Mth.sin((ageInTicks * 3.15F) + 1.1F) * shakeAmount;

        float side = arm == HumanoidArm.RIGHT ? 1.0F : -1.0F;
        float xRot = targetX - (grip * 0.06F) - (resistance * 0.06F) + (pull * 0.28F) + strain;
        float yRot = targetY + (side * ((grip * -0.04F) - (resistance * 0.04F) + (pull * 0.18F) + (lateralStrain * 0.65F)));
        float zRot = targetZ + (side * ((grip * 0.03F) + (resistance * 0.04F) + (pull * 0.09F) + (strain * 0.75F)));
        return new ThirdPersonArmPose(arm, reach, xRot, yRot, zRot);
    }

    private static boolean canStart(LocalPlayer player, Minecraft minecraft) {
        return minecraft.screen == null
                && LodgedConfig.enablePlayerArrowRemoval()
                && player.isAlive()
                && minecraft.options.keyUse.isDown()
                && player.isUsingItem()
                && hasShieldArrows(player, player.getUsedItemHand());
    }

    private static int maxLocalShieldRemovalTicks() {
        return LodgedConfig.shieldArrowRemovalMaxTicks() + LOCAL_REMOVAL_GRACE_TICKS;
    }

    private static int maxLocalInWorldRemovalTicks() {
        return Math.max(
                LodgedConfig.tamedMobArrowRemovalMaxTicks(),
                Math.max(
                        LodgedConfig.armorArrowRemovalTicks(),
                        LodgedConfig.inWorldBodyArrowRemovalMaxTicks())) + LOCAL_REMOVAL_GRACE_TICKS;
    }

    private static boolean canContinue(LocalPlayer player, Minecraft minecraft, InteractionHand shieldHand) {
        return minecraft.screen == null
                && player.isAlive()
                && minecraft.options.keyUse.isDown()
                && player.isUsingItem()
                && player.getUsedItemHand() == shieldHand
                && hasShieldArrows(player, shieldHand);
    }

    private static boolean canStartInWorldRemoval(LocalPlayer player, Minecraft minecraft) {
        return minecraft.screen == null
                && (LodgedConfig.enablePlayerArrowRemoval() || LodgedConfig.enableTamedMobArrowRemoval())
                && player.isAlive()
                && !player.isUsingItem()
                && priorityInWorldArrow(player, minecraft) != null;
    }

    private static boolean canContinueInWorldRemoval(LocalPlayer player, Minecraft minecraft) {
        return minecraft.screen == null
                && player.isAlive()
                && !player.isUsingItem()
                && REMOVE_SHIELD_ARROW_KEY.isDown()
                && hasActiveTargetArrows(player, minecraft);
    }

    private static boolean hasShieldArrows(LocalPlayer player, InteractionHand hand) {
        ItemStack shield = player.getItemInHand(hand);
        return !shield.isEmpty()
                && shield.canPerformAction(ItemAbilities.SHIELD_BLOCK)
                && !LodgedShieldArrowStorage.readAll(shield).isEmpty();
    }

    private static boolean hasArmorArrows(LocalPlayer player) {
        for (EquipmentSlot slot : LodgedArmorArrowStorage.armorSlots()) {
            if (!LodgedArmorArrowStorage.readAll(player.getItemBySlot(slot)).isEmpty()) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasBodyArrows() {
        return ClientArrowState.removableArrowCount() > 0;
    }

    private static boolean hasInWorldArrows(LocalPlayer player) {
        return hasArmorArrows(player) || hasBodyArrows();
    }

    private static InWorldArrowTarget priorityInWorldArrow(LocalPlayer player, Minecraft minecraft) {
        if (LodgedConfig.enablePlayerArrowRemoval()) {
            InWorldArrowTarget armorTarget = priorityArmorArrow(player);
            if (armorTarget != null) {
                return armorTarget;
            }
            InWorldArrowTarget bodyTarget = priorityBodyArrow();
            if (bodyTarget != null) {
                return bodyTarget;
            }
        }
        if (!LodgedConfig.enableTamedMobArrowRemoval()) {
            return null;
        }
        if (player.getVehicle() instanceof LivingEntity mount) {
            InWorldArrowTarget mountTarget = priorityMobArrow(player, mount);
            if (mountTarget != null) {
                return mountTarget;
            }
        }
        if (!(minecraft.hitResult instanceof EntityHitResult hit)
                || !(hit.getEntity() instanceof LivingEntity mob)
                || player.distanceTo(mob) > LodgedConfig.tamedMobArrowRemovalRange()) {
            return null;
        }
        return priorityMobArrow(player, mob);
    }

    private static InWorldArrowTarget priorityArmorArrow(LocalPlayer player) {
        InWorldArrowTarget best = null;
        double bestScore = Double.MAX_VALUE;
        for (EquipmentSlot slot : LodgedArmorArrowStorage.armorSlots()) {
            for (LodgedArrowVisual arrow : LodgedArmorArrowStorage.readAll(player.getItemBySlot(slot))) {
                double score = LodgedArrowRemovalScoring.armorArrowPriorityScore(slot, arrow, player.getMainArm());
                if (score < bestScore) {
                    bestScore = score;
                    best = new InWorldArrowTarget(Target.ARMOR, slot, player.getId(), arrow);
                }
            }
        }
        return best;
    }

    private static InWorldArrowTarget priorityBodyArrow() {
        List<LodgedArrowVisual> arrows = ClientArrowState.removableArrows();
        int arrowCount = Math.min(arrows.size(), ClientArrowState.syncedArrowCount());
        arrowCount = Math.min(arrowCount, LodgedConfig.maxRemovablePlayerArrows());
        InWorldArrowTarget best = null;
        double bestScore = Double.MAX_VALUE;
        for (int index = 0; index < arrowCount; index++) {
            LodgedArrowVisual arrow = arrows.get(index);
            double score = LodgedArrowRemovalScoring.bodyArrowPriorityScore(
                    arrow,
                    LodgedConfig.playerArrowRemovalSuccessChance(arrow.bodyPart())
                            * LodgedConfig.arrowDepthRemovalSuccessMultiplier(arrow.depth()));
            if (score < bestScore) {
                bestScore = score;
                best = new InWorldArrowTarget(Target.BODY, EquipmentSlot.CHEST, Minecraft.getInstance().player.getId(), arrow);
            }
        }
        return best;
    }

    private static InWorldArrowTarget priorityMobArrow(LocalPlayer player, LivingEntity mob) {
        if (!isEligibleTamedMob(player, mob)) {
            return null;
        }
        InWorldArrowTarget best = null;
        double bestScore = Double.MAX_VALUE;
        if (mob instanceof AbstractHorse horse) {
            for (LodgedArrowVisual arrow : LodgedArmorArrowStorage.readAll(horse.getItemBySlot(EquipmentSlot.BODY))) {
                double score = LodgedArrowRemovalScoring.armorArrowPriorityScore(
                        EquipmentSlot.BODY,
                        arrow,
                        player.getMainArm());
                if (score < bestScore) {
                    bestScore = score;
                    best = new InWorldArrowTarget(Target.ARMOR, EquipmentSlot.BODY, mob.getId(), arrow);
                }
            }
        }
        List<LodgedArrowVisual> arrows = ClientArrowState.entityArrows(mob).arrows();
        for (LodgedArrowVisual arrow : arrows) {
            double score = LodgedArrowRemovalScoring.bodyArrowPriorityScore(
                    arrow,
                    LodgedConfig.tamedMobArrowRemovalSuccessChance(arrow.bodyPart())
                            * LodgedConfig.arrowDepthRemovalSuccessMultiplier(arrow.depth()));
            if (score < bestScore) {
                bestScore = score;
                best = new InWorldArrowTarget(Target.BODY, EquipmentSlot.CHEST, mob.getId(), arrow);
            }
        }
        return best;
    }

    private static boolean isEligibleTamedMob(LocalPlayer player, LivingEntity mob) {
        if (!mob.isAlive() || mob == player) {
            return false;
        }
        boolean tamed;
        boolean owned;
        if (mob instanceof TamableAnimal tamable) {
            tamed = tamable.isTame();
            owned = tamable.isOwnedBy(player);
        } else if (mob instanceof AbstractHorse horse) {
            tamed = horse.isTamed();
            owned = player.getUUID().equals(horse.getOwnerUUID());
        } else {
            return false;
        }
        return tamed && (!LodgedConfig.requireMobOwnership() || owned);
    }

    private static boolean hasActiveTargetArrows(LocalPlayer player, Minecraft minecraft) {
        if (activeTargetEntityId == player.getId()) {
            return hasInWorldArrows(player);
        }
        Entity entity = minecraft.level.getEntity(activeTargetEntityId);
        return entity instanceof LivingEntity living
                && isEligibleTamedMob(player, living)
                && (player.getVehicle() == living
                        || player.distanceTo(living) <= LodgedConfig.tamedMobArrowRemovalRange())
                && hasMobArrows(living);
    }

    private static boolean hasMobArrows(LivingEntity mob) {
        return !ClientArrowState.entityArrows(mob).arrows().isEmpty()
                || mob instanceof AbstractHorse horse
                && !LodgedArmorArrowStorage.readAll(horse.getItemBySlot(EquipmentSlot.BODY)).isEmpty();
    }

    private static InteractionHand pullingHand(InteractionHand shieldHand) {
        return shieldHand == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
    }

    private static HumanoidArm handArm(LivingEntity entity, InteractionHand hand) {
        if (entity == null) {
            return HumanoidArm.RIGHT;
        }
        return hand == InteractionHand.MAIN_HAND ? entity.getMainArm() : entity.getMainArm().getOpposite();
    }

    private static InteractionHand handForArm(LivingEntity entity, HumanoidArm arm) {
        return entity.getMainArm() == arm ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
    }

    private static void renderPlayerHand(
            PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight,
            AbstractClientPlayer player,
            HumanoidArm arm) {
        float side = arm == HumanoidArm.RIGHT ? 1.0F : -1.0F;
        poseStack.translate(side * -1.0F, 3.6F, 3.5F);
        poseStack.mulPose(Axis.ZP.rotationDegrees(side * 120.0F));
        poseStack.mulPose(Axis.XP.rotationDegrees(200.0F));
        poseStack.mulPose(Axis.YP.rotationDegrees(side * -135.0F));
        poseStack.translate(side * 5.6F, 0.0F, 0.0F);

        PlayerRenderer playerRenderer = (PlayerRenderer) Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(player);
        if (arm == HumanoidArm.RIGHT) {
            playerRenderer.renderRightHand(poseStack, buffer, packedLight, player);
        } else {
            playerRenderer.renderLeftHand(poseStack, buffer, packedLight, player);
        }
    }

    private static float firstPersonPullProgress(float ticks) {
        return smootherStep(Math.min(ticks / FIRST_PERSON_REACH_TICKS, 1.0F));
    }

    private static float firstPersonHandRiseProgress(float ticks) {
        return smoothStep(Math.min(ticks / 6.0F, 1.0F));
    }

    private static float smoothStep(float value) {
        return value * value * (3.0F - (2.0F * value));
    }

    private static float smootherStep(float value) {
        return value * value * value * (value * ((value * 6.0F) - 15.0F) + 10.0F);
    }

    private static float firstPersonRemovalEffort(LocalPlayer player) {
        return removalEffort(firstPersonRemovalArrow(player));
    }

    private static float removalEffort(LodgedArrowVisual arrow) {
        return switch (arrow.depth()) {
            case SHALLOW -> 0.6F;
            case LODGED -> 1.0F;
            case DEEP_LODGED -> 1.55F;
        };
    }

    private static LodgedArrowVisual firstPersonRemovalArrow(LocalPlayer player) {
        ShieldArrowRemovalState shieldState = ClientArrowState.shieldArrowRemoval(player);
        if (shieldState.active()) {
            return shieldRemovalArrow(player, shieldState.shieldHand());
        }

        return ClientArrowState.armorArrowRemoval(player).arrow();
    }

    private static LodgedArrowVisual shieldRemovalArrow(LivingEntity entity, InteractionHand hand) {
        List<LodgedArrowVisual> arrows = LodgedShieldArrowStorage.readAll(entity.getItemInHand(hand));
        LodgedArrowVisual best = LodgedArrowVisual.DEFAULT;
        double bestScore = Double.MAX_VALUE;
        for (LodgedArrowVisual arrow : arrows) {
            double score = Math.abs(arrow.modelX()) + Math.abs(arrow.modelY() * 0.75D);
            if (score < bestScore) {
                bestScore = score;
                best = arrow;
            }
        }
        return best;
    }

    private static int pullingTicks(LocalPlayer player) {
        ShieldArrowRemovalState shieldState = ClientArrowState.shieldArrowRemoval(player);
        if (shieldState.active()) {
            return shieldState.ticks();
        }
        return ClientArrowState.armorArrowRemoval(player).ticks();
    }

    private static void clearLocalState() {
        clearShieldLocalState();
        clearArmorLocalState();
    }

    private static void clearShieldLocalState() {
        activeShieldHand = null;
        activeTicks = 0;
    }

    private static void clearArmorLocalState() {
        activeArmorRemoval = false;
        activeTargetEntityId = -1;
        activeArmorTicks = 0;
    }

    public record ThirdPersonArmPose(HumanoidArm arm, float reach, float xRot, float yRot, float zRot) {
    }

    private record InWorldArrowTarget(Target target, EquipmentSlot slot, int targetEntityId, LodgedArrowVisual arrow) {
    }
}
