package com.jvn.lodged.client;

import com.jvn.lodged.Lodged;
import com.jvn.lodged.client.InventoryArrowHitTesting.ArrowHitZone;
import com.jvn.lodged.client.InventoryArrowTooltips.HoveredArrow;
import com.jvn.lodged.compat.PlayerInventoryMenus;
import com.jvn.lodged.config.LodgedConfig;
import com.jvn.lodged.network.ClientArrowState;
import com.jvn.lodged.network.payload.ArrowRemovalResultPayload;
import com.jvn.lodged.network.payload.RemovePlayerArrowPayload.Target;
import com.jvn.lodged.network.payload.RemovePlayerArrowPayload;
import com.jvn.lodged.world.LodgedArmorArrowStorage;
import com.jvn.lodged.world.LodgedArrowVisual;
import com.jvn.lodged.world.LodgedShieldArrowStorage;
import com.jvn.toucanlib.neoforge.network.ToucanNetwork;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.common.ItemAbilities;

@EventBusSubscriber(modid = Lodged.MOD_ID, value = Dist.CLIENT)
public final class LodgedInventoryArrowUi {
    private static final float PREVIEW_SCROLL_YAW_DEGREES = 12.0F;
    private static final int RISK_OUTLINE_ALPHA = 255;
    private static boolean draggingPreview;
    private static int hoveredArrowIndex = -1;
    private static int hoveredShieldArrowIndex = -1;
    private static InteractionHand hoveredShieldHand = InteractionHand.MAIN_HAND;
    private static int hoveredArmorArrowIndex = -1;
    private static EquipmentSlot hoveredArmorSlot = EquipmentSlot.CHEST;
    private static Screen supportedBackpackScreen;
    private static boolean backpackPreviewRendered;
    private static boolean warnedMissingBackpackPreview;

    private LodgedInventoryArrowUi() {
    }

    @SubscribeEvent
    public static void onScreenRenderPre(ScreenEvent.Render.Pre event) {
        backpackPreviewRendered = false;
        if (!LodgedConfig.enablePlayerArrowRemoval()
                || !(event.getScreen() instanceof AbstractContainerScreen<?> screen)
                || !isPlayerInventoryScreen(screen)) {
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

        ArrowHitZone hoveredZone = InventoryArrowHitTesting.findHoveredZone(screen, player, event.getMouseX(), event.getMouseY());
        setHoveredArrow(hoveredZone);
    }

    @SubscribeEvent
    public static void onScreenRenderPost(ScreenEvent.Render.Post event) {
        if (event.getScreen() instanceof AbstractContainerScreen<?> containerScreen
                && PlayerInventoryMenus.isModularBackpack(containerScreen.getMenu())
                && !backpackPreviewRendered) {
            supportedBackpackScreen = null;
            clearHoveredArrows();
            resetPreviewRotation();
            if (!warnedMissingBackpackPreview) {
                warnedMissingBackpackPreview = true;
                Lodged.LOGGER.warn("Disabled Lodged's Modular Backpacks preview controls because its render hook did not run");
            }
            return;
        }
        if (!(event.getScreen() instanceof AbstractContainerScreen<?> screen) || !isPlayerInventoryScreen(screen)) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player != null) {
            InventoryRemovalAnimations.renderRemovalAnimations(event.getGuiGraphics(), screen, player, event.getMouseX(), event.getMouseY());
            InventoryRemovalAnimations.renderRemovalSubtitle(event.getGuiGraphics(), screen);
        }

        if (LodgedConfig.enablePlayerArrowRemoval() && player != null) {
            HoveredArrow hoveredArrow = hoveredArrow(player);
            if (hoveredArrow != null) {
                InventoryArrowTooltips.renderArrowRemovalTooltip(event.getGuiGraphics(), hoveredArrow, event.getMouseX(), event.getMouseY());
                return;
            }
        }

        if (!LodgedConfig.enablePlayerArrowRemoval()
                || !LodgedConfig.showInventoryTurnHint()
                || !LodgedConfig.showInventoryTurnHintTooltip()
                || !InventoryPreviewRenderer.isInInventoryTurnHint(screen, event.getMouseX(), event.getMouseY())) {
            return;
        }

        InventoryPreviewRenderer.renderInventoryTurnTooltip(
                event.getGuiGraphics(), event.getMouseX(), event.getMouseY());
    }

    @SubscribeEvent
    public static void onMouseClicked(ScreenEvent.MouseButtonPressed.Pre event) {
        if (!LodgedConfig.enablePlayerArrowRemoval()
                || !(event.getScreen() instanceof AbstractContainerScreen<?> screen)
                || !isPlayerInventoryScreen(screen)) {
            return;
        }

        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }

        if (isMouseOverScreenWidget(screen, event.getMouseX(), event.getMouseY())) {
            return;
        }

        if (event.getButton() == 2 && InventoryArrowHitTesting.isOnRenderedPlayerModel(screen, player, event.getMouseX(), event.getMouseY())) {
            resetPreviewRotation();
            event.setCanceled(true);
            return;
        }

        if (event.getButton() != 0) {
            return;
        }

        ArrowHitZone hoveredZone = InventoryArrowHitTesting.findHoveredZone(screen, player, event.getMouseX(), event.getMouseY());
        if (hoveredZone != null) {
            ToucanNetwork.sendToServer(new RemovePlayerArrowPayload(
                    hoveredZone.index(),
                    hoveredZone.target(),
                    hoveredZone.hand(),
                    hoveredZone.armorSlot()));
            event.setCanceled(true);
            return;
        }

