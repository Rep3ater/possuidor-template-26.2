package ladysnake.requiem.common.construct.ai;

import ladysnake.requiem.core.entity.SoulHolderComponent;
import ladysnake.requiem.core.util.NbtCompat;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import java.util.UUID;

public class SoulAiManager {

    public static void injectSoul(PathfinderMob mob, String soulTypeStr, @Nullable Player owner) {
        mob.getPersistentData().putString("requiem:injected_soul", soulTypeStr);
        if (owner != null) {
            NbtCompat.putUuid(mob.getPersistentData(), "requiem:soul_owner", owner.getUUID());
        }

        SoulHolderComponent.get(mob).giveSoulBack();
        applyInjectedGoals(mob, soulTypeStr, owner != null ? owner.getUUID() : null);

        if (mob.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.SOUL, mob.getX(), mob.getY() + mob.getBbHeight() / 2, mob.getZ(), 25, 0.4, 0.4, 0.4, 0.08);
            serverLevel.playSound(null, mob.getX(), mob.getY(), mob.getZ(), SoundEvents.SOUL_ESCAPE.value(), SoundSource.NEUTRAL, 1.2f, 1.0f);
        }
    }

    public static void extractSoul(PathfinderMob mob) {
        mob.getPersistentData().remove("requiem:injected_soul");
        mob.getPersistentData().remove("requiem:soul_owner");
        mob.goalSelector.removeAllGoals(g -> true);
        mob.targetSelector.removeAllGoals(g -> true);
        SoulHolderComponent.get(mob).removeSoul();

        if (mob.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.POOF, mob.getX(), mob.getY() + mob.getBbHeight() / 2, mob.getZ(), 15, 0.3, 0.3, 0.3, 0.05);
            serverLevel.playSound(null, mob.getX(), mob.getY(), mob.getZ(), SoundEvents.BOTTLE_FILL, SoundSource.PLAYERS, 1.0f, 1.0f);
        }
    }

    public static boolean hasInjectedSoul(PathfinderMob mob) {
        return mob.getPersistentData().contains("requiem:injected_soul");
    }

    public static String getInjectedSoulType(PathfinderMob mob) {
        return mob.getPersistentData().getString("requiem:injected_soul").orElse("");
    }

    public static void applyInjectedGoals(PathfinderMob mob, String soulTypeStr, @Nullable UUID ownerUuid) {
        mob.goalSelector.removeAllGoals(g -> true);
        mob.targetSelector.removeAllGoals(g -> true);

        // Ensure mob can deal at least base damage
        if (mob.getAttribute(Attributes.ATTACK_DAMAGE) != null && mob.getAttributeBaseValue(Attributes.ATTACK_DAMAGE) <= 0.0) {
            mob.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(1.5);
        }

        String lower = soulTypeStr.toLowerCase();

        if (lower.contains("wolf") && ownerUuid != null) {
            // Tamed Wolf Companion AI! Follows player, attacks player targets, defends player
            mob.goalSelector.addGoal(1, new FloatGoal(mob));
            mob.goalSelector.addGoal(2, new FollowSoulOwnerGoal(mob, ownerUuid, 1.25D, 10.0F, 2.5F));
            mob.goalSelector.addGoal(3, new MeleeAttackGoal(mob, 1.3D, true));
            mob.goalSelector.addGoal(4, new WaterAvoidingRandomStrollGoal(mob, 0.8D));
            mob.goalSelector.addGoal(5, new LookAtPlayerGoal(mob, Player.class, 8.0F));
            mob.goalSelector.addGoal(6, new RandomLookAroundGoal(mob));

            mob.targetSelector.addGoal(1, new OwnerHurtByTargetSoulGoal(mob, ownerUuid));
            mob.targetSelector.addGoal(2, new OwnerHurtTargetSoulGoal(mob, ownerUuid));
            mob.targetSelector.addGoal(3, new HurtByTargetGoal(mob));
        } else if (lower.contains("zombie") || lower.contains("drowned") || lower.contains("husk") || lower.contains("piglin")) {
            // Hostile Zombie AI! Chases players with fiery rage
            mob.goalSelector.addGoal(1, new FloatGoal(mob));
            mob.goalSelector.addGoal(2, new MeleeAttackGoal(mob, 1.25D, false));
            mob.goalSelector.addGoal(3, new WaterAvoidingRandomStrollGoal(mob, 1.0D));
            mob.goalSelector.addGoal(4, new LookAtPlayerGoal(mob, Player.class, 8.0F));
            mob.goalSelector.addGoal(5, new RandomLookAroundGoal(mob));

            mob.targetSelector.addGoal(1, new HurtByTargetGoal(mob));
            mob.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(mob, Player.class, true));
        } else {
            // Standard / Generic AI
            mob.goalSelector.addGoal(1, new FloatGoal(mob));
            mob.goalSelector.addGoal(2, new MeleeAttackGoal(mob, 1.15D, true));
            mob.goalSelector.addGoal(3, new WaterAvoidingRandomStrollGoal(mob, 1.0D));
            mob.goalSelector.addGoal(4, new LookAtPlayerGoal(mob, Player.class, 8.0F));
            mob.goalSelector.addGoal(5, new RandomLookAroundGoal(mob));

            mob.targetSelector.addGoal(1, new HurtByTargetGoal(mob));
            mob.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(mob, Player.class, true));
        }
    }

    public static class FollowSoulOwnerGoal extends Goal {
        private final PathfinderMob mob;
        private final UUID ownerUuid;
        private LivingEntity owner;
        private final double speedModifier;
        private final float startDist;
        private final float stopDist;
        private int timeToRecalcPath;

        public FollowSoulOwnerGoal(PathfinderMob mob, UUID ownerUuid, double speedModifier, float startDist, float stopDist) {
            this.mob = mob;
            this.ownerUuid = ownerUuid;
            this.speedModifier = speedModifier;
            this.startDist = startDist;
            this.stopDist = stopDist;
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if (this.owner == null || this.owner.isDeadOrDying() || !this.owner.isAlive()) {
                if (this.mob.level() instanceof ServerLevel serverLevel) {
                    Player p = serverLevel.getPlayerByUUID(this.ownerUuid);
                    if (p != null) this.owner = p;
                }
            }
            return this.owner != null && !this.owner.isSpectator() && this.mob.distanceToSqr(this.owner) > (double) (this.startDist * this.startDist);
        }

        @Override
        public boolean canContinueToUse() {
            return this.owner != null && !this.mob.getNavigation().isDone() && this.mob.distanceToSqr(this.owner) > (double) (this.stopDist * this.stopDist);
        }

        @Override
        public void start() {
            this.timeToRecalcPath = 0;
        }

        @Override
        public void tick() {
            this.mob.getLookControl().setLookAt(this.owner, 10.0F, (float) this.mob.getMaxHeadXRot());
            if (--this.timeToRecalcPath <= 0) {
                this.timeToRecalcPath = 10;
                this.mob.getNavigation().moveTo(this.owner, this.speedModifier);
            }
        }
    }

    public static class OwnerHurtByTargetSoulGoal extends TargetGoal {
        private final PathfinderMob mob;
        private final UUID ownerUuid;
        private LivingEntity ownerLastHurtBy;
        private int timestamp;

        public OwnerHurtByTargetSoulGoal(PathfinderMob mob, UUID ownerUuid) {
            super(mob, false);
            this.mob = mob;
            this.ownerUuid = ownerUuid;
            this.setFlags(EnumSet.of(Goal.Flag.TARGET));
        }

        @Override
        public boolean canUse() {
            if (this.mob.level() instanceof ServerLevel serverLevel) {
                Player owner = serverLevel.getPlayerByUUID(this.ownerUuid);
                if (owner != null) {
                    this.ownerLastHurtBy = owner.getLastHurtByMob();
                    int lastHurtTimestamp = owner.getLastHurtByMobTimestamp();
                    return lastHurtTimestamp != this.timestamp && this.canAttack(this.ownerLastHurtBy, TargetingConditions.DEFAULT);
                }
            }
            return false;
        }

        @Override
        public void start() {
            this.mob.setTarget(this.ownerLastHurtBy);
            if (this.mob.level() instanceof ServerLevel serverLevel) {
                Player owner = serverLevel.getPlayerByUUID(this.ownerUuid);
                if (owner != null) {
                    this.timestamp = owner.getLastHurtByMobTimestamp();
                }
            }
            super.start();
        }
    }

    public static class OwnerHurtTargetSoulGoal extends TargetGoal {
        private final PathfinderMob mob;
        private final UUID ownerUuid;
        private LivingEntity ownerTarget;
        private int timestamp;

        public OwnerHurtTargetSoulGoal(PathfinderMob mob, UUID ownerUuid) {
            super(mob, false);
            this.mob = mob;
            this.ownerUuid = ownerUuid;
            this.setFlags(EnumSet.of(Goal.Flag.TARGET));
        }

        @Override
        public boolean canUse() {
            if (this.mob.level() instanceof ServerLevel serverLevel) {
                Player owner = serverLevel.getPlayerByUUID(this.ownerUuid);
                if (owner != null) {
                    this.ownerTarget = owner.getLastHurtMob();
                    int lastHurtTimestamp = owner.getLastHurtMobTimestamp();
                    return lastHurtTimestamp != this.timestamp && this.canAttack(this.ownerTarget, TargetingConditions.DEFAULT);
                }
            }
            return false;
        }

        @Override
        public void start() {
            this.mob.setTarget(this.ownerTarget);
            if (this.mob.level() instanceof ServerLevel serverLevel) {
                Player owner = serverLevel.getPlayerByUUID(this.ownerUuid);
                if (owner != null) {
                    this.timestamp = owner.getLastHurtMobTimestamp();
                }
            }
            super.start();
        }
    }
}
