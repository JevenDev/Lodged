package com.jvn.lodged.client;

import com.jvn.lodged.item.BandageItem;
import com.jvn.lodged.item.LodgedItems;
import com.jvn.lodged.network.ClientBandageState;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;

public final class BandageAnimation {
    private static final float ENTRY_END = 0.18F;
    private static final float EXIT_START = 0.90F;

    private BandageAnimation() {
    }

    public static boolean isUsingBandage(LivingEntity entity) {
        return isUsingHeldBandage(entity) || ClientBandageState.isActive(entity);
    }

    public static InteractionHand usedHand(LivingEntity entity) {
        if (isUsingHeldBandage(entity)) {
            return entity.getUsedItemHand();
        }
        return ClientBandageState.isActive(entity)
                ? ClientBandageState.hand(entity)
                : null;
    }

    public static HumanoidArm holdingArm(LivingEntity entity) {
        InteractionHand usedHand = usedHand(entity);
        if (usedHand == null) {
            return null;
        }

        return usedHand == InteractionHand.MAIN_HAND
                ? entity.getMainArm()
                : entity.getMainArm().getOpposite();
    }

    public static HumanoidArm treatedArm(LivingEntity entity) {
        HumanoidArm holdingArm = holdingArm(entity);
        return holdingArm == null ? null : holdingArm.getOpposite();
    }

    public static void renderFirstPersonBandagingArms(
            LocalPlayer player,
            float partialTick,
            float equippedProgress,
            PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight) {
        HumanoidArm holdingArm = holdingArm(player);
        if (holdingArm == null || player.isInvisible()) {
            return;
        }

        renderFirstPersonArm(
                player,
                holdingArm.getOpposite(),
                false,
                partialTick,
                equippedProgress,
                poseStack,
                buffer,
                packedLight);
        renderFirstPersonArm(
                player,
                holdingArm,
                true,
                partialTick,
                equippedProgress,
                poseStack,
                buffer,
                packedLight);
    }

    private static void renderFirstPersonArm(
            LocalPlayer player,
            HumanoidArm arm,
            boolean holdingBandage,
            float partialTick,
            float equippedProgress,
            PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight) {
        float progress = useProgress(player, partialTick);
        float amount = movementAmount(progress);
        float wrappingProgress = wrappingProgress(progress);
        float envelope = wrappingEnvelope(wrappingProgress);
        float stroke = Mth.sin(wrappingProgress * Mth.PI * 6.0F) * envelope;
        float orbit = Mth.cos(wrappingProgress * Mth.PI * 6.0F) * envelope;
        float directedStroke = holdingBandage ? -stroke : stroke;
        float side = arm == HumanoidArm.RIGHT ? 1.0F : -1.0F;

        float inward = holdingBandage ? 0.35F : 0.45F;
        float lift = holdingBandage ? 0.20F : 0.28F;
        float depth = holdingBandage ? 0.10F : 0.08F;
        float xMotion = holdingBandage ? directedStroke * 0.07F : 0.0F;
        float yMotion = holdingBandage ? orbit * 0.06F : orbit * 0.015F;
        float zMotion = holdingBandage ? directedStroke * 0.03F : 0.0F;
        float roleXOffset = amount * (holdingBandage ? 0.33F : -0.43F);
        float roleYOffset = amount * (holdingBandage ? 0.0F : -0.34F);
        float roleDepthOffset = amount * (holdingBandage ? -0.23F : -0.28F);

        poseStack.pushPose();
        poseStack.translate(
                side * (0.72F - amount * inward + xMotion) + roleXOffset,
                -0.60F - equippedProgress * 0.6F + amount * lift + yMotion + roleYOffset,
                -0.72F + amount * depth + zMotion + roleDepthOffset);
        poseStack.mulPose(Axis.YP.rotationDegrees(
                side * (45.0F - amount * (holdingBandage ? 32.0F : 50.0F))));
        poseStack.mulPose(Axis.XP.rotationDegrees(
                amount * (holdingBandage ? -22.0F : -12.0F)
                        + orbit * (holdingBandage ? 4.0F : 1.5F)));
        poseStack.mulPose(Axis.ZP.rotationDegrees(
                side * amount * (holdingBandage ? -28.0F : -52.0F)
                        + side * directedStroke * (holdingBandage ? 14.0F : 2.0F)));
        renderPlayerArm(player, arm, poseStack, buffer, packedLight);
        poseStack.popPose();
    }

