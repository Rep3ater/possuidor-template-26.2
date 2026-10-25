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

import com.mojang.datafixers.util.Unit;
import ladysnake.requiem.api.v1.record.EntityPointer;
import ladysnake.requiem.api.v1.record.GlobalRecord;
import ladysnake.requiem.api.v1.record.GlobalRecordKeeper;
import ladysnake.requiem.common.RequiemRecordTypes;
import ladysnake.requiem.common.particle.WispTrailParticleEffect;
import ladysnake.requiem.common.sound.RequiemSoundEvents;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.intellij.lang.annotations.MagicConstant;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.UUID;

public class ReleasedSoulEntity extends SoulEntity {
    private static final byte TELEPORT_AWAY_STATUS = 2;
    private static final byte MERGE_WITH_BODY_STATUS = 3;

    /**The entity is not found anywhere, probably dead*/
    public static final byte BODY_MISSING = -1;
    /**The entity is loaded and close enough to the soul to fly there*/
    public static final byte BODY_FOUND = 0;
    /**The entity is in the wrong world, way too far, or unloaded with a player*/
    public static final byte BODY_ISEKAI = 1;

    public static final EntityDataAccessor<Byte> BODY_STATUS = SynchedEntityData.defineId(ReleasedSoulEntity.class, EntityDataSerializers.BYTE);
    private @Nullable UUID ownerRecord;

    public ReleasedSoulEntity(EntityType<? extends ReleasedSoulEntity> type, Level world) {
        this(type, world, null);
    }

    public ReleasedSoulEntity(EntityType<? extends ReleasedSoulEntity> type, Level world, @Nullable UUID ownerRecord) {
        super(type, world);
        this.ownerRecord = ownerRecord;
        this.noPhysics = true;
        this.targetChangeCooldown = 40;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(BODY_STATUS, BODY_FOUND);
    }

    @Override
    public void tick() {
        if (!this.level().isClientSide()) {
            this.setBodyStatus(
                this.getOwnerRef()
                    .map(ptr -> ptr.resolve(((ServerLevel) this.level()).getServer())
                        .filter(e -> e.level() == this.level())
                        .filter(e -> e.distanceTo(this) < 100)
                        .isPresent() ? BODY_FOUND : BODY_ISEKAI)
                    .orElse(BODY_MISSING)
            );

            if (this.getBodyStatus() != BODY_FOUND) {
                this.setMaxAge(60);
            } else {
                this.setMaxAge(-1);

                if (this.isCollidingWithOwner()) {
                    this.releaseSoul(MERGE_WITH_BODY_STATUS);
                }
            }
        }
        super.tick();
    }

    private void releaseSoul(
        @MagicConstant(intValues = {SOUL_EXPIRED_STATUS, TELEPORT_AWAY_STATUS, MERGE_WITH_BODY_STATUS}) byte status
    ) {
        // Will be retrieved later (see FilledSoulVesselItem#registerCallbacks)
        this.getOwnerRecord().ifPresent(data -> data.put(RequiemRecordTypes.RELEASED_SOUL, Unit.INSTANCE));
        this.level().broadcastEntityEvent(this, status);
        this.discard();
    }

    @Override
    protected void tickTimeInSolid() {
        // NO-OP
    }

    @Override
    protected void expire() {
        if (this.getBodyStatus() == BODY_ISEKAI) {
            this.releaseSoul(TELEPORT_AWAY_STATUS);
        } else {
            // RIP
            this.getOwnerRecord().ifPresent(GlobalRecord::invalidate);
            super.expire();
        }
    }

    private void setBodyStatus(byte value) {
        this.getEntityData().set(BODY_STATUS, value);
    }

    private byte getBodyStatus() {
        return this.getEntityData().get(BODY_STATUS);
    }

    private boolean isCollidingWithOwner() {
        return this.getOwnerRef()
            .flatMap(ptr -> ptr.resolve(((ServerLevel) this.level()).getServer()))
            .filter(e -> e.getBoundingBox().intersects(this.getBoundingBox()))
            .isPresent();
    }

    private Optional<EntityPointer> getOwnerRef() {
        return this.getOwnerRecord().flatMap(record -> record.get(RequiemRecordTypes.ENTITY_REF));
    }

    private Optional<GlobalRecord> getOwnerRecord() {
        return Optional.ofNullable(ownerRecord)
            .flatMap(GlobalRecordKeeper.get(this.level())::getRecord);
    }

