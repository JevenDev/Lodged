package com.jvn.lodged.client;

import com.jvn.lodged.Lodged;
import com.jvn.lodged.config.LodgedConfig;
import com.mojang.blaze3d.systems.RenderSystem;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import org.joml.Quaternionf;
import org.joml.Vector3f;

final class InventoryPreviewRenderer {
    private static final int INVENTORY_TURN_TEXTURE_WIDTH = 17;
    private static final int INVENTORY_TURN_TEXTURE_HEIGHT = 4;
    private static final int INVENTORY_TURN_BOTTOM_PADDING = 2;
    private static final int INVENTORY_TURN_Z = 350;
    private static final ResourceLocation INVENTORY_TURN_TEXTURE =
            Lodged.IDS.texture("gui/inventory_turn.png");
    private static final List<Component> INVENTORY_TURN_TOOLTIP = List.of(
            Component.translatable("tooltip.lodged.inventory_turn.title").withStyle(ChatFormatting.YELLOW),
            Component.translatable("tooltip.lodged.inventory_turn.rotate"),
            Component.translatable("tooltip.lodged.inventory_turn.reset"),
            Component.translatable("tooltip.lodged.inventory_turn.remove_arrows"));

    private InventoryPreviewRenderer() {
    }

    static void renderInventoryPlayer(
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
            InventoryBloodRenderer.renderInventoryBlood(guiGraphics, x1, y1, x2, y2, mouseX, mouseY, entity);
            return;
        }

        if (InventoryPreviewProjection.customYawActive()) {
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

        InventoryBloodRenderer.renderInventoryBlood(guiGraphics, x1, y1, x2, y2, mouseX, mouseY, entity);
        if (LodgedConfig.showInventoryTurnHint()) {
            renderInventoryTurnHint(guiGraphics, x1, y1, x2, y2);
        }
    }

    static void renderInventoryTurnHint(GuiGraphics guiGraphics, int x1, int y1, int x2, int y2) {
        InventoryTurnHintBounds bounds = inventoryTurnHintBounds(x1, x2, y2);
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

    static boolean isInInventoryTurnHint(
            int previewLeft,
            int previewRight,
            int previewBottom,
            double mouseX,
            double mouseY) {
        InventoryTurnHintBounds bounds = inventoryTurnHintBounds(previewLeft, previewRight, previewBottom);
        return mouseX >= bounds.x()
                && mouseX < bounds.x() + INVENTORY_TURN_TEXTURE_WIDTH
                && mouseY >= bounds.y()
                && mouseY < bounds.y() + INVENTORY_TURN_TEXTURE_HEIGHT;
    }

    static void renderInventoryTurnTooltip(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.renderComponentTooltip(Minecraft.getInstance().font, INVENTORY_TURN_TOOLTIP, mouseX, mouseY);
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

        float renderYaw = 180.0F + InventoryPreviewProjection.yawDegrees();
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

    static boolean isInInventoryTurnHint(AbstractContainerScreen<?> screen, double mouseX, double mouseY) {
        return isInInventoryTurnHint(
                screen.getGuiLeft() + InventoryPreviewProjection.MODEL_LEFT,
                screen.getGuiLeft() + InventoryPreviewProjection.MODEL_RIGHT,
                screen.getGuiTop() + InventoryPreviewProjection.MODEL_BOTTOM,
                mouseX,
                mouseY);
    }

    private static InventoryTurnHintBounds inventoryTurnHintBounds(int previewLeft, int previewRight, int previewBottom) {
        int x = previewLeft + ((previewRight - previewLeft - INVENTORY_TURN_TEXTURE_WIDTH) / 2);
        int y = previewBottom - INVENTORY_TURN_TEXTURE_HEIGHT - INVENTORY_TURN_BOTTOM_PADDING;
        return new InventoryTurnHintBounds(x, y);
    }

    private record InventoryTurnHintBounds(int x, int y) {
    }
}
