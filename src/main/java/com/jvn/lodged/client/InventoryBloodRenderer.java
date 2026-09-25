package com.jvn.lodged.client;

import com.jvn.lodged.Lodged;
import com.jvn.lodged.config.LodgedConfig;
import com.jvn.lodged.effect.BloodColors;
import com.jvn.lodged.effect.LodgedEffects;
import com.jvn.lodged.network.ClientArrowState;
import com.jvn.lodged.world.LodgedArrowVisual;
import com.mojang.blaze3d.systems.RenderSystem;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import org.joml.Vector3f;

final class InventoryBloodRenderer {
    private static final ResourceLocation BLEED_HANG_TEXTURE =
            Lodged.IDS.texture("particle/bleed_hang.png");
    private static final ResourceLocation BLEED_FALL_TEXTURE =
            Lodged.IDS.texture("particle/bleed_fall.png");
    private static final ResourceLocation BLEED_LAND_TEXTURE =
            Lodged.IDS.texture("particle/bleed_land.png");
    private static final int BLOOD_TEXTURE_SIZE = 8;
    private static final int GUI_BLOOD_SIZE = 4;
    private static final int GUI_BLOOD_Z = 300;
    static final int GUI_BLOOD_HANG_TICKS = 6;
    static final int GUI_BLOOD_FALL_TICKS = 18;
    static final int GUI_BLOOD_LAND_TICKS = 7;
    private static final LodgedArrowVisual[] FALLBACK_BLEEDING_WOUNDS = {
            new LodgedArrowVisual(0.0F, 0.32F, -0.13F, 0.0F, 0.0F, 1.0F),
            new LodgedArrowVisual(-0.38F, 0.52F, -0.08F, 0.0F, 0.0F, 1.0F),
            new LodgedArrowVisual(0.16F, 0.98F, -0.10F, 0.0F, 0.0F, 1.0F)
    };

    private InventoryBloodRenderer() {
    }

    static void renderInventoryBlood(
            GuiGraphics guiGraphics,
            int x1,
            int y1,
            int x2,
            int y2,
            float mouseX,
            float mouseY,
            LivingEntity entity) {
        if (!LodgedConfig.enableBleeding() || !LodgedConfig.bleedingDripParticles() || !(entity instanceof LocalPlayer player)) {
            return;
        }

        MobEffectInstance bleeding = player.getEffect(LodgedEffects.BLEEDING);
        if (bleeding == null) {
            return;
        }

        float centerX = (x1 + x2) / 2.0F;
        float centerY = (y1 + y2) / 2.0F;
        int interval = Math.max(1, LodgedConfig.bleedingDripInterval(bleeding.getAmplifier()));
        float time = player.level().getGameTime() + Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(false);
        int streams = bleeding.getAmplifier() > 0 ? 2 : 1;
        int color = BloodColors.colorFor(player);

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        guiGraphics.setColor(
                ((color >> 16) & 0xFF) / 255.0F,
                ((color >> 8) & 0xFF) / 255.0F,
                (color & 0xFF) / 255.0F,
                0.96F);
        int arrowBloodSources = renderInventoryBloodFromArrows(guiGraphics, player, centerX, centerY, mouseX, mouseY, time, interval, streams);
        if (arrowBloodSources <= 0) {
            renderInventoryBloodFallback(guiGraphics, player, centerX, centerY, mouseX, mouseY, time, interval, streams);
        }
        guiGraphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.disableBlend();
    }

