package com.jvn.lodged.client;

import com.jvn.lodged.Lodged;
import com.jvn.lodged.mixin.client.LevelRendererAccessor;
import com.jvn.toucanlib.client.render.ToucanPostChain;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;

final class InventoryArrowOutline {
    private static final ResourceLocation SHARP_OUTLINE_SHADER =
            Lodged.IDS.id("shaders/post/arrow_outline.json");
    private static final ToucanPostChain SHARP_OUTLINE_EFFECT = new ToucanPostChain(
            SHARP_OUTLINE_SHADER,
            exception -> Lodged.LOGGER.warn(
                    "Failed to load sharp arrow hover outline shader: {}",
                    SHARP_OUTLINE_SHADER,
                    exception));
    private static RenderTarget sharpOutlineTarget;
    private static RenderTarget previousEntityTarget;

    private InventoryArrowOutline() {
    }

    static boolean prepareArrowOutlineTarget() {
        RenderTarget outlineTarget = sharpOutlineTarget();
        if (outlineTarget == null) {
            return false;
        }

        LevelRendererAccessor levelRenderer = (LevelRendererAccessor) Minecraft.getInstance().levelRenderer;
        previousEntityTarget = levelRenderer.lodged$getEntityTarget();
        levelRenderer.lodged$setEntityTarget(outlineTarget);
        outlineTarget.clear(Minecraft.ON_OSX);
        outlineTarget.bindWrite(false);
        return true;
    }

    static void processArrowOutlineTarget() {
        restoreVanillaEntityTarget();
        if (!SHARP_OUTLINE_EFFECT.isLoaded() || sharpOutlineTarget == null) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        SHARP_OUTLINE_EFFECT.processAndRestore(minecraft, 0.0F);
        RenderSystem.enableBlend();
        RenderSystem.blendFuncSeparate(
                GlStateManager.SourceFactor.SRC_ALPHA,
                GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
                GlStateManager.SourceFactor.ZERO,
                GlStateManager.DestFactor.ONE);
        Window window = minecraft.getWindow();
        sharpOutlineTarget.blitToScreen(window.getWidth(), window.getHeight(), false);
        RenderSystem.disableBlend();
        RenderSystem.defaultBlendFunc();
    }

    static void restoreVanillaEntityTarget() {
        if (previousEntityTarget == null) {
            return;
        }

        ((LevelRendererAccessor) Minecraft.getInstance().levelRenderer).lodged$setEntityTarget(previousEntityTarget);
        previousEntityTarget = null;
    }

    private static RenderTarget sharpOutlineTarget() {
        Minecraft minecraft = Minecraft.getInstance();
        if (SHARP_OUTLINE_EFFECT.isLoaded() && SHARP_OUTLINE_EFFECT.matchesWindow(minecraft)) {
            return sharpOutlineTarget;
        }

        closeSharpOutlineEffect();
        sharpOutlineTarget = SHARP_OUTLINE_EFFECT.tempTarget(minecraft, "final");
        return sharpOutlineTarget;
    }

    private static void closeSharpOutlineEffect() {
        SHARP_OUTLINE_EFFECT.close();
        sharpOutlineTarget = null;
    }
}
