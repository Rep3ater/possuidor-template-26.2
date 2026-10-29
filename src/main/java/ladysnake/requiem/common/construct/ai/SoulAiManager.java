package ladysnake.requiem.common.construct.ai;

import ladysnake.requiem.core.entity.SoulHolderComponent;
import ladysnake.requiem.core.util.NbtCompat;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import java.util.UUID;
import java.util.Locale;

public class SoulAiManager {

    public static void injectSoul(PathfinderMob mob, String soulTypeStr, @Nullable Player owner) {
        mob.getPersistentData().putString("requiem:injected_soul", soulTypeStr);
        // An explicitly injected soul replaces any previously transplanted homunculus brain.
        mob.getPersistentData().remove("requiem:homunculus_souled");
        if (owner != null) {
            NbtCompat.putUuid(mob.getPersistentData(), "requiem:soul_owner", owner.getUUID());
        } else {
            mob.getPersistentData().remove("requiem:soul_owner");
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
        mob.setTarget(null);
        mob.getNavigation().stop();

        // Souls supply behavior, not the donor's attributes. Keep each vessel body's own
        // health, speed, armor, attack damage and dimensions intact.
        String lower = soulTypeStr == null ? "" : soulTypeStr.toLowerCase(Locale.ROOT);
        Identifier parsedType = Identifier.tryParse(lower);
        EntityType<?> sourceType = parsedType == null
            ? null
            : BuiltInRegistries.ENTITY_TYPE.getOptional(parsedType).orElse(null);
        boolean foxSoul = lower.equals("fox") || lower.equals("minecraft:fox") || lower.endsWith(":fox");
        boolean wolfSoul = lower.endsWith(":wolf") || lower.equals("wolf");
        boolean homunculusSoul = lower.equals("requiem:homunculus") || lower.equals("homunculus");
        boolean sufferingSoul = lower.equals("requiem:immortal_homunculus") || lower.equals("immortal_homunculus");
        boolean hostileByName = lower.matches(".*:(zombie|drowned|husk|zombified_piglin|piglin|piglin_brute|pillager|vindicator|evoker|illusioner|ravager|vex|witch|skeleton|stray|bogged|creeper|spider|cave_spider|enderman|blaze|ghast|guardian|elder_guardian|silverfish|endermite|slime|magma_cube|phantom|warden|breeze|hoglin|zoglin|shulker|wither|ender_dragon)$");
        boolean hostileSoul = hostileByName || sufferingSoul || (sourceType != null && sourceType.getCategory() == MobCategory.MONSTER);

        if (foxSoul) {
            // Foxes hunt small prey and fish but never choose players.
            mob.goalSelector.addGoal(1, new FloatGoal(mob));
            mob.goalSelector.addGoal(2, new SoulMeleeAttackGoal(mob, 1.0D));
            mob.goalSelector.addGoal(3, new WaterAvoidingRandomStrollGoal(mob, 1.0D));
            mob.goalSelector.addGoal(4, new LookAtPlayerGoal(mob, Player.class, 8.0F));
            mob.goalSelector.addGoal(5, new RandomLookAroundGoal(mob));
            mob.targetSelector.addGoal(1, new FoxPreyTargetGoal(mob));
            return;
        }

        if ((wolfSoul || homunculusSoul) && ownerUuid != null) {
            // Wolf and programmable-homunculus souls act as companions and protect their injector.
            mob.goalSelector.addGoal(1, new FloatGoal(mob));
            mob.goalSelector.addGoal(2, new FollowSoulOwnerGoal(mob, ownerUuid, 1.0D, 10.0F, 2.5F));
            mob.goalSelector.addGoal(3, new SoulMeleeAttackGoal(mob, 1.0D));
            mob.goalSelector.addGoal(4, new WaterAvoidingRandomStrollGoal(mob, 1.0D));
            mob.goalSelector.addGoal(5, new LookAtPlayerGoal(mob, Player.class, 8.0F));
            mob.goalSelector.addGoal(6, new RandomLookAroundGoal(mob));
            mob.targetSelector.addGoal(1, new OwnerHurtByTargetSoulGoal(mob, ownerUuid));
            mob.targetSelector.addGoal(2, new OwnerHurtTargetSoulGoal(mob, ownerUuid));
            mob.targetSelector.addGoal(3, new HurtByTargetGoal(mob));
            return;
        }

        // Use the donor type's registry category, rather than a short hand-written list, so
        // pillagers and modded monsters receive hostile behavior while villagers/passive souls
        // stay peaceful unless attacked. The name fallback handles a few vanilla monsters whose
        // categories have varied between game versions.
        mob.goalSelector.addGoal(1, new FloatGoal(mob));
        mob.goalSelector.addGoal(2, new SoulMeleeAttackGoal(mob, 1.0D));
        mob.goalSelector.addGoal(3, new WaterAvoidingRandomStrollGoal(mob, 1.0D));
        mob.goalSelector.addGoal(4, new LookAtPlayerGoal(mob, Player.class, 8.0F));
        mob.goalSelector.addGoal(5, new RandomLookAroundGoal(mob));
        mob.targetSelector.addGoal(1, new HurtByTargetGoal(mob));
        if (hostileSoul) {
            mob.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(mob, Player.class, true));
        }
    }

