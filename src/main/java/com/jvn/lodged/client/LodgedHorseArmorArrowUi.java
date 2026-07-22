package com.jvn.lodged.client;

import com.jvn.lodged.Lodged;
import com.jvn.lodged.config.LodgedConfig;
import com.jvn.lodged.mixin.client.HorseInventoryScreenAccessor;
import com.jvn.lodged.network.payload.RemoveHorseArmorArrowPayload;
import com.jvn.lodged.world.LodgedArmorArrowStorage;
import com.jvn.lodged.world.LodgedArrowVisual;
import java.util.List;
import net.minecraft.client.gui.screens.inventory.HorseInventoryScreen;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.joml.Matrix4f;
import org.joml.Vector3f;

@EventBusSubscriber(modid = Lodged.MOD_ID, value = Dist.CLIENT)
public final class LodgedHorseArmorArrowUi {
    private static final int PREVIEW_LEFT = 26;
    private static final int PREVIEW_TOP = 18;
    private static final int PREVIEW_RIGHT = 78;
    private static final int PREVIEW_BOTTOM = 70;
    private static final int PREVIEW_SCALE = 17;
    private static final float PREVIEW_Y_OFFSET = 0.25F;
    private static final float HORSE_RENDER_SCALE = 1.1F;
    private static final float MODEL_RENDER_Y_OFFSET = -1.501F;
    private static final float ROTATION_MULTIPLIER_DEGREES = 20.0F;
    private static final int HIT_ZONE_RADIUS = 9;

    private static int hoveredHorseId = -1;
    private static int hoveredArrowIndex = -1;

    private LodgedHorseArmorArrowUi() {
    }

    @SubscribeEvent
    public static void onScreenRenderPre(ScreenEvent.Render.Pre event) {
        if (!LodgedConfig.enablePlayerArrowRemoval()
                || !(event.getScreen() instanceof HorseInventoryScreen screen)) {
            clearHover();
            return;
        }

        AbstractHorse horse = horse(screen);
        if (isMouseOverSlot(screen, event.getMouseX(), event.getMouseY())) {
            clearHover();
            return;
        }

        hoveredHorseId = horse.getId();
        hoveredArrowIndex = findHoveredArrow(
                screen,
                horse,
                event.getMouseX(),
                event.getMouseY());
    }

    @SubscribeEvent
    public static void onScreenRenderPost(ScreenEvent.Render.Post event) {
        if (!(event.getScreen() instanceof HorseInventoryScreen screen)) {
            return;
        }

        AbstractHorse horse = horse(screen);
        LodgedArrowVisual arrow = hoveredArrow(horse);
        if (arrow != null) {
            LodgedInventoryArrowUi.renderHorseArmorRemovalTooltip(
                    event.getGuiGraphics(),
                    arrow,
                    event.getMouseX(),
                    event.getMouseY());
        }
    }