        if (InventoryArrowHitTesting.isOnRenderedPlayerModel(screen, player, event.getMouseX(), event.getMouseY())) {
            draggingPreview = true;
            InventoryPreviewProjection.enableCustomYaw();
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onMouseDragged(ScreenEvent.MouseDragged.Pre event) {
        if (!draggingPreview || event.getMouseButton() != 0 || !isPlayerInventoryScreen(event.getScreen())) {
            return;
        }

        InventoryPreviewProjection.rotate(-(float) event.getDragX() * 2.0F);
        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onMouseScrolled(ScreenEvent.MouseScrolled.Pre event) {
        if (!LodgedConfig.enablePlayerArrowRemoval()
                || !(event.getScreen() instanceof AbstractContainerScreen<?> screen)
                || !isPlayerInventoryScreen(screen)) {
            return;
        }

        if (isMouseOverScreenWidget(screen, event.getMouseX(), event.getMouseY())
                || !InventoryPreviewProjection.isInPlayerPreviewArea(screen, event.getMouseX(), event.getMouseY())) {
            return;
        }

        double scrollDelta = Math.abs(event.getScrollDeltaX()) > Math.abs(event.getScrollDeltaY())
                ? event.getScrollDeltaX()
                : event.getScrollDeltaY();
        if (scrollDelta == 0.0D) {
            return;
        }

        InventoryPreviewProjection.rotate(-(float) scrollDelta * PREVIEW_SCROLL_YAW_DEGREES);
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
        if (isPlayerInventoryScreen(event.getScreen())) {
            resetPreviewRotation();
        }
        if (event.getScreen() == supportedBackpackScreen) {
            supportedBackpackScreen = null;
            clearHoveredArrows();
        }
    }

    public static void onBackpackPreviewRendered() {
        backpackPreviewRendered = true;
        supportedBackpackScreen = Minecraft.getInstance().screen;
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
        return InventoryArrowTooltips.riskOutlineColor(arrow);
    }

    public static OutlineColor shieldRiskOutlineColor() {
        return InventoryArrowTooltips.shieldRiskOutlineColor();
    }

    public static OutlineColor armorArrowOutlineColor() {
        return InventoryArrowTooltips.armorArrowOutlineColor();
    }

    public static OutlineColor tamedMobRiskOutlineColor(LodgedArrowVisual arrow) {
        return InventoryArrowTooltips.tamedMobRiskOutlineColor(arrow);
    }

    public static void renderHorseArmorRemovalTooltip(
            GuiGraphics guiGraphics,
            LodgedArrowVisual arrow,
            int mouseX,
            int mouseY) {
        InventoryArrowTooltips.renderHorseArmorRemovalTooltip(guiGraphics, arrow, mouseX, mouseY);
    }

    public static void renderHorseBodyRemovalTooltip(
            GuiGraphics guiGraphics,
            LodgedArrowVisual arrow,
            int mouseX,
            int mouseY) {
        InventoryArrowTooltips.renderHorseBodyRemovalTooltip(guiGraphics, arrow, mouseX, mouseY);
    }

    public static void handleRemovalResult(ArrowRemovalResultPayload payload) {
        InventoryRemovalAnimations.handleRemovalResult(payload);
    }

    public static boolean prepareArrowOutlineTarget() {
        return InventoryArrowOutline.prepareArrowOutlineTarget();
    }

    public static void processArrowOutlineTarget() {
        InventoryArrowOutline.processArrowOutlineTarget();
    }

    public static void restoreVanillaEntityTarget() {
        InventoryArrowOutline.restoreVanillaEntityTarget();
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
        InventoryPreviewRenderer.renderInventoryPlayer(guiGraphics, x1, y1, x2, y2, scale, size, mouseX, mouseY, entity);
    }

    public static void renderInventoryTurnHint(GuiGraphics guiGraphics, int x1, int y1, int x2, int y2) {
        InventoryPreviewRenderer.renderInventoryTurnHint(guiGraphics, x1, y1, x2, y2);
    }

    public static boolean isInInventoryTurnHint(
            int previewLeft,
            int previewRight,
            int previewBottom,
            double mouseX,
            double mouseY) {
        return InventoryPreviewRenderer.isInInventoryTurnHint(previewLeft, previewRight, previewBottom, mouseX, mouseY);
    }

    public static void renderInventoryTurnTooltip(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        InventoryPreviewRenderer.renderInventoryTurnTooltip(guiGraphics, mouseX, mouseY);
    }

    private static boolean isPlayerInventoryScreen(Screen screen) {
        return screen instanceof InventoryScreen
                || (screen == supportedBackpackScreen
                        && screen instanceof AbstractContainerScreen<?> containerScreen
                        && PlayerInventoryMenus.isModularBackpack(containerScreen.getMenu()));
    }

    private static boolean isMouseOverScreenWidget(AbstractContainerScreen<?> screen, double mouseX, double mouseY) {
        for (GuiEventListener child : screen.children()) {
            if (child.isMouseOver(mouseX, mouseY)) {
                return true;
            }
        }
        return false;
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

    private static void resetPreviewRotation() {
        InventoryPreviewProjection.resetRotation();
        draggingPreview = false;
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

    public record OutlineColor(int red, int green, int blue, int alpha) {
        static OutlineColor fromRgb(int rgb) {
            return new OutlineColor((rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, rgb & 0xFF, RISK_OUTLINE_ALPHA);
        }

        int rgb() {
            return 0xFF000000 | (red << 16) | (green << 8) | blue;
        }
    }
}
