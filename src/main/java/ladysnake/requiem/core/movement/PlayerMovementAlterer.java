/*
 * Requiem
 * Copyright (C) 2017-2023 Ladysnake
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU License
 * along with this program. If not, see <https://www.gnu.org/licenses>.
 *
 * Linking this mod statically or dynamically with other
 * modules is making a combined work based on this mod.
 * Thus, the terms and conditions of the GNU General Public License cover the whole combination.
 *
 * In addition, as a special exception, the copyright holders of
 * this mod give you permission to combine this mod
 * with free software programs or libraries that are released under the GNU LGPL
 * and with code included in the standard release of Minecraft under All Rights Reserved (or
 * modified versions of such code, with unchanged license).
 * You may copy and distribute such a system following the terms of the GNU GPL for this mod
 * and the licenses of the other code concerned.
 *
 * Note that people who make modified versions of this mod are not obligated to grant
 * this special exception for their modified versions; it is their choice whether to do so.
 * The GNU General Public License gives permission to release a modified version without this exception;
 * this exception also makes it possible to release a modified version which carries forward this exception.
 */
package ladysnake.requiem.core.movement;

import com.demonwav.mcdev.annotations.CheckEnv;
import com.demonwav.mcdev.annotations.Env;
import io.github.ladysnake.pal.AbilitySource;
import io.github.ladysnake.pal.Pal;
import io.github.ladysnake.pal.VanillaAbilities;
import ladysnake.requiem.api.v1.entity.MovementAlterer;
import ladysnake.requiem.api.v1.entity.MovementConfig;
import ladysnake.requiem.api.v1.entity.MovementRegistry;
import ladysnake.requiem.api.v1.entity.movement.SwimMode;
import ladysnake.requiem.api.v1.entity.movement.WalkMode;
import ladysnake.requiem.api.v1.possession.PossessionComponent;
import ladysnake.requiem.api.v1.remnant.RemnantComponent;
import ladysnake.requiem.core.RequiemCore;
import ladysnake.requiem.core.RequiemCoreNetworking;
import ladysnake.requiem.core.mixin.access.EntityAccessor;
import ladysnake.requiem.core.tag.RequiemCoreTags;
import net.fabricmc.fabric.api.util.TriState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.AbstractFish;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Ghast;
import net.minecraft.world.entity.monster.Phantom;
import net.minecraft.world.entity.animal.WaterAnimal;
import net.minecraft.world.entity.monster.Slime;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import java.util.UUID;
import java.util.function.ToDoubleFunction;

import static ladysnake.requiem.api.v1.entity.MovementConfig.MovementMode.*;

public abstract class PlayerMovementAlterer implements MovementAlterer {
    public static final AbilitySource MOVEMENT_ALTERER_ABILITIES = Pal.getAbilitySource(RequiemCore.id("movement_alterer"), AbilitySource.FREE);
    public static final byte DEFAULT_SYNC = 0;
    public static final byte SYNC_NO_CLIP = 1;
    public static final byte SYNC_PHASING_PARTICLES = 2;
    public static final ResourceLocation SPEED_MODIFIER_ID = RequiemCore.id("altered_movement_speed");
    public static final ResourceLocation WATER_SPEED_MODIFIER_ID = RequiemCore.id("water_speed_modifier");
    public static final UUID SPEED_MODIFIER_UUID = UUID.fromString("3708adba-b37f-413f-8b66-62e05330c7da");
    public static final UUID WATER_SPEED_MODIFIER_UUID = UUID.fromString("0e602dd1-2672-4179-852e-2a26d1579df4");
    public static final int TICKS_BEFORE_PHASING = 30;

    @Nullable
    private MovementConfig config;
    protected final Player player;
    private Vec3 lastVelocity = Vec3.ZERO;
    private int ticksAgainstWall = 0;
    private boolean noClipping = false;
    private boolean hasEnteredWall = false;
    private int noClipTicks = 0;

    private boolean underwaterJumpAscending;
    private double underwaterJumpStartY;

    public PlayerMovementAlterer(Player player) {
        this.player = player;
    }

    @Override
    public void setConfig(@CheckForNull MovementConfig config) {
        this.config = config;
        this.applyConfig();
    }

    @Override
    public void applyConfig() {
        if (!this.player.level().isClientSide()) {
            if (getActualFlightMode(config, getCurrentBody(player)) == DISABLED) {
                Pal.revokeAbility(player, VanillaAbilities.ALLOW_FLYING, MOVEMENT_ALTERER_ABILITIES);
            } else {
                Pal.grantAbility(player, VanillaAbilities.ALLOW_FLYING, MOVEMENT_ALTERER_ABILITIES);
            }
            this.hugWall(false);
            this.underwaterJumpAscending = false;
            this.updateSpeedModifier(getCurrentBody(this.player), SPEED_MODIFIER_ID, MovementConfig::getLandedSpeedModifier, true);
        }
    }