    @Override
    protected void spawnTrailParticle() {
        switch (this.getBodyStatus()) {
            case BODY_ISEKAI -> this.level().addParticle(ParticleTypes.PORTAL, this.getRandomX(0.5D), this.getY(0.5D) - 0.25D, this.getRandomZ(0.5D), (this.random.nextDouble() - 0.5D) * 2.0D, -this.random.nextDouble(), (this.random.nextDouble() - 0.5D) * 2.0D);
            case BODY_MISSING -> this.level().addParticle(ParticleTypes.SMOKE, this.getX() + random.nextDouble() / 5.0D, this.getY() + random.nextDouble() / 3, this.getZ() + random.nextDouble() / 5.0D, 0.0D, 0.05D, 0.0D);
            default -> super.spawnTrailParticle();
        }
    }

    @Override
    protected float getMaxSpeedModifier() {
        final float maxSpeed404 = 0.4f;
        if (this.getBodyStatus() != BODY_FOUND) return maxSpeed404;
        return super.getMaxSpeedModifier();
    }

    @Override
    protected Vec3 selectNextTarget() {
        return switch (this.getBodyStatus()) {
            case BODY_FOUND -> this.getOwnerRef().map(EntityPointer::pos).map(this::selectPosTowards).orElseThrow();
            case BODY_ISEKAI -> this.position().add(0, this.random.nextGaussian() * 0.1, 0);
            default -> this.position().add(
                random.nextGaussian(),
                random.nextGaussian(),
                random.nextGaussian()
            );
        };
    }

    @Override
    public void handleEntityEvent(byte status) {
        switch (status) {
            case TELEPORT_AWAY_STATUS -> {
                this.playSound(RequiemSoundEvents.ENTITY_SOUL_TELEPORT, 1, 1);
                for(double angle = 0.0D; angle < Math.PI * 2; angle += Math.PI / 20.0) {
                    this.level().addParticle(ParticleTypes.PORTAL, this.getX() + Math.cos(angle) * 5.0D, this.getY() - 0.4D, this.getZ() + Math.sin(angle) * 5.0D, Math.cos(angle) * -5.0D, 0.0D, Math.sin(angle) * -5.0D);
                    this.level().addParticle(ParticleTypes.PORTAL, this.getX() + Math.cos(angle) * 5.0D, this.getY() - 0.4D, this.getZ() + Math.sin(angle) * 5.0D, Math.cos(angle) * -7.0D, 0.0D, Math.sin(angle) * -7.0D);
                }
            }
            case MERGE_WITH_BODY_STATUS -> {
                this.playSound(SoundEvents.AMETHYST_BLOCK_CHIME, 1, 1);
                for (int i = 0; i < 25; i++) {
                    this.level().addParticle(new WispTrailParticleEffect(1.0f, 1.0f, 1.0f, -0.1f, -0.01f, 0.0f), this.getX() + random.nextGaussian() / 15, this.getY() + random.nextGaussian() / 15, this.getZ() + random.nextGaussian() / 15, 0, 0.2d, 0);
                }
            }
            default -> super.handleEntityEvent(status);
        }
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        this.ownerRecord = input.read("owner_record", UUIDUtil.CODEC).orElse(null);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.storeNullable("owner_record", UUIDUtil.CODEC, this.ownerRecord);
    }

    protected Vec3 selectPosTowards(Vec3 targetPos) {
        float convergenceFactor = (float) this.targetChanges;
        double distanceToTarget = this.position().distanceTo(targetPos);
        final double maxStepDistance = 15.0;
        double scuffedDistanceToTarget = Math.min(maxStepDistance, distanceToTarget);
        Vec3 towardsTarget = targetPos.subtract(this.position()).normalize().scale(scuffedDistanceToTarget);
        assert convergenceFactor > 0;
        double fuzzyFactor = (scuffedDistanceToTarget * 1.5) / convergenceFactor;
        Vec3 fuzzyTarget = new Vec3(
            towardsTarget.x * Math.abs(1 + random.nextGaussian() * fuzzyFactor),
            towardsTarget.y * Math.abs(1 + random.nextGaussian() * fuzzyFactor),
            towardsTarget.z * Math.abs(1 + random.nextGaussian() * fuzzyFactor)
        );
        return this.position().add(fuzzyTarget);
    }
}
