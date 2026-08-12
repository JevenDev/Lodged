package com.jvn.lodged.mixin.client;

import com.jvn.lodged.client.LodgedEntityArrowLayer;
import com.jvn.lodged.client.LodgedModelHitboxRenderer;
import com.jvn.lodged.collision.ModelHitboxCache;
import com.jvn.lodged.config.LodgedConfig;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin {
    @Inject(method = "<init>", at = @At("TAIL"))
    @SuppressWarnings({"rawtypes", "unchecked"})
    private void lodged$addEntityArrowLayer(
            EntityRendererProvider.Context context,
            EntityModel<?> model,
            float shadowRadius,
            CallbackInfo callbackInfo) {
        LivingEntityRenderer renderer = (LivingEntityRenderer) (Object) this;
        if (model instanceof PlayerModel<?>) {
            return;
        }
        renderer.addLayer(new LodgedEntityArrowLayer<>(
                renderer,
                context.getEntityRenderDispatcher()));
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void lodged$renderModelHitboxes(
            LivingEntity entity,
            float entityYaw,
            float partialTicks,
            PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight,
            CallbackInfo callbackInfo) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!LodgedConfig.enableModelAccurateProjectileCollision()
                || !LodgedConfig.showModelHitboxesInDebug()
                || !ModelHitboxCache.supports(entity)
                || entity.isInvisible()
                || minecraft.showOnlyReducedInfo()
                || !minecraft.getEntityRenderDispatcher().shouldRenderHitBoxes()) {
            return;
        }
        LodgedModelHitboxRenderer.render(entity, partialTicks, poseStack, buffer);
    }
}
