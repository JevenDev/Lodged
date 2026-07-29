package com.jvn.lodged.mixin.client;

import com.jvn.lodged.client.LodgedHorseArmorArrowUi;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.HorseInventoryScreen;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(HorseInventoryScreen.class)
public abstract class HorseInventoryScreenMixin {
    @Redirect(
            method = "renderBg",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/screens/inventory/InventoryScreen;renderEntityInInventoryFollowsMouse(Lnet/minecraft/client/gui/GuiGraphics;IIIIIFFFLnet/minecraft/world/entity/LivingEntity;)V"))
    private void lodged$renderDraggableHorsePreview(
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
        LodgedHorseArmorArrowUi.renderHorsePreview(
                guiGraphics, x1, y1, x2, y2, scale, size, mouseX, mouseY, entity);
    }
}
