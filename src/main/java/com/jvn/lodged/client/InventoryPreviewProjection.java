package com.jvn.lodged.client;

import com.jvn.lodged.world.LodgedArrowBodyPart;
import com.jvn.lodged.world.LodgedArrowVisual;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

final class InventoryPreviewProjection {
    static final int MODEL_LEFT = 26;
    static final int MODEL_TOP = 8;
    static final int MODEL_RIGHT = 75;
    static final int MODEL_BOTTOM = 78;
    private static final float PREVIEW_SCALE = 30.0F;
    private static final float PREVIEW_Y_OFFSET = 0.0625F;
    private static final float PLAYER_RENDER_SCALE = 0.9375F;
    private static final float MODEL_RENDER_Y_OFFSET = -1.501F;
    private static final float DEGREES_TO_RADIANS = (float) (Math.PI / 180.0D);
    private static final float ARM_ORIGIN_X = 5.0F / 16.0F;
    private static final float ARM_ORIGIN_Y = 2.0F / 16.0F;
    private static final float HELD_ITEM_LAYER_X_ROT = -90.0F * DEGREES_TO_RADIANS;
    private static final float HELD_ITEM_LAYER_Y_ROT = 180.0F * DEGREES_TO_RADIANS;
    private static final float HELD_ITEM_ARM_X_OFFSET = 1.0F / 16.0F;
    private static final float HELD_ITEM_ARM_Y_OFFSET = 0.125F;
    private static final float HELD_ITEM_ARM_Z_OFFSET = -0.625F;
    private static final float SHIELD_ITEM_X_ROT = 0.0F;
    private static final float SHIELD_ITEM_Y_ROT = 90.0F * DEGREES_TO_RADIANS;
    private static final float SHIELD_ITEM_Z_ROT = 0.0F;
    private static final float SHIELD_THIRD_PERSON_X = 10.0F / 16.0F;
    private static final float SHIELD_THIRD_PERSON_Y = 6.0F / 16.0F;
    private static final float SHIELD_THIRD_PERSON_RIGHT_Z = -4.0F / 16.0F;
    private static final float SHIELD_THIRD_PERSON_LEFT_Z = 12.0F / 16.0F;
    private static final float ITEM_RENDERER_MODEL_OFFSET = -0.5F;
    private static final float ITEM_ARM_X_ROT = -(float) (Math.PI / 10.0D);
    private static float yawDegrees;
    private static boolean customYawActive;

    private InventoryPreviewProjection() {
    }

    static Vector3f projectArrowToScreen(
            AbstractContainerScreen<?> screen,
            LocalPlayer player,
            LodgedArrowVisual arrow,
            double mouseX,
            double mouseY) {
        int left = screen.getGuiLeft();
        int top = screen.getGuiTop();
        float centerX = left + ((MODEL_LEFT + MODEL_RIGHT) / 2.0F);
        float centerY = top + ((MODEL_TOP + MODEL_BOTTOM) / 2.0F);
        Vector3f modelPosition = renderModelPosition(player, arrow, centerX, centerY, mouseX, mouseY);
        return projectModelToScreen(player, modelPosition, centerX, centerY, mouseX, mouseY);
    }

    static Vector3f projectShieldArrowToScreen(
            AbstractContainerScreen<?> screen,
            LocalPlayer player,
            LodgedArrowVisual arrow,
            InteractionHand hand,
            double mouseX,
            double mouseY) {
        int left = screen.getGuiLeft();
        int top = screen.getGuiTop();
        float centerX = left + ((MODEL_LEFT + MODEL_RIGHT) / 2.0F);
        float centerY = top + ((MODEL_TOP + MODEL_BOTTOM) / 2.0F);
        return projectModelToScreen(
                player,
                shieldModelPosition(player, arrow, hand),
                centerX,
                centerY,
                mouseX,
                mouseY);
    }

    private static Vector3f shieldModelPosition(LocalPlayer player, LodgedArrowVisual arrow, InteractionHand hand) {
        HumanoidArm arm = hand == InteractionHand.MAIN_HAND
                ? player.getMainArm()
                : player.getMainArm().getOpposite();
        boolean leftHand = arm == HumanoidArm.LEFT;
        float armSide = leftHand ? 1.0F : -1.0F;
        float itemTransformSide = leftHand ? -1.0F : 1.0F;
        float shieldZ = leftHand ? SHIELD_THIRD_PERSON_LEFT_Z : SHIELD_THIRD_PERSON_RIGHT_Z;

        return new Matrix4f()
                .translate(armSide * ARM_ORIGIN_X, ARM_ORIGIN_Y, 0.0F)
                .rotate(new Quaternionf().rotationZYX(0.0F, 0.0F, ITEM_ARM_X_ROT))
                .rotateX(HELD_ITEM_LAYER_X_ROT)
                .rotateY(HELD_ITEM_LAYER_Y_ROT)
                .translate(-armSide * HELD_ITEM_ARM_X_OFFSET, HELD_ITEM_ARM_Y_OFFSET, HELD_ITEM_ARM_Z_OFFSET)
                .translate(itemTransformSide * SHIELD_THIRD_PERSON_X, SHIELD_THIRD_PERSON_Y, shieldZ)
                .rotate(new Quaternionf().rotationXYZ(
                        SHIELD_ITEM_X_ROT,
                        itemTransformSide * SHIELD_ITEM_Y_ROT,
                        itemTransformSide * SHIELD_ITEM_Z_ROT))
                .translate(ITEM_RENDERER_MODEL_OFFSET, ITEM_RENDERER_MODEL_OFFSET, ITEM_RENDERER_MODEL_OFFSET)
                .scale(1.0F, -1.0F, -1.0F)
                .transformPosition(arrow.modelX(), arrow.modelY(), arrow.modelZ(), new Vector3f());
    }

