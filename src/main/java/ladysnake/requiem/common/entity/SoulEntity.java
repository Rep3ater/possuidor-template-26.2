/*
 * Requiem
 * Copyright (C) 2017-2023 Ladysnake
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
 * along with this program. If not, see <https://www.gnu.org/licenses>.
 */
package ladysnake.requiem.common.entity;

import ladysnake.requiem.common.particle.WispTrailParticleEffect;
import ladysnake.requiem.common.sound.RequiemSoundEvents;
import ladysnake.requiem.common.tag.RequiemBlockTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

import java.util.Objects;
import java.util.Optional;

public class SoulEntity extends Entity {
    public static final byte SOUL_EXPIRED_STATUS = 1;
    public static final double RADIANS_TO_DEGREES = 180 / Math.PI;

    public static final EntityDataAccessor<Optional<Vec3>> NEXT_TARGET = SynchedEntityData.defineId(SoulEntity.class, RequiemTrackedDataHandlers.OPTIONAL_VEC_3D);
    public static final EntityDataAccessor<Float> SPEED_MODIFIER = SynchedEntityData.defineId(SoulEntity.class, EntityDataSerializers.FLOAT);

    protected int maxAge;
    protected int targetChangeCooldown = 0;
    protected int timeInSolid = -1;
    protected int targetChanges = 0;

    public SoulEntity(EntityType<? extends SoulEntity> type, Level world) {
        super(type, world);
        this.maxAge = 600 + random.nextInt(600);
    }

    public void setMaxAge(int maxAge) {
        this.maxAge = maxAge;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(NEXT_TARGET, Optional.empty());
        builder.define(SPEED_MODIFIER, 0f);
    }

    @Override
    public void tick() {
        super.tick();

        if (!this.level().isClientSide()) {
            if (this.maxAge >= 0 && this.tickCount >= this.maxAge) {
                this.expire();
            }

            this.targetChangeCooldown -= this.position().distanceToSqr(this.xOld, this.yOld, this.zOld) < 0.0125 ? 10 : 1;

            if ((this.level().getGameTime() % 20 == 0) && (this.getTarget().map(this.position()::distanceToSqr).orElse(Double.NaN) < 9 || targetChangeCooldown <= 0)) {
                this.updateTarget();
            }

            this.tickTimeInSolid();
        }

        this.getTarget().ifPresent(target -> {
            Vec3 targetVector = target.subtract(this.position());
            double length = targetVector.length();
            targetVector = targetVector.scale(getSpeedModifier() / Math.max(1, length));

            // newVelocity = 0.9 * velocity + 0.1 * targetVector
            Vec3 newVelocity = this.getDeltaMovement().scale(0.9).add(targetVector.scale(0.1));
            if (Objects.equals(BlockPos.containing(target), this.blockPosition())) {
                newVelocity = newVelocity.scale(0.8);
            }
            this.setDeltaMovement(newVelocity);

            if (newVelocity.lengthSqr() > 0.001) {
                float f = (float) Math.sqrt(newVelocity.x * newVelocity.x + newVelocity.z * newVelocity.z);
                // it's okay, the maths check out
                //noinspection SuspiciousNameCombination
                this.setYRot((float) (Mth.atan2(newVelocity.x, newVelocity.z) * RADIANS_TO_DEGREES));
                this.setXRot((float) (Mth.atan2(newVelocity.y, f) * RADIANS_TO_DEGREES));
            }
        });

        if (this.getDeltaMovement().lengthSqr() > 0.0001) {
            this.move(MoverType.SELF, this.getDeltaMovement());
        }

        if (this.level().isClientSide()) {
            for (int i = 0; i < 10 * this.getSpeedModifier(); i++) {
                this.spawnTrailParticle();
            }

            if (random.nextInt(20) == 0) {
                this.level().playLocalSound(this.getX(), this.getY(), this.getZ(), SoundEvents.SOUL_ESCAPE.value(), SoundSource.AMBIENT, 1.0f, 1.5f, true);
            }
        }
    }

    protected void tickTimeInSolid() {
        if (!this.getBlockStateOn().isAir()) {
            if (timeInSolid > -1) {
                timeInSolid += 1;
            }
        } else {
            timeInSolid = 0;
        }

        if (timeInSolid > 25) {
            this.discard();
        }
    }

