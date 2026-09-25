package com.jvn.lodged.client;

import com.jvn.lodged.client.LodgedInventoryArrowUi.OutlineColor;
import com.jvn.lodged.config.LodgedConfig;
import com.jvn.lodged.network.payload.RemovePlayerArrowPayload.Target;
import com.jvn.lodged.world.LodgedArrowBodyPart;
import com.jvn.lodged.world.LodgedArrowDepth;
import com.jvn.lodged.world.LodgedArrowVisual;
import com.mojang.blaze3d.platform.Window;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextColor;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;

final class InventoryArrowTooltips {
    private static final int RISK_EASY_COLOR = 0x89F2A0;
    private static final int RISK_MODERATE_COLOR = 0xFFD86A;
    private static final int RISK_DANGEROUS_COLOR = 0xFF6A5E;
    private static final int ARMOR_ARROW_OUTLINE_COLOR = 0x8DDCFF;
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

    private InventoryArrowTooltips() {
    }

    static OutlineColor riskOutlineColor(LodgedArrowVisual arrow) {
        return outlineColorFor(removalChance(arrow));
    }

    static OutlineColor shieldRiskOutlineColor() {
        return outlineColorFor(shieldRemovalChance());
    }

    static OutlineColor armorArrowOutlineColor() {
        return OutlineColor.fromRgb(ARMOR_ARROW_OUTLINE_COLOR);
    }

    static OutlineColor tamedMobRiskOutlineColor(LodgedArrowVisual arrow) {
        return outlineColorFor(tamedMobRemovalChance(arrow));
    }

    static void renderHorseArmorRemovalTooltip(
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

    static void renderHorseBodyRemovalTooltip(
            GuiGraphics guiGraphics,
            LodgedArrowVisual arrow,
            int mouseX,
            int mouseY) {
        renderArrowRemovalTooltip(
                guiGraphics,
                new HoveredArrow(arrow, Target.BODY, InteractionHand.MAIN_HAND, EquipmentSlot.BODY),
                mouseX,
                mouseY,
                tamedMobRemovalChance(arrow));
    }

    static void renderArrowRemovalTooltip(GuiGraphics guiGraphics, HoveredArrow hoveredArrow, int mouseX, int mouseY) {
        renderArrowRemovalTooltip(guiGraphics, hoveredArrow, mouseX, mouseY, removalChance(hoveredArrow));
    }

    static void renderArrowRemovalTooltip(
            GuiGraphics guiGraphics,
            HoveredArrow hoveredArrow,
            int mouseX,
            int mouseY,
            double chance) {
        Minecraft minecraft = Minecraft.getInstance();
        Font font = minecraft.font;
        int chancePercent = Math.round((float) (Mth.clamp(chance, 0.0D, 1.0D) * 100.0D));
        OutlineColor outlineColor = outlineColorFor(chance);
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

    private static double removalChance(LodgedArrowVisual arrow) {
        return removalChance(arrow.bodyPart()) * LodgedConfig.arrowDepthRemovalSuccessMultiplier(arrow.depth());
    }

    private static double tamedMobRemovalChance(LodgedArrowVisual arrow) {
        return Mth.clamp(
                LodgedConfig.tamedMobArrowRemovalSuccessChance(arrow.bodyPart())
                        * LodgedConfig.arrowDepthRemovalSuccessMultiplier(arrow.depth()),
                0.0D,
                1.0D);
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

    static OutlineColor riskOutlineColor(HoveredArrow hoveredArrow) {
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

    private static OutlineColor outlineColorFor(double chance) {
        if (chance < 0.5D) {
            return OutlineColor.fromRgb(RISK_DANGEROUS_COLOR);
        }
        if (chance < 0.8D) {
            return OutlineColor.fromRgb(RISK_MODERATE_COLOR);
        }
        return OutlineColor.fromRgb(RISK_EASY_COLOR);
    }

    record HoveredArrow(LodgedArrowVisual arrow, Target target, InteractionHand hand, EquipmentSlot armorSlot) {
    }
}
