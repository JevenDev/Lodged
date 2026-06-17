package com.jvn.lodged.client;

import com.google.gson.JsonSyntaxException;
import com.jvn.lodged.Lodged;
import com.jvn.lodged.config.LodgedConfig;
import com.jvn.lodged.effect.LodgedEffects;
import com.jvn.lodged.mixin.client.LevelRendererAccessor;
import com.jvn.lodged.network.ClientArrowState;
import com.jvn.lodged.network.payload.RemovePlayerArrowPayload;
import com.jvn.lodged.world.LodgedArrowBodyPart;
import com.jvn.lodged.world.LodgedArrowVisual;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import java.io.IOException;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

@EventBusSubscriber(modid = Lodged.MOD_ID, value = Dist.CLIENT)
public final class LodgedInventoryArrowUi {
    private static final int MODEL_LEFT = 26;
    private static final int MODEL_TOP = 8;
    private static final int MODEL_RIGHT = 75;
    private static final int MODEL_BOTTOM = 78;
    private static final int HIT_ZONE_RADIUS = 12;
    private static final float PREVIEW_SCALE = 30.0F;
    private static final float PREVIEW_Y_OFFSET = 0.0625F;
    private static final float PLAYER_RENDER_SCALE = 0.9375F;
    private static final float MODEL_RENDER_Y_OFFSET = -1.501F;
    private static final float DEGREES_TO_RADIANS = (float) (Math.PI / 180.0D);
    private static final ResourceLocation SHARP_OUTLINE_SHADER =
            ResourceLocation.fromNamespaceAndPath(Lodged.MOD_ID, "shaders/post/arrow_outline.json");
    private static final ResourceLocation BLEED_HANG_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Lodged.MOD_ID, "textures/particle/bleed_hang.png");
    private static final ResourceLocation BLEED_FALL_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Lodged.MOD_ID, "textures/particle/bleed_fall.png");
    private static final ResourceLocation BLEED_LAND_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Lodged.MOD_ID, "textures/particle/bleed_land.png");
    private static final int BLOOD_TEXTURE_SIZE = 8;
    private static final int GUI_BLOOD_SIZE = 4;
    private static final int GUI_BLOOD_Z = 300;
    private static final int GUI_BLOOD_HANG_TICKS = 6;
    private static final int GUI_BLOOD_FALL_TICKS = 18;
    private static final int GUI_BLOOD_LAND_TICKS = 7;
    private static final LodgedArrowVisual[] FALLBACK_BLEEDING_WOUNDS = {
            new LodgedArrowVisual(0.0F, 0.32F, -0.13F, 0.0F, 0.0F, 1.0F),
            new LodgedArrowVisual(-0.38F, 0.52F, -0.08F, 0.0F, 0.0F, 1.0F),
            new LodgedArrowVisual(0.16F, 0.98F, -0.10F, 0.0F, 0.0F, 1.0F)
    };

    private static float yawDegrees;
    private static boolean draggingPreview;
    private static boolean customYawActive;
    private static int hoveredArrowIndex = -1;
    private static PostChain sharpOutlineEffect;
    private static RenderTarget sharpOutlineTarget;
    private static int sharpOutlineWidth = -1;
    private static int sharpOutlineHeight = -1;
    private static boolean sharpOutlineLoadFailed;
    private static RenderTarget previousEntityTarget;

    private LodgedInventoryArrowUi() {
    }

    @SubscribeEvent
    public static void onScreenRenderPre(ScreenEvent.Render.Pre event) {
        if (!LodgedConfig.enablePlayerArrowRemoval() || !(event.getScreen() instanceof InventoryScreen screen)) {
            hoveredArrowIndex = -1;
            return;
        }

        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            hoveredArrowIndex = -1;
            return;
        }

        ArrowHitZone hoveredZone = findHoveredZone(screen, player, event.getMouseX(), event.getMouseY());
        hoveredArrowIndex = hoveredZone == null ? -1 : hoveredZone.index();
    }

    @SubscribeEvent
    public static void onMouseClicked(ScreenEvent.MouseButtonPressed.Pre event) {
        if (!LodgedConfig.enablePlayerArrowRemoval() || !(event.getScreen() instanceof InventoryScreen screen)) {
            return;
        }

        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }

        if (event.getButton() == 2 && isInPlayerPreview(screen, event.getMouseX(), event.getMouseY())) {
            resetPreviewRotation();
            event.setCanceled(true);
            return;
        }

        if (event.getButton() != 0) {
            return;
        }

        ArrowHitZone hoveredZone = findHoveredZone(screen, player, event.getMouseX(), event.getMouseY());
        if (hoveredZone != null) {
            PacketDistributor.sendToServer(new RemovePlayerArrowPayload(hoveredZone.index()));
            event.setCanceled(true);
            return;
        }

        if (isInPlayerPreview(screen, event.getMouseX(), event.getMouseY())) {
            draggingPreview = true;
            customYawActive = true;
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onMouseDragged(ScreenEvent.MouseDragged.Pre event) {
        if (!draggingPreview || event.getMouseButton() != 0 || !(event.getScreen() instanceof InventoryScreen)) {
            return;
        }

        yawDegrees -= (float) event.getDragX() * 2.0F;
        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onMouseReleased(ScreenEvent.MouseButtonReleased.Pre event) {
        if (event.getButton() == 0 && draggingPreview) {
            draggingPreview = false;
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onScreenClosing(ScreenEvent.Closing event) {
        if (event.getScreen() instanceof InventoryScreen) {
            resetPreviewRotation();
        }
    }

    public static int hoveredArrowIndex() {
        return hoveredArrowIndex;
    }

    public static boolean prepareArrowOutlineTarget() {
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

    public static void processArrowOutlineTarget() {
        restoreVanillaEntityTarget();
        if (sharpOutlineEffect == null || sharpOutlineTarget == null) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        sharpOutlineEffect.process(0.0F);
        minecraft.getMainRenderTarget().bindWrite(false);
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

    public static void restoreVanillaEntityTarget() {
        if (previousEntityTarget == null) {
            return;
        }

        ((LevelRendererAccessor) Minecraft.getInstance().levelRenderer).lodged$setEntityTarget(previousEntityTarget);
        previousEntityTarget = null;
    }

    public static void renderInventoryPlayer(
            GuiGraphics guiGraphics,
            int x1,
            int y1,
            int x2,
            int y2,
            int scale,
            float size,
            float mouseX,
            float mouseY,
            LivingEntity entity) {
        if (!LodgedConfig.enablePlayerArrowRemoval()) {
            InventoryScreen.renderEntityInInventoryFollowsMouse(guiGraphics, x1, y1, x2, y2, scale, size, mouseX, mouseY, entity);
            renderInventoryBlood(guiGraphics, x1, y1, x2, y2, mouseX, mouseY, entity);
            return;
        }

        if (customYawActive) {
            renderInventoryPlayerWithLockedHead(
                    guiGraphics,
                    x1,
                    y1,
                    x2,
                    y2,
                    scale,
                    size,
                    mouseY,
                    entity);
        } else {
            InventoryScreen.renderEntityInInventoryFollowsMouse(guiGraphics, x1, y1, x2, y2, scale, size, mouseX, mouseY, entity);
        }

        renderInventoryBlood(guiGraphics, x1, y1, x2, y2, mouseX, mouseY, entity);
    }

    private static void renderInventoryPlayerWithLockedHead(
            GuiGraphics guiGraphics,
            int x1,
            int y1,
            int x2,
            int y2,
            int scale,
            float size,
            float mouseY,
            LivingEntity entity) {
        float centerX = (x1 + x2) / 2.0F;
        float centerY = (y1 + y2) / 2.0F;
        float pitch = (float) Math.atan((centerY - mouseY) / 40.0F);
        Quaternionf bodyPose = new Quaternionf().rotateZ((float) Math.PI);
        Quaternionf cameraPitch = new Quaternionf().rotateX(pitch * 20.0F * ((float) Math.PI / 180.0F));
        bodyPose.mul(cameraPitch);

        float previousBodyYaw = entity.yBodyRot;
        float previousBodyYawOld = entity.yBodyRotO;
        float previousYaw = entity.getYRot();
        float previousPitch = entity.getXRot();
        float previousHeadYawOld = entity.yHeadRotO;
        float previousHeadYaw = entity.yHeadRot;

        float renderYaw = 180.0F + yawDegrees;
        entity.yBodyRot = renderYaw;
        entity.yBodyRotO = renderYaw;
        entity.setYRot(renderYaw);
        entity.setXRot(-pitch * 20.0F);
        entity.yHeadRot = renderYaw;
        entity.yHeadRotO = renderYaw;

        float entityScale = entity.getScale();
        Vector3f translation = new Vector3f(0.0F, (entity.getBbHeight() / 2.0F) + (size * entityScale), 0.0F);
        InventoryScreen.renderEntityInInventory(
                guiGraphics,
                centerX,
                centerY,
                scale / entityScale,
                translation,
                bodyPose,
                cameraPitch,
                entity);

        entity.yBodyRot = previousBodyYaw;
        entity.yBodyRotO = previousBodyYawOld;
        entity.setYRot(previousYaw);
        entity.setXRot(previousPitch);
        entity.yHeadRotO = previousHeadYawOld;
        entity.yHeadRot = previousHeadYaw;
    }

    private static ArrowHitZone findHoveredZone(InventoryScreen screen, LocalPlayer player, double mouseX, double mouseY) {
        List<LodgedArrowVisual> arrows = ClientArrowState.removableArrows();
        int arrowCount = Math.min(arrows.size(), ClientArrowState.syncedArrowCount());
        arrowCount = Math.min(arrowCount, LodgedConfig.maxRemovablePlayerArrows());
        ArrowHitZone closestZone = null;
        double closestDistance = Double.MAX_VALUE;
        for (int index = 0; index < arrowCount; index++) {
            ArrowHitZone zone = zoneFor(screen, player, arrows.get(index), index, mouseX, mouseY);
            if (zone.contains(mouseX, mouseY)) {
                double distance = zone.distanceToSqr(mouseX, mouseY);
                if (distance < closestDistance) {
                    closestZone = zone;
                    closestDistance = distance;
                }
            }
        }
        return closestZone;
    }

    private static ArrowHitZone zoneFor(
            InventoryScreen screen,
            LocalPlayer player,
            LodgedArrowVisual arrow,
            int index,
            double mouseX,
            double mouseY) {
        Vector3f projected = projectArrowToScreen(screen, player, arrow, mouseX, mouseY);
        return new ArrowHitZone(index, projected.x(), projected.y(), HIT_ZONE_RADIUS);
    }

    private static Vector3f projectArrowToScreen(
            InventoryScreen screen,
            LocalPlayer player,
            LodgedArrowVisual arrow,
            double mouseX,
            double mouseY) {
        int left = screen.getGuiLeft();
        int top = screen.getGuiTop();
        float centerX = left + ((MODEL_LEFT + MODEL_RIGHT) / 2.0F);
        float centerY = top + ((MODEL_TOP + MODEL_BOTTOM) / 2.0F);
        Vector3f modelPosition = renderModelPosition(arrow, centerX, centerY, mouseX, mouseY);
        return projectModelToScreen(player, modelPosition, centerX, centerY, mouseX, mouseY);
    }

    private static Vector3f projectModelToScreen(
            LocalPlayer player,
            Vector3f modelPosition,
            float centerX,
            float centerY,
            double mouseX,
            double mouseY) {
        float entityScale = player.getScale();
        float previewScale = PREVIEW_SCALE / entityScale;
        float pitchRadians = currentPreviewPitchRadians(centerY, mouseY);
        float yawRadians = -currentPreviewYawDegrees(centerX, mouseX) * ((float) Math.PI / 180.0F);

        Matrix4f matrix = new Matrix4f()
                .translation(centerX, centerY, 50.0F)
                .scale(previewScale, previewScale, -previewScale)
                .translate(0.0F, (player.getBbHeight() / 2.0F) + (PREVIEW_Y_OFFSET * entityScale), 0.0F)
                .rotateZ((float) Math.PI)
                .rotateX(pitchRadians)
                .scale(entityScale, entityScale, entityScale)
                .rotateY(yawRadians)
                .scale(-1.0F, -1.0F, 1.0F)
                .scale(PLAYER_RENDER_SCALE, PLAYER_RENDER_SCALE, PLAYER_RENDER_SCALE)
                .translate(0.0F, MODEL_RENDER_Y_OFFSET, 0.0F);
        return matrix.transformPosition(modelPosition.x(), modelPosition.y(), modelPosition.z(), new Vector3f());
    }

    private static void renderInventoryBlood(
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

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        guiGraphics.setColor(1.0F, 1.0F, 1.0F, 0.96F);
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
            Vector3f projected = projectModelToScreen(
                    player,
                    renderModelPosition(arrow, centerX, centerY, mouseX, mouseY),
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
            Vector3f projected = projectModelToScreen(
                    player,
                    renderModelPosition(wound, centerX, centerY, mouseX, mouseY),
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

    private static void drawInventoryBloodDrop(GuiGraphics guiGraphics, float sourceX, float sourceY, float phase, int seed) {
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

    private static Vector3f renderModelPosition(
            LodgedArrowVisual arrow,
            float centerX,
            float centerY,
            double mouseX,
            double mouseY) {
        if (arrow.bodyPart() != LodgedArrowBodyPart.HEAD) {
            return new Vector3f(arrow.modelX(), arrow.modelY(), arrow.modelZ());
        }

        return new Matrix4f()
                .rotate(new Quaternionf().rotationZYX(
                        0.0F,
                        currentHeadYawRadians(centerX, mouseX),
                        currentHeadPitchRadians(centerY, mouseY)))
                .transformPosition(arrow.modelX(), arrow.modelY(), arrow.modelZ(), new Vector3f());
    }

    private static float currentPreviewPitchRadians(float centerY, double mouseY) {
        float pitch = (float) Math.atan((centerY - mouseY) / 40.0F);
        return pitch * 20.0F * DEGREES_TO_RADIANS;
    }

    private static float currentPreviewYawDegrees(float centerX, double mouseX) {
        if (customYawActive) {
            return yawDegrees;
        }
        return (float) Math.atan((centerX - mouseX) / 40.0F) * 20.0F;
    }

    private static float currentHeadYawRadians(float centerX, double mouseX) {
        if (customYawActive) {
            return 0.0F;
        }
        return (float) Math.atan((centerX - mouseX) / 40.0F) * 20.0F * DEGREES_TO_RADIANS;
    }

    private static float currentHeadPitchRadians(float centerY, double mouseY) {
        return -currentPreviewPitchRadians(centerY, mouseY);
    }

    private static boolean isInPlayerPreview(InventoryScreen screen, double mouseX, double mouseY) {
        int left = screen.getGuiLeft();
        int top = screen.getGuiTop();
        return mouseX >= left + MODEL_LEFT
                && mouseX < left + MODEL_RIGHT
                && mouseY >= top + MODEL_TOP
                && mouseY < top + MODEL_BOTTOM;
    }

    private static void resetPreviewRotation() {
        yawDegrees = 0.0F;
        draggingPreview = false;
        customYawActive = false;
        hoveredArrowIndex = -1;
    }

    private static RenderTarget sharpOutlineTarget() {
        if (sharpOutlineLoadFailed) {
            return null;
        }

        Minecraft minecraft = Minecraft.getInstance();
        Window window = minecraft.getWindow();
        int width = window.getWidth();
        int height = window.getHeight();
        if (sharpOutlineEffect != null && width == sharpOutlineWidth && height == sharpOutlineHeight) {
            return sharpOutlineTarget;
        }

        closeSharpOutlineEffect();
        try {
            sharpOutlineEffect = new PostChain(
                    minecraft.getTextureManager(),
                    minecraft.getResourceManager(),
                    minecraft.getMainRenderTarget(),
                    SHARP_OUTLINE_SHADER);
            sharpOutlineEffect.resize(width, height);
            sharpOutlineTarget = sharpOutlineEffect.getTempTarget("final");
            sharpOutlineWidth = width;
            sharpOutlineHeight = height;
        } catch (IOException | JsonSyntaxException exception) {
            Lodged.LOGGER.warn("Failed to load sharp arrow hover outline shader: {}", SHARP_OUTLINE_SHADER, exception);
            closeSharpOutlineEffect();
            sharpOutlineLoadFailed = true;
        }
        return sharpOutlineTarget;
    }

    private static void closeSharpOutlineEffect() {
        if (sharpOutlineEffect != null) {
            sharpOutlineEffect.close();
        }
        sharpOutlineEffect = null;
        sharpOutlineTarget = null;
        sharpOutlineWidth = -1;
        sharpOutlineHeight = -1;
    }

    private record ArrowHitZone(int index, float centerX, float centerY, int radius) {
        boolean contains(double mouseX, double mouseY) {
            return distanceToSqr(mouseX, mouseY) <= radius * radius;
        }

        double distanceToSqr(double mouseX, double mouseY) {
            double deltaX = mouseX - centerX;
            double deltaY = mouseY - centerY;
            return (deltaX * deltaX) + (deltaY * deltaY);
        }
    }
}
