package com.jvn.lodged.client;

import com.jvn.lodged.config.LodgedConfig;
import com.jvn.lodged.world.LodgedArrowVisual;
import com.jvn.lodged.world.LodgedShieldArrowStorage;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.OutlineBufferSource;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

public final class LodgedShieldArrowRenderer {
    private static final int SHIELD_HOVER_HIGHLIGHT_RED = 255;
    private static final int SHIELD_HOVER_HIGHLIGHT_GREEN = 255;
    private static final int SHIELD_HOVER_HIGHLIGHT_BLUE = 255;
    private static final int SHIELD_HOVER_HIGHLIGHT_ALPHA = 255;
    private static final int HOVER_OUTLINE_BUFFER_SIZE = 1536;

    private LodgedShieldArrowRenderer() {
    }

    public static void render(
            ItemStack shield,
            ItemDisplayContext displayContext,
            PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight,
            float partialTicks) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null || !shouldRenderOwnFirstPersonShieldArrows(minecraft, shield, displayContext)) {
            return;
        }

        List<LodgedShieldArrowStorage.LodgedShieldArrowData> arrows = LodgedShieldArrowStorage.readData(
                shield,
                level.registryAccess());
        if (arrows.isEmpty()) {
            return;
        }

        EntityRenderDispatcher dispatcher = minecraft.getEntityRenderDispatcher();
        int hoveredArrowIndex = isHandRender(displayContext) ? LodgedInventoryArrowUi.hoveredShieldArrowIndex(shield) : -1;
        int arrowCount = Math.min(arrows.size(), LodgedConfig.maxTrackedArrowsPerShield());
        for (int index = 0; index < arrowCount; index++) {
            LodgedArrowVisual arrow = arrows.get(index).visual();
            poseStack.pushPose();
            poseStack.scale(1.0F, -1.0F, -1.0F);
            poseStack.translate(arrow.modelX(), arrow.modelY(), arrow.modelZ());
            renderArrow(dispatcher, level, poseStack, buffer, packedLight, arrow, partialTicks);
            if (index == hoveredArrowIndex) {
                renderHoverOutline(dispatcher, level, poseStack, packedLight, arrow, partialTicks);
            }
            poseStack.popPose();
        }
    }

    private static boolean isHandRender(ItemDisplayContext displayContext) {
        return displayContext == ItemDisplayContext.THIRD_PERSON_LEFT_HAND
                || displayContext == ItemDisplayContext.THIRD_PERSON_RIGHT_HAND
                || displayContext == ItemDisplayContext.FIRST_PERSON_LEFT_HAND
                || displayContext == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND;
    }

    private static boolean shouldRenderOwnFirstPersonShieldArrows(
            Minecraft minecraft,
            ItemStack shield,
            ItemDisplayContext displayContext) {
        if (LodgedConfig.renderOwnShieldArrowsInFirstPerson()
                || displayContext != ItemDisplayContext.FIRST_PERSON_LEFT_HAND
                        && displayContext != ItemDisplayContext.FIRST_PERSON_RIGHT_HAND) {
            return true;
        }

        LocalPlayer player = minecraft.player;
        return player == null || shield != player.getMainHandItem() && shield != player.getOffhandItem();
    }

    private static void renderHoverOutline(
            EntityRenderDispatcher dispatcher,
            ClientLevel level,
            PoseStack poseStack,
            int packedLight,
            LodgedArrowVisual arrow,
            float partialTicks) {
        if (!LodgedInventoryArrowUi.prepareArrowOutlineTarget()) {
            return;
        }

        MultiBufferSource.BufferSource delegate = MultiBufferSource.immediate(new ByteBufferBuilder(HOVER_OUTLINE_BUFFER_SIZE));
        OutlineBufferSource outlineBuffer = new OutlineBufferSource(delegate);
        outlineBuffer.setColor(
                SHIELD_HOVER_HIGHLIGHT_RED,
                SHIELD_HOVER_HIGHLIGHT_GREEN,
                SHIELD_HOVER_HIGHLIGHT_BLUE,
                SHIELD_HOVER_HIGHLIGHT_ALPHA);
        try {
            renderArrow(dispatcher, level, poseStack, outlineBuffer, packedLight, arrow, partialTicks);
            outlineBuffer.endOutlineBatch();
        } finally {
            LodgedInventoryArrowUi.restoreVanillaEntityTarget();
        }
        Minecraft.getInstance().getMainRenderTarget().bindWrite(false);
        LodgedInventoryArrowUi.processArrowOutlineTarget();
    }

    private static void renderArrow(
            EntityRenderDispatcher dispatcher,
            ClientLevel level,
            PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight,
            LodgedArrowVisual arrow,
            float partialTicks) {
        AbstractArrow renderedArrow = LodgedArrowRenderHelper.createRenderedArrow(level, 0.0D, 0.0D, 0.0D, arrow);
        dispatcher.render(renderedArrow, 0.0D, 0.0D, 0.0D, 0.0F, partialTicks, poseStack, buffer, packedLight);
    }
}