    static Vector3f projectModelToScreen(
            LocalPlayer player,
            Vector3f modelPosition,
            float centerX,
            float centerY,
            double mouseX,
            double mouseY) {
        float entityScale = player.getScale();
        float previewScale = PREVIEW_SCALE / entityScale;
        float pitchRadians = currentPreviewPitchRadians(centerY, mouseY);
        float yawRadians = -currentPreviewYawDegrees(centerX, mouseX) * ((float) Math.PI / 180.0F);

        Matrix4f matrix = new Matrix4f()
                .translation(centerX, centerY, 50.0F)
                .scale(previewScale, previewScale, -previewScale)
                .translate(0.0F, (player.getBbHeight() / 2.0F) + (PREVIEW_Y_OFFSET * entityScale), 0.0F)
                .rotateZ((float) Math.PI)
                .rotateX(pitchRadians)
                .scale(entityScale, entityScale, entityScale)
                .rotateY(yawRadians)
                .scale(-1.0F, -1.0F, 1.0F)
                .scale(PLAYER_RENDER_SCALE, PLAYER_RENDER_SCALE, PLAYER_RENDER_SCALE)
                .translate(0.0F, MODEL_RENDER_Y_OFFSET, 0.0F);
        return matrix.transformPosition(modelPosition.x(), modelPosition.y(), modelPosition.z(), new Vector3f());
    }

    static Vector3f renderModelPosition(
            LocalPlayer player,
            LodgedArrowVisual arrow,
            float centerX,
            float centerY,
            double mouseX,
            double mouseY) {
        if (Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(player) instanceof PlayerRenderer renderer) {
            return LodgedArrowRenderHelper.humanoidModelPosition(renderer.getModel(), arrow);
        }

        if (arrow.bodyPart() != LodgedArrowBodyPart.HEAD) {
            return new Vector3f(arrow.modelX(), arrow.modelY(), arrow.modelZ());
        }

        return new Matrix4f()
                .rotate(new Quaternionf().rotationZYX(
                        0.0F,
                        currentHeadYawRadians(centerX, mouseX),
                        currentHeadPitchRadians(centerY, mouseY)))
                .transformPosition(arrow.modelX(), arrow.modelY(), arrow.modelZ(), new Vector3f());
    }

    private static float currentPreviewPitchRadians(float centerY, double mouseY) {
        float pitch = (float) Math.atan((centerY - mouseY) / 40.0F);
        return pitch * 20.0F * DEGREES_TO_RADIANS;
    }

    private static float currentPreviewYawDegrees(float centerX, double mouseX) {
        if (customYawActive) {
            return yawDegrees;
        }
        return (float) Math.atan((centerX - mouseX) / 40.0F) * 20.0F;
    }

    static float currentHeadYawRadians(float centerX, double mouseX) {
        if (customYawActive) {
            return 0.0F;
        }
        return (float) Math.atan((centerX - mouseX) / 40.0F) * 20.0F * DEGREES_TO_RADIANS;
    }

    static float currentHeadPitchRadians(float centerY, double mouseY) {
        return -currentPreviewPitchRadians(centerY, mouseY);
    }

    static boolean isInPlayerPreviewArea(AbstractContainerScreen<?> screen, double mouseX, double mouseY) {
        int left = screen.getGuiLeft();
        int top = screen.getGuiTop();
        return mouseX >= left + MODEL_LEFT
                && mouseX < left + MODEL_RIGHT
                && mouseY >= top + MODEL_TOP
                && mouseY < top + MODEL_BOTTOM;
    }

    static float yawDegrees() {
        return yawDegrees;
    }

    static boolean customYawActive() {
        return customYawActive;
    }

    static void enableCustomYaw() {
        customYawActive = true;
    }

    static void rotate(float deltaDegrees) {
        yawDegrees += deltaDegrees;
        customYawActive = true;
    }

    static void resetRotation() {
        yawDegrees = 0.0F;
        customYawActive = false;
    }
}
