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
import com.jvn.lodged.world.LodgedArrowBodyPart;
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
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.extensions.IKeyMappingExtension;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import net.neoforged.neoforge.common.ItemAbilities;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

public final class LodgedShieldArrowRemovalClient {
    private static final int LOCAL_REMOVAL_MAX_TICKS = 65;
    private static final float PULL_EASE_TICKS = 16.0F;
    private static final float FIRST_PERSON_ITEM_DROP_TICKS = 8.0F;
    private static final float ARM_X_ROT = -1.35F;
    private static final float ARM_PULL_X_ROT = -0.25F;
    private static final float ARM_Y_ROT = -0.58F;
    private static final float ARM_Z_ROT = 0.18F;
    private static final float ARM_PULSE_SPEED = 0.45F;
    private static final float ARM_PULSE_AMOUNT = 0.08F;
    private static final KeyMapping REMOVE_SHIELD_ARROW_KEY = new KeyMapping(
            "key.lodged.remove_shield_arrow",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_G,
            KeyMapping.CATEGORY_GAMEPLAY);

    private static InteractionHand activeShieldHand;
    private static int activeTicks;
    private static boolean activeArmorRemoval;
    private static int activeArmorTicks;

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
            } else if (!activeArmorRemoval && activeShieldHand == null && canStartInWorldRemoval(player, minecraft)) {
                activeArmorRemoval = true;
                activeArmorTicks = 0;
                InWorldArrowTarget target = priorityInWorldArrow(player);
                ClientArrowState.setLocalArmorArrowRemoval(
                        player.getId(),
                        true,
                        target.target(),
                        target.slot(),
                        target.arrow());
                PacketDistributor.sendToServer(new ArmorArrowRemovalActionPayload(ArmorArrowRemovalActionPayload.Action.START));
            }
        }

        if (activeShieldHand != null) {
            activeTicks++;
            if (!ClientArrowState.shieldArrowRemoval(player).active()) {
                clearShieldLocalState();
            } else if (!canContinue(player, minecraft, activeShieldHand) || activeTicks > LOCAL_REMOVAL_MAX_TICKS) {
                PacketDistributor.sendToServer(new ShieldArrowRemovalActionPayload(Action.CANCEL, activeShieldHand));
                ClientArrowState.setLocalShieldArrowRemoval(player.getId(), activeShieldHand, false);
                clearShieldLocalState();
            }
        }

        if (activeArmorRemoval) {
            activeArmorTicks++;
            if (!ClientArrowState.armorArrowRemoval(player).active()) {
                clearArmorLocalState();
            } else if (!canContinueInWorldRemoval(player, minecraft) || activeArmorTicks > LOCAL_REMOVAL_MAX_TICKS) {
                PacketDistributor.sendToServer(new ArmorArrowRemovalActionPayload(ArmorArrowRemovalActionPayload.Action.CANCEL));
                ClientArrowState.setLocalArmorArrowRemoval(
                        player.getId(),
                        false,
                        Target.ARMOR,
                        EquipmentSlot.CHEST,
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
        float pull = firstPersonPullProgress(handAge);
        float rise = heldItemDropping ? firstPersonHandRiseProgress(handAge) : 1.0F;
        float pulse = Mth.sin(age * ARM_PULSE_SPEED) * pull;
        float grip = Mth.sin((age * ARM_PULSE_SPEED) + Mth.PI) * pull;

        poseStack.pushPose();
        poseStack.translate(
                side * 0.64000005F,
                -0.6F + (equippedProgress * -0.6F) - ((1.0F - rise) * 0.82F),
                -0.71999997F + (grip * 0.012F));
        poseStack.mulPose(Axis.YP.rotationDegrees(side * Mth.lerp(pull, 45.0F, 128.0F)));
        poseStack.mulPose(Axis.ZP.rotationDegrees(side * ((-28.0F * pull) + (pulse * 4.0F))));
        poseStack.mulPose(Axis.XP.rotationDegrees((-14.0F * pull) + (grip * 2.0F)));

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

    public static float pullProgress(LivingEntity entity) {
        return pullProgress(ClientArrowState.shieldArrowRemoval(entity).ticks());
    }

    public static HumanoidArm armorPullingArm(LivingEntity entity) {
        ArmorArrowRemovalState state = ClientArrowState.armorArrowRemoval(entity);
        LodgedArrowVisual arrow = state.arrow();
        if (arrow.bodyPart().isArm()) {
            return arrow.modelX() < 0.0F ? HumanoidArm.LEFT : HumanoidArm.RIGHT;
        }
        if (Math.abs(arrow.modelX()) > 0.05F) {
            return arrow.modelX() < 0.0F ? HumanoidArm.RIGHT : HumanoidArm.LEFT;
        }
        return entity.getMainArm();
    }

    public static float armorPullProgress(LivingEntity entity) {
        return pullProgress(ClientArrowState.armorArrowRemoval(entity).ticks());
    }

    public static float armorPullingArmXRot(LivingEntity entity, float ageInTicks, float pull) {
        LodgedArrowVisual arrow = ClientArrowState.armorArrowRemoval(entity).arrow();
        if (ClientArrowState.armorArrowRemoval(entity).target() == Target.BODY) {
            return pullingArmXRot(ageInTicks, pull);
        }

        float target = switch (arrow.bodyPart()) {
            case HEAD -> -2.15F;
            case CHEST, LEFT_ARM, RIGHT_ARM -> -1.35F;
            case LEFT_LEG, RIGHT_LEG -> -0.45F;
        };
        float pulse = (float) Math.sin(ageInTicks * ARM_PULSE_SPEED) * ARM_PULSE_AMOUNT;
        return Mth.lerp(pull, ARM_X_ROT, target) + pulse;
    }

    public static float armorPullingArmYRot(LivingEntity entity) {
        LodgedArrowVisual arrow = ClientArrowState.armorArrowRemoval(entity).arrow();
        float side = armorPullingArm(entity) == HumanoidArm.RIGHT ? 1.0F : -1.0F;
        if (ClientArrowState.armorArrowRemoval(entity).target() == Target.BODY) {
            return side * pullingArmYRot();
        }

        return side * Mth.clamp(Math.abs(arrow.modelX()) * 1.5F + 0.2F, 0.2F, 0.75F);
    }

    public static float armorPullingArmZRot(LivingEntity entity) {
        LodgedArrowVisual arrow = ClientArrowState.armorArrowRemoval(entity).arrow();
        float side = armorPullingArm(entity) == HumanoidArm.RIGHT ? 1.0F : -1.0F;
        if (ClientArrowState.armorArrowRemoval(entity).target() == Target.BODY) {
            return side * pullingArmZRot();
        }

        float target = arrow.bodyPart().isLeg() ? -0.35F : 0.25F;
        return side * target;
    }

    public static float pullingArmXRot(float ageInTicks, float pull) {
        float pulse = (float) Math.sin(ageInTicks * ARM_PULSE_SPEED) * ARM_PULSE_AMOUNT;
        return ARM_X_ROT + (ARM_PULL_X_ROT * pull) + pulse;
    }

    public static float pullingArmYRot() {
        return ARM_Y_ROT;
    }

    public static float pullingArmZRot() {
        return ARM_Z_ROT;
    }

    private static boolean canStart(LocalPlayer player, Minecraft minecraft) {
        return minecraft.screen == null
                && LodgedConfig.enablePlayerArrowRemoval()
                && player.isAlive()
                && minecraft.options.keyUse.isDown()
                && player.isUsingItem()
                && hasShieldArrows(player, player.getUsedItemHand());
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
                && LodgedConfig.enablePlayerArrowRemoval()
                && player.isAlive()
                && !player.isUsingItem()
                && priorityInWorldArrow(player) != null;
    }

    private static boolean canContinueInWorldRemoval(LocalPlayer player, Minecraft minecraft) {
        return minecraft.screen == null
                && player.isAlive()
                && !player.isUsingItem()
                && REMOVE_SHIELD_ARROW_KEY.isDown()
                && hasInWorldArrows(player);
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

    private static InWorldArrowTarget priorityInWorldArrow(LocalPlayer player) {
        InWorldArrowTarget armorTarget = priorityArmorArrow(player);
        return armorTarget != null ? armorTarget : priorityBodyArrow();
    }

    private static InWorldArrowTarget priorityArmorArrow(LocalPlayer player) {
        InWorldArrowTarget best = null;
        double bestScore = Double.MAX_VALUE;
        for (EquipmentSlot slot : LodgedArmorArrowStorage.armorSlots()) {
            for (LodgedArrowVisual arrow : LodgedArmorArrowStorage.readAll(player.getItemBySlot(slot))) {
                double score = armorArrowPriorityScore(slot, arrow);
                if (score < bestScore) {
                    bestScore = score;
                    best = new InWorldArrowTarget(Target.ARMOR, slot, arrow);
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
            double score = bodyArrowPriorityScore(arrow);
            if (score < bestScore) {
                bestScore = score;
                best = new InWorldArrowTarget(Target.BODY, EquipmentSlot.CHEST, arrow);
            }
        }
        return best;
    }

    private static double armorArrowPriorityScore(EquipmentSlot slot, LodgedArrowVisual arrow) {
        double visibleScore = arrow.modelZ() <= 0.0F ? 0.0D : 1000.0D;
        return visibleScore + (armorSlotPriority(slot) * 100.0D) + arrowReachDistance(arrow);
    }

    private static int armorSlotPriority(EquipmentSlot slot) {
        return switch (slot) {
            case HEAD -> 0;
            case CHEST -> 1;
            case LEGS -> 2;
            case FEET -> 3;
            default -> 4;
        };
    }

    private static double bodyArrowPriorityScore(LodgedArrowVisual arrow) {
        double successChance = Mth.clamp(
                LodgedConfig.playerArrowRemovalSuccessChance(arrow.bodyPart())
                        * LodgedConfig.arrowDepthRemovalSuccessMultiplier(arrow.depth()),
                0.0D,
                1.0D);
        return (bodyPartRemovalPriority(arrow.bodyPart()) * 100.0D) - successChance;
    }

    private static int bodyPartRemovalPriority(LodgedArrowBodyPart bodyPart) {
        return switch (bodyPart) {
            case LEFT_ARM, RIGHT_ARM, LEFT_LEG, RIGHT_LEG -> 0;
            case CHEST -> 1;
            case HEAD -> 2;
        };
    }

    private static double arrowReachDistance(LodgedArrowVisual arrow) {
        HumanoidArm arm = arrow.modelX() < 0.0F ? HumanoidArm.RIGHT : HumanoidArm.LEFT;
        double armX = arm == HumanoidArm.RIGHT ? -5.0D / 16.0D : 5.0D / 16.0D;
        double armY = 6.0D / 16.0D;
        double dx = arrow.modelX() - armX;
        double dy = arrow.modelY() - armY;
        return (dx * dx) + (dy * dy);
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

    private static float pullProgress(float ticks) {
        return smoothStep(Math.min(ticks / PULL_EASE_TICKS, 1.0F));
    }

    private static float firstPersonPullProgress(float ticks) {
        float value = Math.min(ticks / FIRST_PERSON_ITEM_DROP_TICKS, 1.0F);
        return 1.0F - ((1.0F - value) * (1.0F - value));
    }

    private static float firstPersonHandRiseProgress(float ticks) {
        return smoothStep(Math.min(ticks / 6.0F, 1.0F));
    }

    private static float smoothStep(float value) {
        return value * value * (3.0F - (2.0F * value));
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
        activeArmorTicks = 0;
    }

    private record InWorldArrowTarget(Target target, EquipmentSlot slot, LodgedArrowVisual arrow) {
    }
}
