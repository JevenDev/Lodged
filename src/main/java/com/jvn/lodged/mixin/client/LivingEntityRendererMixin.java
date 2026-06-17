package com.jvn.lodged.mixin.client;

import com.jvn.lodged.client.LodgedEntityArrowLayer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
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
        renderer.addLayer(new LodgedEntityArrowLayer<>(
                renderer,
                context.getEntityRenderDispatcher()));
    }
}
