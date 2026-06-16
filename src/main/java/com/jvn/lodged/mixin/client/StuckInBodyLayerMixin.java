package com.jvn.lodged.mixin.client;

import com.jvn.lodged.config.LodgedConfig;
import com.jvn.lodged.network.ClientArrowState;
import com.jvn.lodged.world.LodgedArrowVisual;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
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
        for (int index = 0; index < arrowCount; index++) {
            LodgedArrowVisual arrow = arrows.get(index);
            poseStack.pushPose();
            poseStack.translate(arrow.modelX(), arrow.modelY(), arrow.modelZ());
            this.renderStuckItem(
                    poseStack,
                    buffer,
                    packedLight,
                    livingEntity,
                    arrow.directionX(),
                    arrow.directionY(),
                    arrow.directionZ(),
                    partialTicks);
            poseStack.popPose();
        }
    }
}
