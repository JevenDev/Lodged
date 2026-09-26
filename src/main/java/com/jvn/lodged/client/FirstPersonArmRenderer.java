package com.jvn.lodged.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.world.entity.HumanoidArm;

public final class FirstPersonArmRenderer {
    private static boolean renderingArm;

    private FirstPersonArmRenderer() {
    }

    public static boolean isRenderingArm() {
        return renderingArm;
    }

    static void render(
            AbstractClientPlayer player,
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
        boolean wasRenderingArm = renderingArm;
        renderingArm = true;
        try {
            if (arm == HumanoidArm.RIGHT) {
                renderer.renderRightHand(poseStack, buffer, packedLight, player);
            } else {
                renderer.renderLeftHand(poseStack, buffer, packedLight, player);
            }
        } finally {
            renderingArm = wasRenderingArm;
        }
    }
}
