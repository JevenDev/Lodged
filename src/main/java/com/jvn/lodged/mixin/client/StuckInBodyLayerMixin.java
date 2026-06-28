package com.jvn.lodged.mixin.client;

import com.jvn.lodged.client.LodgedInventoryArrowUi;
import com.jvn.lodged.client.LodgedArrowRenderHelper;
import com.jvn.lodged.config.LodgedConfig;
import com.jvn.lodged.network.ClientArrowState;
import com.jvn.lodged.network.ClientArrowState.EntityArrows;
import com.jvn.lodged.world.LodgedArmorArrowStorage;
import com.jvn.lodged.world.LodgedArrowBodyPart;
import com.jvn.lodged.world.LodgedArrowVisual;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.List;
import java.util.ArrayList;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.OutlineBufferSource;
import net.minecraft.client.renderer.entity.layers.ArrowLayer;
import net.minecraft.client.renderer.entity.layers.StuckInBodyLayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(StuckInBodyLayer.class)
public abstract class StuckInBodyLayerMixin {
    private static final int HOVER_OUTLINE_BUFFER_SIZE = 1536;

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
        ArrowRenderList arrowList = arrowsFor(livingEntity, isLocalPlayer);
        List<LodgedArrowVisual> arrows = arrowList.arrows();
        int arrowCount = arrows.size();
        if (arrowCount <= 0) {
            if (isLocalPlayer && ClientArrowState.hasSynced()) {
                callbackInfo.cancel();
            }
            return;
        }

        callbackInfo.cancel();
        int hoveredArrowIndex = LodgedInventoryArrowUi.hoveredArrowIndex();
        int hoveredArmorCombinedIndex = hoveredArmorCombinedIndex(livingEntity, isLocalPlayer, arrowList.bodyArrowCount());
        for (int index = 0; index < arrowCount; index++) {
            LodgedArrowVisual arrow = arrows.get(index);
            boolean highlightedArmor = index == hoveredArmorCombinedIndex;
            boolean highlighted = index == hoveredArrowIndex || highlightedArmor;
            poseStack.pushPose();
            ArrowAnchor anchor = anchorFor(lodged$getParentModel(), arrow);
            anchor.part().translateAndRotate(poseStack);
            poseStack.translate(anchor.localX(), anchor.localY(), anchor.localZ());
            renderArrow(poseStack, buffer, packedLight, livingEntity, arrow, partialTicks);
            if (highlighted) {
                renderHoverOutline(poseStack, packedLight, livingEntity, arrow, highlightedArmor, partialTicks);
            }
            poseStack.popPose();
        }
    }

    private static ArrowRenderList arrowsFor(LivingEntity livingEntity, boolean isLocalPlayer) {
        List<LodgedArrowVisual> armorArrows = LodgedConfig.renderArmorArrows()
                ? LodgedArmorArrowStorage.readAllEquipped(livingEntity)
                : List.of();
        if (isLocalPlayer && LodgedConfig.enablePlayerArrowRemoval()) {
            List<LodgedArrowVisual> bodyArrows = ClientArrowState.removableArrows();
            int bodyArrowCount = Math.min(bodyArrows.size(), ClientArrowState.syncedArrowCount());
            bodyArrowCount = Math.min(bodyArrowCount, LodgedConfig.maxRemovablePlayerArrows());
            return new ArrowRenderList(combinedArrows(bodyArrows, bodyArrowCount, armorArrows), bodyArrowCount);
        }

        EntityArrows entityArrows = ClientArrowState.entityArrows(livingEntity);
        int bodyArrowCount = Math.min(entityArrows.arrows().size(), entityArrows.arrowCount());
        return new ArrowRenderList(combinedArrows(entityArrows.arrows(), bodyArrowCount, armorArrows), bodyArrowCount);
    }

    private static List<LodgedArrowVisual> combinedArrows(
            List<LodgedArrowVisual> bodyArrows,
            int bodyArrowCount,
            List<LodgedArrowVisual> armorArrows) {
        if (bodyArrowCount <= 0) {
            return armorArrows;
        }
        if (armorArrows.isEmpty() && bodyArrowCount == bodyArrows.size()) {
            return bodyArrows;
        }

        List<LodgedArrowVisual> arrows = new ArrayList<>(bodyArrowCount + armorArrows.size());
        for (int index = 0; index < bodyArrowCount; index++) {
            arrows.add(bodyArrows.get(index));
        }
        arrows.addAll(armorArrows);
        return arrows;
    }

    private static int hoveredArmorCombinedIndex(LivingEntity livingEntity, boolean isLocalPlayer, int bodyArrowCount) {
        int hoveredArmorArrowIndex = LodgedInventoryArrowUi.hoveredArmorArrowIndex();
        if (!isLocalPlayer || hoveredArmorArrowIndex < 0 || !LodgedConfig.renderArmorArrows()) {
            return -1;
        }

        EquipmentSlot hoveredArmorSlot = LodgedInventoryArrowUi.hoveredArmorSlot();
        int combinedIndex = bodyArrowCount;
        for (EquipmentSlot slot : LodgedArmorArrowStorage.armorSlots()) {
            List<LodgedArrowVisual> arrows = LodgedArmorArrowStorage.readAll(livingEntity.getItemBySlot(slot));
            int arrowCount = Math.min(arrows.size(), LodgedConfig.maxTrackedArrowsPerArmorPiece());
            if (slot == hoveredArmorSlot) {
                return hoveredArmorArrowIndex < arrowCount ? combinedIndex + hoveredArmorArrowIndex : -1;
            }
            combinedIndex += arrowCount;
        }
        return -1;
    }

    private void renderHoverOutline(
            PoseStack poseStack,
            int packedLight,
            LivingEntity livingEntity,
            LodgedArrowVisual arrow,
            boolean armorArrow,
            float partialTicks) {
        if (!LodgedInventoryArrowUi.prepareArrowOutlineTarget()) {
            return;
        }

        MultiBufferSource.BufferSource delegate = MultiBufferSource.immediate(new ByteBufferBuilder(HOVER_OUTLINE_BUFFER_SIZE));
        OutlineBufferSource outlineBuffer = new OutlineBufferSource(delegate);
        LodgedInventoryArrowUi.OutlineColor outlineColor = armorArrow
                ? LodgedInventoryArrowUi.armorArrowOutlineColor()
                : LodgedInventoryArrowUi.riskOutlineColor(arrow);
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
            float partialTicks) {
        AbstractArrow renderedArrow = LodgedArrowRenderHelper.createRenderedArrow(
                livingEntity.level(),
                livingEntity.getX(),
                livingEntity.getY(),
                livingEntity.getZ(),
                arrow);
        Minecraft.getInstance().getEntityRenderDispatcher().render(
                renderedArrow,
                0.0D,
                0.0D,
                0.0D,
                0.0F,
                partialTicks,
                poseStack,
                buffer,
                packedLight);
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
            case RIGHT_LEG -> model.rightLeg;
            case LEFT_LEG -> model.leftLeg;
            case RIGHT_ARM -> model.rightArm;
            case LEFT_ARM -> model.leftArm;
            case CHEST -> model.body;
        };
    }

    private record ArrowAnchor(ModelPart part, float localX, float localY, float localZ) {
    }

    private record ArrowRenderList(List<LodgedArrowVisual> arrows, int bodyArrowCount) {
    }
}
