package com.jvn.lodged.mixin.client;

import com.jvn.lodged.client.LodgedInventoryArrowUi;
import com.jvn.lodged.config.LodgedConfig;
import com.jvn.lodged.network.ClientArrowState;
import com.jvn.lodged.world.LodgedArrowVisual;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.OutlineBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.layers.ArrowLayer;
import net.minecraft.client.renderer.entity.layers.StuckInBodyLayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(StuckInBodyLayer.class)
public abstract class StuckInBodyLayerMixin {
    private static final int HOVER_OUTLINE_RED = 255;
    private static final int HOVER_OUTLINE_GREEN = 241;
    private static final int HOVER_OUTLINE_BLUE = 168;
    private static final int HOVER_OUTLINE_ALPHA = 255;
    private static final float HOVER_OUTLINE_RED_FLOAT = HOVER_OUTLINE_RED / 255.0F;
    private static final float HOVER_OUTLINE_GREEN_FLOAT = HOVER_OUTLINE_GREEN / 255.0F;
    private static final float HOVER_OUTLINE_BLUE_FLOAT = HOVER_OUTLINE_BLUE / 255.0F;
    private static final float ARROW_RENDER_SCALE = 0.05625F;

    @Shadow
    protected abstract void renderStuckItem(
            PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight,
            Entity entity,
            float x,
            float y,
            float z,
            float partialTick);

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void lodged$renderTrackedPlayerArrows(
            PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight,
            LivingEntity livingEntity,
            float limbSwing,
            float limbSwingAmount,
            float partialTicks,
            float ageInTicks,
            float netHeadYaw,
            float headPitch,
            CallbackInfo callbackInfo) {
        if (!LodgedConfig.enablePlayerArrowRemoval()
                || !((Object) this instanceof ArrowLayer)
                || livingEntity != Minecraft.getInstance().player) {
            return;
        }

        List<LodgedArrowVisual> arrows = ClientArrowState.removableArrows();
        int arrowCount = Math.min(arrows.size(), livingEntity.getArrowCount());
        arrowCount = Math.min(arrowCount, LodgedConfig.maxRemovablePlayerArrows());
        if (arrowCount <= 0) {
            return;
        }

        callbackInfo.cancel();
        int hoveredArrowIndex = LodgedInventoryArrowUi.hoveredArrowIndex();
        for (int index = 0; index < arrowCount; index++) {
            LodgedArrowVisual arrow = arrows.get(index);
            boolean highlighted = index == hoveredArrowIndex;
            poseStack.pushPose();
            poseStack.translate(arrow.modelX(), arrow.modelY(), arrow.modelZ());
            OutlineBufferSource outlineBuffer = createOutlineBuffer(buffer, highlighted);
            this.renderStuckItem(
                    poseStack,
                    outlineBuffer == null ? buffer : outlineBuffer,
                    packedLight,
                    livingEntity,
                    arrow.directionX(),
                    arrow.directionY(),
                    arrow.directionZ(),
                    partialTicks);
            if (outlineBuffer != null) {
                outlineBuffer.endOutlineBatch();
            }
            if (highlighted) {
                renderArrowLineOutline(poseStack, buffer, arrow);
            }
            poseStack.popPose();
        }
    }

    private static OutlineBufferSource createOutlineBuffer(MultiBufferSource buffer, boolean highlighted) {
        if (!highlighted || !(buffer instanceof MultiBufferSource.BufferSource bufferSource)) {
            return null;
        }

        OutlineBufferSource outlineBuffer = new OutlineBufferSource(bufferSource);
        outlineBuffer.setColor(HOVER_OUTLINE_RED, HOVER_OUTLINE_GREEN, HOVER_OUTLINE_BLUE, HOVER_OUTLINE_ALPHA);
        return outlineBuffer;
    }

    private static void renderArrowLineOutline(PoseStack poseStack, MultiBufferSource buffer, LodgedArrowVisual arrow) {
        float horizontalLength = Mth.sqrt((arrow.directionX() * arrow.directionX()) + (arrow.directionZ() * arrow.directionZ()));
        float yRot = (float) (Math.atan2(arrow.directionX(), arrow.directionZ()) * 180.0F / Math.PI);
        float xRot = (float) (Math.atan2(arrow.directionY(), horizontalLength) * 180.0F / Math.PI);

        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(yRot - 90.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(xRot));
        poseStack.mulPose(Axis.XP.rotationDegrees(45.0F));
        poseStack.scale(ARROW_RENDER_SCALE, ARROW_RENDER_SCALE, ARROW_RENDER_SCALE);
        poseStack.translate(-4.0F, 0.0F, 0.0F);
        LevelRenderer.renderLineBox(
                poseStack,
                buffer.getBuffer(RenderType.lines()),
                -8.75D,
                -2.75D,
                -2.75D,
                8.75D,
                2.75D,
                2.75D,
                HOVER_OUTLINE_RED_FLOAT,
                HOVER_OUTLINE_GREEN_FLOAT,
                HOVER_OUTLINE_BLUE_FLOAT,
                1.0F);
        poseStack.popPose();
    }
}
