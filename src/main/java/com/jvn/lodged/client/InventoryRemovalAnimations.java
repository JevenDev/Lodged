package com.jvn.lodged.client;

import com.jvn.lodged.config.LodgedConfig;
import com.jvn.lodged.effect.BloodColors;
import com.jvn.lodged.network.payload.ArrowRemovalResultPayload.Result;
import com.jvn.lodged.network.payload.ArrowRemovalResultPayload;
import com.jvn.lodged.network.payload.RemovePlayerArrowPayload.Target;
import com.jvn.lodged.world.LodgedArrowDepth;
import com.jvn.toucanlib.client.ToucanEasing;
import com.mojang.blaze3d.systems.RenderSystem;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.OptionalInt;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.inventory.Slot;
import org.joml.Vector3f;

final class InventoryRemovalAnimations {
    private static final int REMOVAL_SUBTITLE_Z = 450;
    private static final int REMOVAL_ANIMATION_Z = 420;
    private static final int MAX_REMOVAL_ANIMATIONS = 6;
    private static final int REMOVAL_SUBTITLE_MS = 1400;
    private static final int REMOVAL_SUBTITLE_FADE_MS = 350;
    private static final int REMOVAL_SUBTITLE_COLOR = 0xFFD8D8D8;
    private static final List<RemovalAnimation> removalAnimations = new ArrayList<>();
    private static Component removalSubtitle;
    private static long removalSubtitleStartedAtMs;

    private InventoryRemovalAnimations() {
    }

    static void handleRemovalResult(ArrowRemovalResultPayload payload) {
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

    static void renderRemovalAnimations(
            GuiGraphics guiGraphics,
            AbstractContainerScreen<?> screen,
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
            AbstractContainerScreen<?> screen,
            LocalPlayer player,
            ArrowRemovalResultPayload payload,
            int mouseX,
            int mouseY,
            float progress) {
        Vector3f source = removalAnimationSource(screen, player, payload, mouseX, mouseY);
        Vector3f destination = removalAnimationDestination(screen, payload);
        float eased = ToucanEasing.easeOutCubic(progress);
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
            AbstractContainerScreen<?> screen,
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
            InventoryBloodRenderer.drawInventoryBloodDrop(
                    guiGraphics,
                    source.x(),
                    source.y(),
                    progress * (InventoryBloodRenderer.GUI_BLOOD_HANG_TICKS + InventoryBloodRenderer.GUI_BLOOD_FALL_TICKS + InventoryBloodRenderer.GUI_BLOOD_LAND_TICKS),
                    97);
            guiGraphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
            RenderSystem.disableBlend();
        }
    }

    private static Vector3f removalAnimationSource(
            AbstractContainerScreen<?> screen,
            LocalPlayer player,
            ArrowRemovalResultPayload payload,
            int mouseX,
            int mouseY) {
        return payload.target() == Target.SHIELD
                ? InventoryPreviewProjection.projectShieldArrowToScreen(screen, player, payload.arrow(), payload.hand(), mouseX, mouseY)
                : InventoryPreviewProjection.projectArrowToScreen(screen, player, payload.arrow(), mouseX, mouseY);
    }

    private static Vector3f removalAnimationDestination(AbstractContainerScreen<?> screen, ArrowRemovalResultPayload payload) {
        Slot slot = inventorySlot(screen, payload.inventorySlot());
        if (slot != null) {
            return new Vector3f(
                    screen.getGuiLeft() + slot.x + 8.0F,
                    screen.getGuiTop() + slot.y + 8.0F,
                    0.0F);
        }

        return new Vector3f(screen.getGuiLeft() + 150.0F, screen.getGuiTop() + 71.0F, 0.0F);
    }

    private static Slot inventorySlot(AbstractContainerScreen<?> screen, int inventorySlot) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || inventorySlot < 0) {
            return null;
        }
        OptionalInt menuSlot = screen.getMenu().findSlot(player.getInventory(), inventorySlot);
        return menuSlot.isPresent() ? screen.getMenu().getSlot(menuSlot.getAsInt()) : null;
    }

    static void renderRemovalSubtitle(GuiGraphics guiGraphics, AbstractContainerScreen<?> screen) {
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
        int x = screen.getGuiLeft() + ((InventoryPreviewProjection.MODEL_LEFT + InventoryPreviewProjection.MODEL_RIGHT) / 2) - (font.width(removalSubtitle) / 2);
        int y = screen.getGuiTop() + InventoryPreviewProjection.MODEL_BOTTOM + 6;
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(0.0F, 0.0F, REMOVAL_SUBTITLE_Z);
        guiGraphics.drawString(font, removalSubtitle, x, y, (alpha << 24) | (REMOVAL_SUBTITLE_COLOR & 0xFFFFFF), false);
        guiGraphics.pose().popPose();
    }

    private static int removalAnimationMs(ArrowRemovalResultPayload payload) {
        int baseDuration = payload.target() == Target.BODY
                ? LodgedConfig.arrowDepthRemovalAnimationMs(payload.arrow().depth())
                : LodgedConfig.arrowDepthRemovalAnimationMs(LodgedArrowDepth.LODGED);
        return Math.max(1, (int) Math.round(baseDuration / LodgedConfig.arrowRemovalAnimationSpeedMultiplier()));
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

    private record RemovalAnimation(ArrowRemovalResultPayload payload, long startedAtMs) {
    }
}
