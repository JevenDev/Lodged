package com.jvn.lodged.client;

import com.jvn.lodged.network.ClientArrowState;
import com.jvn.lodged.world.LodgedArrowBodyPart;
import com.jvn.lodged.world.LodgedArrowVisual;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.List;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.ItemStack;

public final class LodgedEntityArrowLayer<T extends LivingEntity, M extends EntityModel<T>> extends RenderLayer<T, M> {
    private final EntityRenderDispatcher dispatcher;

    public LodgedEntityArrowLayer(RenderLayerParent<T, M> renderer, EntityRenderDispatcher dispatcher) {
        super(renderer);
        this.dispatcher = dispatcher;
    }

    @Override
    public void render(
            PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight,
            T livingEntity,
            float limbSwing,
            float limbSwingAmount,
            float partialTicks,
            float ageInTicks,
            float netHeadYaw,
            float headPitch) {
        if (livingEntity instanceof Player) {
            return;
        }

        ClientArrowState.EntityArrows entityArrows = ClientArrowState.entityArrows(livingEntity);
        List<LodgedArrowVisual> arrows = entityArrows.arrows();
        int arrowCount = Math.min(arrows.size(), entityArrows.arrowCount());
        if (arrowCount <= 0) {
            return;
        }

        EntityModel<T> model = getParentModel();
        for (int index = 0; index < arrowCount; index++) {
            LodgedArrowVisual arrow = arrows.get(index);
            poseStack.pushPose();
            if (model instanceof HumanoidModel<?> humanoidModel) {
                renderHumanoidArrow(poseStack, buffer, packedLight, livingEntity, humanoidModel, arrow, partialTicks);
            } else {
                renderRootArrow(poseStack, buffer, packedLight, livingEntity, arrow, partialTicks);
            }
            poseStack.popPose();
        }
    }

    private void renderHumanoidArrow(
            PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight,
            LivingEntity livingEntity,
            HumanoidModel<?> model,
            LodgedArrowVisual arrow,
            float partialTicks) {
        ArrowAnchor anchor = anchorFor(model, arrow);
        anchor.part().translateAndRotate(poseStack);
        poseStack.translate(anchor.localX(), anchor.localY(), anchor.localZ());
        renderArrow(poseStack, buffer, packedLight, livingEntity, arrow, partialTicks);
    }

    private void renderRootArrow(
            PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight,
            LivingEntity livingEntity,
            LodgedArrowVisual arrow,
            float partialTicks) {
        poseStack.translate(arrow.modelX(), arrow.modelY(), arrow.modelZ());
        renderArrow(poseStack, buffer, packedLight, livingEntity, arrow, partialTicks);
    }

    private void renderArrow(
            PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight,
            LivingEntity livingEntity,
            LodgedArrowVisual arrow,
            float partialTicks) {
        float x = arrow.directionX();
        float y = arrow.directionY();
        float z = arrow.directionZ();
        float horizontalLength = Mth.sqrt(x * x + z * z);
        Arrow renderedArrow = new Arrow(livingEntity.level(), livingEntity.getX(), livingEntity.getY(), livingEntity.getZ(), ItemStack.EMPTY, null);
        renderedArrow.setYRot((float) (Math.atan2(x, z) * 180.0F / Math.PI));
        renderedArrow.setXRot((float) (Math.atan2(y, horizontalLength) * 180.0F / Math.PI));
        renderedArrow.yRotO = renderedArrow.getYRot();
        renderedArrow.xRotO = renderedArrow.getXRot();
        this.dispatcher.render(renderedArrow, 0.0D, 0.0D, 0.0D, 0.0F, partialTicks, poseStack, buffer, packedLight);
    }

    private static ArrowAnchor anchorFor(HumanoidModel<?> model, LodgedArrowVisual arrow) {
        ModelPart part = partFor(model, arrow);
        PartPose initialPose = part.getInitialPose();
        return new ArrowAnchor(
                part,
                arrow.modelX() - (initialPose.x / 16.0F),
                arrow.modelY() - (initialPose.y / 16.0F),
                arrow.modelZ() - (initialPose.z / 16.0F));
    }

    private static ModelPart partFor(HumanoidModel<?> model, LodgedArrowVisual arrow) {
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