    private void updateSpeedModifier(LivingEntity currentBody, ResourceLocation speedModifierId, ToDoubleFunction<MovementConfig> property, boolean shouldApplyModifier) {
        AttributeInstance speedAttribute = currentBody.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speedAttribute != null) {
            speedAttribute.removeModifier(speedModifierId);
            if (shouldApplyModifier && config != null && property.applyAsDouble(config) != 1.0) {
                speedAttribute.addTransientModifier(new AttributeModifier(speedModifierId, property.applyAsDouble(config), AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
            }
        }
    }

    @CheckEnv(Env.CLIENT)
    @Override
    public void alterControls() {
        if (this.config != null
            && getActualSwimMode(this.config, getCurrentBody(this.player)) == SwimMode.FLOATING
            && isInFluid(this.player)
            && this.player.getRandom().nextFloat() < 0.8F
        ) {
            this.player.setJumping(true);
        }
    }

    // Based on SwimGoal#canStart
    private static boolean isInFluid(LivingEntity entity) {
        return entity.isInWater() && entity.getFluidHeight(FluidTags.WATER) > entity.getFluidJumpThreshold() || entity.isInLava();
    }

    @Override
    public float getSwimmingAcceleration(float baseAcceleration) {
        if (this.config != null && getActualSwimMode(this.config, getCurrentBody(player)) == SwimMode.FORCED) {
            return 0.96F;
        }
        return baseAcceleration;
    }

    @Override
    public double getSwimmingUpwardsVelocity(double baseUpwardsVelocity) {
        if (this.config != null && getActualSwimMode(this.config, getCurrentBody(player)) == SwimMode.SINKING) {
            double y = this.player.getY();
            if (this.player.onGround()) {    // starting the jump
                this.underwaterJumpStartY = y;
                this.underwaterJumpAscending = true;
            } else if (this.underwaterJumpAscending && y > underwaterJumpStartY + (this.player.isInWater() ? 0.8 : 1.0)) {    // reaching peak
                this.underwaterJumpAscending = false;
            } else if (!this.underwaterJumpAscending) { // sinking again
                return 0;
            }
        }
        return baseUpwardsVelocity;
    }

    @Override
    public boolean canClimbWalls() {
        return this.config != null && this.config.canClimbWalls();
    }

    @Override
    public void clientTick() {
        LocalPlayer mainPlayer = Minecraft.getInstance().player;
        if (this.config == null) {
            LivingEntity body = getCurrentBody(player);
            if (body != player && body != null) {
                this.config = MovementRegistry.get(player.level()).getEntityMovementConfig(body.getType());
            }
        }
        if (this.config != null && this.player == mainPlayer && mainPlayer != null) {
            if (this.config.canPhaseThroughWalls()) {
                if (!this.noClipping && !this.player.noPhysics) {
                    Vec3 movement = getIntendedMovement(this.player);
                    Vec3 adjusted = ((EntityAccessor) this.player).requiem$invokeCollide(movement);
                    // 10.0 is an arbitrary constant that corresponds to mostly blocked movement
                    if (movement.length() / adjusted.length() > 10.0 && this.player.getLookAngle().dot(movement.normalize()) > 0.5) {
                        this.ticksAgainstWall++;
                        RequiemCoreNetworking.sendHugWallMessage(true);
                    } else if (this.ticksAgainstWall > 0) {
                        this.ticksAgainstWall = 0;
                        RequiemCoreNetworking.sendHugWallMessage(false);
                    }
                } else if (this.noClipping) {
                    this.noClipTicks++;
                    if (this.player.getRandom().nextFloat() > 0.6f) {
                        this.playPhaseEffects();
                    }

                    // Smooth ghost glide forward through the wall
                    if (mainPlayer.input.keyPresses.forward()) {
                        Vec3 look = this.player.getLookAngle();
                        this.player.setDeltaMovement(look.x * 0.18, this.player.getDeltaMovement().y * 0.5, look.z * 0.18);
                    }

                    // Check if player is intersecting blocks
                    this.noClipping = false;
                    boolean inBlocks = !this.player.level().noCollision(this.player);
                    this.noClipping = true;

                    if (inBlocks) {
                        this.hasEnteredWall = true;
                    } else if (this.hasEnteredWall) {
                        // Successfully emerged on the other side of the wall
                        this.hasEnteredWall = false;
                        this.noClipTicks = 0;
                        RequiemCoreNetworking.sendHugWallMessage(false);
                    } else if (this.noClipTicks > 100) {
                        // Safety timeout
                        this.hasEnteredWall = false;
                        this.noClipTicks = 0;
                        RequiemCoreNetworking.sendHugWallMessage(false);
                    }
                }
            }
            if (getActualWalkMode(this.config, getCurrentBody(player)) == WalkMode.JUMPY && this.player.onGround() && !getIntendedMovement(player).equals(Vec3.ZERO)) {
                this.player.jumpFromGround();
            }
            if (this.underwaterJumpAscending && !mainPlayer.input.keyPresses.jump()) {
                this.underwaterJumpAscending = false;
            }
        }
        this.tick();
    }

    @NotNull
    protected static Vec3 getIntendedMovement(Player player) {
        if (player instanceof LocalPlayer localPlayer) {
            float verticalMovement = (localPlayer.input.keyPresses.jump() ? 1 : 0) - (localPlayer.input.keyPresses.shift() ? 1 : 0);
            return EntityAccessor.requiem$invokeGetInputVector(new Vec3(player.xxa, verticalMovement, player.zza), 1, player.getYRot());
        } else {
            return Vec3.ZERO;
        }
    }

    @Override
    public void serverTick() {
        this.updateSpeedModifier(
            getCurrentBody(this.player),
            WATER_SPEED_MODIFIER_ID,
            MovementConfig::getWaterSpeedModifier,
            player.isInWater()
        );
        this.tick();
    }

    @Override
    public void tick() {
        if (this.config == null) {
            return;
        }
        LivingEntity body = getCurrentBody(player);
        updateSwimming();

        if (this.noClipping) {
            this.player.noPhysics = true;
            this.player.getAbilities().flying = true;
        } else if (!this.player.isSpectator()) {
            this.player.noPhysics = false;
        }
        if (getActualFlightMode(config, body) == FORCED) {
            this.player.getAbilities().flying = true;
        }
        if (this.player.onGround() && shouldActuallyFlopOnLand(config, body) && this.player.level().getFluidState(this.player.blockPosition()).isEmpty()) {
            this.player.jumpFromGround();
        }
        Vec3 velocity = this.player.getDeltaMovement();
        velocity = applyGravity(velocity, this.config.getAddedGravity());
        velocity = applyFallSpeedModifier(velocity, this.config.getFallSpeedModifier());
        velocity = applyInertia(velocity, this.config.getInertia());
        this.player.setDeltaMovement(velocity);
        this.lastVelocity = velocity;
    }

    private boolean shouldActuallyFlopOnLand(MovementConfig config, LivingEntity body) {
        TriState value = config.shouldFlopOnLand();
        if (value != TriState.DEFAULT) {
            return value.get();
        }
        return body instanceof AbstractFish;
    }

    @Override
    public void updateSwimming() {
        if (config != null) {
            SwimMode swimMode = getActualSwimMode(config, getCurrentBody(player));
            if (swimMode.sprintSwims() != TriState.DEFAULT) {
                player.setSwimming(swimMode.sprintSwims().get());
            }
        }
    }

    @Override
    public boolean disablesSwimming() {
        return config != null && !getActualSwimMode(config, getCurrentBody(player)).sprintSwims().orElse(true);
    }

    @Override
    public void hugWall(boolean hugging) {
        if (this.config != null && this.config.canPhaseThroughWalls() && hugging) {
            this.ticksAgainstWall++;

            if (this.ticksAgainstWall > TICKS_BEFORE_PHASING && !this.noClipping) {
                this.noClipping = true;
                this.ticksAgainstWall = 0;
                this.hasEnteredWall = false;
                this.noClipTicks = 0;
                KEY.sync(this.player, (buf, player) -> writeToPacket(buf, SYNC_NO_CLIP), player -> shouldSyncWith(player, SYNC_NO_CLIP));
            } else if (this.ticksAgainstWall % 10 == 0) {
                KEY.sync(this.player, (buf, player) -> writeToPacket(buf, SYNC_PHASING_PARTICLES), player -> shouldSyncWith(player, SYNC_PHASING_PARTICLES));
            }
        } else {
            this.ticksAgainstWall = 0;
            this.noClipping = false;
            this.hasEnteredWall = false;
            this.noClipTicks = 0;
            KEY.sync(this.player, (buf, player) -> writeToPacket(buf, SYNC_NO_CLIP), player -> shouldSyncWith(player, SYNC_NO_CLIP));
        }
    }

    @Override
    public boolean isNoClipping() {
        return this.noClipping;
    }

    @Override
    public boolean shouldSyncWith(ServerPlayer player) {
        return this.shouldSyncWith(player, 0);
    }

    private boolean shouldSyncWith(ServerPlayer player, int syncOp) {
        return (
            syncOp == SYNC_NO_CLIP && player == this.player
        ) || (
            syncOp == SYNC_PHASING_PARTICLES
                && RemnantComponent.get(player).getRemnantType().isDemon()
                && player.distanceToSqr(this.player) < 16 * 16
        );
    }

    @Override
    public void writeSyncPacket(RegistryFriendlyByteBuf buf, ServerPlayer recipient) {
        this.writeToPacket(buf, DEFAULT_SYNC);
    }

    private void writeToPacket(RegistryFriendlyByteBuf buf, int syncOp) {
        buf.writeByte(syncOp);
        if (syncOp == SYNC_NO_CLIP) {
            buf.writeBoolean(this.noClipping);
        }
    }

    @Override
    public void applySyncPacket(RegistryFriendlyByteBuf buf) {
        byte syncOp = buf.readByte();
        if (syncOp == SYNC_NO_CLIP) {
            this.noClipping = buf.readBoolean();
            this.ticksAgainstWall = 0;
            this.hasEnteredWall = false;
            this.noClipTicks = 0;
        } else if (syncOp == SYNC_PHASING_PARTICLES) {
            this.playPhaseEffects();
        }
    }

    @CheckEnv(Env.CLIENT)
    protected abstract void playPhaseEffects();

    private Vec3 applyGravity(Vec3 velocity, float addedGravity) {
        return velocity.subtract(0, addedGravity, 0);
    }

    private Vec3 applyFallSpeedModifier(Vec3 velocity, float fallSpeedModifier) {
        LivingEntity body = getCurrentBody(player);
        if (body != null) {
            boolean slowFalling = body.getType() == net.minecraft.world.entity.EntityType.CHICKEN
                || body.getType().builtInRegistryHolder().is(net.minecraft.tags.EntityTypeTags.FALL_DAMAGE_IMMUNE)
                || body.isNoGravity()
                || (body.getAttributes().hasAttribute(net.minecraft.world.entity.ai.attributes.Attributes.FALL_DAMAGE_MULTIPLIER) && body.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.FALL_DAMAGE_MULTIPLIER) <= 0.0);
            if (slowFalling && fallSpeedModifier == 1.0f) {
                fallSpeedModifier = 0.6f;
            }
        }
        if (!this.player.onGround() && velocity.y < 0) {
            velocity = velocity.multiply(1.0, fallSpeedModifier, 1.0);
            if (fallSpeedModifier < 0.9f) {
                this.player.resetFallDistance();
            }
        }
        return velocity;
    }

