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
package ladysnake.requiem.common.possession.jump;

import com.google.common.base.Preconditions;
import dev.onyxstudios.cca.api.v3.component.TransientComponent;
import ladysnake.requiem.api.v1.entity.ExternalJumpingMount;
import ladysnake.requiem.api.v1.possession.Possessable;
import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/**
 * A fake JumpingMount that allows players possessing a mob to long jump by themselves
 */
public class DummyJumpingMount implements ExternalJumpingMount, TransientComponent {
    protected final LivingEntity mob;
    private final SoundEvent stepSound;
    private float jumpStrength;
    private boolean inAir;
    private final double baseJumpStrength;

    public DummyJumpingMount(LivingEntity mob, double baseJumpStrength, SoundEvent stepSound) {
        this.mob = mob;
        this.baseJumpStrength = baseJumpStrength;
        this.stepSound = stepSound;
    }

    @Override
    public void setJumpStrength(int strength) {
        if (strength < 0) {
            strength = 0;
        } else {
            Player possessor = ((Possessable) this.mob).getPossessor();
            if (possessor != null) {
                possessor.setJumping(true);
            }
        }

        if (strength >= 90) {
            this.jumpStrength = 1.0F;
        } else {
            this.jumpStrength = 0.4F + 0.4F * (float) strength / 90.0F;
        }
    }

    @Override
    public boolean canJump(Player playerEntity) {
        return true;
    }

    @Override
    public boolean canJump() {
        return true;
    }

    @Override
    public void onPlayerJump(int strength) {
        setJumpStrength(strength);
    }

    @Override
    public void handleStartJump(int height) {
        startJumping(height);
    }

    @Override
    public void handleStopJump() {
        stopJumping();
    }

    @Override
    public void startJumping(int height) {
        this.mob.setPose(Pose.LONG_JUMPING);
    }

    @Override
    public void stopJumping() {
        this.mob.level().playSound(null, this.mob.getX(), this.mob.getY(), this.mob.getZ(), this.stepSound, SoundSource.NEUTRAL, 2.0F, 1.0F);
        this.mob.setPose(Pose.STANDING);
    }

    private static double getBlockJumpFactor(LivingEntity entity) {
        float jumpFactorHere = entity.level().getBlockState(entity.blockPosition()).getBlock().getJumpFactor();
        float jumpFactorBelow = entity.level().getBlockState(entity.getBlockPosBelowThatAffectsMyMovement()).getBlock().getJumpFactor();
        return jumpFactorHere == 1.0f ? jumpFactorBelow : jumpFactorHere;
    }

    @Override
    public void attemptJump() {
        Player possessor = ((Possessable) this.mob).getPossessor();

        if (possessor != null && possessor.onGround()) {
            if (!this.inAir && this.jumpStrength > 0.0F) {
                double naturalStrength = getBaseJumpingStrength();
                double baseJumpVelocity = naturalStrength * this.jumpStrength * getBlockJumpFactor(this.mob);
                double jumpVelocity = baseJumpVelocity + this.mob.getJumpBoostPower();
                Vec3 baseVelocity = possessor.getDeltaMovement();
                possessor.setDeltaMovement(baseVelocity.x, jumpVelocity, baseVelocity.z);
                this.inAir = true;
                if (possessor.zza > 0.0F) {
                    float vx = Mth.sin(possessor.getYRot() * (float) (Math.PI / 180.0));
                    float vz = Mth.cos(possessor.getYRot() * (float) (Math.PI / 180.0));
                    possessor.setDeltaMovement(possessor.getDeltaMovement().add(-0.4F * vx * this.jumpStrength, 0.0, 0.4F * vz * this.jumpStrength));
                }

                this.beginClientJump(possessor);
            } else if (this.inAir) {
                this.finishClientJump(possessor);
            }
        }
    }

    protected double getBaseJumpingStrength() {
        return baseJumpStrength;
    }

    protected void beginClientJump(Player possessor) {
        this.mob.setPose(Pose.LONG_JUMPING);
        this.jumpStrength = 0.0F;
    }

    protected void finishClientJump(Player possessor) {
        Preconditions.checkState(this.mob.level().isClientSide(), "endJump should only be called clientside");

        this.jumpStrength = 0.0F;
        this.inAir = false;

        if (Minecraft.getInstance().getConnection() != null) {
            Minecraft.getInstance().getConnection().send(new ServerboundPlayerCommandPacket(this.mob, ServerboundPlayerCommandPacket.Action.STOP_RIDING_JUMP));
        }
    }
}
