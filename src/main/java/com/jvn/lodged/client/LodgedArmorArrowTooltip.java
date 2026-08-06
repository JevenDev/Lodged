package com.jvn.lodged.client;

import com.jvn.lodged.world.LodgedArmorArrowStorage;
import com.jvn.lodged.world.LodgedShieldArrowStorage;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.ItemAbilities;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

public final class LodgedArmorArrowTooltip {
    private LodgedArmorArrowTooltip() {
    }

    public static void onItemTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        int arrowCount;
        if (stack.getItem() instanceof ArmorItem) {
            arrowCount = LodgedArmorArrowStorage.count(stack);
        } else if (stack.canPerformAction(ItemAbilities.SHIELD_BLOCK)) {
            arrowCount = LodgedShieldArrowStorage.count(stack);
        } else {
            return;
        }

        if (arrowCount <= 0) {
            return;
        }

        event.getToolTip().add(Component.translatable("tooltip.lodged.armor_arrows", arrowCount)
                .withStyle(ChatFormatting.YELLOW));
    }
}
