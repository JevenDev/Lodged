package com.jvn.lodged.mixin.client;

import com.jvn.lodged.client.LodgedShieldArrowRemovalClient;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemInHandRenderer.class)
public abstract class ItemInHandRendererMixin {
    @Shadow
    public abstract void renderItem(
            LivingEntity entity,
            ItemStack itemStack,
            ItemDisplayContext displayContext,
            boolean leftHand,
            PoseStack poseStack,
            MultiBufferSource buffer,
            int seed);

    @Inject(method = "renderArmWithItem", at = @At("HEAD"), cancellable = true)
    private void lodged$renderShieldArrowPullingArm(
            AbstractClientPlayer player,
            float partialTicks,
            float pitch,
            InteractionHand hand,
            float swingProgress,
            ItemStack stack,
            float equippedProgress,
            PoseStack poseStack,
            MultiBufferSource buffer,
            int combinedLight,
            CallbackInfo callbackInfo) {
        if (!(player instanceof LocalPlayer localPlayer)
                || !LodgedShieldArrowRemovalClient.shouldRenderFirstPersonPullingHand(localPlayer, hand)) {
            return;
        }

        lodged$renderDroppingHeldItem(
                localPlayer, hand, stack, partialTicks, equippedProgress, poseStack, buffer, combinedLight);
        LodgedShieldArrowRemovalClient.renderFirstPersonPullingArm(
                localPlayer, hand, partialTicks, equippedProgress, poseStack, buffer, combinedLight);
        callbackInfo.cancel();
    }

    private void lodged$renderDroppingHeldItem(
            LocalPlayer player,
            InteractionHand hand,
            ItemStack stack,
            float partialTicks,
            float equippedProgress,
            PoseStack poseStack,
            MultiBufferSource buffer,
            int combinedLight) {
        if (stack.isEmpty()) {
            return;
        }

        float drop = LodgedShieldArrowRemovalClient.firstPersonHeldItemDropProgress(player, hand, partialTicks);
        if (drop >= 1.0F) {
            return;
        }

        HumanoidArm arm = hand == InteractionHand.MAIN_HAND ? player.getMainArm() : player.getMainArm().getOpposite();
        boolean rightHand = arm == HumanoidArm.RIGHT;
        float side = rightHand ? 1.0F : -1.0F;
        poseStack.pushPose();
        poseStack.translate(side * 0.56F, -0.52F + (equippedProgress * -0.6F) - (drop * 1.15F), -0.72F);
        poseStack.mulPose(Axis.YP.rotationDegrees(side * 45.0F));
        this.renderItem(
                player,
                stack,
                rightHand ? ItemDisplayContext.FIRST_PERSON_RIGHT_HAND : ItemDisplayContext.FIRST_PERSON_LEFT_HAND,
                !rightHand,
                poseStack,
                buffer,
                combinedLight);
        poseStack.popPose();
    }
}
