package com.jvn.lodged.client;

import com.jvn.lodged.client.ScreenPolygon.ScreenPoint;
import com.jvn.lodged.config.LodgedConfig;
import com.jvn.lodged.network.ClientArrowState;
import com.jvn.lodged.network.payload.RemovePlayerArrowPayload.Target;
import com.jvn.lodged.world.LodgedArmorArrowStorage;
import com.jvn.lodged.world.LodgedArrowVisual;
import com.jvn.lodged.world.LodgedShieldArrowStorage;
import java.util.List;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.ItemAbilities;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

final class InventoryArrowHitTesting {
    private static final int HIT_ZONE_RADIUS = 12;
    private static final int PLAYER_MODEL_HIT_MARGIN = 3;
    private static final float MODEL_HEAD_TOP = -8.0F / 16.0F;
    private static final float MODEL_HEAD_BOTTOM = 0.0F;
    private static final float MODEL_LEG_TOP = 12.0F / 16.0F;
    private static final float MODEL_FEET_Y = 24.0F / 16.0F;
    private static final float MODEL_HEAD_HALF_WIDTH = 4.0F / 16.0F;
    private static final float MODEL_BODY_HALF_WIDTH = 4.0F / 16.0F;
    private static final float MODEL_ARM_OUTER_X = 8.0F / 16.0F;
    private static final float MODEL_LIMB_HALF_DEPTH = 2.0F / 16.0F;
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

    private InventoryArrowHitTesting() {
    }

    static ArrowHitZone findHoveredZone(AbstractContainerScreen<?> screen, LocalPlayer player, double mouseX, double mouseY) {
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
            AbstractContainerScreen<?> screen,
            LocalPlayer player,
            LodgedArrowVisual arrow,
            int index,
            double mouseX,
            double mouseY) {
        Vector3f projected = InventoryPreviewProjection.projectArrowToScreen(screen, player, arrow, mouseX, mouseY);
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
            AbstractContainerScreen<?> screen,
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
            Vector3f projected = InventoryPreviewProjection.projectShieldArrowToScreen(screen, player, arrows.get(index), hand, mouseX, mouseY);
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
            AbstractContainerScreen<?> screen,
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
                Vector3f projected = InventoryPreviewProjection.projectArrowToScreen(screen, player, arrows.get(index), mouseX, mouseY);
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

    static boolean isOnRenderedPlayerModel(AbstractContainerScreen<?> screen, LocalPlayer player, double mouseX, double mouseY) {
        int left = screen.getGuiLeft();
        int top = screen.getGuiTop();
        float centerX = left + ((InventoryPreviewProjection.MODEL_LEFT + InventoryPreviewProjection.MODEL_RIGHT) / 2.0F);
        float centerY = top + ((InventoryPreviewProjection.MODEL_TOP + InventoryPreviewProjection.MODEL_BOTTOM) / 2.0F);
        for (ModelHitBox box : PLAYER_MODEL_HIT_BOXES) {
            if (box.containsScreenPoint(player, centerX, centerY, mouseX, mouseY)) {
                return true;
            }
        }
        return false;
    }

    record ArrowHitZone(
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

    private record ShieldZoneResult(ArrowHitZone zone, double distanceToSqr) {
        static final ShieldZoneResult EMPTY = new ShieldZoneResult(null, Double.MAX_VALUE);
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

            ScreenPoint[] hull = ScreenPolygon.convexHull(corners);
            return ScreenPolygon.isInsideOrNearPolygon(hull, mouseX, mouseY, PLAYER_MODEL_HIT_MARGIN);
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
                                InventoryPreviewProjection.currentHeadYawRadians(centerX, mouseX),
                                InventoryPreviewProjection.currentHeadPitchRadians(centerY, mouseY)))
                        .transformPosition(point);
            }

            Vector3f projected = InventoryPreviewProjection.projectModelToScreen(player, point, centerX, centerY, mouseX, mouseY);
            return new ScreenPoint(projected.x(), projected.y());
        }
    }
}
