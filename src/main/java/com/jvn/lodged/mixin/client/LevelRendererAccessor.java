package com.jvn.lodged.mixin.client;

import com.mojang.blaze3d.pipeline.RenderTarget;
import net.minecraft.client.renderer.LevelRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(LevelRenderer.class)
public interface LevelRendererAccessor {
    @Accessor("entityTarget")
    RenderTarget lodged$getEntityTarget();

    @Accessor("entityTarget")
    void lodged$setEntityTarget(RenderTarget entityTarget);
}
