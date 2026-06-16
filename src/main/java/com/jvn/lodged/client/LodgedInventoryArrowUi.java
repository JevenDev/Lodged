package com.jvn.lodged.client;

import com.jvn.lodged.Lodged;
import com.jvn.lodged.config.LodgedConfig;
import com.jvn.lodged.network.ClientArrowState;
import com.jvn.lodged.network.payload.RemovePlayerArrowPayload;
import com.jvn.lodged.world.LodgedArrowVisual;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ContainerScreenEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.joml.Quaternionf;
import org.joml.Vector3f;

@EventBusSubscriber(modid = Lodged.MOD_ID, value = Dist.CLIENT)
public final class LodgedInventoryArrowUi {
    private static final int MODEL_LEFT = 26;
    private static final int MODEL_TOP = 8;
    private static final int MODEL_RIGHT = 75;
    private static final int MODEL_BOTTOM = 78;
    private static final int HIT_ZONE_SIZE = 8;
    private static final double MODEL_PIXEL_SCALE = 30.0D;
    private static final double MODEL_SCREEN_TOP_OFFSET = 16.0D;

    private static float yawDegrees;
    private static boolean draggingPreview;
    private static boolean customYawActive;

    private LodgedInventoryArrowUi() {
    }

    @SubscribeEvent
    public static void onContainerBackgroundRender(ContainerScreenEvent.Render.Background event) {
        if (!LodgedConfig.enablePlayerArrowRemoval() || !(event.getContainerScreen() instanceof InventoryScreen screen)) {
            return;
        }

        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }

        GuiGraphics guiGraphics = event.getGuiGraphics();
        ArrowHitZone hoveredZone = findHoveredZone(screen, player, event.getMouseX(), event.getMouseY());
        if (hoveredZone != null) {
            renderHover(guiGraphics, hoveredZone);
        }
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
        int arrowCount = Math.min(arrows.size(), player.getArrowCount());
        arrowCount = Math.min(arrowCount, LodgedConfig.maxRemovablePlayerArrows());
        for (int index = 0; index < arrowCount; index++) {
            ArrowHitZone zone = zoneFor(screen, arrows.get(index), index);
            if (zone.contains(mouseX, mouseY)) {
                return zone;
            }
        }
        return null;
    }

    private static ArrowHitZone zoneFor(InventoryScreen screen, LodgedArrowVisual arrow, int index) {
        int left = screen.getGuiLeft();
        int top = screen.getGuiTop();
        int centerX = left + ((MODEL_LEFT + MODEL_RIGHT) / 2);
        double yawRadians = Math.toRadians(yawDegrees);
        double screenOffsetX = ((double) arrow.modelX() * Math.cos(yawRadians) - (double) arrow.modelZ() * Math.sin(yawRadians)) * MODEL_PIXEL_SCALE;
        int x = centerX + (int) Math.round(screenOffsetX) - (HIT_ZONE_SIZE / 2);
        int y = top + MODEL_TOP + (int) Math.round(MODEL_SCREEN_TOP_OFFSET + arrow.modelY() * MODEL_PIXEL_SCALE) - (HIT_ZONE_SIZE / 2);
        return new ArrowHitZone(index, x, y, HIT_ZONE_SIZE);
    }

    private static void renderHover(GuiGraphics guiGraphics, ArrowHitZone zone) {
        guiGraphics.fill(zone.x(), zone.y(), zone.x() + zone.size(), zone.y() + zone.size(), 0x55FFF1A8);
        guiGraphics.renderOutline(zone.x(), zone.y(), zone.size(), zone.size(), 0xFFFFF1A8);
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
    }

    private record ArrowHitZone(int index, int x, int y, int size) {
        boolean contains(double mouseX, double mouseY) {
            return mouseX >= x && mouseX < x + size && mouseY >= y && mouseY < y + size;
        }
    }
}