    @SubscribeEvent
    public static void onMouseClicked(ScreenEvent.MouseButtonPressed.Pre event) {
        if (!LodgedConfig.enablePlayerArrowRemoval()
                || event.getButton() != 0
                || !(event.getScreen() instanceof HorseInventoryScreen screen)
                || isMouseOverSlot(screen, event.getMouseX(), event.getMouseY())) {
            return;
        }

        AbstractHorse horse = horse(screen);
        int arrowIndex = findHoveredArrow(screen, horse, event.getMouseX(), event.getMouseY());
        if (arrowIndex < 0) {
            return;
        }

        PacketDistributor.sendToServer(new RemoveHorseArmorArrowPayload(horse.getId(), arrowIndex));
        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onScreenClosing(ScreenEvent.Closing event) {
        if (event.getScreen() instanceof HorseInventoryScreen) {
            clearHover();
        }
    }

    public static boolean isHovered(AbstractHorse horse, LodgedArrowVisual arrow) {
        LodgedArrowVisual hovered = hoveredArrow(horse);
        return hovered != null && hovered.matches(arrow);
    }

    private static int findHoveredArrow(
            HorseInventoryScreen screen,
            AbstractHorse horse,
            double mouseX,
            double mouseY) {
        if (!isInPreview(screen, mouseX, mouseY)) {
            return -1;
        }

        ItemStack armor = horse.getItemBySlot(EquipmentSlot.BODY);
        if (!LodgedArmorArrowStorage.isHorseArmor(armor)) {
            return -1;
        }

        List<LodgedArrowVisual> arrows = LodgedArmorArrowStorage.readAll(armor);
        int arrowCount = Math.min(arrows.size(), LodgedConfig.maxTrackedArrowsPerArmorPiece());
        int closestIndex = -1;
        double closestDistance = Double.MAX_VALUE;
        for (int index = 0; index < arrowCount; index++) {
            Vector3f projected = projectArrow(screen, horse, arrows.get(index), mouseX, mouseY);
            double deltaX = mouseX - projected.x();
            double deltaY = mouseY - projected.y();
            double distance = deltaX * deltaX + deltaY * deltaY;
            if (distance <= HIT_ZONE_RADIUS * HIT_ZONE_RADIUS && distance < closestDistance) {
                closestIndex = index;
                closestDistance = distance;
            }
        }
        return closestIndex;
    }

    private static Vector3f projectArrow(
            HorseInventoryScreen screen,
            AbstractHorse horse,
            LodgedArrowVisual arrow,
            double mouseX,
            double mouseY) {
        float centerX = screen.getGuiLeft() + (PREVIEW_LEFT + PREVIEW_RIGHT) / 2.0F;
        float centerY = screen.getGuiTop() + (PREVIEW_TOP + PREVIEW_BOTTOM) / 2.0F;
        float yawComponent = (float) Math.atan((centerX - mouseX) / 40.0F);
        float pitchComponent = (float) Math.atan((centerY - mouseY) / 40.0F);
        float entityScale = horse.getScale();
        float previewScale = PREVIEW_SCALE / entityScale;
        float degreesToRadians = (float) (Math.PI / 180.0D);

        return new Matrix4f()
                .translation(centerX, centerY, 50.0F)
                .scale(previewScale, previewScale, -previewScale)
                .translate(0.0F, horse.getBbHeight() / 2.0F + PREVIEW_Y_OFFSET * entityScale, 0.0F)
                .rotateZ((float) Math.PI)
                .rotateX(pitchComponent * ROTATION_MULTIPLIER_DEGREES * degreesToRadians)
                .scale(entityScale, entityScale, entityScale)
                .rotateY(-yawComponent * ROTATION_MULTIPLIER_DEGREES * degreesToRadians)
                .scale(-1.0F, -1.0F, 1.0F)
                .scale(HORSE_RENDER_SCALE, HORSE_RENDER_SCALE, HORSE_RENDER_SCALE)
                .translate(0.0F, MODEL_RENDER_Y_OFFSET, 0.0F)
                .transformPosition(arrow.modelX(), arrow.modelY(), arrow.modelZ(), new Vector3f());
    }

    private static LodgedArrowVisual hoveredArrow(AbstractHorse horse) {
        if (hoveredArrowIndex < 0 || horse.getId() != hoveredHorseId) {
            return null;
        }

        ItemStack armor = horse.getItemBySlot(EquipmentSlot.BODY);
        List<LodgedArrowVisual> arrows = LodgedArmorArrowStorage.readAll(armor);
        int arrowCount = Math.min(arrows.size(), LodgedConfig.maxTrackedArrowsPerArmorPiece());
        return hoveredArrowIndex < arrowCount ? arrows.get(hoveredArrowIndex) : null;
    }

    private static boolean isInPreview(HorseInventoryScreen screen, double mouseX, double mouseY) {
        int left = screen.getGuiLeft() + PREVIEW_LEFT;
        int top = screen.getGuiTop() + PREVIEW_TOP;
        return mouseX >= left
                && mouseX < screen.getGuiLeft() + PREVIEW_RIGHT
                && mouseY >= top
                && mouseY < screen.getGuiTop() + PREVIEW_BOTTOM;
    }

    private static boolean isMouseOverSlot(HorseInventoryScreen screen, double mouseX, double mouseY) {
        int relativeX = (int) mouseX - screen.getGuiLeft();
        int relativeY = (int) mouseY - screen.getGuiTop();
        for (Slot slot : screen.getMenu().slots) {
            if (slot.isActive()
                    && relativeX >= slot.x
                    && relativeX < slot.x + 16
                    && relativeY >= slot.y
                    && relativeY < slot.y + 16) {
                return true;
            }
        }
        return false;
    }

    private static AbstractHorse horse(HorseInventoryScreen screen) {
        return ((HorseInventoryScreenAccessor) screen).lodged$getHorse();
    }

    private static void clearHover() {
        hoveredHorseId = -1;
        hoveredArrowIndex = -1;
    }
}
