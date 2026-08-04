package com.jvn.lodged.client;

import com.jvn.lodged.network.ClientArrowState;
import com.jvn.lodged.config.LodgedConfig;
import com.jvn.lodged.world.LodgedArmorArrowStorage;
import com.jvn.lodged.world.LodgedArrowVisual;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.OutlineBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.projectile.AbstractArrow;
import org.joml.Vector3f;

public final class LodgedEntityArrowLayer<T extends LivingEntity, M extends EntityModel<T>> extends RenderLayer<T, M> {
    private static final int HOVER_OUTLINE_BUFFER_SIZE = 1536;
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
        List<LodgedArrowVisual> arrows = arrowsFor(livingEntity);
        int arrowCount = arrows.size();
        if (arrowCount <= 0) {
            return;
        }

        EntityModel<T> model = getParentModel();
        for (int index = 0; index < arrowCount; index++) {
            LodgedArrowVisual arrow = arrows.get(index);
            poseStack.pushPose();
            Vector3f partLocalDirection =
                    LodgedArrowRenderHelper.translateToModelPart(poseStack, model, arrow);
            if (partLocalDirection != null) {
                renderArrow(
                        poseStack,
                        buffer,
                        packedLight,
                        livingEntity,
                        arrow,
                        partLocalDirection,
                        partialTicks);
            } else {
                renderRootArrow(poseStack, buffer, packedLight, livingEntity, arrow, partialTicks);
            }
            if (livingEntity instanceof AbstractHorse horse
                    && LodgedHorseArmorArrowUi.isHovered(horse, arrow)) {
                renderHorseArmorHoverOutline(poseStack, packedLight, livingEntity, arrow, partialTicks);
            }
            poseStack.popPose();
        }
    }

    private static List<LodgedArrowVisual> arrowsFor(LivingEntity livingEntity) {
        ClientArrowState.EntityArrows entityArrows = ClientArrowState.entityArrows(livingEntity);
        int bodyArrowCount = Math.min(entityArrows.arrows().size(), entityArrows.arrowCount());
        List<LodgedArrowVisual> armorArrows = LodgedConfig.renderArmorArrows()
                ? LodgedArmorArrowStorage.readAllEquipped(livingEntity)
                : List.of();
        if (bodyArrowCount <= 0) {
            return armorArrows;
        }

        java.util.ArrayList<LodgedArrowVisual> arrows = new java.util.ArrayList<>(bodyArrowCount + armorArrows.size());
        for (int index = 0; index < bodyArrowCount; index++) {
            arrows.add(entityArrows.arrows().get(index));
        }
        arrows.addAll(armorArrows);
        return arrows;
    }

    private void renderHumanoidArrow(
            PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight,
            LivingEntity livingEntity,
            HumanoidModel<?> model,
            LodgedArrowVisual arrow,
            float partialTicks) {
        Vector3f partLocalDirection = LodgedArrowRenderHelper.translateToHumanoidPart(poseStack, model, arrow);
        renderArrow(poseStack, buffer, packedLight, livingEntity, arrow, partLocalDirection, partialTicks);
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
        AbstractArrow renderedArrow = LodgedArrowRenderHelper.createRenderedArrow(
                livingEntity.level(),
                livingEntity.getX(),
                livingEntity.getY(),
                livingEntity.getZ(),
                arrow);
        this.dispatcher.render(renderedArrow, 0.0D, 0.0D, 0.0D, 0.0F, partialTicks, poseStack, buffer, packedLight);
    }

    private void renderHorseArmorHoverOutline(
            PoseStack poseStack,
            int packedLight,
            LivingEntity livingEntity,
            LodgedArrowVisual arrow,
            float partialTicks) {
        if (!LodgedInventoryArrowUi.prepareArrowOutlineTarget()) {
            return;
        }

        MultiBufferSource.BufferSource delegate =
                MultiBufferSource.immediate(new ByteBufferBuilder(HOVER_OUTLINE_BUFFER_SIZE));
        OutlineBufferSource outlineBuffer = new OutlineBufferSource(delegate);
        LodgedInventoryArrowUi.OutlineColor outlineColor = LodgedInventoryArrowUi.armorArrowOutlineColor();
        outlineBuffer.setColor(outlineColor.red(), outlineColor.green(), outlineColor.blue(), outlineColor.alpha());
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
            Vector3f direction,
            float partialTicks) {
        AbstractArrow renderedArrow = LodgedArrowRenderHelper.createRenderedArrow(
                livingEntity.level(),
                livingEntity.getX(),
                livingEntity.getY(),
                livingEntity.getZ(),
                arrow,
                direction);
        this.dispatcher.render(renderedArrow, 0.0D, 0.0D, 0.0D, 0.0F, partialTicks, poseStack, buffer, packedLight);
    }

}
