package ladysnake.requiem.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.core.particles.SimpleParticleType;

public class GhostParticle extends SingleQuadParticle {
    private final SpriteSet sprites;
    private TextureAtlasSprite currentSprite;

    public GhostParticle(ClientLevel level, double x, double y, double z, double vx, double vy, double vz, SpriteSet sprites) {
        super(level, x, y, z, vx, vy, vz);
        this.sprites = sprites;
        this.currentSprite = sprites.get(0, 1);
        this.lifetime = 30 + this.random.nextInt(25);
        this.quadSize = 0.22F * (this.random.nextFloat() * 0.5F + 0.8F);
        this.xd = vx * 0.5D;
        this.yd = vy * 0.5D + 0.02D;
        this.zd = vz * 0.5D;
        this.hasPhysics = false;

    }

    @Override
    protected float getU0() { return this.currentSprite.getU0(); }

    @Override
    protected float getU1() { return this.currentSprite.getU1(); }

    @Override
    protected float getV0() { return this.currentSprite.getV0(); }

    @Override
    protected float getV1() { return this.currentSprite.getV1(); }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    /** Emissive: ignore world lighting so ghosts glow in the dark. */
    @Override
    protected int getLightColor(float partialTick) {
        return 0xF000F0;
    }

    @Override
    public void tick() {
        super.tick();
        this.currentSprite = this.sprites.get(this.age, this.lifetime);
        float fadeOut = Math.max(0.0F, 1.0F - (float) this.age / (float) this.lifetime);
        float fadeIn = Math.min(1.0F, (this.age + 1) / 6.0F);
        float shimmer = 0.85F + 0.15F * (float) Math.sin(this.age * 0.6F);
        this.alpha = fadeOut * fadeIn * shimmer;
    }

    public static class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;

        public Provider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z, double vx, double vy, double vz) {
            return new GhostParticle(level, x, y, z, vx, vy, vz, this.sprites);
        }
    }
}
