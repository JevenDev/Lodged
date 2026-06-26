package com.jvn.lodged.client;

import com.jvn.lodged.world.LodgedArrowVisual;
import com.jvn.lodged.world.LodgedShieldArrowStorage;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.OutlineBufferSource;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.projectile.Arrow;
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
        List<LodgedArrowVisual> arrows = LodgedShieldArrowStorage.readAll(shield);
        if (arrows.isEmpty()) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null) {
            return;
        }

        EntityRenderDispatcher dispatcher = minecraft.getEntityRenderDispatcher();
        int hoveredArrowIndex = isHandRender(displayContext) ? LodgedInventoryArrowUi.hoveredShieldArrowIndex(shield) : -1;
        for (int index = 0; index < arrows.size(); index++) {
            LodgedArrowVisual arrow = arrows.get(index);
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
        float x = arrow.directionX();
        float y = arrow.directionY();
        float z = arrow.directionZ();
        float horizontalLength = Mth.sqrt(x * x + z * z);
        Arrow renderedArrow = new Arrow(level, 0.0D, 0.0D, 0.0D, ItemStack.EMPTY, null);
        renderedArrow.setYRot((float) (Math.atan2(x, z) * 180.0F / Math.PI));
        renderedArrow.setXRot((float) (Math.atan2(y, horizontalLength) * 180.0F / Math.PI));
        renderedArrow.yRotO = renderedArrow.getYRot();
        renderedArrow.xRotO = renderedArrow.getXRot();
        dispatcher.render(renderedArrow, 0.0D, 0.0D, 0.0D, 0.0F, partialTicks, poseStack, buffer, packedLight);
    }
}