    protected void expire() {
        this.level().broadcastEntityEvent(this, SOUL_EXPIRED_STATUS);
        this.discard();
    }

    protected void spawnTrailParticle() {
        if (this.getBlockStateOn().is(RequiemBlockTags.WANDERING_SOUL_TRAVERSABLE)) {
            this.level().addParticle(ParticleTypes.SOUL, this.getX() + random.nextGaussian() / 10, this.getY() + random.nextGaussian() / 10, this.getZ() + random.nextGaussian() / 10, random.nextGaussian() / 20, random.nextGaussian() / 20, random.nextGaussian() / 20);
        } else {
            this.level().addParticle(new WispTrailParticleEffect(1.0f, 1.0f, 1.0f, -0.1f, -0.01f, 0.0f), this.getX() + random.nextGaussian() / 15, this.getY() + random.nextGaussian() / 15, this.getZ() + random.nextGaussian() / 15, 0, 0.2d, 0);
        }
    }

    private Optional<Vec3> getTarget() {
        return this.getEntityData().get(NEXT_TARGET);
    }

    private void setTarget(Vec3 target) {
        this.getEntityData().set(NEXT_TARGET, Optional.of(target));
    }

    protected float getSpeedModifier() {
        return this.getEntityData().get(SPEED_MODIFIER);
    }

    protected void setSpeedModifier(float speedModifier) {
        this.getEntityData().set(SPEED_MODIFIER, speedModifier);
    }

    @Override
    public void handleEntityEvent(byte status) {
        switch (status) {
            case SOUL_EXPIRED_STATUS -> {
                for (int i = 0; i < 25; i++) {
                    this.level().addParticle(new WispTrailParticleEffect(1.0f, 1.0f, 1.0f, -0.1f, -0.01f, 0.0f), this.getX() + random.nextGaussian() / 15, this.getY() + random.nextGaussian() / 15, this.getZ() + random.nextGaussian() / 15, 0, 0.2d, 0);
                    this.level().addParticle(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.SOUL_SAND.defaultBlockState()), this.getX() + random.nextGaussian() / 10, this.getY() + random.nextGaussian() / 10, this.getZ() + random.nextGaussian() / 10, random.nextGaussian() / 20, random.nextGaussian() / 20, random.nextGaussian() / 20);
                }
                this.level().playLocalSound(this.getX(), this.getY(), this.getZ(), SoundEvents.SOUL_ESCAPE.value(), SoundSource.AMBIENT, 1.0f, 1.5f, true);
                this.level().playLocalSound(this.getX(), this.getY(), this.getZ(), this.getDisintegrationSound(), SoundSource.AMBIENT, 1.0f, 1.0f, true);
            }
            default -> super.handleEntityEvent(status);
        }
    }

    protected SoundEvent getDisintegrationSound() {
        return RequiemSoundEvents.ENTITY_SOUL_DISINTEGRATES;
    }

    protected void updateTarget() {
        this.targetChanges++;

        Vec3 newTarget = this.selectNextTarget();
        this.setTarget(newTarget);

        BlockPos targetPos = BlockPos.containing(newTarget);
        if (this.level().getBlockState(targetPos).isSolidRender(this.level(), targetPos) && !this.level().getBlockState(targetPos).is(RequiemBlockTags.WANDERING_SOUL_TRAVERSABLE)) {
            this.targetChangeCooldown = 0;
            return;
        }

        this.setSpeedModifier(0.1f + Math.max(0, random.nextFloat() * this.getMaxSpeedModifier() - 0.1f));
        this.targetChangeCooldown = random.nextInt() % (int) (100 / getSpeedModifier());
    }

    protected float getMaxSpeedModifier() {
        return 1;
    }

    protected Vec3 selectNextTarget() {
        return this.position().add(
            random.nextGaussian() * 10,
            random.nextGaussian() * 10,
            random.nextGaussian() * 10
        );
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        return false;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag input) {
        if (input.contains("max_age")) this.maxAge = input.getInt("max_age");
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag output) {
        output.putInt("max_age", this.maxAge);
    }
}