    public static ArmPose thirdPersonArmPose(LivingEntity entity, HumanoidArm arm, float ageInTicks) {
        HumanoidArm holdingArm = holdingArm(entity);
        if (holdingArm == null) {
            return null;
        }

        float partialTick = Mth.clamp(ageInTicks - entity.tickCount, 0.0F, 1.0F);
        float progress = useProgress(entity, partialTick);
        float amount = movementAmount(progress);
        float wrappingProgress = wrappingProgress(progress);
        float envelope = wrappingEnvelope(wrappingProgress);
        float stroke = Mth.sin(wrappingProgress * Mth.PI * 6.0F) * envelope;
        float orbit = Mth.cos(wrappingProgress * Mth.PI * 6.0F) * envelope;
        float side = arm == HumanoidArm.RIGHT ? 1.0F : -1.0F;
        boolean wrappingArm = arm == holdingArm;

        float xRot = wrappingArm
                ? -1.25F + orbit * 0.10F
                : -1.08F + orbit * 0.025F;
        float yRot = wrappingArm
                ? side * (-0.72F + stroke * 0.14F)
                : side * -0.52F;
        float zRot = wrappingArm
                ? side * (0.12F + orbit * 0.06F)
                : side * (0.04F + stroke * 0.015F);
        return new ArmPose(arm, amount, xRot, yRot, zRot);
    }

    private static void renderPlayerArm(
            LocalPlayer player,
            HumanoidArm arm,
            PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight) {
        float side = arm == HumanoidArm.RIGHT ? 1.0F : -1.0F;
        poseStack.translate(side * -1.0F, 3.6F, 3.5F);
        poseStack.mulPose(Axis.ZP.rotationDegrees(side * 120.0F));
        poseStack.mulPose(Axis.XP.rotationDegrees(200.0F));
        poseStack.mulPose(Axis.YP.rotationDegrees(side * -135.0F));
        poseStack.translate(side * 5.6F, 0.0F, 0.0F);

        PlayerRenderer renderer =
                (PlayerRenderer) Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(player);
        if (arm == HumanoidArm.RIGHT) {
            renderer.renderRightHand(poseStack, buffer, packedLight, player);
        } else {
            renderer.renderLeftHand(poseStack, buffer, packedLight, player);
        }
    }

    private static float useProgress(LivingEntity entity, float partialTick) {
        int ticks = isUsingHeldBandage(entity)
                ? entity.getTicksUsingItem()
                : ClientBandageState.ticks(entity);
        return Mth.clamp(
                (ticks + partialTick) / BandageItem.USE_DURATION,
                0.0F,
                1.0F);
    }

    private static boolean isUsingHeldBandage(LivingEntity entity) {
        return entity.isUsingItem()
                && entity.getUseItem().is(LodgedItems.BANDAGE.get());
    }

    private static float movementAmount(float progress) {
        float enter = smootherStep(Mth.clamp(progress / ENTRY_END, 0.0F, 1.0F));
        float exit = 1.0F - smootherStep(Mth.clamp(
                (progress - EXIT_START) / (1.0F - EXIT_START),
                0.0F,
                1.0F));
        return enter * exit;
    }

    private static float wrappingProgress(float progress) {
        return Mth.clamp(
                (progress - ENTRY_END) / (EXIT_START - ENTRY_END),
                0.0F,
                1.0F);
    }

    private static float wrappingEnvelope(float wrappingProgress) {
        float enter = smootherStep(Mth.clamp(wrappingProgress / 0.12F, 0.0F, 1.0F));
        float exit = smootherStep(Mth.clamp((1.0F - wrappingProgress) / 0.12F, 0.0F, 1.0F));
        return enter * exit;
    }

    private static float smootherStep(float value) {
        return value * value * value * (value * (value * 6.0F - 15.0F) + 10.0F);
    }

    public record ArmPose(HumanoidArm arm, float amount, float xRot, float yRot, float zRot) {
    }
}
