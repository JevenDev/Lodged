package com.jvn.lodged.compat;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.MenuType;

public final class PlayerInventoryMenus {
    private static final ResourceLocation MODULAR_BACKPACK_MENU =
            ResourceLocation.fromNamespaceAndPath("modular_backpacks", "backpack_menu");

    private PlayerInventoryMenus() {
    }

    public static boolean supportsArrowRemoval(AbstractContainerMenu menu) {
        return menu instanceof InventoryMenu || isModularBackpack(menu);
    }

    public static boolean isModularBackpack(AbstractContainerMenu menu) {
        MenuType<?> type;
        try {
            type = menu.getType();
        } catch (UnsupportedOperationException exception) {
            // menus without a registered type, such as horse inventories, cannot match
            return false;
        }
        return MODULAR_BACKPACK_MENU.equals(BuiltInRegistries.MENU.getKey(type));
    }
}
