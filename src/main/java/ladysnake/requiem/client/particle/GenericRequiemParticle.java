package ladysnake.requiem.client.particle;

import ladysnake.requiem.common.particle.RequiemEntityParticleEffect;
import ladysnake.requiem.common.particle.WispTrailParticleEffect;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.core.particles.SimpleParticleType;

public class GenericRequiemParticle extends SingleQuadParticle {
    private final SpriteSet sprites;

    public GenericRequiemParticle(ClientLevel level, double x, double y, double z, double vx, double vy, double vz, SpriteSet sprites) {
        super(level, x, y, z, vx, vy, vz, sprites.get(0, 1));
        this.sprites = sprites;
        this.lifetime = 20 + this.random.nextInt(20);
        this.quadSize = 0.15F * (this.random.nextFloat() * 0.4F + 0.8F);
        this.xd = vx;
        this.yd = vy;
        this.zd = vz;
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

    public static class SimpleProvider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;

        public SimpleProvider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z, double vx, double vy, double vz, net.minecraft.util.RandomSource random) {
            return new GenericRequiemParticle(level, x, y, z, vx, vy, vz, this.sprites);
        }
    }

    public static class WispTrailProvider implements ParticleProvider<WispTrailParticleEffect> {
        private final SpriteSet sprites;

        public WispTrailProvider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(WispTrailParticleEffect type, ClientLevel level, double x, double y, double z, double vx, double vy, double vz, net.minecraft.util.RandomSource random) {
            GenericRequiemParticle p = new GenericRequiemParticle(level, x, y, z, vx, vy, vz, this.sprites);
            p.setColor(type.red(), type.green(), type.blue());
            return p;
        }
    }

    public static class EntityDustProvider implements ParticleProvider<RequiemEntityParticleEffect> {
        private final SpriteSet sprites;

        public EntityDustProvider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(RequiemEntityParticleEffect type, ClientLevel level, double x, double y, double z, double vx, double vy, double vz, net.minecraft.util.RandomSource random) {
            return new GenericRequiemParticle(level, x, y, z, vx, vy, vz, this.sprites);
        }
    }
}
