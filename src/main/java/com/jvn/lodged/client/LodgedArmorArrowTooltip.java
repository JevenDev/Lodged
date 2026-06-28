package com.jvn.lodged.client;

import com.jvn.lodged.world.LodgedArmorArrowStorage;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

public final class LodgedArmorArrowTooltip {
    private LodgedArmorArrowTooltip() {
    }

    public static void onItemTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        if (!(stack.getItem() instanceof ArmorItem)) {
            return;
        }

        int arrowCount = LodgedArmorArrowStorage.readAll(stack).size();
        if (arrowCount <= 0) {
            return;
        }

        event.getToolTip().add(Component.translatable("tooltip.lodged.armor_arrows", arrowCount)
                .withStyle(ChatFormatting.YELLOW));
    }
}