    /** Safe melee for body types (like chickens/rabbits) that normally do not attack in melee. */
    private static final class SoulMeleeAttackGoal extends Goal {
        private final PathfinderMob mob;
        private final double speedModifier;
        private int attackCooldown;

        private SoulMeleeAttackGoal(PathfinderMob mob, double speedModifier) {
            this.mob = mob;
            this.speedModifier = speedModifier;
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity target = this.mob.getTarget();
            return target != null && target != this.mob && target.isAlive();
        }

        @Override
        public boolean canContinueToUse() {
            LivingEntity target = this.mob.getTarget();
            return target != null && target != this.mob && target.isAlive()
                && this.mob.distanceToSqr(target) <= followRange(this.mob) * followRange(this.mob);
        }

        @Override
        public void start() {
            this.attackCooldown = 0;
        }

        @Override
        public void stop() {
            this.mob.getNavigation().stop();
        }

        @Override
        public void tick() {
            LivingEntity target = this.mob.getTarget();
            if (target == null || !target.isAlive()) return;
            this.mob.getLookControl().setLookAt(target, 30.0F, 30.0F);
            double reach = this.mob.getBbWidth() * 2.0D + target.getBbWidth();
            if (this.mob.distanceToSqr(target) <= reach * reach) {
                this.mob.getNavigation().stop();
                if (this.attackCooldown > 0) this.attackCooldown--;
                if (this.attackCooldown == 0 && this.mob.level() instanceof ServerLevel serverLevel) {
                    this.attackCooldown = 20;
                    var attack = this.mob.getAttribute(Attributes.ATTACK_DAMAGE);
                    // Read physical damage from the body. Only bodies with no attack attribute
                    // use a small fallback so transferred aggressive AI remains functional.
                    float damage = attack == null || attack.getValue() <= 0.0D
                        ? 1.0F : (float) Math.max(0.0D, attack.getValue());
                    if (damage > 0.0F) {
                        target.hurtServer(serverLevel, serverLevel.damageSources().mobAttack(this.mob), damage);
                    }
                }
            } else {
                if (this.attackCooldown > 0) this.attackCooldown--;
                this.mob.getNavigation().moveTo(target, this.speedModifier);
            }
        }
    }

    /** Target only entities a vanilla fox preys on. Players are deliberately excluded. */
    private static final class FoxPreyTargetGoal extends TargetGoal {
        private final PathfinderMob fox;
        @Nullable private LivingEntity selected;

        private FoxPreyTargetGoal(PathfinderMob fox) {
            super(fox, false);
            this.fox = fox;
            this.setFlags(EnumSet.of(Goal.Flag.TARGET));
        }

        @Override
        public boolean canUse() {
            double range = followRange(this.fox);
            AABB search = this.fox.getBoundingBox().inflate(range);
            this.selected = this.fox.level().getEntitiesOfClass(LivingEntity.class, search, prey ->
                prey != this.fox && prey.isAlive() && isFoxPrey(prey)
                    && this.canAttack(prey, TargetingConditions.DEFAULT))
                .stream().min((a, b) -> Double.compare(a.distanceToSqr(this.fox), b.distanceToSqr(this.fox)))
                .orElse(null);
            return this.selected != null;
        }

        @Override
        public boolean canContinueToUse() {
            LivingEntity target = this.fox.getTarget();
            return target != null && target.isAlive() && isFoxPrey(target)
                && this.fox.distanceToSqr(target) <= followRange(this.fox) * followRange(this.fox);
        }

        @Override
        public void start() {
            this.fox.setTarget(this.selected);
            super.start();
        }

        @Override
        public void stop() {
            if (this.fox.getTarget() == this.selected) this.fox.setTarget(null);
            this.selected = null;
            super.stop();
        }
    }

    private static boolean isFoxPrey(LivingEntity entity) {
        var type = entity.getType();
        return type == EntityTypes.CHICKEN
            || type == EntityTypes.RABBIT
            || type == EntityTypes.COD
            || type == EntityTypes.SALMON
            || type == EntityTypes.TROPICAL_FISH
            || type == EntityTypes.PUFFERFISH
            || type == EntityTypes.TURTLE;
    }

    private static double followRange(PathfinderMob mob) {
        var range = mob.getAttribute(Attributes.FOLLOW_RANGE);
        return range == null ? 16.0D : Math.max(1.0D, range.getValue());
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
