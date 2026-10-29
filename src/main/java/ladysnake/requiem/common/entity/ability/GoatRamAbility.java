/*
 * Requiem
 * Copyright (C) 2019-2023 Ladysnake
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package ladysnake.requiem.common.entity.ability;

import ladysnake.requiem.Requiem;
import ladysnake.requiem.core.entity.ability.DirectAbilityBase;
import ladysnake.requiem.core.entity.ability.MeleeAbility;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.animal.goat.Goat;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.ToDoubleFunction;
import java.util.function.ToIntFunction;

public class GoatRamAbility<O extends PathfinderMob> extends DirectAbilityBase<O, Entity> {
    private static final long RAM_DURATION = 40;
    private static final ResourceLocation SPEED_MODIFIER_ID = Requiem.id("ram_speed_bonus");

    public static <O extends PathfinderMob> GoatRamAbility<O> create(O owner) {
        Function<? super O, SoundEvent> prepareRamSound = o -> o instanceof Goat g && g.isScreamingGoat() ? SoundEvents.GOAT_SCREAMING_PREPARE_RAM : SoundEvents.GOAT_PREPARE_RAM;
        Function<? super O, SoundEvent> impactRamSound = o -> o instanceof Goat g && g.isScreamingGoat() ? SoundEvents.GOAT_SCREAMING_RAM_IMPACT : SoundEvents.GOAT_RAM_IMPACT;
        ToIntFunction<? super O> cooldown = o -> o instanceof Goat g && g.isScreamingGoat() ? 100 : 600;
        ToDoubleFunction<? super O> strengthMultiplier = o -> o instanceof Goat g && g.isScreamingGoat() ? 2.0 : 1.0;
        TargetingConditions targeting = TargetingConditions.forCombat().selector((target, level) -> !target.getType().equals(EntityType.GOAT) && target.level().getWorldBorder().isWithinBounds(target.getBoundingBox()));

        return new GoatRamAbility<>(
            owner,
            cooldown,
            prepareRamSound,
            impactRamSound,
            strengthMultiplier,
            targeting,
            1.25F,
            7
        );
    }

    private final MeleeAbility backup;
    private final ToIntFunction<? super O> cooldownFactory;
    private final Function<? super O, SoundEvent> prepareRamSoundFactory;
    private final Function<? super O, SoundEvent> impactRamSoundFactory;
    private final ToDoubleFunction<? super O> strengthMultiplierFactory;
    private final TargetingConditions targetPredicate;
    private final float speed;
    private long ramStartTime;
    private boolean started;
    private Vec3 direction = Vec3.ZERO;
    private Vec3 target = Vec3.ZERO;

    protected GoatRamAbility(O owner, ToIntFunction<? super O> cooldownFactory, Function<? super O, SoundEvent> prepareRamSoundFactory, Function<? super O, SoundEvent> impactRamSoundFactory, ToDoubleFunction<? super O> strengthMultiplierFactory, TargetingConditions targetPredicate, float speed, int maxRange) {
        super(owner, cooldownFactory.applyAsInt(owner), maxRange, Entity.class);
        this.backup = new MeleeAbility(owner, false);
        this.cooldownFactory = cooldownFactory;
        this.prepareRamSoundFactory = prepareRamSoundFactory;
        this.impactRamSoundFactory = impactRamSoundFactory;
        this.strengthMultiplierFactory = strengthMultiplierFactory;
        this.targetPredicate = targetPredicate;
        this.speed = speed;
    }

    @Override
    public int getCooldownTime() {
        return this.cooldownFactory.applyAsInt(this.owner);
    }

    @Override
    public boolean canTarget(Entity target) {
        // Clientside, we only want to scan for targets when ramming is available
        return (!this.owner.level().isClientSide() || this.owner.isSprinting()) && super.canTarget(target);
    }

    @Override
    public InteractionResult trigger(Entity target) {
        if (super.trigger(target).consumesAction()) return InteractionResult.CONSUME;
        return this.backup.trigger(target);
    }

    @Override
    protected boolean run(Entity target) {
        if (target instanceof LivingEntity && this.owner.isSprinting()) {
            if (!this.owner.level().isClientSide() && this.owner.level() instanceof ServerLevel world) {
                long time = world.getGameTime();
                BlockPos startPos = this.owner.blockPosition();
                AttributeInstance speedAttribute = Objects.requireNonNull(this.owner.getAttribute(Attributes.MOVEMENT_SPEED));
                speedAttribute.removeModifier(SPEED_MODIFIER_ID);
                speedAttribute.addTransientModifier(
                    new AttributeModifier(SPEED_MODIFIER_ID, this.speed * 3, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
                this.target = target.position();
                this.ramStartTime = time;
                this.direction = (new Vec3(startPos.getX() - target.getX(), 0.0, startPos.getZ() - target.getZ())).normalize();
                this.started = true;
                world.broadcastEntityEvent(this.owner, EntityEvent.START_RAM);
                world.playSound(null, this.owner.getX(), this.owner.getY(), this.owner.getZ(), this.prepareRamSoundFactory.apply(this.owner), SoundSource.HOSTILE, 1.0F, this.owner.getVoicePitch());
            }
            return true;
        }
        return false;
    }

    @Override
    public void update() {
        super.update();

        if (this.started && this.owner.level() instanceof ServerLevel world) {
            long time = world.getGameTime();

            if (this.owner.isSprinting() && time - this.ramStartTime < RAM_DURATION) {
                List<LivingEntity> list = world.getNearbyEntities(LivingEntity.class, this.targetPredicate, this.owner, this.owner.getBoundingBox());
                if (!list.isEmpty()) {
                    LivingEntity livingEntity = list.get(0);
                    livingEntity.hurtServer(world, this.owner.damageSources().mobAttack(this.owner), (float) this.owner.getAttributeValue(Attributes.ATTACK_DAMAGE));
                    int speedAmount = this.owner.hasEffect(MobEffects.SPEED) ? this.owner.getEffect(MobEffects.SPEED).getAmplifier() + 1 : 0;
                    int slownessAmount = this.owner.hasEffect(MobEffects.SLOWNESS) ? this.owner.getEffect(MobEffects.SLOWNESS).getAmplifier() + 1 : 0;
                    float multiplier = 0.25F * (float) (speedAmount - slownessAmount);
                    float knockbackSpeed = (float) Mth.clamp(owner.getAttributeValue(Attributes.MOVEMENT_SPEED) * 2.4, 0.2F, 3.0F) + multiplier;
                    float knockbackModifier = livingEntity.isBlocking() ? 0.5F : 1.0F;
                    livingEntity.knockback((double) (knockbackModifier * knockbackSpeed) * this.strengthMultiplierFactory.applyAsDouble(this.owner), this.direction.x, this.direction.z, this.owner.damageSources().mobAttack(this.owner), 1.0f);
                    world.playSound(null, this.owner.getX(), this.owner.getY(), this.owner.getZ(), this.impactRamSoundFactory.apply(this.owner), SoundSource.HOSTILE, 1.0F, 1.0F);
                    this.finishRam();
                } else if (this.owner.position().distanceTo(this.target) < 0.25) {
                    this.finishRam();
                }
            } else {
                this.finishRam();
            }
        }
    }

    private void finishRam() {
        if (this.owner.level() instanceof ServerLevel world) {
            world.broadcastEntityEvent(this.owner, EntityEvent.END_RAM);
        }
        AttributeInstance speedAttr = this.owner.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speedAttr != null) {
            speedAttr.removeModifier(SPEED_MODIFIER_ID);
        }
        this.started = false;
    }
}