    private static LivingEntity getCurrentBody(Player player) {
        LivingEntity possessed = PossessionComponent.get(player).getHost();
        return possessed == null ? player : possessed;
    }

    @SuppressWarnings("deprecation")    // backwards compatibility
    private static boolean shouldActuallySinkInWater(MovementConfig config, Entity entity) {
        if (config.shouldSinkInWater() == TriState.DEFAULT) {
            EntityType<?> type = entity.getType();
            return type.builtInRegistryHolder().is(RequiemCoreTags.Entity.GOLEMS) || (entity instanceof LivingEntity living && living.getType().builtInRegistryHolder().is(EntityTypeTags.UNDEAD));
        }
        return config.shouldSinkInWater().get();
    }

    private static WalkMode getActualWalkMode(MovementConfig config, LivingEntity body) {
        WalkMode walkMode = config.getWalkMode();
        if (walkMode == WalkMode.UNSPECIFIED) {
            return body instanceof Slime ? WalkMode.JUMPY : WalkMode.NORMAL;
        }
        return walkMode;
    }

    private static SwimMode getActualSwimMode(MovementConfig config, Entity entity) {
        if (config.getSwimMode() == SwimMode.UNSPECIFIED) {
            if (shouldActuallySinkInWater(config, entity)) {
                return SwimMode.SINKING;
            } else if (entity instanceof WaterAnimal) {
                return SwimMode.FORCED;
            } else if (entity instanceof Slime) {
                return SwimMode.FLOATING;
            }
            return SwimMode.DISABLED;
        }
        return config.getSwimMode();
    }

    private static MovementConfig.MovementMode getActualFlightMode(@Nullable MovementConfig config, Entity entity) {
        if (config == null) {
            return DISABLED;
        }
        if (config.getFlightMode() == UNSPECIFIED) {
            return isFlyer(entity) ? FORCED : DISABLED;
        }
        return config.getFlightMode();
    }

    private static boolean isFlyer(Entity entity) {
        return entity instanceof Phantom
            || entity instanceof Ghast
            || (entity instanceof Mob mob && mob.getMoveControl() instanceof FlyingMoveControl);
    }

    private Vec3 applyInertia(Vec3 velocity, float inertia) {
        // velocity = velocity * (1 - inertia) + lastVelocity * inertia
        return velocity.scale(1 - inertia).add(this.lastVelocity.scale(inertia));
    }

    @Override
    public void readFromNbt(CompoundTag tag) {
        // NO-OP
    }

    @Override
    public void writeToNbt(CompoundTag tag) {
        // NO-OP
    }
}
