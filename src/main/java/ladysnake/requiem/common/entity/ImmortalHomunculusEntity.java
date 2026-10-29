package ladysnake.requiem.common.entity;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraft.util.Mth;

/** Suffering-transmuted homunculus: dangerous, but still mortal. */
public class ImmortalHomunculusEntity extends PathfinderMob {
    private static final String STUN_UNTIL = "requiem:homunculus_stun_until";
    private static final String STUN_BASE_YAW = "requiem:immortal_stun_base_yaw";
    private static final String CHASE_SOUND_LEVEL = "requiem:immortal_chase_sound_level";

    public ImmortalHomunculusEntity(EntityType<? extends ImmortalHomunculusEntity> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
            .add(Attributes.MAX_HEALTH, 100.0)
            // A deliberate walk speed. The hunt controller raises this to player walking speed
            // only while a target is being chased.
            .add(Attributes.MOVEMENT_SPEED, 0.10)
            .add(Attributes.ATTACK_DAMAGE, 20.0)
            .add(Attributes.FOLLOW_RANGE, 32.0);
    }

    @Override
    protected void registerGoals() {
        // Marbas or an injected soul installs the active behavior.
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.HOGLIN_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.HOGLIN_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.HOGLIN_DEATH;
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide() || !(this.level() instanceof ServerLevel serverLevel)) return;

        // Old saved copies may still carry the previous invulnerability flag. Clear it every tick;
        // /kill and ordinary damage must work on this entity.
        this.setInvulnerable(false);

        long now = serverLevel.getGameTime();
        var data = this.getPersistentData();
        long stunUntil = data.getLong(STUN_UNTIL).orElse(0L);
        if (this.isInLava()) {
            if (now >= stunUntil) {
                data.putFloat(STUN_BASE_YAW, this.getYRot());
                // A small, subdued cry only when the lava stun begins; not every tick in lava.
                this.playSound(SoundEvents.ALLAY_HURT, 1.0F, 0.52F);
            }
            stunUntil = Math.max(stunUntil, now + 3_600L); // three minutes at 20 TPS
            data.putLong(STUN_UNTIL, stunUntil);
        }

        boolean stunned = now < stunUntil;
        if (stunned) {
            this.getNavigation().stop();
            this.setTarget(null);
            this.setDeltaMovement(Vec3.ZERO);
            float baseYaw = data.getFloat(STUN_BASE_YAW).orElse(this.getYRot());
            this.setYRot(baseYaw + Mth.sin(this.tickCount * 1.75F) * 5.0F);
            this.setXRot(Mth.sin(this.tickCount * 2.4F) * 7.0F);
            if (this.tickCount % 8 == 0) {
                serverLevel.sendParticles(ParticleTypes.SMOKE,
                    this.getX(), this.getY() + 0.8D, this.getZ(),
                    2, this.getBbWidth() * 0.25D, this.getBbHeight() * 0.22D,
                    this.getBbWidth() * 0.25D, 0.01D);
                serverLevel.sendParticles(ParticleTypes.ASH,
                    this.getX(), this.getY() + this.getBbHeight() * 0.55D, this.getZ(),
                    1, this.getBbWidth() * 0.2D, this.getBbHeight() * 0.2D,
                    this.getBbWidth() * 0.2D, 0.005D);
            }
        } else {
            if (data.contains(STUN_BASE_YAW)) {
                this.setYRot(data.getFloat(STUN_BASE_YAW).orElse(this.getYRot()));
                data.remove(STUN_BASE_YAW);
            }
            this.setXRot(0.0F);
        }

        // A heavy, low-pitched footfall fades in during pursuit and fades out for several
        // seconds afterwards. It is sparse enough not to become a constant looping noise.
        boolean chasing = !stunned && this.getTarget() != null && this.getTarget().isAlive();
        int soundLevel = data.getInt(CHASE_SOUND_LEVEL).orElse(0);
        soundLevel = chasing ? Math.min(100, soundLevel + 3) : Math.max(0, soundLevel - 1);
        data.putInt(CHASE_SOUND_LEVEL, soundLevel);
        if (soundLevel > 0 && this.tickCount % 14 == 0) {
            float volume = 0.85F * soundLevel / 100.0F;
            float pitch = 0.42F + 0.15F * (100 - soundLevel) / 100.0F;
            serverLevel.playSound(null, this.getX(), this.getY(), this.getZ(),
                SoundEvents.RAVAGER_STEP, SoundSource.HOSTILE, volume, pitch);
        }
    }
}
