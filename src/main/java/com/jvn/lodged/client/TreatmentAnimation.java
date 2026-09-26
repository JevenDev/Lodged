package com.jvn.lodged.client;

final class TreatmentAnimation {
    static final float ITEM_DROP_TICKS = 8.0F;
    private static final float REACH_TICKS = 7.0F;
    private static final float GRIP_TICKS = 3.0F;
    private static final float TUG_TICKS = 18.0F;

    private static final float ENTRY_END = 0.18F;
    private static final float WRAP_END = 0.72F;
    private static final float PRESS_END = 0.88F;
    private static final float EXIT_START = 0.84F;

    private TreatmentAnimation() {
    }

    static RemovalMotion removal(float ticks, boolean loweringItem, float effort) {
        float handAge = Math.max(ticks - (loweringItem ? ITEM_DROP_TICKS : 0.0F), 0.0F);
        float reach = ease(handAge / REACH_TICKS);
        float rise = loweringItem ? ease(handAge / 6.0F) : 1.0F;
        float grip = ease((handAge - REACH_TICKS) / GRIP_TICKS);
        float pullAge = Math.max(handAge - REACH_TICKS - GRIP_TICKS, 0.0F);
        float load = ease(pullAge / 12.0F);
        float phase = pullAge * ((float) Math.PI * 2.0F) / TUG_TICKS;
        float tug = (1.0F - (float) Math.cos(phase)) * 0.5F;
        tug *= tug;
        float pull = load * (0.30F + tug * 0.32F);
        float resistance = load * effort * (0.45F + tug * 0.55F);
        float shake = load * tug * effort;
        float strain = ((float) Math.sin(pullAge * 1.8F) + (float) Math.sin(pullAge * 3.7F) * 0.25F) * shake;
        float lateralStrain = (float) Math.sin(pullAge * 2.4F + 1.1F) * shake;
        return new RemovalMotion(reach, rise, grip, pull, resistance, strain, lateralStrain);
    }

    static float itemDrop(float ticks) {
        return ease(ticks / ITEM_DROP_TICKS);
    }

    static float interpolatedTicks(int ticks, int entityTicks, float ageInTicks) {
        return ticks + Math.clamp(ageInTicks - entityTicks, 0.0F, 1.0F);
    }

    static BandageMotion bandage(float progress) {
        float exit = 1.0F - ease((progress - EXIT_START) / (1.0F - EXIT_START));
        float amount = ease(progress / ENTRY_END) * exit;
        float supportAmount = ease(progress / 0.14F) * exit;
        float wrapping = Math.clamp((progress - ENTRY_END) / (WRAP_END - ENTRY_END), 0.0F, 1.0F);
        float envelope = ease(wrapping / 0.16F) * ease((1.0F - wrapping) / 0.16F);
        float phase = wrapping * ((float) Math.PI * 2.0F) * 2.0F;
        phase -= (float) Math.sin(phase) * 0.28F;
        float stroke = (float) Math.sin(phase) * envelope;
        float orbit = (float) Math.cos(phase) * envelope;
        float press = Math.max(0.0F, (float) Math.sin(Math.PI * ease((progress - WRAP_END) / (PRESS_END - WRAP_END))));
        return new BandageMotion(amount, supportAmount, stroke, orbit, press * amount);
    }

    static BandageArmPose bandageArm(
            BandageMotion motion, boolean holdingBandage, boolean rightArm, float equippedProgress) {
        float amount = holdingBandage ? motion.amount() : motion.supportAmount();
        float side = rightArm ? 1.0F : -1.0F;
        float stroke = holdingBandage ? -motion.stroke() : motion.stroke();
        float orbit = motion.orbit();
        float press = motion.press();
        float lowering = (1.0F - amount) * (1.2F + equippedProgress * 0.6F);
        float x = side * (0.62F + (holdingBandage ? stroke * 0.09F - press * 0.035F : 0.0F));
        float y = (holdingBandage ? -0.40F : -0.66F) - lowering
                + (holdingBandage ? orbit * 0.07F - press * 0.025F : orbit * 0.008F);
        float z = (holdingBandage ? -0.85F : -0.92F)
                + (holdingBandage ? stroke * 0.045F - press * 0.04F : 0.0F);
        float xRot = (holdingBandage ? -22.0F : -12.0F)
                + orbit * (holdingBandage ? 5.0F : 1.0F)
                - press * (holdingBandage ? 7.0F : 0.0F);
        float yRot = side * (holdingBandage ? 13.0F : -5.0F);
        float zRot = side * ((holdingBandage ? -28.0F : -52.0F)
                + stroke * (holdingBandage ? 16.0F : 1.0F)
                - press * (holdingBandage ? 5.0F : 0.0F));
        return new BandageArmPose(x, y, z, xRot, yRot, zRot);
    }

    record BandageArmPose(float x, float y, float z, float xRot, float yRot, float zRot) {
    }

    record BandageMotion(float amount, float supportAmount, float stroke, float orbit, float press) {
    }

    private static float ease(float progress) {
        float amount = Math.clamp(progress, 0.0F, 1.0F);
        return amount * amount * amount * (amount * (amount * 6.0F - 15.0F) + 10.0F);
    }

    record RemovalMotion(
            float reach,
            float rise,
            float grip,
            float pull,
            float resistance,
            float strain,
            float lateralStrain) {
    }
}
