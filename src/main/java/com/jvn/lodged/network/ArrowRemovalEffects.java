package com.jvn.lodged.network;

import com.jvn.lodged.world.LodgedArrowVisual;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

final class ArrowRemovalEffects {
    private static final int SHIELD_ARROW_BREAK_PARTICLES = 12;
    private static final int ARMOR_ARROW_BREAK_PARTICLES = 10;
    private static final double SHIELD_CENTER_HEIGHT_RATIO = 0.55D;
    private static final double SHIELD_CENTER_FORWARD_OFFSET = 0.35D;
    private static final double SHIELD_HAND_SIDE_OFFSET = 0.28D;

    private ArrowRemovalEffects() {
    }

    static void spawnShieldArrowBreakParticles(ServerPlayer player, InteractionHand hand, LodgedArrowVisual arrow) {
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }

        Vec3 location = shieldArrowWorldPosition(player, hand, arrow);
        level.sendParticles(
                new BlockParticleOption(ParticleTypes.BLOCK, Blocks.OAK_PLANKS.defaultBlockState()),
                location.x,
                location.y,
                location.z,
                SHIELD_ARROW_BREAK_PARTICLES,
                0.04D,
                0.04D,
                0.04D,
                0.04D);
    }

    static void spawnArmorArrowBreakParticles(LivingEntity entity, LodgedArrowVisual arrow) {
        if (!(entity.level() instanceof ServerLevel level)) {
            return;
        }

        Vec3 location = armorArrowWorldPosition(entity, arrow);
        level.sendParticles(
                new BlockParticleOption(ParticleTypes.BLOCK, Blocks.IRON_BLOCK.defaultBlockState()),
                location.x,
                location.y,
                location.z,
                ARMOR_ARROW_BREAK_PARTICLES,
                0.035D,
                0.035D,
                0.035D,
                0.035D);
    }

    private static Vec3 shieldArrowWorldPosition(ServerPlayer player, InteractionHand hand, LodgedArrowVisual arrow) {
        float yawRadians = player.getYHeadRot() * ((float) Math.PI / 180.0F);
        Vec3 right = new Vec3(Math.cos(yawRadians), 0.0D, Math.sin(yawRadians));
        Vec3 forward = new Vec3(-Math.sin(yawRadians), 0.0D, Math.cos(yawRadians));
        HumanoidArm shieldArm = hand == InteractionHand.MAIN_HAND
                ? player.getMainArm()
                : player.getMainArm().getOpposite();
        double side = shieldArm == HumanoidArm.RIGHT ? -1.0D : 1.0D;
        Vec3 shieldCenter = new Vec3(
                player.getX(),
                player.getY() + player.getBbHeight() * SHIELD_CENTER_HEIGHT_RATIO,
                player.getZ())
                .add(forward.scale(SHIELD_CENTER_FORWARD_OFFSET))
                .add(right.scale(side * SHIELD_HAND_SIDE_OFFSET));
        return shieldCenter
                .add(right.scale(arrow.modelX()))
                .add(0.0D, -arrow.modelY(), 0.0D)
                .add(forward.scale(-arrow.modelZ()));
    }

    private static Vec3 armorArrowWorldPosition(LivingEntity entity, LodgedArrowVisual arrow) {
        float yawRadians = entity.yBodyRot * ((float) Math.PI / 180.0F);
        Vec3 right = new Vec3(Math.cos(yawRadians), 0.0D, Math.sin(yawRadians));
        Vec3 forward = new Vec3(-Math.sin(yawRadians), 0.0D, Math.cos(yawRadians));
        Vec3 local = arrow.toEntityLocalPosition(entity);
        return new Vec3(entity.getX(), entity.getY(), entity.getZ())
                .add(right.scale(local.x))
                .add(0.0D, local.y, 0.0D)
                .add(forward.scale(local.z));
    }

    static Vec3 bodyArrowWorldPosition(LivingEntity target, LodgedArrowVisual arrow) {
        float yawRadians = target.yBodyRot * ((float) Math.PI / 180.0F);
        Vec3 right = new Vec3(Math.cos(yawRadians), 0.0D, Math.sin(yawRadians));
        Vec3 forward = new Vec3(-Math.sin(yawRadians), 0.0D, Math.cos(yawRadians));
        Vec3 local = arrow.toEntityLocalPosition(target);
        return target.position()
                .add(right.scale(local.x))
                .add(0.0D, local.y, 0.0D)
                .add(forward.scale(local.z));
    }

    static void playSound(LivingEntity entity, net.minecraft.sounds.SoundEvent sound, float volume, float pitch) {
        entity.level().playSound(null, entity.getX(), entity.getY(), entity.getZ(), sound, SoundSource.PLAYERS, volume, pitch);
    }

    static void playSound(ServerPlayer player, net.minecraft.sounds.SoundEvent sound, float volume, float pitch) {
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), sound, SoundSource.PLAYERS, volume, pitch);
    }
}
