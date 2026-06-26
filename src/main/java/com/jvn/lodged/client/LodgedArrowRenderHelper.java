package com.jvn.lodged.client;

import com.jvn.lodged.world.LodgedArrowVisual;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.entity.projectile.SpectralArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

public final class LodgedArrowRenderHelper {
    private LodgedArrowRenderHelper() {
    }

    public static AbstractArrow createRenderedArrow(Level level, double x, double y, double z, LodgedArrowVisual arrow) {
        ItemStack stack = renderStack(arrow);
        AbstractArrow renderedArrow = stack.is(Items.SPECTRAL_ARROW)
                ? new SpectralArrow(level, x, y, z, stack, null)
                : new Arrow(level, x, y, z, stack, null);

        float directionX = arrow.directionX();
        float directionY = arrow.directionY();
        float directionZ = arrow.directionZ();
        float horizontalLength = Mth.sqrt(directionX * directionX + directionZ * directionZ);
        renderedArrow.setYRot((float) (Math.atan2(directionX, directionZ) * 180.0F / Math.PI));
        renderedArrow.setXRot((float) (Math.atan2(directionY, horizontalLength) * 180.0F / Math.PI));
        renderedArrow.yRotO = renderedArrow.getYRot();
        renderedArrow.xRotO = renderedArrow.getXRot();
        return renderedArrow;
    }

    private static ItemStack renderStack(LodgedArrowVisual arrow) {
        ItemStack stack = arrow.stack();
        if (stack.isEmpty()) {
            return new ItemStack(Items.ARROW);
        }

        return stack.copyWithCount(1);
    }
}
