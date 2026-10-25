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

import ladysnake.requiem.common.block.obelisk.InertRunestoneBlock;
import ladysnake.requiem.common.particle.WispTrailParticleEffect;
import ladysnake.requiem.common.sound.RequiemSoundEvents;
import ladysnake.requiem.core.movement.PlayerMovementAlterer;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public class ObeliskSoulEntity extends SoulEntity {
    public static final EntityDataAccessor<Boolean> PHASING = SynchedEntityData.defineId(ObeliskSoulEntity.class, EntityDataSerializers.BOOLEAN);

    private @Nullable BlockPos targetPos;
    private int ticksAgainstWall = 0;

    public ObeliskSoulEntity(EntityType<? extends ObeliskSoulEntity> type, Level world) {
        this(type, world, null);
    }

    public ObeliskSoulEntity(EntityType<? extends ObeliskSoulEntity> type, Level world, @Nullable BlockPos targetPos) {
        super(type, world);
        this.targetPos = targetPos;
        this.targetChangeCooldown = 20;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(PHASING, false);
    }

    public boolean isPhasing() {
        return this.getEntityData().get(PHASING);
    }

    public void setPhasing(boolean phasing) {
        this.getEntityData().set(PHASING, phasing);
    }

    @Override
    public void tick() {
        this.noPhysics = this.isPhasing() || this.tickCount >= this.maxAge;

        if (!this.level().isClientSide()) {
            if (this.targetPos != null && this.isTargetStale(targetPos)) {
                this.setMaxAge(60);
                this.targetPos = null;
            }
            if (isActivatedRunestone(this.getBlockStateOn())) {
                this.level().broadcastEntityEvent(this, SOUL_EXPIRED_STATUS);
                this.discard();
            }
        }
        super.tick();
        if (!this.level().isClientSide()) {
            if (this.xOld == this.getX() && this.yOld == this.getY() && this.zOld == this.getZ()) {
                this.ticksAgainstWall++;
                if (this.ticksAgainstWall > PlayerMovementAlterer.TICKS_BEFORE_PHASING) {
                    this.setPhasing(true);
                }
            } else if (this.level().noCollision(this)) {
                this.setPhasing(false);
                this.ticksAgainstWall = 0;
            }
        }
    }

    @Override
    protected void tickTimeInSolid() {
        if (this.targetPos == null) {
            super.tickTimeInSolid();
        }
    }

    @Override
    protected void spawnTrailParticle() {
        float redEvolution = -0.05f * (1f - this.getConversionProgress());
        float greenEvolution = -0.06f * this.getConversionProgress();
        float blueEvolution = 0.0f;
        this.level().addParticle(new WispTrailParticleEffect(1.0f, 1.0f, 1.0f, redEvolution, greenEvolution, blueEvolution), this.getX() + random.nextGaussian() / 15, this.getY() + random.nextGaussian() / 15, this.getZ() + random.nextGaussian() / 15, 0, 0.2d, 0);
    }

    public float getConversionProgress() {
        float conversionTime = 40f;
        return Math.min(1f, this.tickCount / conversionTime);
    }

    @Override
    protected SoundEvent getDisintegrationSound() {
        return RequiemSoundEvents.ENTITY_OBELISK_SOUL_DISINTEGRATES;
    }

    @Override
    protected float getSpeedModifier() {
        return (this.isPhasing() ? 0.4f : 1) * super.getSpeedModifier();
    }

    @Override
    protected float getMaxSpeedModifier() {
        return 0.6f;
    }

    @Override
    protected Vec3 selectNextTarget() {
        if (this.targetPos != null) {
            return this.selectPosAround(Vec3.atCenterOf(targetPos));
        } else {
            return super.selectNextTarget();
        }
    }

    @Override
    protected void expire() {
        // obelisk souls head straight to the core instead of disappearing instantly
        if (this.targetPos == null) {
            super.expire();
        }
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        this.targetPos = input.read("target_pos", BlockPos.CODEC).orElse(null);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.storeNullable("target_pos", BlockPos.CODEC, this.targetPos);
    }

    private boolean isTargetStale(BlockPos targetPos) {
        if (this.level().hasChunkAt(targetPos)) {
            return !isActivatedRunestone(this.level().getBlockState(targetPos));
        }
        return true;
    }

    private static boolean isActivatedRunestone(BlockState blockState) {
        return blockState.getBlock() instanceof InertRunestoneBlock && blockState.getValue(InertRunestoneBlock.ACTIVATED);
    }

    protected Vec3 selectPosAround(Vec3 targetPos) {
        if (this.tickCount >= this.maxAge) return targetPos;

        float convergenceFactor = Math.max(1, Mth.sqrt(this.targetChanges / 3f));
        float wanderingRange = 15f;
        double fuzzyFactor = wanderingRange / convergenceFactor;
        return targetPos.add(
            random.nextGaussian() * fuzzyFactor,
            random.nextGaussian() * fuzzyFactor,
            random.nextGaussian() * fuzzyFactor
        );
    }
}
