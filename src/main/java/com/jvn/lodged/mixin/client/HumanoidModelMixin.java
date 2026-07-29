package com.jvn.lodged.mixin.client;

import com.jvn.lodged.client.LodgedShieldArrowRemovalClient;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;
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
        var pose = LodgedShieldArrowRemovalClient.thirdPersonRemovalArmPose(entity, ageInTicks);
        if (pose == null) {
            return;
        }

        ModelPart modelPart = pose.arm() == HumanoidArm.RIGHT ? rightArm : leftArm;
        modelPart.xRot = Mth.lerp(pose.reach(), modelPart.xRot, pose.xRot());
        modelPart.yRot = Mth.lerp(pose.reach(), modelPart.yRot, pose.yRot());
        modelPart.zRot = Mth.lerp(pose.reach(), modelPart.zRot, pose.zRot());
    }
}
