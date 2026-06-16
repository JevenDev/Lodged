package com.jvn.lodged.mixin.client;

import com.jvn.lodged.client.LodgedInventoryArrowUi;
import com.jvn.lodged.config.LodgedConfig;
import com.jvn.lodged.network.ClientArrowState;
import com.jvn.lodged.world.LodgedArrowVisual;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.OutlineBufferSource;
import net.minecraft.client.renderer.entity.layers.ArrowLayer;
import net.minecraft.client.renderer.entity.layers.StuckInBodyLayer;
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
    private static final float MODEL_HEAD_BOTTOM = 0.0F;
    private static final float MODEL_LEG_TOP = 12.0F / 16.0F;
    private static final float MODEL_BODY_HALF_WIDTH = 4.0F / 16.0F;

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
            ArrowAnchor anchor = anchorFor(lodged$getParentModel(), arrow);
            anchor.part().translateAndRotate(poseStack);
            poseStack.translate(anchor.localX(), anchor.localY(), anchor.localZ());
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
            poseStack.popPose();
        }
    }

    private static OutlineBufferSource createOutlineBuffer(MultiBufferSource buffer, boolean highlighted) {
        if (!highlighted || !(buffer instanceof MultiBufferSource.BufferSource bufferSource)) {
            return null;
        }

        OutlineBufferSource outlineBuffer = new OutlineBufferSource(bufferSource);
        outlineBuffer.setColor(HOVER_OUTLINE_RED, HOVER_OUTLINE_GREEN, HOVER_OUTLINE_BLUE, HOVER_OUTLINE_ALPHA);
        LodgedInventoryArrowUi.markOutlineRendered();
        return outlineBuffer;
    }

    @SuppressWarnings("unchecked")
    private PlayerModel<LivingEntity> lodged$getParentModel() {
        return ((StuckInBodyLayer<LivingEntity, PlayerModel<LivingEntity>>) (Object) this).getParentModel();
    }

    private static ArrowAnchor anchorFor(PlayerModel<LivingEntity> model, LodgedArrowVisual arrow) {
        ModelPart part = partFor(model, arrow);
        PartPose initialPose = part.getInitialPose();
        return new ArrowAnchor(
                part,
                arrow.modelX() - (initialPose.x / 16.0F),
                arrow.modelY() - (initialPose.y / 16.0F),
                arrow.modelZ() - (initialPose.z / 16.0F));
    }

    private static ModelPart partFor(PlayerModel<LivingEntity> model, LodgedArrowVisual arrow) {
        if (arrow.modelY() <= MODEL_HEAD_BOTTOM) {
            return model.head;
        }

        if (arrow.modelY() >= MODEL_LEG_TOP) {
            return arrow.modelX() < 0.0F ? model.rightLeg : model.leftLeg;
        }

        if (arrow.modelX() < -MODEL_BODY_HALF_WIDTH) {
            return model.rightArm;
        }

        if (arrow.modelX() > MODEL_BODY_HALF_WIDTH) {
            return model.leftArm;
        }

        return model.body;
    }

    private record ArrowAnchor(ModelPart part, float localX, float localY, float localZ) {
    }
}
