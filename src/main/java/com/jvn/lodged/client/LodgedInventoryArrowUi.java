package com.jvn.lodged.client;

import com.google.gson.JsonSyntaxException;
import com.jvn.lodged.Lodged;
import com.jvn.lodged.config.LodgedConfig;
import com.jvn.lodged.effect.BloodColors;
import com.jvn.lodged.effect.LodgedEffects;
import com.jvn.lodged.mixin.client.LevelRendererAccessor;
import com.jvn.lodged.network.ClientArrowState;
import com.jvn.lodged.network.payload.RemovePlayerArrowPayload;
import com.jvn.lodged.network.payload.RemovePlayerArrowPayload.Target;
import com.jvn.lodged.world.LodgedArrowBodyPart;
import com.jvn.lodged.world.LodgedArrowVisual;
import com.jvn.lodged.world.LodgedShieldArrowStorage;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.ItemAbilities;
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
    private static final int INVENTORY_TURN_TEXTURE_WIDTH = 17;
    private static final int INVENTORY_TURN_TEXTURE_HEIGHT = 4;
    private static final int INVENTORY_TURN_BOTTOM_PADDING = 2;
    private static final int INVENTORY_TURN_Z = 350;
    private static final int HIT_ZONE_RADIUS = 12;
    private static final int PLAYER_MODEL_HIT_MARGIN = 3;
    private static final float PREVIEW_SCALE = 30.0F;
    private static final float PREVIEW_Y_OFFSET = 0.0625F;
    private static final float PLAYER_RENDER_SCALE = 0.9375F;
    private static final float MODEL_RENDER_Y_OFFSET = -1.501F;
    private static final float DEGREES_TO_RADIANS = (float) (Math.PI / 180.0D);
    private static final float MODEL_HEAD_TOP = -8.0F / 16.0F;
    private static final float MODEL_HEAD_BOTTOM = 0.0F;
    private static final float MODEL_LEG_TOP = 12.0F / 16.0F;
    private static final float MODEL_FEET_Y = 24.0F / 16.0F;
    private static final float MODEL_HEAD_HALF_WIDTH = 4.0F / 16.0F;
    private static final float MODEL_BODY_HALF_WIDTH = 4.0F / 16.0F;
    private static final float MODEL_ARM_OUTER_X = 8.0F / 16.0F;
    private static final float MODEL_LIMB_HALF_DEPTH = 2.0F / 16.0F;
    private static final float ARM_ORIGIN_X = 5.0F / 16.0F;
    private static final float ARM_ORIGIN_Y = 2.0F / 16.0F;
    private static final float HELD_ITEM_LAYER_X_ROT = -90.0F * DEGREES_TO_RADIANS;
    private static final float HELD_ITEM_LAYER_Y_ROT = 180.0F * DEGREES_TO_RADIANS;
    private static final float HELD_ITEM_ARM_X_OFFSET = 1.0F / 16.0F;
    private static final float HELD_ITEM_ARM_Y_OFFSET = 0.125F;
    private static final float HELD_ITEM_ARM_Z_OFFSET = -0.625F;
    private static final float SHIELD_ITEM_X_ROT = 0.0F;
    private static final float SHIELD_ITEM_Y_ROT = 90.0F * DEGREES_TO_RADIANS;
    private static final float SHIELD_ITEM_Z_ROT = 0.0F;
    private static final float SHIELD_THIRD_PERSON_X = 10.0F / 16.0F;
    private static final float SHIELD_THIRD_PERSON_Y = 6.0F / 16.0F;
    private static final float SHIELD_THIRD_PERSON_RIGHT_Z = -4.0F / 16.0F;
    private static final float SHIELD_THIRD_PERSON_LEFT_Z = 12.0F / 16.0F;
    private static final float ITEM_RENDERER_MODEL_OFFSET = -0.5F;
    private static final float ITEM_ARM_X_ROT = -(float) (Math.PI / 10.0D);
    private static final ResourceLocation SHARP_OUTLINE_SHADER =
            ResourceLocation.fromNamespaceAndPath(Lodged.MOD_ID, "shaders/post/arrow_outline.json");
    private static final ResourceLocation BLEED_HANG_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Lodged.MOD_ID, "textures/particle/bleed_hang.png");
    private static final ResourceLocation BLEED_FALL_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Lodged.MOD_ID, "textures/particle/bleed_fall.png");
    private static final ResourceLocation BLEED_LAND_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Lodged.MOD_ID, "textures/particle/bleed_land.png");
    private static final ResourceLocation INVENTORY_TURN_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Lodged.MOD_ID, "textures/gui/inventory_turn.png");
    private static final int BLOOD_TEXTURE_SIZE = 8;
    private static final int GUI_BLOOD_SIZE = 4;
    private static final int GUI_BLOOD_Z = 300;
    private static final int GUI_BLOOD_HANG_TICKS = 6;
    private static final int GUI_BLOOD_FALL_TICKS = 18;
    private static final int GUI_BLOOD_LAND_TICKS = 7;
    private static final List<Component> INVENTORY_TURN_TOOLTIP = List.of(
            Component.translatable("tooltip.lodged.inventory_turn.rotate"),
            Component.translatable("tooltip.lodged.inventory_turn.reset"),
            Component.translatable("tooltip.lodged.inventory_turn.remove_arrows"));
    private static final LodgedArrowVisual[] FALLBACK_BLEEDING_WOUNDS = {
            new LodgedArrowVisual(0.0F, 0.32F, -0.13F, 0.0F, 0.0F, 1.0F),
            new LodgedArrowVisual(-0.38F, 0.52F, -0.08F, 0.0F, 0.0F, 1.0F),
            new LodgedArrowVisual(0.16F, 0.98F, -0.10F, 0.0F, 0.0F, 1.0F)
    };
    private static final ModelHitBox[] PLAYER_MODEL_HIT_BOXES = {
            new ModelHitBox(
                    -MODEL_HEAD_HALF_WIDTH,
                    MODEL_HEAD_TOP,
                    -MODEL_HEAD_HALF_WIDTH,
                    MODEL_HEAD_HALF_WIDTH,
                    MODEL_HEAD_BOTTOM,
                    MODEL_HEAD_HALF_WIDTH,
                    true),
            new ModelHitBox(
                    -MODEL_BODY_HALF_WIDTH,
                    MODEL_HEAD_BOTTOM,
                    -MODEL_LIMB_HALF_DEPTH,
                    MODEL_BODY_HALF_WIDTH,
                    MODEL_LEG_TOP,
                    MODEL_LIMB_HALF_DEPTH,
                    false),
            new ModelHitBox(
                    -MODEL_ARM_OUTER_X,
                    MODEL_HEAD_BOTTOM,
                    -MODEL_LIMB_HALF_DEPTH,
                    -MODEL_BODY_HALF_WIDTH,
                    MODEL_LEG_TOP,
                    MODEL_LIMB_HALF_DEPTH,
                    false),
            new ModelHitBox(
                    MODEL_BODY_HALF_WIDTH,
                    MODEL_HEAD_BOTTOM,
                    -MODEL_LIMB_HALF_DEPTH,
                    MODEL_ARM_OUTER_X,
                    MODEL_LEG_TOP,
                    MODEL_LIMB_HALF_DEPTH,
                    false),
            new ModelHitBox(
                    (-1.9F - 2.0F) / 16.0F,
                    MODEL_LEG_TOP,
                    -MODEL_LIMB_HALF_DEPTH,
                    (-1.9F + 2.0F) / 16.0F,
                    MODEL_FEET_Y,
                    MODEL_LIMB_HALF_DEPTH,
                    false),
            new ModelHitBox(
                    (1.9F - 2.0F) / 16.0F,
                    MODEL_LEG_TOP,
                    -MODEL_LIMB_HALF_DEPTH,
                    (1.9F + 2.0F) / 16.0F,
                    MODEL_FEET_Y,
                    MODEL_LIMB_HALF_DEPTH,
                    false)
    };

    private static float yawDegrees;
    private static boolean draggingPreview;
    private static boolean customYawActive;
    private static int hoveredArrowIndex = -1;
    private static int hoveredShieldArrowIndex = -1;
    private static InteractionHand hoveredShieldHand = InteractionHand.MAIN_HAND;
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
            clearHoveredArrows();
            return;
        }

        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            clearHoveredArrows();
            return;
        }

        if (isMouseOverScreenWidget(screen, event.getMouseX(), event.getMouseY())) {
            clearHoveredArrows();
            return;
        }

        ArrowHitZone hoveredZone = findHoveredZone(screen, player, event.getMouseX(), event.getMouseY());
        setHoveredArrow(hoveredZone);
    }

    @SubscribeEvent
    public static void onScreenRenderPost(ScreenEvent.Render.Post event) {
        if (!LodgedConfig.enablePlayerArrowRemoval()
                || !LodgedConfig.showInventoryTurnHint()
                || !LodgedConfig.showInventoryTurnHintTooltip()
                || !(event.getScreen() instanceof InventoryScreen screen)
                || !isInInventoryTurnHint(screen, event.getMouseX(), event.getMouseY())) {
            return;
        }

        event.getGuiGraphics().renderComponentTooltip(
                Minecraft.getInstance().font,
                INVENTORY_TURN_TOOLTIP,
                event.getMouseX(),
                event.getMouseY());
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

        if (isMouseOverScreenWidget(screen, event.getMouseX(), event.getMouseY())) {
            return;
        }

        if (event.getButton() == 2 && isOnRenderedPlayerModel(screen, player, event.getMouseX(), event.getMouseY())) {
            resetPreviewRotation();
            event.setCanceled(true);
            return;
        }

        if (event.getButton() != 0) {
            return;
        }

        ArrowHitZone hoveredZone = findHoveredZone(screen, player, event.getMouseX(), event.getMouseY());
        if (hoveredZone != null) {
            PacketDistributor.sendToServer(new RemovePlayerArrowPayload(
                    hoveredZone.index(),
                    hoveredZone.target(),
                    hoveredZone.hand()));
            event.setCanceled(true);
            return;
        }

        if (isOnRenderedPlayerModel(screen, player, event.getMouseX(), event.getMouseY())) {
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

    public static int hoveredShieldArrowIndex(ItemStack shield) {
        if (hoveredShieldArrowIndex < 0) {
            return -1;
        }

        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || shield != player.getItemInHand(hoveredShieldHand)) {
            return -1;
        }

        return hoveredShieldArrowIndex;
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
        if (LodgedConfig.showInventoryTurnHint()) {
            renderInventoryTurnHint(guiGraphics, x1, y1, x2, y2);
        }
    }

    private static void renderInventoryTurnHint(GuiGraphics guiGraphics, int x1, int y1, int x2, int y2) {
        InventoryTurnHintBounds bounds = inventoryTurnHintBounds(x1, y2);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        guiGraphics.blit(
                INVENTORY_TURN_TEXTURE,
                bounds.x(),
                bounds.y(),
                INVENTORY_TURN_Z,
                0.0F,
                0.0F,
                INVENTORY_TURN_TEXTURE_WIDTH,
                INVENTORY_TURN_TEXTURE_HEIGHT,
                INVENTORY_TURN_TEXTURE_WIDTH,
                INVENTORY_TURN_TEXTURE_HEIGHT);
        RenderSystem.disableBlend();
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
            double distance = zone.distanceToSqr(mouseX, mouseY);
            if (zone.contains(mouseX, mouseY) && distance < closestDistance) {
                closestZone = zone;
                closestDistance = distance;
            }
        }

        ShieldZoneResult mainHandZone = findShieldHoveredZone(
                screen,
                player,
                player.getMainHandItem(),
                InteractionHand.MAIN_HAND,
                mouseX,
                mouseY,
                closestDistance);
        if (mainHandZone.zone() != null) {
            closestZone = mainHandZone.zone();
            closestDistance = mainHandZone.distanceToSqr();
        }

        ShieldZoneResult offhandZone = findShieldHoveredZone(
                screen,
                player,
                player.getOffhandItem(),
                InteractionHand.OFF_HAND,
                mouseX,
                mouseY,
                closestDistance);
        if (offhandZone.zone() != null) {
            closestZone = offhandZone.zone();
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
        return new ArrowHitZone(index, Target.BODY, InteractionHand.MAIN_HAND, projected.x(), projected.y(), HIT_ZONE_RADIUS);
    }

    private static ShieldZoneResult findShieldHoveredZone(
            InventoryScreen screen,
            LocalPlayer player,
            ItemStack shield,
            InteractionHand hand,
            double mouseX,
            double mouseY,
            double closestDistance) {
        if (shield.isEmpty() || !shield.canPerformAction(ItemAbilities.SHIELD_BLOCK)) {
            return ShieldZoneResult.EMPTY;
        }

        List<LodgedArrowVisual> arrows = LodgedShieldArrowStorage.readAll(shield);
        int arrowCount = Math.min(arrows.size(), LodgedConfig.maxTrackedArrowsPerShield());
        ArrowHitZone closestZone = null;
        for (int index = 0; index < arrowCount; index++) {
            Vector3f projected = projectShieldArrowToScreen(screen, player, arrows.get(index), hand, mouseX, mouseY);
            ArrowHitZone zone = new ArrowHitZone(index, Target.SHIELD, hand, projected.x(), projected.y(), HIT_ZONE_RADIUS);
            double distance = zone.distanceToSqr(mouseX, mouseY);
            if (zone.contains(mouseX, mouseY) && distance < closestDistance) {
                closestZone = zone;
                closestDistance = distance;
            }
        }
        return new ShieldZoneResult(closestZone, closestDistance);
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

    private static Vector3f projectShieldArrowToScreen(
            InventoryScreen screen,
            LocalPlayer player,
            LodgedArrowVisual arrow,
            InteractionHand hand,
            double mouseX,
            double mouseY) {
        int left = screen.getGuiLeft();
        int top = screen.getGuiTop();
        float centerX = left + ((MODEL_LEFT + MODEL_RIGHT) / 2.0F);
        float centerY = top + ((MODEL_TOP + MODEL_BOTTOM) / 2.0F);
        return projectModelToScreen(
                player,
                shieldModelPosition(player, arrow, hand),
                centerX,
                centerY,
                mouseX,
                mouseY);
    }

    private static Vector3f shieldModelPosition(LocalPlayer player, LodgedArrowVisual arrow, InteractionHand hand) {
        HumanoidArm arm = hand == InteractionHand.MAIN_HAND
                ? player.getMainArm()
                : player.getMainArm().getOpposite();
        boolean leftHand = arm == HumanoidArm.LEFT;
        float armSide = leftHand ? 1.0F : -1.0F;
        float itemTransformSide = leftHand ? -1.0F : 1.0F;
        float shieldZ = leftHand ? SHIELD_THIRD_PERSON_LEFT_Z : SHIELD_THIRD_PERSON_RIGHT_Z;

        return new Matrix4f()
                .translate(armSide * ARM_ORIGIN_X, ARM_ORIGIN_Y, 0.0F)
                .rotate(new Quaternionf().rotationZYX(0.0F, 0.0F, ITEM_ARM_X_ROT))
                .rotateX(HELD_ITEM_LAYER_X_ROT)
                .rotateY(HELD_ITEM_LAYER_Y_ROT)
                .translate(-armSide * HELD_ITEM_ARM_X_OFFSET, HELD_ITEM_ARM_Y_OFFSET, HELD_ITEM_ARM_Z_OFFSET)
                .translate(itemTransformSide * SHIELD_THIRD_PERSON_X, SHIELD_THIRD_PERSON_Y, shieldZ)
                .rotate(new Quaternionf().rotationXYZ(
                        SHIELD_ITEM_X_ROT,
                        itemTransformSide * SHIELD_ITEM_Y_ROT,
                        itemTransformSide * SHIELD_ITEM_Z_ROT))
                .translate(ITEM_RENDERER_MODEL_OFFSET, ITEM_RENDERER_MODEL_OFFSET, ITEM_RENDERER_MODEL_OFFSET)
                .scale(1.0F, -1.0F, -1.0F)
                .transformPosition(arrow.modelX(), arrow.modelY(), arrow.modelZ(), new Vector3f());
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

    private static boolean isOnRenderedPlayerModel(InventoryScreen screen, LocalPlayer player, double mouseX, double mouseY) {
        int left = screen.getGuiLeft();
        int top = screen.getGuiTop();
        float centerX = left + ((MODEL_LEFT + MODEL_RIGHT) / 2.0F);
        float centerY = top + ((MODEL_TOP + MODEL_BOTTOM) / 2.0F);
        for (ModelHitBox box : PLAYER_MODEL_HIT_BOXES) {
            if (box.containsScreenPoint(player, centerX, centerY, mouseX, mouseY)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isMouseOverScreenWidget(InventoryScreen screen, double mouseX, double mouseY) {
        for (GuiEventListener child : screen.children()) {
            if (child.isMouseOver(mouseX, mouseY)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isInInventoryTurnHint(InventoryScreen screen, double mouseX, double mouseY) {
        InventoryTurnHintBounds bounds = inventoryTurnHintBounds(
                screen.getGuiLeft() + MODEL_LEFT,
                screen.getGuiTop() + MODEL_BOTTOM);
        return mouseX >= bounds.x()
                && mouseX < bounds.x() + INVENTORY_TURN_TEXTURE_WIDTH
                && mouseY >= bounds.y()
                && mouseY < bounds.y() + INVENTORY_TURN_TEXTURE_HEIGHT;
    }

    private static InventoryTurnHintBounds inventoryTurnHintBounds(int previewLeft, int previewBottom) {
        int x = previewLeft + ((MODEL_RIGHT - MODEL_LEFT - INVENTORY_TURN_TEXTURE_WIDTH) / 2);
        int y = previewBottom - INVENTORY_TURN_TEXTURE_HEIGHT - INVENTORY_TURN_BOTTOM_PADDING;
        return new InventoryTurnHintBounds(x, y);
    }

    private static void resetPreviewRotation() {
        yawDegrees = 0.0F;
        draggingPreview = false;
        customYawActive = false;
        clearHoveredArrows();
    }

    private static void setHoveredArrow(ArrowHitZone hoveredZone) {
        clearHoveredArrows();
        if (hoveredZone == null) {
            return;
        }

        if (hoveredZone.target() == Target.BODY) {
            hoveredArrowIndex = hoveredZone.index();
        } else {
            hoveredShieldArrowIndex = hoveredZone.index();
            hoveredShieldHand = hoveredZone.hand();
        }
    }

    private static void clearHoveredArrows() {
        hoveredArrowIndex = -1;
        hoveredShieldArrowIndex = -1;
        hoveredShieldHand = InteractionHand.MAIN_HAND;
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

    private record ArrowHitZone(int index, Target target, InteractionHand hand, float centerX, float centerY, int radius) {
        boolean contains(double mouseX, double mouseY) {
            return distanceToSqr(mouseX, mouseY) <= radius * radius;
        }

        double distanceToSqr(double mouseX, double mouseY) {
            double deltaX = mouseX - centerX;
            double deltaY = mouseY - centerY;
            return (deltaX * deltaX) + (deltaY * deltaY);
        }
    }

    private record InventoryTurnHintBounds(int x, int y) {
    }

    private record ShieldZoneResult(ArrowHitZone zone, double distanceToSqr) {
        private static final ShieldZoneResult EMPTY = new ShieldZoneResult(null, Double.MAX_VALUE);
    }

    private record ModelHitBox(
            float minX,
            float minY,
            float minZ,
            float maxX,
            float maxY,
            float maxZ,
            boolean followsHead) {
        boolean containsScreenPoint(
                LocalPlayer player,
                float centerX,
                float centerY,
                double mouseX,
                double mouseY) {
            ScreenPoint[] corners = new ScreenPoint[8];
            int index = 0;
            for (float x : new float[]{minX, maxX}) {
                for (float y : new float[]{minY, maxY}) {
                    for (float z : new float[]{minZ, maxZ}) {
                        corners[index++] = projectCorner(player, centerX, centerY, mouseX, mouseY, x, y, z);
                    }
                }
            }

            ScreenPoint[] hull = convexHull(corners);
            return isInsideOrNearPolygon(hull, mouseX, mouseY, PLAYER_MODEL_HIT_MARGIN);
        }

        private ScreenPoint projectCorner(
                LocalPlayer player,
                float centerX,
                float centerY,
                double mouseX,
                double mouseY,
                float x,
                float y,
                float z) {
            Vector3f point = new Vector3f(x, y, z);
            if (followsHead) {
                point = new Matrix4f()
                        .rotate(new Quaternionf().rotationZYX(
                                0.0F,
                                currentHeadYawRadians(centerX, mouseX),
                                currentHeadPitchRadians(centerY, mouseY)))
                        .transformPosition(point);
            }

            Vector3f projected = projectModelToScreen(player, point, centerX, centerY, mouseX, mouseY);
            return new ScreenPoint(projected.x(), projected.y());
        }
    }

    private record ScreenPoint(double x, double y) {
    }

    private static ScreenPoint[] convexHull(ScreenPoint[] points) {
        ScreenPoint[] sorted = Arrays.copyOf(points, points.length);
        Arrays.sort(sorted, Comparator.comparingDouble(ScreenPoint::x).thenComparingDouble(ScreenPoint::y));

        ScreenPoint[] hull = new ScreenPoint[sorted.length * 2];
        int size = 0;
        for (ScreenPoint point : sorted) {
            while (size >= 2 && cross(hull[size - 2], hull[size - 1], point) <= 0.0D) {
                size--;
            }
            hull[size++] = point;
        }

        int lowerSize = size;
        for (int index = sorted.length - 2; index >= 0; index--) {
            ScreenPoint point = sorted[index];
            while (size > lowerSize && cross(hull[size - 2], hull[size - 1], point) <= 0.0D) {
                size--;
            }
            hull[size++] = point;
        }

        if (size <= 1) {
            return Arrays.copyOf(hull, size);
        }
        return Arrays.copyOf(hull, size - 1);
    }

    private static boolean isInsideOrNearPolygon(ScreenPoint[] polygon, double mouseX, double mouseY, double margin) {
        if (polygon.length < 3) {
            double marginSqr = margin * margin;
            for (ScreenPoint point : polygon) {
                double deltaX = mouseX - point.x();
                double deltaY = mouseY - point.y();
                if ((deltaX * deltaX) + (deltaY * deltaY) <= marginSqr) {
                    return true;
                }
            }
            return false;
        }

        boolean hasPositive = false;
        boolean hasNegative = false;
        ScreenPoint mousePoint = new ScreenPoint(mouseX, mouseY);
        for (int index = 0; index < polygon.length; index++) {
            ScreenPoint start = polygon[index];
            ScreenPoint end = polygon[(index + 1) % polygon.length];
            double side = cross(start, end, mousePoint);
            hasPositive |= side > 0.0D;
            hasNegative |= side < 0.0D;
            if (hasPositive && hasNegative) {
                return isNearPolygonEdge(polygon, mouseX, mouseY, margin);
            }
        }
        return true;
    }

    private static boolean isNearPolygonEdge(ScreenPoint[] polygon, double mouseX, double mouseY, double margin) {
        double marginSqr = margin * margin;
        for (int index = 0; index < polygon.length; index++) {
            ScreenPoint start = polygon[index];
            ScreenPoint end = polygon[(index + 1) % polygon.length];
            if (distanceToSegmentSqr(mouseX, mouseY, start, end) <= marginSqr) {
                return true;
            }
        }
        return false;
    }

    private static double distanceToSegmentSqr(double mouseX, double mouseY, ScreenPoint start, ScreenPoint end) {
        double deltaX = end.x() - start.x();
        double deltaY = end.y() - start.y();
        double lengthSqr = (deltaX * deltaX) + (deltaY * deltaY);
        if (lengthSqr <= 1.0E-7D) {
            double pointDeltaX = mouseX - start.x();
            double pointDeltaY = mouseY - start.y();
            return (pointDeltaX * pointDeltaX) + (pointDeltaY * pointDeltaY);
        }

        double progress = ((mouseX - start.x()) * deltaX + (mouseY - start.y()) * deltaY) / lengthSqr;
        progress = Math.max(0.0D, Math.min(1.0D, progress));
        double closestX = start.x() + (deltaX * progress);
        double closestY = start.y() + (deltaY * progress);
        double closestDeltaX = mouseX - closestX;
        double closestDeltaY = mouseY - closestY;
        return (closestDeltaX * closestDeltaX) + (closestDeltaY * closestDeltaY);
    }

    private static double cross(ScreenPoint start, ScreenPoint end, ScreenPoint point) {
        return ((end.x() - start.x()) * (point.y() - start.y()))
                - ((end.y() - start.y()) * (point.x() - start.x()));
    }
}
