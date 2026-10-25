package ladysnake.requiem.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.core.particles.SimpleParticleType;

public class GhostParticle extends SingleQuadParticle {
    private final SpriteSet sprites;

    public GhostParticle(ClientLevel level, double x, double y, double z, double vx, double vy, double vz, SpriteSet sprites) {
        super(level, x, y, z, vx, vy, vz, sprites.get(0, 1));
        this.sprites = sprites;
        this.lifetime = 30 + this.random.nextInt(25);
        this.quadSize = 0.22F * (this.random.nextFloat() * 0.5F + 0.8F);
        this.xd = vx * 0.5D;
        this.yd = vy * 0.5D + 0.02D;
        this.zd = vz * 0.5D;
        this.hasPhysics = false;
        this.setSpriteFromAge(sprites);
    }

    @Override
    protected Layer getLayer() {
        return Layer.TRANSLUCENT;
    }

    @Override
    public void tick() {
        super.tick();
        this.setSpriteFromAge(this.sprites);
        this.alpha = Math.max(0.0F, 1.0F - (float) this.age / (float) this.lifetime);
    }

    public static class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;

        public Provider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z, double vx, double vy, double vz, net.minecraft.util.RandomSource random) {
            return new GhostParticle(level, x, y, z, vx, vy, vz, this.sprites);
        }
    }
}
