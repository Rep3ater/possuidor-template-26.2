package ladysnake.requiem.common.entity;

import ladysnake.requiem.common.construct.ai.SoulAiManager;

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
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraft.util.Mth;

/** Suffering-transmuted homunculus: immune to ordinary survival damage, but removable with /kill. */
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
            .add(Attributes.MOVEMENT_SPEED, 0.18)
            .add(Attributes.ATTACK_DAMAGE, 20.0)
            .add(Attributes.FOLLOW_RANGE, 32.0);
    }

    @Override
    protected void registerGoals() {
        // These are native goals rather than a nearby-player-tick script, so it keeps hunting
        // even when no Marbas player is looking at it and does not freeze after the first hit.
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0D, true));
        this.goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.6D));
        this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 12.0F));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
        this.targetSelector.addGoal(3, new SoulAiManager.FocusedTargetGoal(this,
            target -> SoulAiManager.hasEntityType(target, "villager", "iron_golem")));
    }

    @Override
    public float getVoicePitch() {
        return 0.52F;
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
        // Reassert this for saved entities created before the immortality flag was added.
        // The /kill command removes the entity directly; it does not rely on damage.
        if (!this.level().isClientSide()) this.setInvulnerable(true);
        super.tick();
        if (this.level().isClientSide() || !(this.level() instanceof ServerLevel serverLevel)) return;

        long now = serverLevel.getGameTime();
        var data = this.getPersistentData();
        long stunUntil = data.getLong(STUN_UNTIL);
        if (this.isInLava()) {
            if (now >= stunUntil) {
                data.putFloat(STUN_BASE_YAW, this.getYRot());
                // Play a subdued cry only when the lava stun starts, not continuously.
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
            float baseYaw = data.getFloat(STUN_BASE_YAW);
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

        // Walk slowly when idle, but move at player-like walking speed while actively chasing.
        var speed = this.getAttribute(Attributes.MOVEMENT_SPEED);
        boolean chasing = !stunned && this.getTarget() != null && this.getTarget().isAlive();
        if (speed != null) speed.setBaseValue(chasing ? 0.42D : 0.18D);

        // Heavy, low-pitched footsteps ramp up during pursuit and taper off after the target is lost.
        int soundLevel = data.getInt(CHASE_SOUND_LEVEL);
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
