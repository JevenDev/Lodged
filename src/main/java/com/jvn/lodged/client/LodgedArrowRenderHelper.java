package com.jvn.lodged.client;

import com.jvn.lodged.world.LodgedArrowVisual;
import com.jvn.toucanlib.client.model.ToucanModelPartAnchors.AnchoredPoint;
import com.jvn.toucanlib.client.model.ToucanModelPartAnchors;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.entity.projectile.SpectralArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import org.joml.Vector3f;

public final class LodgedArrowRenderHelper {
    private LodgedArrowRenderHelper() {
    }

    public static AbstractArrow createRenderedArrow(Level level, double x, double y, double z, LodgedArrowVisual arrow) {
        return createRenderedArrow(
                level,
                x,
                y,
                z,
                arrow,
                arrow.directionX(),
                arrow.directionY(),
                arrow.directionZ());
    }

    public static AbstractArrow createRenderedArrow(
            Level level,
            double x,
            double y,
            double z,
            LodgedArrowVisual arrow,
            Vector3f direction) {
        return createRenderedArrow(level, x, y, z, arrow, direction.x(), direction.y(), direction.z());
    }

    private static AbstractArrow createRenderedArrow(
            Level level,
            double x,
            double y,
            double z,
            LodgedArrowVisual arrow,
            float directionX,
            float directionY,
            float directionZ) {
        ItemStack stack = renderStack(arrow);
        AbstractArrow renderedArrow = stack.is(Items.SPECTRAL_ARROW)
                ? new SpectralArrow(level, x, y, z, stack, null)
                : new Arrow(level, x, y, z, stack, null);

        float horizontalLength = Mth.sqrt(directionX * directionX + directionZ * directionZ);
        renderedArrow.setYRot((float) (Math.atan2(directionX, directionZ) * 180.0F / Math.PI));
        renderedArrow.setXRot((float) (Math.atan2(directionY, horizontalLength) * 180.0F / Math.PI));
        renderedArrow.yRotO = renderedArrow.getYRot();
        renderedArrow.xRotO = renderedArrow.getXRot();
        return renderedArrow;
    }

    public static Vector3f translateToModelPart(
            PoseStack poseStack,
            EntityModel<?> model,
            LodgedArrowVisual arrow) {
        if (model instanceof HumanoidModel<?> humanoidModel) {
            return translateToHumanoidPart(poseStack, humanoidModel, arrow);
        }
        return ToucanModelPartAnchors.translateToPath(
                poseStack,
                model,
                arrow.modelPartPath(),
                modelPositionPixels(arrow),
                modelDirection(arrow));
    }

    public static Vector3f translateToHumanoidPart(
            PoseStack poseStack,
            HumanoidModel<?> model,
            LodgedArrowVisual arrow) {
        return ToucanModelPartAnchors.translate(poseStack, partAnchor(model, arrow));
    }

    public static Vector3f humanoidModelPosition(HumanoidModel<?> model, LodgedArrowVisual arrow) {
        return ToucanModelPartAnchors.transformedPosition(partAnchor(model, arrow));
    }

    private static AnchoredPoint partAnchor(HumanoidModel<?> model, LodgedArrowVisual arrow) {
        ModelPart part = switch (arrow.bodyPart()) {
            case HEAD -> model.head;
            case CHEST -> model.body;
            case LEFT_ARM -> model.leftArm;
            case RIGHT_ARM -> model.rightArm;
            case LEFT_LEG -> model.leftLeg;
            case RIGHT_LEG -> model.rightLeg;
        };
        return ToucanModelPartAnchors.anchor(part, modelPositionPixels(arrow), modelDirection(arrow));
    }

    private static Vector3f modelPositionPixels(LodgedArrowVisual arrow) {
        return new Vector3f(
                arrow.modelX() * 16.0F,
                arrow.modelY() * 16.0F,
                arrow.modelZ() * 16.0F);
    }

    private static Vector3f modelDirection(LodgedArrowVisual arrow) {
        return new Vector3f(arrow.directionX(), arrow.directionY(), arrow.directionZ());
    }

    private static ItemStack renderStack(LodgedArrowVisual arrow) {
        ItemStack stack = arrow.stack();
        if (stack.isEmpty()) {
            return new ItemStack(Items.ARROW);
        }

        return stack.copyWithCount(1);
    }
}
