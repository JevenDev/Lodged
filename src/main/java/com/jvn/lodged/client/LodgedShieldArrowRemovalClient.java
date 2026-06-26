package com.jvn.lodged.client;

import com.jvn.lodged.config.LodgedConfig;
import com.jvn.lodged.network.ClientArrowState;
import com.jvn.lodged.network.ClientArrowState.ShieldArrowRemovalState;
import com.jvn.lodged.network.payload.ShieldArrowRemovalActionPayload;
import com.jvn.lodged.network.payload.ShieldArrowRemovalActionPayload.Action;
import com.jvn.lodged.world.LodgedShieldArrowStorage;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
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
            }
        }

        if (activeShieldHand == null) {
            return;
        }

        activeTicks++;
        if (!ClientArrowState.shieldArrowRemoval(player).active()) {
            clearLocalState();
        } else if (!canContinue(player, minecraft, activeShieldHand) || activeTicks > LOCAL_REMOVAL_MAX_TICKS) {
            PacketDistributor.sendToServer(new ShieldArrowRemovalActionPayload(Action.CANCEL, activeShieldHand));
            ClientArrowState.setLocalShieldArrowRemoval(player.getId(), activeShieldHand, false);
            clearLocalState();
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
        ShieldArrowRemovalState state = ClientArrowState.shieldArrowRemoval(player);
        if (!state.active() || hand != pullingHand(state.shieldHand()) || player.isInvisible()) {
            return false;
        }

        HumanoidArm pullingArm = handArm(player, hand);
        float side = pullingArm == HumanoidArm.RIGHT ? 1.0F : -1.0F;
        float age = state.ticks() + partialTick;
        float pull = firstPersonPullProgress(age);
        float pulse = Mth.sin(age * ARM_PULSE_SPEED) * pull;
        float grip = Mth.sin((age * ARM_PULSE_SPEED) + Mth.PI) * pull;

        poseStack.pushPose();
        poseStack.translate(
                side * 0.64000005F,
                -0.6F + (equippedProgress * -0.6F),
                -0.71999997F + (grip * 0.012F));
        poseStack.mulPose(Axis.YP.rotationDegrees(side * Mth.lerp(pull, 45.0F, 128.0F)));
        poseStack.mulPose(Axis.ZP.rotationDegrees(side * ((-28.0F * pull) + (pulse * 4.0F))));
        poseStack.mulPose(Axis.XP.rotationDegrees((-14.0F * pull) + (grip * 2.0F)));

        renderPlayerHand(poseStack, buffer, packedLight, player, pullingArm);
        poseStack.popPose();
        return true;
    }

    public static boolean isPullingShieldArrow(LivingEntity entity) {
        return ClientArrowState.shieldArrowRemoval(entity).active();
    }

    public static HumanoidArm pullingArm(LivingEntity entity) {
        return handArm(entity, pullingHand(ClientArrowState.shieldArrowRemoval(entity).shieldHand()));
    }

    public static float pullProgress(LivingEntity entity) {
        return pullProgress(ClientArrowState.shieldArrowRemoval(entity).ticks());
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

    private static boolean hasShieldArrows(LocalPlayer player, InteractionHand hand) {
        ItemStack shield = player.getItemInHand(hand);
        return !shield.isEmpty()
                && shield.canPerformAction(ItemAbilities.SHIELD_BLOCK)
                && !LodgedShieldArrowStorage.readAll(shield).isEmpty();
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
        float value = Math.min(ticks / PULL_EASE_TICKS, 1.0F);
        return 1.0F - ((1.0F - value) * (1.0F - value));
    }

    private static float smoothStep(float value) {
        return value * value * (3.0F - (2.0F * value));
    }

    private static void clearLocalState() {
        activeShieldHand = null;
        activeTicks = 0;
    }
}