    private static int renderInventoryBloodFromArrows(
            GuiGraphics guiGraphics,
            LocalPlayer player,
            float centerX,
            float centerY,
            float mouseX,
            float mouseY,
            float time,
            int interval,
            int streams) {
        List<LodgedArrowVisual> arrows = ClientArrowState.removableArrows();
        int arrowCount = Math.min(arrows.size(), ClientArrowState.syncedArrowCount());
        arrowCount = Math.min(arrowCount, LodgedConfig.maxRemovablePlayerArrows());
        for (int index = 0; index < arrowCount; index++) {
            LodgedArrowVisual arrow = arrows.get(index);
            Vector3f projected = InventoryPreviewProjection.projectModelToScreen(
                    player,
                    InventoryPreviewProjection.renderModelPosition(player, arrow, centerX, centerY, mouseX, mouseY),
                    centerX,
                    centerY,
                    mouseX,
                    mouseY);
            renderInventoryBloodDrop(guiGraphics, projected.x(), projected.y(), time, interval, streams, index * 11);
        }
        return arrowCount;
    }

    private static void renderInventoryBloodFallback(
            GuiGraphics guiGraphics,
            LocalPlayer player,
            float centerX,
            float centerY,
            float mouseX,
            float mouseY,
            float time,
            int interval,
            int streams) {
        int count = streams > 1 ? FALLBACK_BLEEDING_WOUNDS.length : 2;
        for (int index = 0; index < count; index++) {
            LodgedArrowVisual wound = FALLBACK_BLEEDING_WOUNDS[index];
            Vector3f projected = InventoryPreviewProjection.projectModelToScreen(
                    player,
                    InventoryPreviewProjection.renderModelPosition(player, wound, centerX, centerY, mouseX, mouseY),
                    centerX,
                    centerY,
                    mouseX,
                    mouseY);
            renderInventoryBloodDrop(guiGraphics, projected.x(), projected.y(), time, interval, 1, 53 + index * 17);
        }
    }

    private static void renderInventoryBloodDrop(
            GuiGraphics guiGraphics,
            float sourceX,
            float sourceY,
            float time,
            int interval,
            int streams,
            int seed) {
        int cycleTicks = Math.max(interval + GUI_BLOOD_HANG_TICKS + GUI_BLOOD_FALL_TICKS + GUI_BLOOD_LAND_TICKS, 24);
        for (int stream = 0; stream < streams; stream++) {
            float phase = positiveModulo(time + seed + stream * (cycleTicks / (float) streams), cycleTicks);
            drawInventoryBloodDrop(guiGraphics, sourceX, sourceY, phase, seed + stream * 31);
        }
    }

    static void drawInventoryBloodDrop(GuiGraphics guiGraphics, float sourceX, float sourceY, float phase, int seed) {
        ResourceLocation texture;
        float x = sourceX + horizontalDrift(seed);
        float y = sourceY;
        if (phase < GUI_BLOOD_HANG_TICKS) {
            texture = BLEED_HANG_TEXTURE;
        } else if (phase < GUI_BLOOD_HANG_TICKS + GUI_BLOOD_FALL_TICKS) {
            texture = BLEED_FALL_TEXTURE;
            float fallProgress = (phase - GUI_BLOOD_HANG_TICKS) / GUI_BLOOD_FALL_TICKS;
            y += 2.0F + fallProgress * fallProgress * 15.0F;
            x += horizontalDrift(seed + 7) * fallProgress;
        } else {
            texture = BLEED_LAND_TEXTURE;
            y += 17.0F;
            x += horizontalDrift(seed + 7);
        }

        guiGraphics.blit(
                texture,
                Math.round(x) - (GUI_BLOOD_SIZE / 2),
                Math.round(y) - (GUI_BLOOD_SIZE / 2),
                GUI_BLOOD_Z,
                0.0F,
                0.0F,
                GUI_BLOOD_SIZE,
                GUI_BLOOD_SIZE,
                BLOOD_TEXTURE_SIZE,
                BLOOD_TEXTURE_SIZE);
    }

    private static float horizontalDrift(int seed) {
        int value = Math.floorMod(seed * 31 + 17, 9) - 4;
        return value * 0.35F;
    }

    private static float positiveModulo(float value, int modulus) {
        float result = value % modulus;
        return result < 0.0F ? result + modulus : result;
    }
}
