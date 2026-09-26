package com.jvn.lodged.client;

import com.jvn.lodged.item.BandageItem;
import com.jvn.lodged.item.LodgedItems;
import com.jvn.lodged.network.ClientBandageState;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;

public final class BandageAnimation {
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
        TreatmentAnimation.BandageMotion motion = TreatmentAnimation.bandage(useProgress(player, partialTick));
        float amount = holdingBandage ? motion.amount() : motion.supportAmount();
        float stroke = motion.stroke();
        float orbit = motion.orbit();
        float press = motion.press();
        float directedStroke = holdingBandage ? -stroke : stroke;
        float side = arm == HumanoidArm.RIGHT ? 1.0F : -1.0F;

        float inward = holdingBandage ? 0.35F : 0.45F;
        float lift = holdingBandage ? 0.20F : 0.28F;
        float depth = holdingBandage ? 0.10F : 0.08F;
        float xMotion = holdingBandage ? directedStroke * 0.09F - press * 0.035F : 0.0F;
        float yMotion = holdingBandage ? orbit * 0.07F - press * 0.025F : orbit * 0.008F;
        float zMotion = holdingBandage ? directedStroke * 0.045F - press * 0.04F : 0.0F;
        float roleXOffset = side * amount * (holdingBandage ? 0.33F : 0.43F);
        float roleYOffset = amount * (holdingBandage ? 0.0F : -0.34F);
        float roleDepthOffset = amount * (holdingBandage ? -0.23F : -0.28F);

        poseStack.pushPose();
        poseStack.translate(
                side * (0.64F - amount * inward + xMotion) + roleXOffset,
                -0.60F - equippedProgress * 0.6F * (1.0F - amount)
                        + amount * lift + yMotion + roleYOffset,
                -0.72F + amount * depth + zMotion + roleDepthOffset);
        poseStack.mulPose(Axis.YP.rotationDegrees(
                side * (45.0F - amount * (holdingBandage ? 32.0F : 50.0F))));
        poseStack.mulPose(Axis.XP.rotationDegrees(
                amount * (holdingBandage ? -22.0F : -12.0F)
                        + orbit * (holdingBandage ? 5.0F : 1.0F)
                        - press * (holdingBandage ? 7.0F : 0.0F)));
        poseStack.mulPose(Axis.ZP.rotationDegrees(
                side * amount * (holdingBandage ? -28.0F : -52.0F)
                        + side * directedStroke * (holdingBandage ? 16.0F : 1.0F)
                        - side * press * (holdingBandage ? 5.0F : 0.0F)));
        FirstPersonArmRenderer.render(player, arm, poseStack, buffer, packedLight);
        poseStack.popPose();
    }

    public static ArmPose thirdPersonArmPose(LivingEntity entity, HumanoidArm arm, float ageInTicks) {
        HumanoidArm holdingArm = holdingArm(entity);
        if (holdingArm == null) {
            return null;
        }

        float partialTick = Mth.clamp(ageInTicks - entity.tickCount, 0.0F, 1.0F);
        TreatmentAnimation.BandageMotion motion = TreatmentAnimation.bandage(useProgress(entity, partialTick));
        float side = arm == HumanoidArm.RIGHT ? 1.0F : -1.0F;
        boolean wrappingArm = arm == holdingArm;

        float xRot = wrappingArm
                ? -1.30F + motion.orbit() * 0.16F - motion.press() * 0.08F
                : -1.18F + motion.orbit() * 0.015F;
        float yRot = wrappingArm
                ? side * (-0.82F + motion.stroke() * 0.22F - motion.press() * 0.05F)
                : side * -0.62F;
        float zRot = wrappingArm
                ? side * (0.16F + motion.orbit() * 0.08F - motion.press() * 0.06F)
                : side * (0.03F + motion.stroke() * 0.01F);
        float amount = wrappingArm ? motion.amount() : motion.supportAmount();
        return new ArmPose(arm, amount, xRot, yRot, zRot);
    }

    public static float armPoseAmount(LivingEntity entity, HumanoidArm arm, float partialTick) {
        TreatmentAnimation.BandageMotion motion = TreatmentAnimation.bandage(useProgress(entity, partialTick));
        return arm == holdingArm(entity) ? motion.amount() : motion.supportAmount();
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

    public record ArmPose(HumanoidArm arm, float amount, float xRot, float yRot, float zRot) {
    }
}
