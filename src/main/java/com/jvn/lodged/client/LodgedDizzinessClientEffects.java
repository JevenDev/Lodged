package com.jvn.lodged.client;

import com.jvn.lodged.effect.LodgedEffects;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;

public final class LodgedDizzinessClientEffects {
    private static final ResourceLocation VIGNETTE_LOCATION =
            ResourceLocation.withDefaultNamespace("textures/misc/vignette.png");
    private static final float FADE_IN_STEP = 0.11F;
    private static final float FADE_OUT_STEP = 0.095F;
    private static final float MAX_VIGNETTE_ALPHA = 0.30F;
    private static final float MAX_ROLL = 0.72F;
    private static final float MAX_YAW = 0.5F;
    private static final float MAX_PITCH = 0.26F;
    private static float dizzinessBlend;

    private LodgedDizzinessClientEffects() {
    }

    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        float target = targetDizzinessStrength(minecraft);
        float step = target > dizzinessBlend ? FADE_IN_STEP : FADE_OUT_STEP;
        dizzinessBlend = Mth.approach(dizzinessBlend, target, step);
        if (dizzinessBlend < 0.001F) {
            dizzinessBlend = 0.0F;
        }
        LodgedDizzinessPostProcessor.INSTANCE.updateState(dizzinessBlend);
    }

    public static void onRenderGui(RenderGuiEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null || dizzinessBlend <= 0.0F) {
            return;
        }

        GuiGraphics guiGraphics = event.getGuiGraphics();
        int width = guiGraphics.guiWidth();
        int height = guiGraphics.guiHeight();
        float pulse = pulse(minecraft, 0.0F);
        float vignetteAlpha = dizzinessBlend * MAX_VIGNETTE_ALPHA * (0.94F + pulse * 0.06F);

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(0.34F, 0.0F, 0.0F, vignetteAlpha);
        guiGraphics.blit(VIGNETTE_LOCATION, 0, 0, -90, 0.0F, 0.0F, width, height, width, height);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.disableBlend();
    }

    public static void onComputeCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null
                || minecraft.player == null
                || dizzinessBlend <= 0.0F
                || !minecraft.options.getCameraType().isFirstPerson()) {
            return;
        }

        float partialTick = (float) event.getPartialTick();
        float time = (minecraft.level.getGameTime() + partialTick) * 0.05F;
        float waveA = (float) Math.sin(time * 2.0F * (float) Math.PI * 0.74F);
        float waveB = (float) Math.sin((time + 0.37F) * 2.0F * (float) Math.PI * 0.43F);
        float waveC = (float) Math.sin((time + 0.19F) * 2.0F * (float) Math.PI * 0.28F);
        float intensity = dizzinessBlend * (0.78F + pulse(minecraft, partialTick) * 0.22F);

        event.setRoll(event.getRoll() + (0.70F * waveA + 0.30F * waveB) * MAX_ROLL * intensity);
        event.setYaw(event.getYaw() + (0.55F * waveB - 0.25F * waveC) * MAX_YAW * intensity);
        event.setPitch(event.getPitch() + (0.45F * waveC) * MAX_PITCH * intensity);
    }

    private static float pulse(Minecraft minecraft, float partialTick) {
        if (minecraft.level == null) {
            return 0.0F;
        }

        float time = (minecraft.level.getGameTime() + partialTick) * 0.05F;
        return 0.5F + 0.5F * (float) Math.sin(time * 2.0F * (float) Math.PI * 0.55F);
    }

    private static float targetDizzinessStrength(Minecraft minecraft) {
        if (minecraft.level == null || minecraft.player == null) {
            return 0.0F;
        }

        MobEffectInstance dizziness = minecraft.player.getEffect(LodgedEffects.DIZZINESS);
        if (dizziness == null) {
            return 0.0F;
        }

        return Mth.clamp(0.5F + dizziness.getAmplifier() * 0.125F, 0.0F, 1.0F);
    }

}
