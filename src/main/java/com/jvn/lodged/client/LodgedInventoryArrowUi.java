package com.jvn.lodged.client;

import com.google.gson.JsonSyntaxException;
import com.jvn.lodged.Lodged;
import com.jvn.lodged.config.LodgedConfig;
import com.jvn.lodged.effect.BloodColors;
import com.jvn.lodged.effect.LodgedEffects;
import com.jvn.lodged.mixin.client.LevelRendererAccessor;
import com.jvn.lodged.network.ClientArrowState;
import com.jvn.lodged.network.payload.ArrowRemovalResultPayload;
import com.jvn.lodged.network.payload.ArrowRemovalResultPayload.Result;
import com.jvn.lodged.network.payload.RemovePlayerArrowPayload;
import com.jvn.lodged.network.payload.RemovePlayerArrowPayload.Target;
import com.jvn.lodged.world.LodgedArrowBodyPart;
import com.jvn.lodged.world.LodgedArrowDepth;
import com.jvn.lodged.world.LodgedArrowVisual;
import com.jvn.lodged.world.LodgedArmorArrowStorage;
import com.jvn.lodged.world.LodgedShieldArrowStorage;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
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
    private static final float PREVIEW_SCROLL_YAW_DEGREES = 12.0F;
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
    private static final int RISK_EASY_COLOR = 0x89F2A0;
    private static final int RISK_MODERATE_COLOR = 0xFFD86A;
    private static final int RISK_DANGEROUS_COLOR = 0xFF6A5E;
    private static final int ARMOR_ARROW_OUTLINE_COLOR = 0x8DDCFF;
    private static final int RISK_OUTLINE_ALPHA = 255;
    private static final String TOOLTIP_ICON_SPACER = "     ";
    private static final int TOOLTIP_ICON_WIDTH = 16;
    private static final int TOOLTIP_ICON_HEIGHT = 32;
    private static final int TOOLTIP_ICON_Z = 450;
    private static final int[][] SHIELD_ICON_OUTER_SPANS = {
            {7, 9},
            {4, 13},
            {2, 14},
            {1, 15},
            {1, 15},
            {2, 14},
            {2, 14},
            {2, 14},
            {3, 13},
            {3, 13},
            {3, 13},
            {4, 12},
            {5, 11},
            {7, 9}
    };
    private static final int[][] SHIELD_ICON_INNER_SPANS = {
            {5, 12},
            {3, 13},
            {2, 14},
            {2, 14},
            {3, 13},
            {3, 13},
            {3, 13},
            {4, 12},
            {4, 12},
            {4, 12},
            {5, 11},
            {6, 10},
            {7, 9}
    };
    private static final int[][] HORSE_ARMOR_ICON_SPANS = {
            {3, 11, 12},
            {4, 11, 14},
            {5, 10, 12},
            {5, 13, 15},
            {6, 9, 15},
            {7, 9, 15},
            {8, 2, 12},
            {9, 1, 12},
            {10, 1, 13},
            {11, 1, 13},
            {12, 1, 13},
            {13, 1, 13}
    };
    private static final int REMOVAL_SUBTITLE_Z = 450;
    private static final int REMOVAL_ANIMATION_Z = 420;
    private static final int MAX_REMOVAL_ANIMATIONS = 6;
    private static final int REMOVAL_SUBTITLE_MS = 1400;
    private static final int REMOVAL_SUBTITLE_FADE_MS = 350;
    private static final int REMOVAL_SUBTITLE_COLOR = 0xFFD8D8D8;
    private static final List<Component> INVENTORY_TURN_TOOLTIP = List.of(
            Component.translatable("tooltip.lodged.inventory_turn.title").withStyle(ChatFormatting.YELLOW),
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
    private static int hoveredArmorArrowIndex = -1;
    private static EquipmentSlot hoveredArmorSlot = EquipmentSlot.CHEST;
    private static PostChain sharpOutlineEffect;
    private static RenderTarget sharpOutlineTarget;
    private static int sharpOutlineWidth = -1;
    private static int sharpOutlineHeight = -1;
    private static boolean sharpOutlineLoadFailed;
    private static RenderTarget previousEntityTarget;
    private static final List<RemovalAnimation> removalAnimations = new ArrayList<>();
    private static Component removalSubtitle;
    private static long removalSubtitleStartedAtMs;

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
        if (!(event.getScreen() instanceof InventoryScreen screen)) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player != null) {
            renderRemovalAnimations(event.getGuiGraphics(), screen, player, event.getMouseX(), event.getMouseY());
            renderRemovalSubtitle(event.getGuiGraphics(), screen);
        }

        if (LodgedConfig.enablePlayerArrowRemoval() && player != null) {
            HoveredArrow hoveredArrow = hoveredArrow(player);
            if (hoveredArrow != null) {
                renderArrowRemovalTooltip(event.getGuiGraphics(), hoveredArrow, event.getMouseX(), event.getMouseY());
                return;
            }
        }

        if (!LodgedConfig.enablePlayerArrowRemoval()
                || !LodgedConfig.showInventoryTurnHint()
                || !LodgedConfig.showInventoryTurnHintTooltip()
                || !isInInventoryTurnHint(screen, event.getMouseX(), event.getMouseY())) {
            return;
        }

        event.getGuiGraphics().renderComponentTooltip(
                minecraft.font,
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
                    hoveredZone.hand(),
                    hoveredZone.armorSlot()));
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
    public static void onMouseScrolled(ScreenEvent.MouseScrolled.Pre event) {
        if (!LodgedConfig.enablePlayerArrowRemoval() || !(event.getScreen() instanceof InventoryScreen screen)) {
            return;
        }

        if (isMouseOverScreenWidget(screen, event.getMouseX(), event.getMouseY())
                || !isInPlayerPreviewArea(screen, event.getMouseX(), event.getMouseY())) {
            return;
        }

        double scrollDelta = Math.abs(event.getScrollDeltaX()) > Math.abs(event.getScrollDeltaY())
                ? event.getScrollDeltaX()
                : event.getScrollDeltaY();
        if (scrollDelta == 0.0D) {
            return;
        }

        yawDegrees -= (float) scrollDelta * PREVIEW_SCROLL_YAW_DEGREES;
        customYawActive = true;
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

    public static int hoveredArmorArrowIndex() {
        return hoveredArmorArrowIndex;
    }

    public static EquipmentSlot hoveredArmorSlot() {
        return hoveredArmorSlot;
    }

    public static OutlineColor riskOutlineColor(LodgedArrowVisual arrow) {
        return outlineColorFor(removalChance(arrow));
    }

    public static OutlineColor shieldRiskOutlineColor() {
        return outlineColorFor(shieldRemovalChance());
    }

    public static OutlineColor armorArrowOutlineColor() {
        return OutlineColor.fromRgb(ARMOR_ARROW_OUTLINE_COLOR);
    }

    public static void renderHorseArmorRemovalTooltip(
            GuiGraphics guiGraphics,
            LodgedArrowVisual arrow,
            int mouseX,
            int mouseY) {
        renderArrowRemovalTooltip(
                guiGraphics,
                new HoveredArrow(arrow, Target.ARMOR, InteractionHand.MAIN_HAND, EquipmentSlot.BODY),
                mouseX,
                mouseY);
    }

    public static void handleRemovalResult(ArrowRemovalResultPayload payload) {
        Result result = payload.result();
        if (result == Result.TOO_RISKY || result == Result.CANT_REMOVE_NOW) {
            showRemovalSubtitle(result);
            return;
        }

        if (result == Result.INVENTORY_FULL) {
            showRemovalSubtitle(result);
        }

        if (!LodgedConfig.enableArrowRemovalAnimation()) {
            return;
        }

        if (removalAnimations.size() >= MAX_REMOVAL_ANIMATIONS) {
            removalAnimations.remove(0);
        }
        removalAnimations.add(new RemovalAnimation(payload, Util.getMillis()));
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

    private static void renderArrowRemovalTooltip(GuiGraphics guiGraphics, HoveredArrow hoveredArrow, int mouseX, int mouseY) {
        Minecraft minecraft = Minecraft.getInstance();
        Font font = minecraft.font;
        double chance = removalChance(hoveredArrow);
        int chancePercent = Math.round((float) (Mth.clamp(chance, 0.0D, 1.0D) * 100.0D));
        OutlineColor outlineColor = riskOutlineColor(hoveredArrow);
        List<Component> lines = new ArrayList<>();
        lines.add(paddedTooltipLine(hoveredArrow.arrow().stack().getHoverName()));
        if (hoveredArrow.target() != Target.SHIELD) {
            lines.add(paddedTooltipLine(hitLocationTooltip(hoveredArrow)));
        }
        lines.add(coloredTooltipLine(depthTooltip(removalDepth(hoveredArrow)), outlineColor.rgb()));
        lines.add(coloredTooltipLine(
                Component.translatable("tooltip.lodged.arrow_removal.chance", chancePercent),
                outlineColor.rgb()));
        if (hoveredArrow.target() == Target.BODY) {
            lines.add(coloredTooltipLine(Component.translatable("tooltip.lodged.arrow_removal.bleeding_risk"), 0xFFFFA7A0));
        }
        lines.add(paddedTooltipLine(Component.translatable("tooltip.lodged.arrow_removal.click").withStyle(ChatFormatting.GRAY)));

        int textWidth = 0;
        for (Component line : lines) {
            textWidth = Math.max(textWidth, font.width(line));
        }

        int width = textWidth;
        int height = lines.size() == 1 ? 8 : lines.size() * 10 + 2;
        int x = mouseX + 12;
        int y = mouseY - 12;
        Window window = minecraft.getWindow();
        int screenWidth = window.getGuiScaledWidth();
        int screenHeight = window.getGuiScaledHeight();
        if (x + width > screenWidth) {
            x -= 28 + width;
        }
        if (y + height + 6 > screenHeight) {
            y = screenHeight - height - 6;
        }
        y = Math.max(6, y);

        guiGraphics.renderComponentTooltip(font, lines, mouseX, mouseY);
        guiGraphics.flush();
        drawTooltipTargetIcon(
                guiGraphics,
                x + 1,
                y + Math.max(2, (height - TOOLTIP_ICON_HEIGHT) / 2),
                hoveredArrow,
                outlineColor.rgb());
        guiGraphics.flush();
    }

    private static Component coloredTooltipLine(Component component, int color) {
        MutableComponent line = Component.literal(TOOLTIP_ICON_SPACER);
        line.append(component.copy().withStyle(style -> style.withColor(TextColor.fromRgb(color & 0xFFFFFF))));
        return line;
    }

    private static Component paddedTooltipLine(Component component) {
        return Component.literal(TOOLTIP_ICON_SPACER).append(component.copy());
    }

    private static void drawTooltipTargetIcon(
            GuiGraphics guiGraphics,
            int x,
            int y,
            HoveredArrow hoveredArrow,
            int highlightColor) {
        if (hoveredArrow.target() == Target.SHIELD) {
            drawShieldIcon(guiGraphics, x, y, highlightColor);
            return;
        }

        if (hoveredArrow.target() == Target.ARMOR && hoveredArrow.armorSlot() == EquipmentSlot.BODY) {
            drawHorseArmorIcon(guiGraphics, x, y, highlightColor);
            return;
        }

        drawBodyPartIcon(guiGraphics, x, y, hoveredArrow.arrow().bodyPart(), highlightColor);
    }

    private static void drawBodyPartIcon(
            GuiGraphics guiGraphics,
            int x,
            int y,
            LodgedArrowBodyPart bodyPart,
            int highlightColor) {
        int dim = 0xFF37312D;
        guiGraphics.fill(x + 4, y, x + 12, y + 8, TOOLTIP_ICON_Z, bodyPart == LodgedArrowBodyPart.HEAD ? highlightColor : dim);
        guiGraphics.fill(x + 4, y + 8, x + 12, y + 20, TOOLTIP_ICON_Z, bodyPart == LodgedArrowBodyPart.CHEST ? highlightColor : dim);
        guiGraphics.fill(x, y + 8, x + 4, y + 20, TOOLTIP_ICON_Z, bodyPart == LodgedArrowBodyPart.RIGHT_ARM ? highlightColor : dim);
        guiGraphics.fill(x + 12, y + 8, x + TOOLTIP_ICON_WIDTH, y + 20, TOOLTIP_ICON_Z, bodyPart == LodgedArrowBodyPart.LEFT_ARM ? highlightColor : dim);
        guiGraphics.fill(x + 4, y + 20, x + 8, y + 32, TOOLTIP_ICON_Z, bodyPart == LodgedArrowBodyPart.RIGHT_LEG ? highlightColor : dim);
        guiGraphics.fill(x + 8, y + 20, x + 12, y + 32, TOOLTIP_ICON_Z, bodyPart == LodgedArrowBodyPart.LEFT_LEG ? highlightColor : dim);
    }

    private static void drawShieldIcon(GuiGraphics guiGraphics, int x, int y, int highlightColor) {
        int top = y + 8;

        for (int row = 0; row < SHIELD_ICON_OUTER_SPANS.length; row++) {
            int rowY = top + row + 1;
            guiGraphics.fill(
                    x + SHIELD_ICON_OUTER_SPANS[row][0],
                    rowY,
                    x + SHIELD_ICON_OUTER_SPANS[row][1],
                    rowY + 1,
                    TOOLTIP_ICON_Z,
                    highlightColor);
        }
        for (int row = 0; row < SHIELD_ICON_INNER_SPANS.length; row++) {
            int rowY = top + row + 2;
            guiGraphics.fill(
                    x + SHIELD_ICON_INNER_SPANS[row][0],
                    rowY,
                    x + SHIELD_ICON_INNER_SPANS[row][1],
                    rowY + 1,
                    TOOLTIP_ICON_Z,
                    highlightColor);
        }
    }

    private static void drawHorseArmorIcon(GuiGraphics guiGraphics, int x, int y, int highlightColor) {
        int top = y + 8;
        for (int[] span : HORSE_ARMOR_ICON_SPANS) {
            int rowY = top + span[0];
            guiGraphics.fill(
                    x + span[1],
                    rowY,
                    x + span[2],
                    rowY + 1,
                    TOOLTIP_ICON_Z,
                    highlightColor);
        }
    }

    private static void renderRemovalAnimations(
            GuiGraphics guiGraphics,
            InventoryScreen screen,
            LocalPlayer player,
            int mouseX,
            int mouseY) {
        if (removalAnimations.isEmpty()) {
            return;
        }

        long now = Util.getMillis();
        Iterator<RemovalAnimation> iterator = removalAnimations.iterator();
        while (iterator.hasNext()) {
            RemovalAnimation animation = iterator.next();
            float progress = (now - animation.startedAtMs()) / (float) removalAnimationMs(animation.payload());
            if (progress >= 1.0F) {
                iterator.remove();
                continue;
            }

            if (animation.payload().result() == Result.FAILED) {
                renderFailedRemovalAnimation(guiGraphics, screen, player, animation.payload(), mouseX, mouseY, progress);
            } else {
                renderSuccessfulRemovalAnimation(guiGraphics, screen, player, animation.payload(), mouseX, mouseY, progress);
            }
        }
    }

    private static void renderSuccessfulRemovalAnimation(
            GuiGraphics guiGraphics,
            InventoryScreen screen,
            LocalPlayer player,
            ArrowRemovalResultPayload payload,
            int mouseX,
            int mouseY,
            float progress) {
        Vector3f source = removalAnimationSource(screen, player, payload, mouseX, mouseY);
        Vector3f destination = removalAnimationDestination(screen, payload);
        float eased = easeOut(progress);
        float pop = 1.0F + (float) Math.sin(progress * Math.PI) * 0.35F;
        float arc = -10.0F * (float) Math.sin(progress * Math.PI);
        float x = Mth.lerp(eased, source.x() - 8.0F, destination.x() - 8.0F);
        float y = Mth.lerp(eased, source.y() - 8.0F, destination.y() - 8.0F) + arc;

        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(x + 8.0F, y + 8.0F, REMOVAL_ANIMATION_Z);
        guiGraphics.pose().scale(pop, pop, 1.0F);
        guiGraphics.pose().translate(-8.0F, -8.0F, 0.0F);
        guiGraphics.renderItem(payload.arrow().stack(), 0, 0);
        guiGraphics.pose().popPose();
    }

    private static void renderFailedRemovalAnimation(
            GuiGraphics guiGraphics,
            InventoryScreen screen,
            LocalPlayer player,
            ArrowRemovalResultPayload payload,
            int mouseX,
            int mouseY,
            float progress) {
        Vector3f source = removalAnimationSource(screen, player, payload, mouseX, mouseY);
        float shake = (float) Math.sin(progress * Math.PI * 10.0D) * (1.0F - progress) * 3.0F;
        int x = Math.round(source.x() + shake);
        int y = Math.round(source.y());
        int alpha = Math.round((1.0F - progress) * 255.0F) << 24;
        guiGraphics.fill(x - 6, y - 2, x - 1, y, alpha | 0x7A4E2A);
        guiGraphics.fill(x + 1, y + 1, x + 7, y + 3, alpha | 0x7A4E2A);
        guiGraphics.fill(x - 1, y - 4, x + 1, y + 5, alpha | 0xD6D1C2);

        if (payload.target() == Target.BODY && progress < 0.8F) {
            int color = BloodColors.colorFor(player);
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            guiGraphics.setColor(
                    ((color >> 16) & 0xFF) / 255.0F,
                    ((color >> 8) & 0xFF) / 255.0F,
                    (color & 0xFF) / 255.0F,
                    1.0F - progress);
            drawInventoryBloodDrop(
                    guiGraphics,
                    source.x(),
                    source.y(),
                    progress * (GUI_BLOOD_HANG_TICKS + GUI_BLOOD_FALL_TICKS + GUI_BLOOD_LAND_TICKS),
                    97);
            guiGraphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
            RenderSystem.disableBlend();
        }
    }

    private static Vector3f removalAnimationSource(
            InventoryScreen screen,
            LocalPlayer player,
            ArrowRemovalResultPayload payload,
            int mouseX,
            int mouseY) {
        return payload.target() == Target.SHIELD
                ? projectShieldArrowToScreen(screen, player, payload.arrow(), payload.hand(), mouseX, mouseY)
                : projectArrowToScreen(screen, player, payload.arrow(), mouseX, mouseY);
    }

    private static Vector3f removalAnimationDestination(InventoryScreen screen, ArrowRemovalResultPayload payload) {
        Slot slot = inventorySlot(screen, payload.inventorySlot());
        if (slot != null) {
            return new Vector3f(
                    screen.getGuiLeft() + slot.x + 8.0F,
                    screen.getGuiTop() + slot.y + 8.0F,
                    0.0F);
        }

        return new Vector3f(screen.getGuiLeft() + 150.0F, screen.getGuiTop() + 71.0F, 0.0F);
    }

    private static Slot inventorySlot(InventoryScreen screen, int inventorySlot) {
        int menuSlot = menuSlotForInventorySlot(inventorySlot);
        if (menuSlot < 0 || menuSlot >= screen.getMenu().slots.size()) {
            return null;
        }
        return screen.getMenu().slots.get(menuSlot);
    }

    private static int menuSlotForInventorySlot(int inventorySlot) {
        if (inventorySlot >= 0 && inventorySlot < 9) {
            return InventoryMenu.USE_ROW_SLOT_START + inventorySlot;
        }
        if (inventorySlot >= 9 && inventorySlot < 36) {
            return InventoryMenu.INV_SLOT_START + (inventorySlot - 9);
        }
        if (inventorySlot == 40) {
            return InventoryMenu.SHIELD_SLOT;
        }
        return -1;
    }

    private static float easeOut(float progress) {
        float inverse = 1.0F - Mth.clamp(progress, 0.0F, 1.0F);
        return 1.0F - inverse * inverse * inverse;
    }

    private static void renderRemovalSubtitle(GuiGraphics guiGraphics, InventoryScreen screen) {
        if (removalSubtitle == null) {
            return;
        }

        long age = Util.getMillis() - removalSubtitleStartedAtMs;
        if (age >= REMOVAL_SUBTITLE_MS) {
            removalSubtitle = null;
            return;
        }

        int alpha = 0xFF;
        int fadeStart = REMOVAL_SUBTITLE_MS - REMOVAL_SUBTITLE_FADE_MS;
        if (age > fadeStart) {
            alpha = Math.round((1.0F - (age - fadeStart) / (float) REMOVAL_SUBTITLE_FADE_MS) * 255.0F);
        }

        Font font = Minecraft.getInstance().font;
        int x = screen.getGuiLeft() + ((MODEL_LEFT + MODEL_RIGHT) / 2) - (font.width(removalSubtitle) / 2);
        int y = screen.getGuiTop() + MODEL_BOTTOM + 6;
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(0.0F, 0.0F, REMOVAL_SUBTITLE_Z);
        guiGraphics.drawString(font, removalSubtitle, x, y, (alpha << 24) | (REMOVAL_SUBTITLE_COLOR & 0xFFFFFF), false);
        guiGraphics.pose().popPose();
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
            closestDistance = offhandZone.distanceToSqr();
        }

        ShieldZoneResult armorZone = findArmorHoveredZone(
                screen,
                player,
                mouseX,
                mouseY,
                closestDistance);
        if (armorZone.zone() != null) {
            closestZone = armorZone.zone();
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
        return new ArrowHitZone(
                index,
                Target.BODY,
                InteractionHand.MAIN_HAND,
                EquipmentSlot.CHEST,
                projected.x(),
                projected.y(),
                HIT_ZONE_RADIUS);
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
            ArrowHitZone zone = new ArrowHitZone(
                    index,
                    Target.SHIELD,
                    hand,
                    EquipmentSlot.CHEST,
                    projected.x(),
                    projected.y(),
                    HIT_ZONE_RADIUS);
            double distance = zone.distanceToSqr(mouseX, mouseY);
            if (zone.contains(mouseX, mouseY) && distance < closestDistance) {
                closestZone = zone;
                closestDistance = distance;
            }
        }
        return new ShieldZoneResult(closestZone, closestDistance);
    }

    private static ShieldZoneResult findArmorHoveredZone(
            InventoryScreen screen,
            LocalPlayer player,
            double mouseX,
            double mouseY,
            double closestDistance) {
        if (!LodgedConfig.renderArmorArrows()) {
            return ShieldZoneResult.EMPTY;
        }

        ArrowHitZone closestZone = null;
        for (EquipmentSlot slot : LodgedArmorArrowStorage.armorSlots()) {
            ItemStack armor = player.getItemBySlot(slot);
            List<LodgedArrowVisual> arrows = LodgedArmorArrowStorage.readAll(armor);
            int arrowCount = Math.min(arrows.size(), LodgedConfig.maxTrackedArrowsPerArmorPiece());
            for (int index = 0; index < arrowCount; index++) {
                Vector3f projected = projectArrowToScreen(screen, player, arrows.get(index), mouseX, mouseY);
                ArrowHitZone zone = new ArrowHitZone(
                        index,
                        Target.ARMOR,
                        InteractionHand.MAIN_HAND,
                        slot,
                        projected.x(),
                        projected.y(),
                        HIT_ZONE_RADIUS);
                double distance = zone.distanceToSqr(mouseX, mouseY);
                if (zone.contains(mouseX, mouseY) && distance < closestDistance) {
                    closestZone = zone;
                    closestDistance = distance;
                }
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
        Vector3f modelPosition = renderModelPosition(player, arrow, centerX, centerY, mouseX, mouseY);
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
                    renderModelPosition(player, arrow, centerX, centerY, mouseX, mouseY),
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
                    renderModelPosition(player, wound, centerX, centerY, mouseX, mouseY),
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
            LocalPlayer player,
            LodgedArrowVisual arrow,
            float centerX,
            float centerY,
            double mouseX,
            double mouseY) {
        if (Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(player) instanceof PlayerRenderer renderer) {
            return LodgedArrowRenderHelper.humanoidModelPosition(renderer.getModel(), arrow);
        }

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

    private static boolean isInPlayerPreviewArea(InventoryScreen screen, double mouseX, double mouseY) {
        int left = screen.getGuiLeft();
        int top = screen.getGuiTop();
        return mouseX >= left + MODEL_LEFT
                && mouseX < left + MODEL_RIGHT
                && mouseY >= top + MODEL_TOP
                && mouseY < top + MODEL_BOTTOM;
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

    private static HoveredArrow hoveredArrow(LocalPlayer player) {
        if (hoveredArrowIndex >= 0) {
            List<LodgedArrowVisual> arrows = ClientArrowState.removableArrows();
            int arrowCount = Math.min(arrows.size(), ClientArrowState.syncedArrowCount());
            arrowCount = Math.min(arrowCount, LodgedConfig.maxRemovablePlayerArrows());
            if (hoveredArrowIndex < arrowCount) {
                return new HoveredArrow(
                        arrows.get(hoveredArrowIndex),
                        Target.BODY,
                        InteractionHand.MAIN_HAND,
                        EquipmentSlot.CHEST);
            }
        }

        if (hoveredShieldArrowIndex < 0) {
            if (hoveredArmorArrowIndex < 0) {
                return null;
            }

            ItemStack armor = player.getItemBySlot(hoveredArmorSlot);
            List<LodgedArrowVisual> armorArrows = LodgedArmorArrowStorage.readAll(armor);
            int armorArrowCount = Math.min(armorArrows.size(), LodgedConfig.maxTrackedArrowsPerArmorPiece());
            if (hoveredArmorArrowIndex >= armorArrowCount) {
                return null;
            }

            return new HoveredArrow(
                    armorArrows.get(hoveredArmorArrowIndex),
                    Target.ARMOR,
                    InteractionHand.MAIN_HAND,
                    hoveredArmorSlot);
        }

        ItemStack shield = player.getItemInHand(hoveredShieldHand);
        if (shield.isEmpty() || !shield.canPerformAction(ItemAbilities.SHIELD_BLOCK)) {
            return null;
        }

        List<LodgedArrowVisual> arrows = LodgedShieldArrowStorage.readAll(shield);
        int arrowCount = Math.min(arrows.size(), LodgedConfig.maxTrackedArrowsPerShield());
        if (hoveredShieldArrowIndex >= arrowCount) {
            return null;
        }

        return new HoveredArrow(arrows.get(hoveredShieldArrowIndex), Target.SHIELD, hoveredShieldHand, EquipmentSlot.CHEST);
    }

    private static double removalChance(LodgedArrowVisual arrow) {
        return removalChance(arrow.bodyPart()) * LodgedConfig.arrowDepthRemovalSuccessMultiplier(arrow.depth());
    }

    private static double removalChance(HoveredArrow hoveredArrow) {
        if (hoveredArrow.target() == Target.SHIELD) {
            return shieldRemovalChance();
        }
        if (hoveredArrow.target() == Target.ARMOR) {
            return armorRemovalChance();
        }
        return removalChance(hoveredArrow.arrow());
    }

    private static OutlineColor riskOutlineColor(HoveredArrow hoveredArrow) {
        return outlineColorFor(removalChance(hoveredArrow));
    }

    private static LodgedArrowDepth removalDepth(HoveredArrow hoveredArrow) {
        return hoveredArrow.target() == Target.BODY ? hoveredArrow.arrow().depth() : LodgedArrowDepth.LODGED;
    }

    private static double removalChance(LodgedArrowBodyPart bodyPart) {
        return Mth.clamp(LodgedConfig.playerArrowRemovalSuccessChance(bodyPart), 0.0D, 1.0D);
    }

    private static double shieldRemovalChance() {
        return Mth.clamp(LodgedConfig.shieldArrowRemovalSuccessChance(), 0.0D, 1.0D);
    }

    private static double armorRemovalChance() {
        double successChance = Mth.clamp(LodgedConfig.armorArrowRemovalSuccessChance(), 0.0D, 1.0D);
        double breakChance = Mth.clamp(LodgedConfig.armorArrowRemovalBreakChance(), 0.0D, 1.0D);
        return successChance * (1.0D - breakChance);
    }

    private static Component depthTooltip(LodgedArrowDepth depth) {
        return Component.translatable("tooltip.lodged.arrow_depth." + depth.serializedName());
    }

    private static Component bodyPartTooltip(LodgedArrowBodyPart bodyPart) {
        return Component.translatable("tooltip.lodged.body_part." + bodyPart.serializedName()).withStyle(ChatFormatting.GRAY);
    }

    private static Component hitLocationTooltip(HoveredArrow hoveredArrow) {
        if (hoveredArrow.target() == Target.ARMOR) {
            if (hoveredArrow.armorSlot() == EquipmentSlot.BODY) {
                return armorPieceTooltip(EquipmentSlot.BODY).copy().withStyle(ChatFormatting.GRAY);
            }
            return Component.translatable(
                    "tooltip.lodged.body_part_with_armor",
                    Component.translatable("tooltip.lodged.body_part." + hoveredArrow.arrow().bodyPart().serializedName()),
                    armorPieceTooltip(hoveredArrow.armorSlot()))
                    .withStyle(ChatFormatting.GRAY);
        }
        return bodyPartTooltip(hoveredArrow.arrow().bodyPart());
    }

    private static Component armorPieceTooltip(EquipmentSlot slot) {
        String piece = switch (slot) {
            case HEAD -> "helmet";
            case CHEST -> "chestplate";
            case LEGS -> "leggings";
            case FEET -> "boots";
            case BODY -> "horse_armor";
            default -> "armor";
        };
        return Component.translatable("tooltip.lodged.armor_piece." + piece);
    }

    private static int removalAnimationMs(ArrowRemovalResultPayload payload) {
        int baseDuration = payload.target() == Target.BODY
                ? LodgedConfig.arrowDepthRemovalAnimationMs(payload.arrow().depth())
                : LodgedConfig.arrowDepthRemovalAnimationMs(LodgedArrowDepth.LODGED);
        return Math.max(1, (int) Math.round(baseDuration / LodgedConfig.arrowRemovalAnimationSpeedMultiplier()));
    }

    private static OutlineColor outlineColorFor(double chance) {
        if (chance < 0.5D) {
            return OutlineColor.fromRgb(RISK_DANGEROUS_COLOR);
        }
        if (chance < 0.8D) {
            return OutlineColor.fromRgb(RISK_MODERATE_COLOR);
        }
        return OutlineColor.fromRgb(RISK_EASY_COLOR);
    }

    private static void showRemovalSubtitle(Result result) {
        removalSubtitle = switch (result) {
            case TOO_RISKY -> Component.translatable("subtitle.lodged.arrow_removal.too_risky");
            case INVENTORY_FULL -> Component.translatable("subtitle.lodged.arrow_removal.inventory_full");
            default -> Component.translatable("subtitle.lodged.arrow_removal.cant_remove_now");
        };
        removalSubtitle = removalSubtitle.copy().withStyle(ChatFormatting.ITALIC);
        removalSubtitleStartedAtMs = Util.getMillis();
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
        } else if (hoveredZone.target() == Target.SHIELD) {
            hoveredShieldArrowIndex = hoveredZone.index();
            hoveredShieldHand = hoveredZone.hand();
        } else {
            hoveredArmorArrowIndex = hoveredZone.index();
            hoveredArmorSlot = hoveredZone.armorSlot();
        }
    }

    private static void clearHoveredArrows() {
        hoveredArrowIndex = -1;
        hoveredShieldArrowIndex = -1;
        hoveredShieldHand = InteractionHand.MAIN_HAND;
        hoveredArmorArrowIndex = -1;
        hoveredArmorSlot = EquipmentSlot.CHEST;
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

    private record ArrowHitZone(
            int index,
            Target target,
            InteractionHand hand,
            EquipmentSlot armorSlot,
            float centerX,
            float centerY,
            int radius) {
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

    private record HoveredArrow(LodgedArrowVisual arrow, Target target, InteractionHand hand, EquipmentSlot armorSlot) {
    }

    private record RemovalAnimation(ArrowRemovalResultPayload payload, long startedAtMs) {
    }

    public record OutlineColor(int red, int green, int blue, int alpha) {
        private static OutlineColor fromRgb(int rgb) {
            return new OutlineColor((rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, rgb & 0xFF, RISK_OUTLINE_ALPHA);
        }

        private int rgb() {
            return 0xFF000000 | (red << 16) | (green << 8) | blue;
        }
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
