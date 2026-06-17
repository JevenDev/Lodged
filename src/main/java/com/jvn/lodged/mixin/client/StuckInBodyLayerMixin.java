package com.jvn.lodged.mixin.client;

import com.jvn.lodged.client.LodgedInventoryArrowUi;
import com.jvn.lodged.config.LodgedConfig;
import com.jvn.lodged.network.ClientArrowState;
import com.jvn.lodged.network.ClientArrowState.EntityArrows;
import com.jvn.lodged.world.LodgedArrowBodyPart;
import com.jvn.lodged.world.LodgedArrowVisual;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
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
    private static final int HOVER_HIGHLIGHT_RED = 255;
    private static final int HOVER_HIGHLIGHT_GREEN = 241;
    private static final int HOVER_HIGHLIGHT_BLUE = 168;
    private static final int HOVER_HIGHLIGHT_ALPHA = 255;
    private static final int HOVER_OUTLINE_BUFFER_SIZE = 1536;

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
    private void lodged$renderTrackedArrows(
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
        if (!((Object) this instanceof ArrowLayer)) {
            return;
        }

        boolean isLocalPlayer = livingEntity == Minecraft.getInstance().player;
        List<LodgedArrowVisual> arrows = arrowsFor(livingEntity, isLocalPlayer);
        int arrowCount = arrowCountFor(livingEntity, arrows, isLocalPlayer);
        if (arrowCount <= 0) {
            if (isLocalPlayer && ClientArrowState.hasSynced()) {
                callbackInfo.cancel();
            }
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
            renderArrow(poseStack, buffer, packedLight, livingEntity, arrow, partialTicks);
            if (highlighted) {
                renderHoverOutline(poseStack, packedLight, livingEntity, arrow, partialTicks);
            }
            poseStack.popPose();
        }
    }

    private static List<LodgedArrowVisual> arrowsFor(LivingEntity livingEntity, boolean isLocalPlayer) {
        if (isLocalPlayer && LodgedConfig.enablePlayerArrowRemoval()) {
            return ClientArrowState.removableArrows();
        }

        return ClientArrowState.entityArrows(livingEntity).arrows();
    }

    private static int arrowCountFor(LivingEntity livingEntity, List<LodgedArrowVisual> arrows, boolean isLocalPlayer) {
        if (arrows.isEmpty()) {
            return 0;
        }

        if (isLocalPlayer && LodgedConfig.enablePlayerArrowRemoval()) {
            int arrowCount = Math.min(arrows.size(), ClientArrowState.syncedArrowCount());
            return Math.min(arrowCount, LodgedConfig.maxRemovablePlayerArrows());
        }

        EntityArrows entityArrows = ClientArrowState.entityArrows(livingEntity);
        return Math.min(arrows.size(), entityArrows.arrowCount());
    }

    private void renderHoverOutline(
            PoseStack poseStack,
            int packedLight,
            LivingEntity livingEntity,
            LodgedArrowVisual arrow,
            float partialTicks) {
        if (!LodgedInventoryArrowUi.prepareArrowOutlineTarget()) {
            return;
        }

        MultiBufferSource.BufferSource delegate = MultiBufferSource.immediate(new ByteBufferBuilder(HOVER_OUTLINE_BUFFER_SIZE));
        OutlineBufferSource outlineBuffer = new OutlineBufferSource(delegate);
        outlineBuffer.setColor(HOVER_HIGHLIGHT_RED, HOVER_HIGHLIGHT_GREEN, HOVER_HIGHLIGHT_BLUE, HOVER_HIGHLIGHT_ALPHA);
        try {
            renderArrow(poseStack, outlineBuffer, packedLight, livingEntity, arrow, partialTicks);
            outlineBuffer.endOutlineBatch();
        } finally {
            LodgedInventoryArrowUi.restoreVanillaEntityTarget();
        }
        Minecraft.getInstance().getMainRenderTarget().bindWrite(false);
        LodgedInventoryArrowUi.processArrowOutlineTarget();
    }

    private void renderArrow(
            PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight,
            LivingEntity livingEntity,
            LodgedArrowVisual arrow,
            float partialTicks) {
        this.renderStuckItem(
                poseStack,
                buffer,
                packedLight,
                livingEntity,
                arrow.directionX(),
                arrow.directionY(),
                arrow.directionZ(),
                partialTicks);
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
        LodgedArrowBodyPart bodyPart = arrow.bodyPart();
        return switch (bodyPart) {
            case HEAD -> model.head;
            case LEG -> arrow.modelX() < 0.0F ? model.rightLeg : model.leftLeg;
            case ARM -> arrow.modelX() < 0.0F ? model.rightArm : model.leftArm;
            case CHEST -> model.body;
        };
    }

    private record ArrowAnchor(ModelPart part, float localX, float localY, float localZ) {
    }
}
