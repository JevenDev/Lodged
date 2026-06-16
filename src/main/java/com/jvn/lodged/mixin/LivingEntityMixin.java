package com.jvn.lodged.mixin;

import com.jvn.lodged.config.LodgedConfig;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {
    @Shadow
    public int removeArrowTime;

    @Inject(method = "tick", at = @At("HEAD"))
    private void lodged$preventStuckArrowDespawn(CallbackInfo callbackInfo) {
        LivingEntity entity = (LivingEntity) (Object) this;
        if (entity.level().isClientSide() || entity.getArrowCount() <= 0 || !shouldPreventArrowDespawn(entity)) {
            return;
        }

        this.removeArrowTime = Integer.MAX_VALUE;
    }

    private static boolean shouldPreventArrowDespawn(LivingEntity entity) {
        if (entity instanceof Player) {
            return LodgedConfig.preventPlayerArrowDespawn();
        }
        return LodgedConfig.preventNonPlayerArrowDespawn();
    }
}
