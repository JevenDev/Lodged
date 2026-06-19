package com.jvn.lodged.client;

import com.jvn.lodged.client.compat.SimpleBloodCompat;
import com.jvn.lodged.config.LodgedConfig;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;

public final class BleedingDropParticle extends TextureSheetParticle {
    private static final int SPRITE_STEPS = 2;

    private final SpriteSet sprites;
    private final int hangTime;
    private final double fallXd;
    private final double fallYd;
    private final double fallZd;
    private boolean released;
    private boolean landed;

    private BleedingDropParticle(
            ClientLevel level,
            double x,
            double y,
            double z,
            double xSpeed,
            double ySpeed,
            double zSpeed,
            SpriteSet sprites) {
        super(level, x, y, z, 0.0D, 0.0D, 0.0D);
        this.sprites = sprites;
        this.hangTime = 4 + this.random.nextInt(7);
        this.fallXd = xSpeed * 0.12D + this.random.nextGaussian() * 0.003D;
        this.fallYd = Math.min(-0.018D, ySpeed * 0.06D - 0.018D - this.random.nextDouble() * 0.018D);
        this.fallZd = zSpeed * 0.12D + this.random.nextGaussian() * 0.003D;
        this.gravity = 0.0F;
        this.friction = 0.96F;
        this.hasPhysics = false;
        this.lifetime = 42 + this.random.nextInt(22);
        this.quadSize *= 0.58F + this.random.nextFloat() * 0.34F;
        this.setSize(0.02F, 0.02F);
        this.setSprite(sprites.get(0, SPRITE_STEPS));
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    @Override
    public void tick() {
        if (!this.released) {
            this.xo = this.x;
            this.yo = this.y;
            this.zo = this.z;
            if (this.age++ >= this.lifetime) {
                this.remove();
                return;
            }

            if (this.age >= this.hangTime) {
                this.released = true;
                this.hasPhysics = true;
                this.gravity = 0.42F;
                this.xd = this.fallXd;
                this.yd = this.fallYd;
                this.zd = this.fallZd;
                this.setSprite(this.sprites.get(1, SPRITE_STEPS));
            } else {
                this.setSprite(this.sprites.get(0, SPRITE_STEPS));
            }
            return;
        }

        super.tick();
        if (this.removed) {
            return;
        }

        if (this.onGround) {
            if (!this.landed) {
                this.landed = true;
                SimpleBloodCompat.spawnGroundBlood(this.level, this.getPos(), this.getQuadSize(0.0F));
                this.age = 0;
                this.lifetime = 8 + this.random.nextInt(6);
                this.xd = 0.0D;
                this.yd = 0.0D;
                this.zd = 0.0D;
                this.gravity = 0.0F;
                this.hasPhysics = false;
            }
            this.setSprite(this.sprites.get(2, SPRITE_STEPS));
        } else if (this.age < 5) {
            this.setSprite(this.sprites.get(0, SPRITE_STEPS));
        } else {
            this.setSprite(this.sprites.get(1, SPRITE_STEPS));
        }
    }

    public static Particle create(
            SpriteSet sprites,
            net.minecraft.core.particles.SimpleParticleType type,
            ClientLevel level,
            double x,
            double y,
            double z,
            double xSpeed,
            double ySpeed,
            double zSpeed) {
        if (!LodgedConfig.bleedingDripParticles()) {
            return null;
        }

        return new BleedingDropParticle(level, x, y, z, xSpeed, ySpeed, zSpeed, sprites);
    }
}
