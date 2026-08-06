package com.jvn.lodged.world;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;

public final class LodgedArrowStacks {
    private LodgedArrowStacks() {
    }

    public static ItemStack copyForRecovery(ItemStack storedStack) {
        if (storedStack.isEmpty()) {
            return ItemStack.EMPTY;
        }

        ItemStack recoveredStack = storedStack.copyWithCount(1);
        recoveredStack.remove(DataComponents.INTANGIBLE_PROJECTILE);
        return recoveredStack;
    }
}
