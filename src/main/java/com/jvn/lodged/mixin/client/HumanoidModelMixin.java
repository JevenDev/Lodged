package com.jvn.lodged.mixin.client;

import com.jvn.lodged.client.LodgedShieldArrowRemovalClient;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HumanoidModel.class)
public abstract class HumanoidModelMixin<T extends LivingEntity> {
    @Shadow
    public ModelPart rightArm;

    @Shadow
    public ModelPart leftArm;

    @Inject(method = "setupAnim(Lnet/minecraft/world/entity/LivingEntity;FFFFF)V", at = @At("TAIL"))
    private void lodged$poseShieldArrowRemoval(
            T entity,
            float limbSwing,
            float limbSwingAmount,
            float ageInTicks,
            float netHeadYaw,
            float headPitch,
            CallbackInfo callbackInfo) {
        if (LodgedShieldArrowRemovalClient.isPullingShieldArrow(entity)) {
            HumanoidArm arm = LodgedShieldArrowRemovalClient.pullingArm(entity);
            ModelPart modelPart = arm == HumanoidArm.RIGHT ? rightArm : leftArm;
            float side = arm == HumanoidArm.RIGHT ? 1.0F : -1.0F;
            float pull = LodgedShieldArrowRemovalClient.pullProgress(entity);
            modelPart.xRot = LodgedShieldArrowRemovalClient.pullingArmXRot(ageInTicks, pull);
            modelPart.yRot = side * LodgedShieldArrowRemovalClient.pullingArmYRot();
            modelPart.zRot = side * LodgedShieldArrowRemovalClient.pullingArmZRot();
            return;
        }

        if (LodgedShieldArrowRemovalClient.isPullingArmorArrow(entity)) {
            HumanoidArm arm = LodgedShieldArrowRemovalClient.armorPullingArm(entity);
            ModelPart modelPart = arm == HumanoidArm.RIGHT ? rightArm : leftArm;
            float pull = LodgedShieldArrowRemovalClient.armorPullProgress(entity);
            modelPart.xRot = LodgedShieldArrowRemovalClient.armorPullingArmXRot(entity, ageInTicks, pull);
            modelPart.yRot = LodgedShieldArrowRemovalClient.armorPullingArmYRot(entity);
            modelPart.zRot = LodgedShieldArrowRemovalClient.armorPullingArmZRot(entity);
        }
    }
}
